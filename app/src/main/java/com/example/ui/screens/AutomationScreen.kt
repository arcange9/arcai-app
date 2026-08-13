package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ArcAiButton
import com.example.ui.theme.ArcPrimary
import com.example.ui.theme.ArcSecondary
import com.example.ui.theme.ArcTertiary
import com.example.util.AndroidCompatibilityLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AutomationStepItem(
    val id: Int,
    val title: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

@Composable
fun AutomationScreen(
    onOpenSettings: () -> Unit
) {
    var commandText by remember { mutableStateOf("") }
    var isAutomationRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var showSafetyConfirmDialog by remember { mutableStateOf(false) }
    var sensitiveActionDetails by remember { mutableStateOf("") }

    // Sample Automation Steps
    val steps = remember(commandText) {
        mutableStateListOf(
            AutomationStepItem(1, "Analyze prompt and determine package", isCompleted = false, isCurrent = false),
            AutomationStepItem(2, "Open target application", isCompleted = false, isCurrent = false),
            AutomationStepItem(3, "Locate active search input via Accessibility", isCompleted = false, isCurrent = false),
            AutomationStepItem(4, "Type query into focused view", isCompleted = false, isCurrent = false),
            AutomationStepItem(5, "Submit search and verify result layout", isCompleted = false, isCurrent = false)
        )
    }

    val coroutineScope = rememberCoroutineScope()

    // Permission States
    var accessibilityEnabled by remember { mutableStateOf(true) }
    var screenCaptureEnabled by remember { mutableStateOf(true) }
    var notificationAccessEnabled by remember { mutableStateOf(true) }
    var overlayEnabled by remember { mutableStateOf(true) }

    fun startAutomationRun() {
        if (commandText.isBlank()) return

        // Safety Check for sensitive keywords
        if (commandText.lowercase().contains("send message") ||
            commandText.lowercase().contains("purchase") ||
            commandText.lowercase().contains("delete") ||
            commandText.lowercase().contains("buy")
        ) {
            sensitiveActionDetails = commandText
            showSafetyConfirmDialog = true
            return
        }

        isAutomationRunning = true
        isPaused = false
        currentStepIndex = 0

        coroutineScope.launch {
            for (i in steps.indices) {
                while (isPaused) { delay(200) }
                if (!isAutomationRunning) break

                steps[i] = steps[i].copy(isCurrent = true, isCompleted = false)
                currentStepIndex = i
                delay(1200)

                steps[i] = steps[i].copy(isCurrent = false, isCompleted = true)
            }
            if (isAutomationRunning) {
                delay(500)
                isAutomationRunning = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Header & Tagline ---
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "ArcAI Automation",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Control your Android device with natural language.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- 2. Status Card ---
        item {
            val isReady = accessibilityEnabled && screenCaptureEnabled
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isReady) ArcTertiary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isReady) ArcTertiary else MaterialTheme.colorScheme.error,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isReady) "● Automation Ready" else "○ Automation Disabled (Permissions Required)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) ArcTertiary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // --- 3. Command Input ---
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "What should I do?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        placeholder = { Text("e.g. 'Open YouTube and search for Python tutorials'", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(Modifier.height(14.dp))

                    ArcAiButton(
                        text = if (isAutomationRunning) "Running Automation..." else "Run Automation",
                        onClick = { startAutomationRun() },
                        enabled = commandText.isNotBlank() && !isAutomationRunning,
                        isLoading = isAutomationRunning,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- 4. Live Automation Progress Interface ---
        if (isAutomationRunning) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ArcPrimary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Automation in progress",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LinearProgressIndicator(
                                progress = { (currentStepIndex + 1) / steps.size.toFloat() },
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = commandText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))

                        steps.forEach { step ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when {
                                    step.isCompleted -> {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ArcTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    step.isCurrent -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = step.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (step.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (step.isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isPaused = !isPaused },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (isPaused) "Resume" else "Pause", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { isAutomationRunning = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("STOP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Permission Center ---
        item {
            Column {
                Text(
                    text = "Automation Permissions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ArcAI needs your explicit permission before interacting with your device.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PermissionCard(
                    title = "Accessibility Service",
                    description = "Allows ArcAI to interact with supported elements on your screen.",
                    isEnabled = accessibilityEnabled,
                    onToggle = { accessibilityEnabled = !accessibilityEnabled }
                )
                Spacer(Modifier.height(10.dp))

                PermissionCard(
                    title = "Screen Capture",
                    description = "Required for vision-based screen understanding and OCR element parsing.",
                    isEnabled = screenCaptureEnabled,
                    onToggle = { screenCaptureEnabled = !screenCaptureEnabled }
                )
                Spacer(Modifier.height(10.dp))

                PermissionCard(
                    title = "Notification Access",
                    description = "Optional: Allows ArcAI to read notification actions when instructed.",
                    isEnabled = notificationAccessEnabled,
                    onToggle = { notificationAccessEnabled = !notificationAccessEnabled }
                )
                Spacer(Modifier.height(10.dp))

                PermissionCard(
                    title = "Display Over Other Apps",
                    description = "Required for rendering active status overlays during automation.",
                    isEnabled = overlayEnabled,
                    onToggle = { overlayEnabled = !overlayEnabled }
                )
            }
        }
    }

    // --- Safety Confirmation Dialog ---
    if (showSafetyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Safety Confirmation Required", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("ArcAI wants to execute a sensitive action:", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sensitiveActionDetails,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Do you explicitly authorize ArcAI to proceed?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSafetyConfirmDialog = false
                        isAutomationRunning = true
                        startAutomationRun()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm & Execute", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSafetyConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isEnabled) ArcTertiary else MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
