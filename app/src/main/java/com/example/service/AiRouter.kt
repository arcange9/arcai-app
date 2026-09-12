package com.example.service

import com.example.data.entity.MessageEntity
import com.example.model.AiProvider

/** Provider failover layer. Candidates are tried in order until a usable response is returned. */
class AiRouter(private val client: UnifiedAiClient = UnifiedAiClient()) {
    suspend fun generateWithFallback(
        candidates: List<Pair<AiProvider, String>>,
        systemPrompt: String,
        history: List<MessageEntity>,
        newPrompt: String
    ): AiResponse {
        require(candidates.isNotEmpty()) { "At least one AI provider candidate is required" }

        var lastResponse: AiResponse? = null
        for ((provider, apiKey) in candidates) {
            if (apiKey.isBlank()) continue
            val response = client.generateChatResponse(
                provider = provider,
                apiKey = apiKey,
                modelId = provider.defaultModel,
                systemPrompt = systemPrompt,
                history = history,
                newPrompt = newPrompt
            )
            lastResponse = response
            if (!response.content.startsWith("❌")) return response
        }

        return lastResponse ?: AiResponse(
            content = "❌ No configured AI provider is available. Add and verify an API key in API Management.",
            providerId = "router",
            modelUsed = "none",
            latencyMs = 0
        )
    }
}
