package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiProvider
import com.example.ui.components.ArcAiOpenSourceBadge
import com.example.ui.theme.ArcPrimary
import com.example.ui.theme.ArcSecondary
import com.example.util.AndroidCompatibilityLayer

@Composable
fun SettingsScreen(
    selectedProvider: AiProvider,
    onOpenApiKeysScreen: () -> Unit,
    onOpenAutomationPermissions: () -> Unit,
    onRestartOnboarding: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenBrandShowcase: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedThemeMode by remember { mutableStateOf("Dark") }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage AI providers, permissions, appearance, and privacy.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- 1. AI Section ---
        item {
            SettingsCategoryHeader("AI & PROVIDERS")
            Spacer(Modifier.height(8.dp))

            SettingsTile(
                title = "AI Providers & API Keys",
                subtitle = "Active: ${selectedProvider.displayName} · Tap to add or edit keys",
                icon = Icons.Outlined.VpnKey,
                onClick = onOpenApiKeysScreen
            )
        }

        // --- 2. Automation Section ---
        item {
            SettingsCategoryHeader("AUTOMATION")
            Spacer(Modifier.height(8.dp))

            SettingsTile(
                title = "Automation & Permissions",
                subtitle = "Manage Accessibility, Screen Capture & Overlays",
                icon = Icons.Outlined.Build,
                onClick = onOpenAutomationPermissions
            )
        }

        // --- 3. Appearance Section ---
        item {
            SettingsCategoryHeader("APPEARANCE")
            Spacer(Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = ArcPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Theme Palette",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Dark", "Light", "System").forEach { themeName ->
                            val isSelected = selectedThemeMode == themeName
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedThemeMode = themeName },
                                label = { Text(themeName, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Voice Section ---
        item {
            SettingsCategoryHeader("VOICE & SPEECH")
            Spacer(Modifier.height(8.dp))

            SettingsTile(
                title = "Text-To-Speech & Speech Recognition",
                subtitle = "Native Speech-To-Text active · Pitch: Standard",
                icon = Icons.Outlined.Mic,
                onClick = {}
            )
        }

        // --- 5. Privacy Section ---
        item {
            SettingsCategoryHeader("PRIVACY & DATA")
            Spacer(Modifier.height(8.dp))

            SettingsTile(
                title = "Privacy Policy & Zero Data Collection",
                subtitle = "All keys stored locally with Keystore encryption",
                icon = Icons.Outlined.Lock,
                onClick = { showPrivacyDialog = true }
            )

            Spacer(Modifier.height(8.dp))

            SettingsTile(
                title = "Clear Chat History",
                subtitle = "Delete all stored local conversations",
                icon = Icons.Outlined.DeleteSweep,
                onClick = { showClearHistoryConfirm = true }
            )
        }

        // --- 6. About & System Compatibility Section ---
        item {
            SettingsCategoryHeader("ABOUT & SYSTEM")
            Spacer(Modifier.height(8.dp))

            SettingsTile(
                title = "ArcAI Brand & Logo Identity",
                subtitle = "Logo system, app icons, dark/light variants & guidelines",
                icon = Icons.Outlined.AutoAwesome,
                onClick = onOpenBrandShowcase
            )

            Spacer(Modifier.height(8.dp))

            SettingsTile(
                title = "Android Compatibility Diagnostics",
                subtitle = AndroidCompatibilityLayer.androidVersionSummary,
                icon = Icons.Outlined.Info,
                onClick = { showDiagnosticsDialog = true }
            )

            Spacer(Modifier.height(8.dp))

            SettingsTile(
                title = "Replay Welcome Onboarding",
                subtitle = "View product walkthrough and setup guide",
                icon = Icons.Outlined.School,
                onClick = onRestartOnboarding
            )

            Spacer(Modifier.height(12.dp))

            ArcAiOpenSourceBadge()
        }
    }

    // --- Android Diagnostics Dialog ---
    if (showDiagnosticsDialog) {
        val diagnostics = remember { AndroidCompatibilityLayer.getCompatibilityDiagnostics(context) }

        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = {
                Text(
                    text = "System Diagnostics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(
                            text = AndroidCompatibilityLayer.androidVersionSummary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    items(diagnostics) { diag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (diag.supported) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (diag.supported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = diag.featureName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = diag.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- Privacy Dialog ---
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("ArcAI Privacy Promise", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "1. Zero Third-Party Servers: Your API keys connect directly from your phone to your chosen provider (Gemini, OpenAI, Anthropic, OpenRouter, etc.).\n\n2. Encrypted Storage: Credentials are encrypted locally using Android Keystore.\n\n3. Explicit Automation: Device automation is transparent, permission-gated, and can be halted instantly with the STOP button at any time.",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }

    // --- Clear History Confirm ---
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear All Conversations?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all chat messages from your device.", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryConfirm = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SettingsTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ArcPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ArcPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
