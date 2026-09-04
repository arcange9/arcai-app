package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AiProvider
import com.example.security.ApiKeyCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.apiKeysDataStore: DataStore<Preferences> by preferencesDataStore(name = "arcai_secure_keys")

enum class KeyStatus {
    VERIFIED,
    INVALID,
    UNTESTED
}

data class StoredKeyInfo(
    val providerId: String,
    val apiKey: String,
    val status: KeyStatus,
    val lastVerifiedTime: Long,
    val selectedModel: String
)

class ApiKeyRepository(private val context: Context) {

    companion object {
        private val DEFAULT_PROVIDER_KEY = stringPreferencesKey("default_provider_id")
        private val SELECTED_MODEL_PREFIX = "selected_model_"
        private val STATUS_PREFIX = "status_"
        private val LAST_VERIFIED_PREFIX = "last_verified_"
    }

    private fun keyForProvider(providerId: String) = stringPreferencesKey("key_$providerId")
    private fun modelKeyForProvider(providerId: String) = stringPreferencesKey("$SELECTED_MODEL_PREFIX$providerId")
    private fun statusKeyForProvider(providerId: String) = stringPreferencesKey("$STATUS_PREFIX$providerId")
    private fun verifiedTimeKeyForProvider(providerId: String) = longPreferencesKey("$LAST_VERIFIED_PREFIX$providerId")

    private fun decodeStoredKey(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return ApiKeyCipher.decrypt(value)
    }

    val defaultProviderFlow: Flow<AiProvider> = context.apiKeysDataStore.data.map { prefs ->
        val providerId = prefs[DEFAULT_PROVIDER_KEY] ?: AiProvider.OPENAI.id
        AiProvider.fromId(providerId)
    }

    fun getKeyFlow(providerId: String): Flow<String?> = context.apiKeysDataStore.data.map { prefs ->
        prefs[keyForProvider(providerId)]?.let(::decodeStoredKey)?.ifBlank { null }
    }

    fun getKeyInfoFlow(provider: AiProvider): Flow<StoredKeyInfo> = context.apiKeysDataStore.data.map { prefs ->
        val key = decodeStoredKey(prefs[keyForProvider(provider.id)])
        val model = prefs[modelKeyForProvider(provider.id)] ?: provider.defaultModel
        val statusStr = prefs[statusKeyForProvider(provider.id)] ?: KeyStatus.UNTESTED.name
        val time = prefs[verifiedTimeKeyForProvider(provider.id)] ?: 0L
        StoredKeyInfo(
            providerId = provider.id,
            apiKey = key,
            status = try { KeyStatus.valueOf(statusStr) } catch (e: Exception) { KeyStatus.UNTESTED },
            lastVerifiedTime = time,
            selectedModel = model
        )
    }

    suspend fun saveApiKey(
        provider: AiProvider,
        apiKey: String,
        status: KeyStatus = KeyStatus.UNTESTED,
        lastVerifiedTime: Long = System.currentTimeMillis()
    ) {
        context.apiKeysDataStore.edit { prefs ->
            if (apiKey.isBlank()) {
                prefs.remove(keyForProvider(provider.id))
                prefs.remove(statusKeyForProvider(provider.id))
                prefs.remove(verifiedTimeKeyForProvider(provider.id))
            } else {
                prefs[keyForProvider(provider.id)] = ApiKeyCipher.encrypt(apiKey.trim())
                prefs[statusKeyForProvider(provider.id)] = status.name
                prefs[verifiedTimeKeyForProvider(provider.id)] = lastVerifiedTime
            }
        }
    }

    suspend fun saveSelectedModel(provider: AiProvider, modelId: String) {
        context.apiKeysDataStore.edit { prefs ->
            prefs[modelKeyForProvider(provider.id)] = modelId
        }
    }

    suspend fun setDefaultProvider(provider: AiProvider) {
        context.apiKeysDataStore.edit { prefs ->
            prefs[DEFAULT_PROVIDER_KEY] = provider.id
        }
    }

    suspend fun removeApiKey(provider: AiProvider) {
        context.apiKeysDataStore.edit { prefs ->
            prefs.remove(keyForProvider(provider.id))
            prefs.remove(statusKeyForProvider(provider.id))
            prefs.remove(verifiedTimeKeyForProvider(provider.id))
        }
    }

    /**
     * Exports provider metadata without exporting plaintext API keys.
     * This prevents an accidental backup/share from becoming a credential leak.
     */
    suspend fun exportKeysAsJson(): String {
        val prefs = context.apiKeysDataStore.data.first()
        val jsonArray = JSONArray()
        for (provider in AiProvider.entries) {
            val key = decodeStoredKey(prefs[keyForProvider(provider.id)])
            if (key.isNotBlank()) {
                val obj = JSONObject().apply {
                    put("providerId", provider.id)
                    put("hasApiKey", true)
                    put("selectedModel", prefs[modelKeyForProvider(provider.id)] ?: provider.defaultModel)
                    put("status", prefs[statusKeyForProvider(provider.id)] ?: KeyStatus.UNTESTED.name)
                }
                jsonArray.put(obj)
            }
        }
        return jsonArray.toString(2)
    }

    /**
     * Imports metadata only. API keys must be entered again on the destination install.
     */
    suspend fun importKeysFromJson(jsonString: String): Int {
        var importedCount = 0
        try {
            val jsonArray = JSONArray(jsonString)
            context.apiKeysDataStore.edit { prefs ->
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val pId = obj.optString("providerId", "")
                    if (pId.isNotEmpty() && obj.optBoolean("hasApiKey", false)) {
                        val status = obj.optString("status", KeyStatus.UNTESTED.name)
                        prefs[statusKeyForProvider(pId)] = status
                        val model = obj.optString("selectedModel", "")
                        if (model.isNotEmpty()) prefs[modelKeyForProvider(pId)] = model
                        importedCount++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return importedCount
    }
}
