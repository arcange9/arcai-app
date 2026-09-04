package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.example.service.ArcAiAutomationWorker

@Composable
fun AutomationScreen(onOpenSettings: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    var command by remember { mutableStateOf("") }
    var delayMinutes by remember { mutableStateOf("0") }
    var scheduledIds by remember { mutableStateOf<List<UUID>>(emptyList()) }
    var status by remember { mutableStateOf("No automation scheduled") }

    val infos by workManager.getWorkInfosByTagFlow("arcai-automation").collectAsState(initial = emptyList())
    LaunchedEffect(infos) {
        val running = infos.count { !it.state.isFinished }
        val finished = infos.count { it.state == WorkInfo.State.SUCCEEDED }
        status = when {
            running > 0 -> "$running automation task(s) scheduled/running"
            finished > 0 -> "$finished automation task(s) completed"
            else -> "No automation scheduled"
        }
    }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Automation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Schedule reliable, user-approved local tasks with WorkManager.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Create task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(command, { command = it }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("Task description") }, placeholder = { Text("Example: prepare my study summary") }, shape = RoundedCornerShape(18.dp))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(delayMinutes, { if (it.all(Char::isDigit)) delayMinutes = it }, Modifier.fillMaxWidth(), label = { Text("Delay (minutes)") }, shape = RoundedCornerShape(18.dp))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        val request = OneTimeWorkRequestBuilder<ArcAiAutomationWorker>()
                            .setInputData(workDataOf(ArcAiAutomationWorker.KEY_COMMAND to command.trim()))
                            .setInitialDelay(delayMinutes.toLongOrNull() ?: 0L, TimeUnit.MINUTES)
                            .addTag("arcai-automation")
                            .build()
                        workManager.enqueue(request)
                        scheduledIds = scheduledIds + request.id
                        command = ""
                        status = "Task scheduled"
                    }, enabled = command.isNotBlank(), Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.Schedule, null); Spacer(Modifier.width(8.dp)); Text("Schedule Task")
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Status", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(status, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = {
                        workManager.cancelAllWorkByTag("arcai-automation")
                        status = "Automation cancelled"
                    }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp)); Text("Cancel All") }
                }
            }
        }
        item { Text("Scheduled history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(infos, key = { it.id }) { info ->
            ListItem(
                headlineContent = { Text(info.tags.filter { it == "arcai-automation" }.ifEmpty { setOf("ArcAI task") }.first()) },
                supportingContent = { Text(info.state.name) },
                leadingContent = { Icon(if (info.state == WorkInfo.State.SUCCEEDED) Icons.Default.CheckCircle else Icons.Default.Schedule, null) },
                trailingContent = { IconButton(onClick = { workManager.cancelWorkById(info.id) }) { Icon(Icons.Default.DeleteOutline, "Cancel") } }
            )
        }
    }
}
