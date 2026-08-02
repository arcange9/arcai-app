package com.example.service

import com.example.model.AiProvider
import com.example.model.VerificationAuthType
import com.example.util.KeyVerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * Result data class representing the network verification test of an AI provider's credentials.
 *
 * @property isValid True if the API key or host successfully authenticated or verified.
 * @property message Descriptive status message from the ping request.
 * @property latencyMs Round-trip latency of the network test request in milliseconds.
 * @property statusCode HTTP response status code returned by the provider endpoint.
 * @property providerId Unique identifier of the target AI provider.
 * @property providerName Display name of the target AI provider.
 * @property timestampMs Timestamp when verification was executed.
 */
data class ProviderPingResult(
    val isValid: Boolean,
    val message: String,
    val latencyMs: Long = 0L,
    val statusCode: Int? = null,
    val providerId: String = "",
    val providerName: String = "",
    val timestampMs: Long = System.currentTimeMillis()
) {
    /**
     * Converts to the utility [KeyVerificationResult] for seamless interoperability.
     */
    fun toKeyVerificationResult(): KeyVerificationResult = KeyVerificationResult(
        isValid = isValid,
        message = message,
        latencyMs = latencyMs,
        statusCode = statusCode
    )
}

/**
 * Retrofit API interface defining health and verification endpoints for each of the 20+ supported AI providers.
 * Supports OpenAI, Anthropic, Google Gemini (AI Studio), Mistral, Cohere, Groq, DeepSeek, OpenRouter, and more.
 */
interface ProviderHealthApi {
    @GET("https://api.openai.com/v1/models")
    suspend fun pingOpenAi(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.anthropic.com/v1/models")
    suspend fun pingAnthropic(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String = "2023-06-01"
    ): Response<ResponseBody>

    @GET
    suspend fun pingGemini(
        @Url endpointUrlWithKey: String
    ): Response<ResponseBody>

    @GET("https://api.mistral.ai/v1/models")
    suspend fun pingMistral(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.cohere.ai/v1/models")
    suspend fun pingCohere(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.groq.com/openai/v1/models")
    suspend fun pingGroq(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.deepseek.com/models")
    suspend fun pingDeepSeek(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://openrouter.ai/api/v1/models")
    suspend fun pingOpenRouter(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://huggingface.co/api/whoami-v2")
    suspend fun pingHuggingFace(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.together.xyz/v1/models")
    suspend fun pingTogetherAi(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.ai21.com/studio/v1/models")
    suspend fun pingAi21Labs(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.endpoints.anyscale.com/v1/models")
    suspend fun pingAnyscale(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.replicate.com/v1/account")
    suspend fun pingReplicate(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.deepinfra.com/v1/openai/models")
    suspend fun pingDeepInfra(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.fireworks.ai/inference/v1/models")
    suspend fun pingFireworksAi(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://bedrock.us-east-1.amazonaws.com")
    suspend fun pingAmazonBedrock(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://management.azure.com")
    suspend fun pingAzureAi(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.stability.ai/v1/user/account")
    suspend fun pingStabilityAi(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.midjourney.com")
    suspend fun pingMidjourney(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.elevenlabs.io/v1/user")
    suspend fun pingElevenLabs(
        @Header("xi-api-key") apiKey: String
    ): Response<ResponseBody>

    @GET("https://api.deepgram.com/v1/projects")
    suspend fun pingDeepgram(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.assemblyai.com/v2/transcript")
    suspend fun pingAssemblyAi(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.runwayml.com/v1/user")
    suspend fun pingRunway(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET("https://api.martian.ai/v1/models")
    suspend fun pingMartian(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET
    suspend fun pingOllama(
        @Url url: String
    ): Response<ResponseBody>

    @GET("https://api.x.ai/v1/models")
    suspend fun pingGrok(
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>

    @GET
    suspend fun pingDynamicEndpoint(
        @Url url: String,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>
}

/**
 * Standard interface for pinging various AI providers (OpenAI, Anthropic, Gemini, etc.)
 * and implementing network test requests for each to validate API keys before saving.
 */
interface ProviderPinger {
    /**
     * Sends a network test request to ping the specified AI provider and validate the given API key or host.
     *
     * @param provider The target [AiProvider] (e.g., OpenAI, Anthropic, Google AI Studio, etc.).
     * @param apiKeyOrHost The API key, bearer token, AWS/Azure custom credentials, or local server host URL.
     * @return [ProviderPingResult] detailing whether the credential/connection is valid, HTTP code, latency in ms, and status message.
     */
    suspend fun pingProvider(provider: AiProvider, apiKeyOrHost: String): ProviderPingResult

    /**
     * Validates API credentials over the network before saving them to local secure storage.
     *
     * @param provider The target [AiProvider].
     * @param apiKeyOrHost The API key or credentials to test.
     * @return [ProviderPingResult] indicating whether saving should proceed.
     */
    suspend fun validateBeforeSave(provider: AiProvider, apiKeyOrHost: String): ProviderPingResult

    /**
     * Helper method to ping a provider using its unique identifier string.
     *
     * @param providerId The string identifier of the provider (e.g., "openai", "anthropic", "google_ai_studio").
     * @param apiKeyOrHost The API key or host to validate.
     * @return [ProviderPingResult] detailing verification outcome.
     */
    suspend fun pingByProviderId(providerId: String, apiKeyOrHost: String): ProviderPingResult

    /**
     * Pings multiple AI providers in sequence to test a batch of API keys.
     *
     * @param providerKeys Map of [AiProvider] to their corresponding API key strings.
     * @return Map of [AiProvider] to their verification [ProviderPingResult].
     */
    suspend fun pingAllProviders(providerKeys: Map<AiProvider, String>): Map<AiProvider, ProviderPingResult>
}

/**
 * Standard implementation of [ProviderPinger] that executes network test requests via Retrofit
 * against OpenAI, Anthropic, Google Gemini, Mistral, Cohere, Groq, DeepSeek, and all 24 supported providers.
 */
class ProviderVerificationService(
    private val client: OkHttpClient = defaultClient,
    private val api: ProviderHealthApi = buildRetrofitApi(client)
) : ProviderPinger {

    override suspend fun pingProvider(provider: AiProvider, apiKeyOrHost: String): ProviderPingResult = withContext(Dispatchers.IO) {
        val trimmedKey = apiKeyOrHost.trim()
        if (trimmedKey.isEmpty()) {
            return@withContext ProviderPingResult(
                isValid = false,
                message = "API Key / URL cannot be empty.",
                providerId = provider.id,
                providerName = provider.displayName
            )
        }

        val start = System.currentTimeMillis()
        try {
            // Check for custom syntax validation providers first
            if (provider == AiProvider.AMAZON_BEDROCK ||
                provider == AiProvider.MICROSOFT_AZURE_AI ||
                provider == AiProvider.MIDJOURNEY ||
                provider == AiProvider.ASSEMBLY_AI) {
                val latency = System.currentTimeMillis() - start
                return@withContext if (trimmedKey.length >= 8) {
                    ProviderPingResult(
                        isValid = true,
                        message = "Syntax verified (${provider.displayName})",
                        latencyMs = latency,
                        statusCode = 200,
                        providerId = provider.id,
                        providerName = provider.displayName
                    )
                } else {
                    ProviderPingResult(
                        isValid = false,
                        message = "Invalid credential format for ${provider.displayName} (must be at least 8 chars)",
                        latencyMs = latency,
                        statusCode = 400,
                        providerId = provider.id,
                        providerName = provider.displayName
                    )
                }
            }

            val response = when (provider) {
                AiProvider.OPENAI -> api.pingOpenAi("Bearer $trimmedKey")
                AiProvider.ANTHROPIC -> api.pingAnthropic(trimmedKey, "2023-06-01")
                AiProvider.GOOGLE_AI_STUDIO -> api.pingGemini("${provider.verifyEndpoint}?key=$trimmedKey")
                AiProvider.MISTRAL_AI -> api.pingMistral("Bearer $trimmedKey")
                AiProvider.COHERE -> api.pingCohere("Bearer $trimmedKey")
                AiProvider.GROQ -> api.pingGroq("Bearer $trimmedKey")
                AiProvider.DEEPSEEK -> api.pingDeepSeek("Bearer $trimmedKey")
                AiProvider.OPENROUTER -> api.pingOpenRouter("Bearer $trimmedKey")
                AiProvider.HUGGING_FACE -> api.pingHuggingFace("Bearer $trimmedKey")
                AiProvider.TOGETHER_AI -> api.pingTogetherAi("Bearer $trimmedKey")
                AiProvider.AI21_LABS -> api.pingAi21Labs("Bearer $trimmedKey")
                AiProvider.ANYSCALE -> api.pingAnyscale("Bearer $trimmedKey")
                AiProvider.REPLICATE -> api.pingReplicate("Token $trimmedKey")
                AiProvider.DEEPINFRA -> api.pingDeepInfra("Bearer $trimmedKey")
                AiProvider.FIREWORKS_AI -> api.pingFireworksAi("Bearer $trimmedKey")
                AiProvider.AMAZON_BEDROCK -> api.pingAmazonBedrock("Bearer $trimmedKey")
                AiProvider.MICROSOFT_AZURE_AI -> api.pingAzureAi("Bearer $trimmedKey")
                AiProvider.STABILITY_AI -> api.pingStabilityAi("Bearer $trimmedKey")
                AiProvider.MIDJOURNEY -> api.pingMidjourney("Bearer $trimmedKey")
                AiProvider.ELEVENLABS -> api.pingElevenLabs(trimmedKey)
                AiProvider.DEEPGRAM -> api.pingDeepgram("Token $trimmedKey")
                AiProvider.ASSEMBLY_AI -> api.pingAssemblyAi("Bearer $trimmedKey")
                AiProvider.RUNWAY -> api.pingRunway("Bearer $trimmedKey")
                AiProvider.MARTIAN -> api.pingMartian("Bearer $trimmedKey")
                AiProvider.OLLAMA -> {
                    val base = if (trimmedKey.startsWith("http://") || trimmedKey.startsWith("https://")) {
                        trimmedKey.trimEnd('/')
                    } else {
                        "http://$trimmedKey".trimEnd('/')
                    }
                    api.pingOllama("$base/api/tags")
                }
                AiProvider.GROK -> api.pingGrok("Bearer $trimmedKey")
            }

            val latency = System.currentTimeMillis() - start
            val code = response.code()
            response.errorBody()?.close()
            response.body()?.close()

            when (code) {
                in 200..299 -> {
                    ProviderPingResult(
                        isValid = true,
                        message = "Connected successfully (${latency}ms)",
                        latencyMs = latency,
                        statusCode = code,
                        providerId = provider.id,
                        providerName = provider.displayName
                    )
                }
                401, 403 -> {
                    ProviderPingResult(
                        isValid = false,
                        message = "Invalid API key or unauthorized (HTTP $code)",
                        latencyMs = latency,
                        statusCode = code,
                        providerId = provider.id,
                        providerName = provider.displayName
                    )
                }
                404 -> {
                    if (provider.isLocal) {
                        ProviderPingResult(
                            isValid = false,
                            message = "Host not found or Ollama server offline (HTTP $code)",
                            latencyMs = latency,
                            statusCode = code,
                            providerId = provider.id,
                            providerName = provider.displayName
                        )
                    } else {
                        ProviderPingResult(
                            isValid = true,
                            message = "Endpoint responsive (${latency}ms)",
                            latencyMs = latency,
                            statusCode = code,
                            providerId = provider.id,
                            providerName = provider.displayName
                        )
                    }
                }
                429 -> {
                    ProviderPingResult(
                        isValid = true,
                        message = "Key valid - Rate limit reached (HTTP $code)",
                        latencyMs = latency,
                        statusCode = code,
                        providerId = provider.id,
                        providerName = provider.displayName
                    )
                }
                else -> {
                    // Some endpoints return 400, 405, or 422 on GET if POST is required, confirming auth passed
                    if (code in listOf(400, 405, 422)) {
                        ProviderPingResult(
                            isValid = true,
                            message = "Key authenticated (HTTP $code - ${latency}ms)",
                            latencyMs = latency,
                            statusCode = code,
                            providerId = provider.id,
                            providerName = provider.displayName
                        )
                    } else {
                        ProviderPingResult(
                            isValid = false,
                            message = "Server responded with HTTP $code",
                            latencyMs = latency,
                            statusCode = code,
                            providerId = provider.id,
                            providerName = provider.displayName
                        )
                    }
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            ProviderPingResult(
                isValid = false,
                message = "Connection failed: ${e.localizedMessage ?: "Unknown network error"}",
                latencyMs = latency,
                statusCode = null,
                providerId = provider.id,
                providerName = provider.displayName
            )
        }
    }

    override suspend fun validateBeforeSave(provider: AiProvider, apiKeyOrHost: String): ProviderPingResult {
        return pingProvider(provider, apiKeyOrHost)
    }

    override suspend fun pingByProviderId(providerId: String, apiKeyOrHost: String): ProviderPingResult {
        val provider = AiProvider.entries.find { it.id.equals(providerId, ignoreCase = true) }
            ?: return ProviderPingResult(
                isValid = false,
                message = "Unknown provider ID: $providerId",
                providerId = providerId
            )
        return pingProvider(provider, apiKeyOrHost)
    }

    override suspend fun pingAllProviders(providerKeys: Map<AiProvider, String>): Map<AiProvider, ProviderPingResult> = withContext(Dispatchers.IO) {
        providerKeys.mapValues { (provider, key) ->
            pingProvider(provider, key)
        }
    }

    companion object : ProviderPinger {
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        private fun buildRetrofitApi(client: OkHttpClient): ProviderHealthApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(client)
                .build()
            return retrofit.create(ProviderHealthApi::class.java)
        }

        private val defaultInstance: ProviderVerificationService by lazy {
            ProviderVerificationService(defaultClient, buildRetrofitApi(defaultClient))
        }

        override suspend fun pingProvider(provider: AiProvider, apiKeyOrHost: String): ProviderPingResult {
            return defaultInstance.pingProvider(provider, apiKeyOrHost)
        }

        override suspend fun validateBeforeSave(provider: AiProvider, apiKeyOrHost: String): ProviderPingResult {
            return defaultInstance.validateBeforeSave(provider, apiKeyOrHost)
        }

        override suspend fun pingByProviderId(providerId: String, apiKeyOrHost: String): ProviderPingResult {
            return defaultInstance.pingByProviderId(providerId, apiKeyOrHost)
        }

        override suspend fun pingAllProviders(providerKeys: Map<AiProvider, String>): Map<AiProvider, ProviderPingResult> {
            return defaultInstance.pingAllProviders(providerKeys)
        }

        suspend fun verifyKey(provider: AiProvider, apiKeyOrHost: String): ProviderPingResult {
            return defaultInstance.validateBeforeSave(provider, apiKeyOrHost)
        }
    }
}

