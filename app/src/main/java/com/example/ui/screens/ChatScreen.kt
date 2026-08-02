package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MessageEntity
import com.example.model.AiProvider
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<MessageEntity>,
    selectedProvider: AiProvider,
    selectedModelId: String,
    isGenerating: Boolean,
    onSendMessage: (String, String?) -> Unit,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (String) -> Unit,
    onDeleteMessage: (MessageEntity) -> Unit,
    onOpenHistoryClick: () -> Unit,
    onOpenApiKeysClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var systemPromptText by remember { mutableStateOf("You are ArcAI Assistant, an enterprise AI powered by the user's own API key.") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showModelDropdown = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Model",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = selectedProvider.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedModelId,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Model Dropdown
                    DropdownMenu(
                        expanded = showModelDropdown,
                        onDismissRequest = { showModelDropdown = false }
                    ) {
                        selectedProvider.availableModels.forEach { minfo ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(minfo.name, fontWeight = FontWeight.Bold)
                                        Text(minfo.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    onSelectModel(minfo.id)
                                    showModelDropdown = false
                                },
                                trailingIcon = if (minfo.id == selectedModelId) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = OpenAiGreen) }
                                } else null
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showSystemPromptDialog = true }) {
                        Icon(Icons.Outlined.Tune, contentDescription = "System Prompt", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onOpenHistoryClick) {
                        Icon(Icons.Outlined.History, contentDescription = "Chat History", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onOpenApiKeysClick) {
                        Icon(Icons.Outlined.VpnKey, contentDescription = "API Keys", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Messages List
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                EmptyChatWelcome(
                    provider = selectedProvider,
                    onSuggestionClick = { suggestion ->
                        inputText = suggestion
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            onCopyClick = { text ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ArcAI Message", text)
                                clipboard.setPrimaryClip(clip)
                            },
                            onDeleteClick = { onDeleteMessage(msg) }
                        )
                    }

                    if (isGenerating) {
                        item {
                            TypingIndicatorBubble(providerName = selectedProvider.displayName)
                        }
                    }
                }
            }
        }

        // Attachment Preview Bar
        AnimatedVisibility(visible = attachedImageBase64 != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = GeminiBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Vision Image attached (Ready for ${selectedModelId})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { attachedImageBase64 = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove attachment", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Bottom Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vision attach button
                IconButton(
                    onClick = {
                        // Simulate attaching an image base64 for Vision multimodal queries
                        attachedImageBase64 = "simulated_base64_vision_attachment"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Vision Image",
                        tint = if (attachedImageBase64 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask anything... (Markdown & Code supported)") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button
                val canSend = inputText.isNotBlank() && !isGenerating
                IconButton(
                    onClick = {
                        if (canSend) {
                            onSendMessage(inputText.trim(), attachedImageBase64)
                            inputText = ""
                            attachedImageBase64 = null
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // System Prompt Dialog
    if (showSystemPromptDialog) {
        AlertDialog(
            onDismissRequest = { showSystemPromptDialog = false },
            title = { Text("System Instruction & Persona") },
            text = {
                Column {
                    Text(
                        "Set custom system instructions for this conversation:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = systemPromptText,
                        onValueChange = { systemPromptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSystemPromptDialog = false }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemPromptDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    onCopyClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Optional Provider & Latency badge for Assistant
        if (!isUser && message.providerName.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = OpenAiGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${message.providerName} • ${message.modelUsed} (${message.latencyMs}ms)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            tonalElevation = if (isUser) 0.dp else 2.dp,
            shadowElevation = if (isUser) 2.dp else 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // If contains code block markdown ``` language
                if (message.hasCodeBlock && !isUser) {
                    FormattedCodeContent(text = message.content, onCopyClick = onCopyClick)
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onCopyClick(message.content) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDeleteClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun FormattedCodeContent(
    text: String,
    onCopyClick: (String) -> Unit
) {
    // Simple parser for markdown code blocks vs normal text
    val parts = text.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { idx, part ->
            if (idx % 2 == 1) {
                // This is a code block
                val firstLineEnd = part.indexOf('\n')
                val language = if (firstLineEnd > 0) part.substring(0, firstLineEnd).trim() else "code"
                val codeBody = if (firstLineEnd > 0) part.substring(firstLineEnd + 1) else part

                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D2D))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.uppercase(),
                                color = Color(0xFFD4D4D4),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            TextButton(
                                onClick = { onCopyClick(codeBody) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy code", color = Color.White, fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = codeBody,
                            color = Color(0xFF9CDCFE),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            } else {
                if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(providerName: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .widthIn(max = 240.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$providerName is generating...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyChatWelcome(
    provider: AiProvider,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to ArcAI Assistant",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Active Provider: ${provider.displayName} (${provider.defaultModel})\nBring Your Own API Key • Zero Markup • Full Privacy",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Try asking:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        val suggestions = listOf(
            "Explain quantum computing in 3 simple bullet points",
            "Write a Python FastAPI backend endpoint with JWT auth",
            "Generate 5 study flashcards for organic chemistry",
            "Compare GPT-4o, Claude 3.5 Sonnet, and DeepSeek R1"
        )

        suggestions.forEach { suggestion ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSuggestionClick(suggestion) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
