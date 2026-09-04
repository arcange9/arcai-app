package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiProvider

@Composable
fun FilesScreen(
    selectedProvider: AiProvider,
    onNavigateToChatWithFile: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var fileText by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("Summarize this file and extract the most important points.") }
    var status by remember { mutableStateOf("Choose a file to begin") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedName = queryDisplayName(context, uri)
            fileText = readTextFile(context, uri)
            status = if (fileText.isNotBlank()) "Text extracted • ${fileText.length} characters" else "File selected • ready for supported vision/file workflows"
        }
    }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Files", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Import a document, code file, text file, or image and send its contents to ArcAI.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.InsertDriveFile, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text(if (selectedName.isBlank()) "No file selected" else selectedName, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { picker.launch(arrayOf("text/*", "application/pdf", "application/json", "image/*", "application/octet-stream")) }) {
                        Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(8.dp)); Text("Import File")
                    }
                }
            }
        }
        item {
            OutlinedTextField(instruction, { instruction = it }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("AI instruction") }, shape = RoundedCornerShape(18.dp))
        }
        item {
            Button(onClick = {
                val payload = if (fileText.isNotBlank()) "File: $selectedName\n\nInstruction: $instruction\n\nContent:\n$fileText" else "File selected: $selectedName\nURI: $selectedUri\n\nInstruction: $instruction"
                onNavigateToChatWithFile(payload)
            }, enabled = selectedUri != null, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Analyze with ${selectedProvider.displayName}")
            }
        }
        if (fileText.isNotBlank()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Extracted preview", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(fileText.take(3000), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else uri.lastPathSegment.orEmpty()
    } ?: uri.lastPathSegment.orEmpty()
}.getOrDefault(uri.lastPathSegment.orEmpty())

private fun readTextFile(context: android.content.Context, uri: Uri): String = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
}.getOrDefault("")
