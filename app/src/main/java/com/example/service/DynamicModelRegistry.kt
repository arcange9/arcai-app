package com.example.service

import com.example.model.AiProvider
import com.example.model.VerificationAuthType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RemoteModel(
    val id: String,
    val name: String,
    val contextLength: Long? = null,
    val inputPricePerMillion: Double? = null,
    val outputPricePerMillion: Double? = null
)

/** Runtime model discovery so the app does not depend only on an outdated static catalog. */
class DynamicModelRegistry {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(provider: AiProvider, apiKey: String): Result<List<RemoteModel>> = withContext(Dispatchers.IO) {
        if (provider.isLocal) return@withContext Result.success(emptyList())

        val builder = Request.Builder()
            .url(provider.verifyEndpoint)
            .header("Accept", "application/json")

        when (provider.authType) {
            VerificationAuthType.GEMINI_QUERY_PARAM -> builder.url("${provider.verifyEndpoint}?key=$apiKey")
            VerificationAuthType.X_API_KEY_ANTHROPIC -> builder.header("x-api-key", apiKey)
            VerificationAuthType.TOKEN_HEADER -> builder.header("Authorization", "Token $apiKey")
            else -> builder.header("Authorization", "Bearer $apiKey")
        }

        runCatching {
            client.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("HTTP ${response.code}: ${body.take(300)}")
                parseModels(body)
            }
        }
    }

    private fun parseModels(body: String): List<RemoteModel> {
        val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
        return buildList {
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optString("id").trim()
                if (id.isBlank()) continue
                val pricing = item.optJSONObject("pricing")
                add(
                    RemoteModel(
                        id = id,
                        name = item.optString("name", id),
                        contextLength = item.optLong("context_length", 0L).takeIf { it > 0 },
                        inputPricePerMillion = pricing?.optString("prompt")?.toDoubleOrNull()?.times(1_000_000),
                        outputPricePerMillion = pricing?.optString("completion")?.toDoubleOrNull()?.times(1_000_000)
                    )
                )
            }
        }
    }
}
