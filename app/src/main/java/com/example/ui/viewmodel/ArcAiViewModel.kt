package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiKeyRepository
import com.example.data.AppDatabase
import com.example.data.KeyStatus
import com.example.data.StoredKeyInfo
import com.example.data.entity.*
import com.example.model.AiProvider
import com.example.model.ProviderCategory
import com.example.service.ProviderVerificationService
import com.example.service.UnifiedAiClient
import com.example.util.ApiKeyVerifier
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class ArcAiViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val assistantDao = db.assistantDao()
    private val keyRepo = ApiKeyRepository(application)
    private val aiClient = UnifiedAiClient()

    private val _selectedProvider = MutableStateFlow(AiProvider.OPENAI)
    val selectedProvider: StateFlow<AiProvider> = _selectedProvider.asStateFlow()

    private val _selectedModelId = MutableStateFlow(AiProvider.OPENAI.defaultModel)
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val _currentChatId = MutableStateFlow<Long?>(null)
    val currentChatId: StateFlow<Long?> = _currentChatId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _notification = MutableSharedFlow<UiNotification>()
    val notification = _notification.asSharedFlow()

    // API Key Info flow map for all providers
    val allProviderKeys: StateFlow<Map<String, StoredKeyInfo>> = combine(
        AiProvider.entries.map { p -> keyRepo.getKeyInfoFlow(p) }
    ) { arrayOfInfos ->
        arrayOfInfos.associateBy { it.providerId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val chatList: StateFlow<List<ChatEntity>> = chatDao.getAllChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyNotes: StateFlow<List<StudyNoteEntity>> = assistantDao.getAllStudyNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val codeSnippets: StateFlow<List<CodeSnippetEntity>> = assistantDao.getAllCodeSnippets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val imageHistory: StateFlow<List<ImageHistoryEntity>> = assistantDao.getAllImageHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messagesForActiveChat = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messagesForActiveChat: StateFlow<List<MessageEntity>> = _messagesForActiveChat.asStateFlow()

    init {
        // Load default provider from preferences
        viewModelScope.launch {
            keyRepo.defaultProviderFlow.collect { p ->
                _selectedProvider.value = p
                _selectedModelId.value = p.defaultModel
            }
        }
        // Initialize with default chat if empty
        viewModelScope.launch {
            chatList.collect { list ->
                if (list.isEmpty() && _currentChatId.value == null) {
                    createNewChat("Welcome to ArcAI", "General")
                } else if (_currentChatId.value == null && list.isNotEmpty()) {
                    selectChat(list.first().id)
                }
            }
        }
    }

    fun selectProvider(provider: AiProvider, modelId: String? = null) {
        _selectedProvider.value = provider
        _selectedModelId.value = modelId ?: provider.defaultModel
        viewModelScope.launch {
            keyRepo.saveSelectedModel(provider, _selectedModelId.value)
        }
    }

    fun selectModel(modelId: String) {
        _selectedModelId.value = modelId
        viewModelScope.launch {
            keyRepo.saveSelectedModel(_selectedProvider.value, modelId)
        }
    }

    fun createNewChat(title: String = "New Chat", folder: String = "General") {
        viewModelScope.launch {
            val provider = _selectedProvider.value
            val newId = chatDao.insertChat(
                ChatEntity(
                    title = title,
                    folder = folder,
                    providerId = provider.id,
                    modelId = _selectedModelId.value
                )
            )
            selectChat(newId)
        }
    }

    fun selectChat(chatId: Long) {
        _currentChatId.value = chatId
        viewModelScope.launch {
            chatDao.getMessagesForChat(chatId).collect { msgs ->
                _messagesForActiveChat.value = msgs
            }
        }
    }

    fun deleteChat(chat: ChatEntity) {
        viewModelScope.launch {
            chatDao.deleteMessagesForChat(chat.id)
            chatDao.deleteChat(chat)
            if (_currentChatId.value == chat.id) {
                _currentChatId.value = null
            }
        }
    }

    fun togglePinChat(chat: ChatEntity) {
        viewModelScope.launch {
            chatDao.updateChat(chat.copy(isPinned = !chat.isPinned))
        }
    }

    // --- API Key Management & Live Verification ---

    fun verifyAndSaveKey(provider: AiProvider, keyOrHost: String) {
        viewModelScope.launch {
            val result = ProviderVerificationService.validateBeforeSave(provider, keyOrHost)
            val status = if (result.isValid) KeyStatus.VERIFIED else KeyStatus.INVALID
            keyRepo.saveApiKey(
                provider = provider,
                apiKey = keyOrHost,
                status = status,
                lastVerifiedTime = System.currentTimeMillis()
            )
            _notification.emit(
                UiNotification(
                    message = "${provider.displayName}: ${result.message}",
                    isError = !result.isValid
                )
            )
        }
    }

    fun removeKey(provider: AiProvider) {
        viewModelScope.launch {
            keyRepo.removeApiKey(provider)
            _notification.emit(UiNotification("Removed API key for ${provider.displayName}"))
        }
    }

    fun setDefaultProvider(provider: AiProvider) {
        viewModelScope.launch {
            keyRepo.setDefaultProvider(provider)
            _selectedProvider.value = provider
            _selectedModelId.value = provider.defaultModel
            _notification.emit(UiNotification("Default provider set to ${provider.displayName}"))
        }
    }

    suspend fun exportApiKeysJson(): String {
        return keyRepo.exportKeysAsJson()
    }

    fun importApiKeysJson(json: String) {
        viewModelScope.launch {
            val count = keyRepo.importKeysFromJson(json)
            _notification.emit(UiNotification("Successfully imported $count API keys!"))
        }
    }

    // --- AI Chat Execution ---

    fun sendChatMessage(userText: String, imageBase64: String? = null) {
        val chatId = _currentChatId.value ?: return
        val provider = _selectedProvider.value
        val model = _selectedModelId.value

        viewModelScope.launch {
            val currentMsgs = _messagesForActiveChat.value
            val userMsg = MessageEntity(
                chatId = chatId,
                role = "user",
                content = userText,
                imageAttachmentBase64 = imageBase64
            )
            chatDao.insertMessage(userMsg)

            _isGenerating.value = true
            try {
                val keyInfo = allProviderKeys.value[provider.id]
                val apiKey = keyInfo?.apiKey ?: ""

                val response = aiClient.generateChatResponse(
                    provider = provider,
                    apiKey = apiKey,
                    modelId = model,
                    systemPrompt = "You are ArcAI Assistant, an enterprise AI powered by the user's own API key. You combine ChatGPT, Gemini, and Claude capabilities in one app. Provide clear, helpful, well-formatted markdown answers.",
                    history = currentMsgs,
                    newPrompt = userText
                )

                val hasCode = response.content.contains("```")
                chatDao.insertMessage(
                    MessageEntity(
                        chatId = chatId,
                        role = "assistant",
                        content = response.content,
                        providerName = provider.displayName,
                        modelUsed = response.modelUsed,
                        latencyMs = response.latencyMs,
                        hasCodeBlock = hasCode
                    )
                )
            } catch (e: Exception) {
                _notification.emit(UiNotification("Generation error: ${e.localizedMessage}", true))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteMessage(msg: MessageEntity) {
        viewModelScope.launch {
            chatDao.deleteMessage(msg)
        }
    }

    // --- Study Assistant Generation ---

    fun generateStudyHelp(
        title: String,
        subject: String,
        rawNotes: String,
        actionType: String // "summarize", "flashcards", "quiz", "explain"
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            val provider = _selectedProvider.value
            val model = _selectedModelId.value
            val keyInfo = allProviderKeys.value[provider.id]
            val apiKey = keyInfo?.apiKey ?: ""

            val prompt = when (actionType) {
                "summarize" -> "Summarize the following study notes clearly with key bullet points:\n\n$rawNotes"
                "flashcards" -> "Generate 5 flashcards from these notes. Format as clear Question and Answer pairs:\n\n$rawNotes"
                "quiz" -> "Generate a 3-question multiple choice practice quiz with answers based on these notes:\n\n$rawNotes"
                else -> "Explain the core concepts in these notes in simple, memorable terms:\n\n$rawNotes"
            }

            try {
                val response = aiClient.generateChatResponse(
                    provider = provider,
                    apiKey = apiKey,
                    modelId = model,
                    systemPrompt = "You are an expert AI Study Assistant. Help the user master topics with clarity and pedagogical precision.",
                    history = emptyList(),
                    newPrompt = prompt
                )

                val note = StudyNoteEntity(
                    title = title,
                    subject = subject,
                    rawText = rawNotes,
                    aiSummary = response.content
                )
                assistantDao.insertStudyNote(note)
                _notification.emit(UiNotification("Generated study $actionType successfully!"))
            } catch (e: Exception) {
                _notification.emit(UiNotification("Study generator failed: ${e.localizedMessage}", true))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteStudyNote(note: StudyNoteEntity) {
        viewModelScope.launch {
            assistantDao.deleteStudyNote(note)
        }
    }

    // --- Coding Assistant Generation ---

    fun generateCodeSnippet(
        title: String,
        language: String,
        instruction: String,
        taskType: String // "generate", "debug", "explain", "refactor", "sql"
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            val provider = _selectedProvider.value
            val model = _selectedModelId.value
            val keyInfo = allProviderKeys.value[provider.id]
            val apiKey = keyInfo?.apiKey ?: ""

            val prompt = when (taskType) {
                "debug" -> "Analyze and debug this $language code. Explain the bug and provide the corrected code in markdown:\n\n$instruction"
                "refactor" -> "Refactor this $language code for optimal performance, readability, and SOLID principles:\n\n$instruction"
                "sql" -> "Generate high-performance, safe SQL queries for the following requirements:\n\n$instruction"
                "explain" -> "Provide a comprehensive line-by-line explanation of this $language code:\n\n$instruction"
                else -> "Write clean, production-ready $language code for the following specification:\n\n$instruction"
            }

            try {
                val response = aiClient.generateChatResponse(
                    provider = provider,
                    apiKey = apiKey,
                    modelId = model,
                    systemPrompt = "You are an expert Principal Software Architect and AI Coding Assistant.",
                    history = emptyList(),
                    newPrompt = prompt
                )

                val snippet = CodeSnippetEntity(
                    title = title,
                    language = language,
                    code = instruction,
                    explanation = response.content,
                    providerId = provider.id
                )
                assistantDao.insertCodeSnippet(snippet)
                _notification.emit(UiNotification("Generated code snippet successfully!"))
            } catch (e: Exception) {
                _notification.emit(UiNotification("Coding assistant failed: ${e.localizedMessage}", true))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteCodeSnippet(snippet: CodeSnippetEntity) {
        viewModelScope.launch {
            assistantDao.deleteCodeSnippet(snippet)
        }
    }

    // --- Image Studio Generation ---

    fun generateOrEnhanceImagePrompt(prompt: String, provider: AiProvider, modelId: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            val keyInfo = allProviderKeys.value[provider.id]
            val apiKey = keyInfo?.apiKey ?: ""

            try {
                val enhancedPrompt = if (prompt.length < 30) {
                    val aiResp = aiClient.generateChatResponse(
                        provider = AiProvider.OPENAI,
                        apiKey = allProviderKeys.value["openai"]?.apiKey ?: "",
                        modelId = "gpt-4o-mini",
                        systemPrompt = "You are an expert prompt engineer for Midjourney and Stable Diffusion. Expand the short prompt into a rich, detailed cinematic prompt.",
                        history = emptyList(),
                        newPrompt = "Enhance this image prompt: '$prompt'"
                    )
                    aiResp.content
                } else prompt

                val historyItem = ImageHistoryEntity(
                    prompt = enhancedPrompt,
                    providerId = provider.id,
                    modelId = modelId,
                    imageUrlOrBase64 = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80"
                )
                assistantDao.insertImageHistory(historyItem)
                _notification.emit(UiNotification("Image prompt generated and saved to history!"))
            } catch (e: Exception) {
                _notification.emit(UiNotification("Image generation failed: ${e.localizedMessage}", true))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteImageHistory(item: ImageHistoryEntity) {
        viewModelScope.launch {
            assistantDao.deleteImageHistory(item)
        }
    }
}
