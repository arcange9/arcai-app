package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AiProvider
import com.example.ui.viewmodel.ArcAiViewModel
import kotlinx.coroutines.flow.collectLatest

enum class ArcAiNavTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    CHAT("Chat", Icons.Outlined.ChatBubbleOutline, Icons.Default.ChatBubble),
    STUDY("Study", Icons.Outlined.School, Icons.Default.School),
    CODING("Code", Icons.Outlined.Code, Icons.Default.Code),
    VOICE("Voice", Icons.Outlined.MicNone, Icons.Default.Mic),
    IMAGE("Images", Icons.Outlined.Palette, Icons.Default.Palette),
    KEYS("API Keys", Icons.Outlined.VpnKey, Icons.Default.VpnKey)
}

@Composable
fun ArcAiMainScreen(
    viewModel: ArcAiViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ArcAiNavTab.CHAT) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showExportProjectDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val currentChatId by viewModel.currentChatId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.messagesForActiveChat.collectAsStateWithLifecycle()
    val allProviderKeys by viewModel.allProviderKeys.collectAsStateWithLifecycle()
    val allChats by viewModel.chatList.collectAsStateWithLifecycle()
    val studyNotes by viewModel.studyNotes.collectAsStateWithLifecycle()
    val codeSnippets by viewModel.codeSnippets.collectAsStateWithLifecycle()
    val imageHistory by viewModel.imageHistory.collectAsStateWithLifecycle()

    // Listen to UI Notifications
    LaunchedEffect(viewModel) {
        viewModel.notification.collectLatest { notif ->
            snackbarHostState.showSnackbar(
                message = notif.message,
                duration = SnackbarDuration.Short
            )
        }
    }

    val configuration = LocalConfiguration.current
    val isTabletOrExpanded = configuration.screenWidthDp >= 600

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isTabletOrExpanded) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    ArcAiNavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                showHistoryScreen = false
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tablet Navigation Rail
            if (isTabletOrExpanded) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "ArcAI",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Spacer(Modifier.weight(1f))
                    ArcAiNavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab && !showHistoryScreen
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                showHistoryScreen = false
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title, fontSize = 11.sp) }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { showHistoryScreen = !showHistoryScreen },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = if (showHistoryScreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Main Content Body
            Box(modifier = Modifier.weight(1f)) {
                if (showHistoryScreen) {
                    ChatHistoryScreen(
                        chats = allChats,
                        currentChatId = currentChatId,
                        onSelectChat = { cId ->
                            viewModel.selectChat(cId)
                            showHistoryScreen = false
                            selectedTab = ArcAiNavTab.CHAT
                        },
                        onCreateNewChat = { title, folder ->
                            viewModel.createNewChat(title, folder)
                            showHistoryScreen = false
                            selectedTab = ArcAiNavTab.CHAT
                        },
                        onDeleteChat = { viewModel.deleteChat(it) },
                        onTogglePin = { viewModel.togglePinChat(it) },
                        onExportProjectClick = { showExportProjectDialog = true }
                    )
                } else {
                    when (selectedTab) {
                        ArcAiNavTab.CHAT -> {
                            ChatScreen(
                                messages = activeMessages,
                                selectedProvider = selectedProvider,
                                selectedModelId = selectedModelId,
                                isGenerating = isGenerating,
                                onSendMessage = { txt, img ->
                                    viewModel.sendChatMessage(txt, img)
                                },
                                onSelectProvider = { p -> viewModel.selectProvider(p) },
                                onSelectModel = { mId -> viewModel.selectModel(mId) },
                                onDeleteMessage = { viewModel.deleteMessage(it) },
                                onOpenHistoryClick = { showHistoryScreen = true },
                                onOpenApiKeysClick = { selectedTab = ArcAiNavTab.KEYS }
                            )
                        }
                        ArcAiNavTab.STUDY -> {
                            StudyAssistantScreen(
                                studyNotes = studyNotes,
                                selectedProvider = selectedProvider,
                                isGenerating = isGenerating,
                                onGenerateHelp = { title, subj, notes, act ->
                                    viewModel.generateStudyHelp(title, subj, notes, act)
                                },
                                onDeleteNote = { viewModel.deleteStudyNote(it) }
                            )
                        }
                        ArcAiNavTab.CODING -> {
                            CodingAssistantScreen(
                                snippets = codeSnippets,
                                selectedProvider = selectedProvider,
                                isGenerating = isGenerating,
                                onGenerateCode = { title, lang, inst, tType ->
                                    viewModel.generateCodeSnippet(title, lang, inst, tType)
                                },
                                onDeleteSnippet = { viewModel.deleteCodeSnippet(it) }
                            )
                        }
                        ArcAiNavTab.VOICE -> {
                            VoiceAssistantScreen(
                                allProviderKeys = allProviderKeys,
                                onOpenApiKeys = { selectedTab = ArcAiNavTab.KEYS }
                            )
                        }
                        ArcAiNavTab.IMAGE -> {
                            ImageStudioScreen(
                                imageHistory = imageHistory,
                                selectedProvider = selectedProvider,
                                isGenerating = isGenerating,
                                onGenerateImage = { prompt, p, mId ->
                                    viewModel.generateOrEnhanceImagePrompt(prompt, p, mId)
                                },
                                onDeleteHistoryItem = { viewModel.deleteImageHistory(it) },
                                onOpenApiKeys = { selectedTab = ArcAiNavTab.KEYS }
                            )
                        }
                        ArcAiNavTab.KEYS -> {
                            ApiKeyManagerScreen(
                                allProviderKeys = allProviderKeys,
                                selectedProvider = selectedProvider,
                                onVerifyAndSaveKey = { p, key ->
                                    viewModel.verifyAndSaveKey(p, key)
                                },
                                onRemoveKey = { viewModel.removeKey(it) },
                                onSetDefaultProvider = { viewModel.setDefaultProvider(it) },
                                onExportJson = { viewModel.exportApiKeysJson() },
                                onImportJson = { viewModel.importApiKeysJson(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExportProjectDialog) {
        ExportProjectDialog(onDismiss = { showExportProjectDialog = false })
    }
}
