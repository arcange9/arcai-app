package com.example.service

import com.example.data.entity.MessageEntity
import com.example.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/** Token streaming for OpenAI-compatible providers using Server-Sent Events. */
class StreamingAiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun streamOpenAiCompatible(
        provider: AiProvider,
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String
    ): Flow<String> = flow {
        require(apiKey.isNotBlank()) { "API key is required" }
        val url = when (provider) {
            AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
            AiProvider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            AiProvider.DEEPSEEK -> "https://api.deepseek.com/chat/completions"
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProvider.MISTRAL_AI -> "https://api.mistral.ai/v1/chat/completions"
            AiProvider.TOGETHER_AI -> "https://api.together.xyz/v1/chat/completions"
            AiProvider.FIREWORKS_AI -> "https://api.fireworks.ai/inference/v1/chat/completions"
            AiProvider.DEEPINFRA -> "https://api.deepinfra.com/v1/openai/chat/completions"
            AiProvider.GROK -> "https://api.x.ai/v1/chat/completions"
            else -> error("${provider.displayName} does not use the OpenAI-compatible streaming adapter")
        }

        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) messages.put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
        history.forEach { message -> messages.put(JSONObject().apply { put("role", message.role); put("content", message.content) }) }
        messages.put(JSONObject().apply { put("role", "user"); put("content", newPrompt) })

        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("stream", true)
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}: ${response.body?.string().orEmpty().take(500)}")
            val stream = response.body?.byteStream() ?: error("Empty response body")
            BufferedReader(InputStreamReader(stream)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isBlank() || payload == "[DONE]") continue
                    val delta = runCatching {
                        JSONObject(payload).optJSONArray("choices")?.optJSONObject(0)
                            ?.optJSONObject("delta")?.optString("content", "").orEmpty()
                    }.getOrDefault("")
                    if (delta.isNotEmpty()) emit(delta)
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
