package com.example.service

import com.example.data.entity.MessageEntity
import com.example.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResponse(
    val content: String,
    val providerId: String,
    val modelUsed: String,
    val latencyMs: Long
)

class UnifiedAiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(40, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(40, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateChatResponse(
        provider: AiProvider,
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String
    ): AiResponse = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()

        if (apiKey.isBlank()) {
            return@withContext AiResponse(
                content = "⚠️ No API Key configured for ${provider.displayName}.\nPlease go to the API Management screen to add your key.",
                providerId = provider.id,
                modelUsed = modelId,
                latencyMs = 0
            )
        }

        try {
            when (provider) {
                AiProvider.OPENAI, AiProvider.GROQ, AiProvider.DEEPSEEK,
                AiProvider.OPENROUTER, AiProvider.MISTRAL_AI, AiProvider.TOGETHER_AI,
                AiProvider.FIREWORKS_AI, AiProvider.ANYSCALE, AiProvider.DEEPINFRA,
                AiProvider.MARTIAN, AiProvider.GROK -> {
                    generateOpenAiCompatible(provider, apiKey, modelId, systemPrompt, history, newPrompt, start)
                }
                AiProvider.GOOGLE_AI_STUDIO -> generateGemini(apiKey, modelId, systemPrompt, history, newPrompt, start)
                AiProvider.ANTHROPIC -> generateAnthropic(apiKey, modelId, systemPrompt, history, newPrompt, start)
                AiProvider.COHERE -> generateCohere(apiKey, modelId, systemPrompt, history, newPrompt, start)
                AiProvider.OLLAMA -> generateOllama(apiKey, modelId, systemPrompt, history, newPrompt, start)
                else -> generateUnsupportedProvider(provider, modelId, start)
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            AiResponse(
                content = "❌ ${provider.displayName} Error:\n${e.localizedMessage ?: "Unknown connection failure. Please verify your API Key and internet connection."}",
                providerId = provider.id,
                modelUsed = modelId,
                latencyMs = latency
            )
        }
    }

    private fun generateOpenAiCompatible(
        provider: AiProvider,
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        val url = when (provider) {
            AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
            AiProvider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            AiProvider.DEEPSEEK -> "https://api.deepseek.com/chat/completions"
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProvider.MISTRAL_AI -> "https://api.mistral.ai/v1/chat/completions"
            AiProvider.TOGETHER_AI -> "https://api.together.xyz/v1/chat/completions"
            AiProvider.FIREWORKS_AI -> "https://api.fireworks.ai/inference/v1/chat/completions"
            AiProvider.ANYSCALE -> "https://api.endpoints.anyscale.com/v1/chat/completions"
            AiProvider.DEEPINFRA -> "https://api.deepinfra.com/v1/openai/chat/completions"
            AiProvider.MARTIAN -> "https://api.martian.ai/v1/chat/completions"
            AiProvider.GROK -> "https://api.x.ai/v1/chat/completions"
            else -> error("Unsupported OpenAI-compatible provider")
        }

        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        history.forEach { message ->
            messages.put(JSONObject().apply {
                put("role", message.role)
                put("content", message.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", newPrompt)
        })

        val bodyJson = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("temperature", 0.7)
            put("stream", false)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - start
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${safeErrorBody(respBody)}")

            val json = JSONObject(respBody)
            val content = json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
                ?: "Empty response"

            return AiResponse(content, provider.id, modelId, latency)
        }
    }

    private fun generateGemini(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
        val contents = JSONArray()
        history.forEach { message ->
            contents.put(JSONObject().apply {
                put("role", if (message.role == "user") "user" else "model")
                put("parts", JSONArray().put(JSONObject().put("text", message.content)))
            })
        }
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", newPrompt)))
        })

        val body = JSONObject().apply {
            put("contents", contents)
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
            }
        }

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - start
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${safeErrorBody(respBody)}")

            val parts = JSONObject(respBody)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
            val content = parts?.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() } ?: "Empty response"
            return AiResponse(content, AiProvider.GOOGLE_AI_STUDIO.id, modelId, latency)
        }
    }

    private fun generateAnthropic(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        val messages = JSONArray()
        history.forEach { message ->
            messages.put(JSONObject().apply {
                put("role", if (message.role == "user") "user" else "assistant")
                put("content", message.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", newPrompt)
        })

        val body = JSONObject().apply {
            put("model", modelId)
            put("max_tokens", 2048)
            put("messages", messages)
            if (systemPrompt.isNotBlank()) put("system", systemPrompt)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - start
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${safeErrorBody(respBody)}")
            val content = JSONObject(respBody).optJSONArray("content")
                ?.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() }
                ?: "Empty response"
            return AiResponse(content, AiProvider.ANTHROPIC.id, modelId, latency)
        }
    }

    private fun generateCohere(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        val body = JSONObject().apply {
            put("model", modelId)
            put("message", newPrompt)
            if (systemPrompt.isNotBlank()) put("preamble", systemPrompt)
            put("chat_history", JSONArray().apply {
                history.forEach { message ->
                    put(JSONObject().apply {
                        put("role", if (message.role == "user") "USER" else "CHATBOT")
                        put("message", message.content)
                    })
                }
            })
        }

        val request = Request.Builder()
            .url("https://api.cohere.ai/v1/chat")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - start
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${safeErrorBody(respBody)}")
            val content = JSONObject(respBody).optString("text").takeIf { it.isNotBlank() } ?: "Empty response"
            return AiResponse(content, AiProvider.COHERE.id, modelId, latency)
        }
    }

    private fun generateOllama(
        hostUrl: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        val baseUrl = hostUrl.trim().removeSuffix("/")
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "Ollama host must start with http:// or https://"
        }

        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        history.forEach { message ->
            messages.put(JSONObject().apply {
                put("role", message.role)
                put("content", message.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", newPrompt)
        })

        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("stream", false)
        }

        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - start
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${safeErrorBody(respBody)}")
            val content = JSONObject(respBody).optJSONObject("message")?.optString("content")
                ?.takeIf { it.isNotBlank() } ?: "Empty response"
            return AiResponse(content, AiProvider.OLLAMA.id, modelId, latency)
        }
    }

    private fun generateUnsupportedProvider(provider: AiProvider, modelId: String, start: Long): AiResponse {
        return AiResponse(
            content = "⚠️ ${provider.displayName} ($modelId) is listed in ArcAI but does not have a chat adapter implemented yet. No simulated response was generated. Please choose a supported chat provider or use the provider's dedicated feature when available.",
            providerId = provider.id,
            modelUsed = modelId,
            latencyMs = System.currentTimeMillis() - start
        )
    }

    private fun safeErrorBody(body: String): String {
        if (body.isBlank()) return "No error details returned by provider."
        return body
            .replace(Regex("(?i)(api[_-]?key|authorization|x-api-key)\\s*[:=]\\s*[\\\"']?[^,\\\"'\\s}]+"), "$1=[REDACTED]")
            .take(1200)
    }
}
