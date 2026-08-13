package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
    HOME("Home", Icons.Outlined.Home, Icons.Default.Home),
    CHATS("Chats", Icons.Outlined.ChatBubbleOutline, Icons.Default.ChatBubble),
    AUTOMATION("Automation", Icons.Outlined.Smartphone, Icons.Default.Smartphone),
    FILES("Files", Icons.Outlined.InsertDriveFile, Icons.Default.InsertDriveFile),
    SETTINGS("Settings", Icons.Outlined.Settings, Icons.Default.Settings)
}

@Composable
fun ArcAiMainScreen(
    viewModel: ArcAiViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ArcAiNavTab.HOME) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showApiKeysManager by remember { mutableStateOf(false) }
    var showBrandShowcase by remember { mutableStateOf(false) }
    var showVoiceOverlay by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showExportProjectDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val currentChatId by viewModel.currentChatId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.messagesForActiveChat.collectAsStateWithLifecycle()
    val allProviderKeys by viewModel.allProviderKeys.collectAsStateWithLifecycle()
    val allChats by viewModel.chatList.collectAsStateWithLifecycle()

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

    if (showOnboarding) {
        OnboardingScreen(
            onFinishOnboarding = { showOnboarding = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isTabletOrExpanded && !showApiKeysManager && !showHistoryScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    ArcAiNavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                showHistoryScreen = false
                                showApiKeysManager = false
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
                                    fontSize = 11.sp,
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
            if (isTabletOrExpanded && !showApiKeysManager && !showHistoryScreen) {
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
                        val isSelected = selectedTab == tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                showHistoryScreen = false
                                showApiKeysManager = false
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
                when {
                    showBrandShowcase -> {
                        BrandShowcaseScreen(
                            onBackClick = { showBrandShowcase = false }
                        )
                    }
                    showApiKeysManager -> {
                        ApiKeyManagerScreen(
                            allProviderKeys = allProviderKeys,
                            selectedProvider = selectedProvider,
                            onVerifyAndSaveKey = { p, key -> viewModel.verifyAndSaveKey(p, key) },
                            onRemoveKey = { viewModel.removeKey(it) },
                            onSetDefaultProvider = { viewModel.setDefaultProvider(it) },
                            onExportJson = { viewModel.exportApiKeysJson() },
                            onImportJson = { viewModel.importApiKeysJson(it) }
                        )
                    }
                    showHistoryScreen -> {
                        ChatHistoryScreen(
                            chats = allChats,
                            currentChatId = currentChatId,
                            onSelectChat = { cId ->
                                viewModel.selectChat(cId)
                                showHistoryScreen = false
                                selectedTab = ArcAiNavTab.CHATS
                            },
                            onCreateNewChat = { title, folder ->
                                viewModel.createNewChat(title, folder)
                                showHistoryScreen = false
                                selectedTab = ArcAiNavTab.CHATS
                            },
                            onDeleteChat = { viewModel.deleteChat(it) },
                            onTogglePin = { viewModel.togglePinChat(it) },
                            onExportProjectClick = { showExportProjectDialog = true }
                        )
                    }
                    else -> {
                        when (selectedTab) {
                            ArcAiNavTab.HOME -> {
                                HomeScreen(
                                    selectedProvider = selectedProvider,
                                    selectedModelId = selectedModelId,
                                    recentChats = allChats,
                                    onNavigateToTab = { tabName ->
                                        selectedTab = when (tabName) {
                                            "Automation" -> ArcAiNavTab.AUTOMATION
                                            "Files" -> ArcAiNavTab.FILES
                                            "Settings" -> ArcAiNavTab.SETTINGS
                                            else -> ArcAiNavTab.CHATS
                                        }
                                    },
                                    onSelectChat = { cId ->
                                        viewModel.selectChat(cId)
                                        selectedTab = ArcAiNavTab.CHATS
                                    },
                                    onQuickAsk = { query ->
                                        viewModel.sendChatMessage(query)
                                        selectedTab = ArcAiNavTab.CHATS
                                    },
                                    onOpenVoiceModal = { showVoiceOverlay = true },
                                    onOpenKeysScreen = { showApiKeysManager = true }
                                )
                            }
                            ArcAiNavTab.CHATS -> {
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
                                    onOpenApiKeysClick = { showApiKeysManager = true }
                                )
                            }
                            ArcAiNavTab.AUTOMATION -> {
                                AutomationScreen(
                                    onOpenSettings = { selectedTab = ArcAiNavTab.SETTINGS }
                                )
                            }
                            ArcAiNavTab.FILES -> {
                                FilesScreen(
                                    selectedProvider = selectedProvider,
                                    onNavigateToChatWithFile = { resultText ->
                                        viewModel.sendChatMessage("Analyzing File Result: $resultText")
                                        selectedTab = ArcAiNavTab.CHATS
                                    }
                                )
                            }
                            ArcAiNavTab.SETTINGS -> {
                                SettingsScreen(
                                    selectedProvider = selectedProvider,
                                    onOpenApiKeysScreen = { showApiKeysManager = true },
                                    onOpenAutomationPermissions = { selectedTab = ArcAiNavTab.AUTOMATION },
                                    onRestartOnboarding = { showOnboarding = true },
                                    onClearHistory = {
                                        allChats.forEach { viewModel.deleteChat(it) }
                                    },
                                    onOpenBrandShowcase = { showBrandShowcase = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Voice Dialog Overlay
    if (showVoiceOverlay) {
        VoiceOverlayDialog(
            onDismiss = { showVoiceOverlay = false },
            onVoiceCommandCaptured = { cmd ->
                showVoiceOverlay = false
                viewModel.sendChatMessage(cmd)
                selectedTab = ArcAiNavTab.CHATS
            }
        )
    }

    if (showExportProjectDialog) {
        ExportProjectDialog(onDismiss = { showExportProjectDialog = false })
    }
}
