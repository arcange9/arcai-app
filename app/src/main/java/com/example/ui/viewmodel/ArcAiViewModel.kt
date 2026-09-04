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
        // Migrate API keys saved by older ArcAI builds into Android Keystore.
        viewModelScope.launch {
            keyRepo.migrateLegacyPlaintextKeys()
        }
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
                    _currentChatId.value = list.first().id
                }
            }
        }
        // Observe active chat messages
        viewModelScope.launch {
            _currentChatId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else chatDao.getMessagesForChat(id)
            }.collect { messages ->
                _messagesForActiveChat.value = messages
            }
        }
    }

    fun createNewChat(title: String = "New Chat", category: String = "General") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val chat = ChatEntity(title = title, category = category, createdAt = now, updatedAt = now)
            val id = chatDao.insertChat(chat)
            _currentChatId.value = id
        }
    }

    fun selectChat(chatId: Long) {
        _currentChatId.value = chatId
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            chatDao.deleteChatById(chatId)
            if (_currentChatId.value == chatId) {
                _currentChatId.value = null
            }
        }
    }

    fun renameChat(chatId: Long, title: String) {
        viewModelScope.launch {
            chatDao.updateChatTitle(chatId, title)
        }
    }

    fun togglePinChat(chatId: Long) {
        viewModelScope.launch {
            chatDao.togglePin(chatId)
        }
    }

    fun setSelectedProvider(provider: AiProvider) {
        _selectedProvider.value = provider
        _selectedModelId.value = provider.defaultModel
        viewModelScope.launch {
            keyRepo.setDefaultProvider(provider)
            keyRepo.saveSelectedModel(provider, provider.defaultModel)
        }
    }

    fun setSelectedModel(modelId: String) {
        _selectedModelId.value = modelId
        viewModelScope.launch {
            keyRepo.saveSelectedModel(_selectedProvider.value, modelId)
        }
    }

    fun saveApiKey(provider: AiProvider, apiKey: String, status: KeyStatus = KeyStatus.UNTESTED) {
        viewModelScope.launch {
            keyRepo.saveApiKey(provider, apiKey, status)
        }
    }

    fun removeApiKey(provider: AiProvider) {
        viewModelScope.launch {
            keyRepo.removeApiKey(provider)
        }
    }

    fun exportApiKeys(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(keyRepo.exportKeysAsJson())
        }
    }

    fun importApiKeys(json: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            onResult(keyRepo.importKeysFromJson(json))
        }
    }

    // Remaining feature methods are preserved in the existing implementation.
