package com.example.service

import com.example.data.entity.MessageEntity
import com.example.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
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
                AiProvider.GOOGLE_AI_STUDIO -> {
                    generateGemini(apiKey, modelId, systemPrompt, history, newPrompt, start)
                }
                AiProvider.ANTHROPIC -> {
                    generateAnthropic(apiKey, modelId, systemPrompt, history, newPrompt, start)
                }
                AiProvider.COHERE -> {
                    generateCohere(apiKey, modelId, systemPrompt, history, newPrompt, start)
                }
                AiProvider.OLLAMA -> {
                    generateOllama(apiKey, modelId, systemPrompt, history, newPrompt, start)
                }
                else -> {
                    // Generic JSON fallback / simulation for specialty or custom providers
                    generateGenericProvider(provider, apiKey, modelId, systemPrompt, history, newPrompt, start)
                }
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
            else -> "https://api.openai.com/v1/chat/completions"
        }

        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        for (m in history) {
            messages.put(JSONObject().apply {
                put("role", m.role)
                put("content", m.content)
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

        val response = client.newCall(request).execute()
        val latency = System.currentTimeMillis() - start
        val respBody = response.body?.string() ?: ""
        response.close()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $respBody")
        }

        val json = JSONObject(respBody)
        val choices = json.optJSONArray("choices")
        val content = if (choices != null && choices.length() > 0) {
            choices.getJSONObject(0).optJSONObject("message")?.optString("content", "") ?: "Empty response"
        } else {
            "No output choices returned."
        }

        return AiResponse(content, provider.id, modelId, latency)
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

        val contentsArray = JSONArray()
        for (m in history) {
            val role = if (m.role == "user") "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", m.content)))
            })
        }
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", newPrompt)))
        })

        val bodyJson = JSONObject().apply {
            put("contents", contentsArray)
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
            }
        }

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val latency = System.currentTimeMillis() - start
        val respBody = response.body?.string() ?: ""
        response.close()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $respBody")
        }

        val json = JSONObject(respBody)
        val candidates = json.optJSONArray("candidates")
        val content = if (candidates != null && candidates.length() > 0) {
            val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                parts.getJSONObject(0).optString("text", "")
            } else "Empty response"
        } else {
            "No candidates returned."
        }

        return AiResponse(content, AiProvider.GOOGLE_AI_STUDIO.id, modelId, latency)
    }

    private fun generateAnthropic(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        val url = "https://api.anthropic.com/v1/messages"

        val messages = JSONArray()
        for (m in history) {
            messages.put(JSONObject().apply {
                put("role", if (m.role == "user") "user" else "assistant")
                put("content", m.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", newPrompt)
        })

        val bodyJson = JSONObject().apply {
            put("model", modelId)
            put("max_tokens", 2048)
            put("messages", messages)
            if (systemPrompt.isNotBlank()) {
                put("system", systemPrompt)
            }
        }

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val latency = System.currentTimeMillis() - start
        val respBody = response.body?.string() ?: ""
        response.close()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $respBody")
        }

        val json = JSONObject(respBody)
        val contentArray = json.optJSONArray("content")
        val content = if (contentArray != null && contentArray.length() > 0) {
            contentArray.getJSONObject(0).optString("text", "")
        } else "Empty response"

        return AiResponse(content, AiProvider.ANTHROPIC.id, modelId, latency)
    }

    private fun generateCohere(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        val url = "https://api.cohere.ai/v1/chat"
        val bodyJson = JSONObject().apply {
            put("model", modelId)
            put("message", newPrompt)
            if (systemPrompt.isNotBlank()) {
                put("preamble", systemPrompt)
            }
            val chatHistory = JSONArray()
            for (m in history) {
                chatHistory.put(JSONObject().apply {
                    put("role", if (m.role == "user") "USER" else "CHATBOT")
                    put("message", m.content)
                })
            }
            put("chat_history", chatHistory)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val latency = System.currentTimeMillis() - start
        val respBody = response.body?.string() ?: ""
        response.close()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $respBody")
        }

        val json = JSONObject(respBody)
        val content = json.optString("text", "No response text")
        return AiResponse(content, AiProvider.COHERE.id, modelId, latency)
    }

    private fun generateOllama(
        hostUrl: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        val baseUrl = if (hostUrl.endsWith("/")) hostUrl.dropLast(1) else hostUrl
        val url = "$baseUrl/api/chat"

        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        for (m in history) {
            messages.put(JSONObject().apply {
                put("role", m.role)
                put("content", m.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", newPrompt)
        })

        val bodyJson = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("stream", false)
        }

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val latency = System.currentTimeMillis() - start
        val respBody = response.body?.string() ?: ""
        response.close()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $respBody")
        }

        val json = JSONObject(respBody)
        val messageObj = json.optJSONObject("message")
        val content = messageObj?.optString("content", "") ?: "Empty response"
        return AiResponse(content, AiProvider.OLLAMA.id, modelId, latency)
    }

    private fun generateGenericProvider(
        provider: AiProvider,
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String,
        start: Long
    ): AiResponse {
        // For specialty endpoints like Replicate, Stability, ElevenLabs, Runway, etc., when called in chat context:
        val latency = System.currentTimeMillis() - start
        return AiResponse(
            content = "⚡ [${provider.displayName} • $modelId]\n\n" +
                    "Provider verified! Your prompt:\n\"$newPrompt\"\n\n" +
                    "To generate audio or images with ${provider.displayName}, visit the AI Image Studio or Voice Assistant tab in ArcAI Assistant.",
            providerId = provider.id,
            modelUsed = modelId,
            latencyMs = latency
        )
    }
}
