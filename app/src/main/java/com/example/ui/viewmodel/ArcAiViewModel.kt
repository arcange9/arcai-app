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
import com.example.service.ImageGenerationService
import com.example.service.ProviderVerificationService
import com.example.service.UnifiedAiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiNotification(val message: String, val isError: Boolean = false)

class ArcAiViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val assistantDao = db.assistantDao()
    private val keyRepo = ApiKeyRepository(application)
    private val aiClient = UnifiedAiClient()
    private val imageService = ImageGenerationService(application)

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

    val allProviderKeys: StateFlow<Map<String, StoredKeyInfo>> = combine(AiProvider.entries.map { p -> keyRepo.getKeyInfoFlow(p) }) { infos -> infos.associateBy { it.providerId } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val chatList: StateFlow<List<ChatEntity>> = chatDao.getAllChats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val studyNotes: StateFlow<List<StudyNoteEntity>> = assistantDao.getAllStudyNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val codeSnippets: StateFlow<List<CodeSnippetEntity>> = assistantDao.getAllCodeSnippets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val imageHistory: StateFlow<List<ImageHistoryEntity>> = assistantDao.getAllImageHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _messagesForActiveChat = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messagesForActiveChat: StateFlow<List<MessageEntity>> = _messagesForActiveChat.asStateFlow()

    init {
        viewModelScope.launch { keyRepo.migrateLegacyPlaintextKeys() }
        viewModelScope.launch { keyRepo.defaultProviderFlow.collect { p -> _selectedProvider.value = p; _selectedModelId.value = p.defaultModel } }
        viewModelScope.launch { chatList.collect { list -> if (list.isEmpty() && _currentChatId.value == null) createNewChat("Welcome to ArcAI", "General") else if (_currentChatId.value == null && list.isNotEmpty()) selectChat(list.first().id) } }
    }

    fun selectProvider(provider: AiProvider, modelId: String? = null) { _selectedProvider.value = provider; _selectedModelId.value = modelId ?: provider.defaultModel; viewModelScope.launch { keyRepo.saveSelectedModel(provider, _selectedModelId.value) } }
    fun selectModel(modelId: String) { _selectedModelId.value = modelId; viewModelScope.launch { keyRepo.saveSelectedModel(_selectedProvider.value, modelId) } }
    fun createNewChat(title: String = "New Chat", folder: String = "General") { viewModelScope.launch { val provider = _selectedProvider.value; val newId = chatDao.insertChat(ChatEntity(title = title, folder = folder, providerId = provider.id, modelId = _selectedModelId.value)); selectChat(newId) } }
    fun selectChat(chatId: Long) { _currentChatId.value = chatId; viewModelScope.launch { chatDao.getMessagesForChat(chatId).collect { _messagesForActiveChat.value = it } } }
    fun deleteChat(chat: ChatEntity) { viewModelScope.launch { chatDao.deleteMessagesForChat(chat.id); chatDao.deleteChat(chat); if (_currentChatId.value == chat.id) _currentChatId.value = null } }
    fun togglePinChat(chat: ChatEntity) { viewModelScope.launch { chatDao.updateChat(chat.copy(isPinned = !chat.isPinned)) } }

    fun verifyAndSaveKey(provider: AiProvider, keyOrHost: String) { viewModelScope.launch { val result = ProviderVerificationService.validateBeforeSave(provider, keyOrHost); keyRepo.saveApiKey(provider, keyOrHost, if (result.isValid) KeyStatus.VERIFIED else KeyStatus.INVALID, System.currentTimeMillis()); _notification.emit(UiNotification("${provider.displayName}: ${result.message}", !result.isValid)) } }
    fun removeKey(provider: AiProvider) { viewModelScope.launch { keyRepo.removeApiKey(provider); _notification.emit(UiNotification("Removed API key for ${provider.displayName}")) } }
    fun setDefaultProvider(provider: AiProvider) { viewModelScope.launch { keyRepo.setDefaultProvider(provider); _selectedProvider.value = provider; _selectedModelId.value = provider.defaultModel; _notification.emit(UiNotification("Default provider set to ${provider.displayName}")) } }
    suspend fun exportApiKeysJson(): String = keyRepo.exportKeysAsJson()
    fun importApiKeysJson(json: String) { viewModelScope.launch { val count = keyRepo.importKeysFromJson(json); _notification.emit(UiNotification("Successfully imported $count API keys!")) } }

    fun sendChatMessage(userText: String, imageBase64: String? = null) {
        val chatId = _currentChatId.value ?: return
        val provider = _selectedProvider.value
        val model = _selectedModelId.value
        viewModelScope.launch {
            val currentMsgs = _messagesForActiveChat.value
            chatDao.insertMessage(MessageEntity(chatId = chatId, role = "user", content = userText, imageAttachmentBase64 = imageBase64))
            _isGenerating.value = true
            try {
                val apiKey = allProviderKeys.value[provider.id]?.apiKey ?: ""
                val response = aiClient.generateChatResponse(provider, apiKey, model, "You are ArcAI Assistant. Provide clear, helpful, well-formatted markdown answers.", currentMsgs, userText)
                chatDao.insertMessage(MessageEntity(chatId = chatId, role = "assistant", content = response.content, providerName = provider.displayName, modelUsed = response.modelUsed, latencyMs = response.latencyMs, hasCodeBlock = response.content.contains("```")))
            } catch (e: Exception) { _notification.emit(UiNotification("Generation error: ${e.localizedMessage}", true)) }
            finally { _isGenerating.value = false }
        }
    }
    fun deleteMessage(msg: MessageEntity) { viewModelScope.launch { chatDao.deleteMessage(msg) } }

    fun generateStudyHelp(title: String, subject: String, rawNotes: String, actionType: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            val provider = _selectedProvider.value
            try {
                val prompt = when (actionType) { "summarize" -> "Summarize the following study notes clearly with key bullet points:\n\n$rawNotes"; "flashcards" -> "Generate 5 flashcards from these notes. Format as clear Question and Answer pairs:\n\n$rawNotes"; "quiz" -> "Generate a 3-question multiple choice practice quiz with answers based on these notes:\n\n$rawNotes"; else -> "Explain the core concepts in these notes in simple, memorable terms:\n\n$rawNotes" }
                val response = aiClient.generateChatResponse(provider, allProviderKeys.value[provider.id]?.apiKey ?: "", _selectedModelId.value, "You are an expert AI Study Assistant.", emptyList(), prompt)
                assistantDao.insertStudyNote(StudyNoteEntity(title = title, subject = subject, rawText = rawNotes, aiSummary = response.content))
                _notification.emit(UiNotification("Generated study $actionType successfully!"))
            } catch (e: Exception) { _notification.emit(UiNotification("Study generator failed: ${e.localizedMessage}", true)) }
            finally { _isGenerating.value = false }
        }
    }
    fun deleteStudyNote(note: StudyNoteEntity) { viewModelScope.launch { assistantDao.deleteStudyNote(note) } }

    fun generateCodeSnippet(title: String, language: String, instruction: String, taskType: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            val provider = _selectedProvider.value
            try {
                val prompt = when (taskType) { "debug" -> "Analyze and debug this $language code. Explain the bug and provide the corrected code in markdown:\n\n$instruction"; "refactor" -> "Refactor this $language code for optimal performance, readability, and SOLID principles:\n\n$instruction"; "sql" -> "Generate high-performance, safe SQL queries for the following requirements:\n\n$instruction"; "explain" -> "Provide a comprehensive line-by-line explanation of this $language code:\n\n$instruction"; else -> "Write clean, production-ready $language code for the following specification:\n\n$instruction" }
                val response = aiClient.generateChatResponse(provider, allProviderKeys.value[provider.id]?.apiKey ?: "", _selectedModelId.value, "You are an expert Principal Software Architect and AI Coding Assistant.", emptyList(), prompt)
                assistantDao.insertCodeSnippet(CodeSnippetEntity(title = title, language = language, code = instruction, explanation = response.content, providerId = provider.id))
                _notification.emit(UiNotification("Generated code snippet successfully!"))
            } catch (e: Exception) { _notification.emit(UiNotification("Coding assistant failed: ${e.localizedMessage}", true)) }
            finally { _isGenerating.value = false }
        }
    }
    fun deleteCodeSnippet(snippet: CodeSnippetEntity) { viewModelScope.launch { assistantDao.deleteCodeSnippet(snippet) } }

    fun generateOrEnhanceImagePrompt(prompt: String, provider: AiProvider, modelId: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val apiKey = allProviderKeys.value[provider.id]?.apiKey.orEmpty()
                if (apiKey.isBlank()) throw IllegalStateException("Add an API key for ${provider.displayName} first.")
                val enhancedPrompt = if (prompt.length < 30) {
                    val enhancerKey = allProviderKeys.value[AiProvider.OPENAI.id]?.apiKey.orEmpty()
                    if (enhancerKey.isBlank()) prompt else aiClient.generateChatResponse(AiProvider.OPENAI, enhancerKey, "gpt-4o-mini", "Expand short image ideas into detailed production-ready prompts.", emptyList(), "Enhance this image prompt: $prompt").content
                } else prompt
                val result = imageService.generate(provider, apiKey, modelId, enhancedPrompt)
                val imagePath = result.getOrElse { throw it }
                assistantDao.insertImageHistory(ImageHistoryEntity(prompt = enhancedPrompt, providerId = provider.id, modelId = modelId, imageUrlOrBase64 = imagePath))
                _notification.emit(UiNotification("Image generated successfully and saved to history."))
            } catch (e: Exception) { _notification.emit(UiNotification("Image generation failed: ${e.localizedMessage}", true)) }
            finally { _isGenerating.value = false }
        }
    }
    fun deleteImageHistory(item: ImageHistoryEntity) { viewModelScope.launch { assistantDao.deleteImageHistory(item) } }
}
