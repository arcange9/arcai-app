package com.example.service

import android.content.Context
import android.util.Base64
import com.example.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

/** Real image generation for providers with a supported image endpoint. */
class ImageGenerationService(private val context: Context) {
    private val client = OkHttpClient.Builder().build()

    suspend fun generate(
        provider: AiProvider,
        apiKey: String,
        modelId: String,
        prompt: String,
        size: String = "1024x1024"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("An API key is required."))
        if (provider != AiProvider.OPENAI) {
            return@withContext Result.failure(UnsupportedOperationException("${provider.displayName} image generation is not implemented yet."))
        }

        val body = JSONObject().apply {
            put("model", modelId.ifBlank { "gpt-image-2" })
            put("prompt", prompt)
            put("size", size)
            put("quality", "auto")
            put("output_format", "png")
        }.toString()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/images/generations")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("Image API ${response.code}: ${parseError(responseBody)}"))
            }

            val data = JSONObject(responseBody).optJSONArray("data")
                ?: return@withContext Result.failure(IllegalStateException("Image API returned no image data."))
            val item = data.optJSONObject(0)
                ?: return@withContext Result.failure(IllegalStateException("Image API returned an empty result."))

            val base64 = item.optString("b64_json")
            if (base64.isBlank()) {
                val url = item.optString("url")
                if (url.isNotBlank()) return@withContext Result.success(url)
                return@withContext Result.failure(IllegalStateException("Image API returned neither base64 data nor a URL."))
            }

            val imageDir = File(context.filesDir, "arcai/images").apply { mkdirs() }
            val imageFile = File(imageDir, "arcai_${System.currentTimeMillis()}.png")
            imageFile.writeBytes(Base64.decode(base64, Base64.DEFAULT))
            Result.success(imageFile.absolutePath)
        }
    }

    private fun parseError(body: String): String = runCatching {
        JSONObject(body).optJSONObject("error")?.optString("message")
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: body.take(300)
}
