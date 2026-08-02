package com.example.util

import com.example.model.AiProvider
import com.example.service.ProviderVerificationService

data class KeyVerificationResult(
    val isValid: Boolean,
    val message: String,
    val latencyMs: Long = 0L,
    val statusCode: Int? = null
)

object ApiKeyVerifier {
    suspend fun verifyKey(provider: AiProvider, keyOrHost: String): KeyVerificationResult {
        return ProviderVerificationService.validateBeforeSave(provider, keyOrHost).toKeyVerificationResult()
    }
}
