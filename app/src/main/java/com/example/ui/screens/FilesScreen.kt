package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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

private const val MAX_TEXT_BYTES = 2 * 1024 * 1024

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
            val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase()
            val result = extractSafeText(context, uri, mimeType)
            fileText = result.text
            status = result.status
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Files", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Import a document, code file, text file, or image and send supported contents to ArcAI.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text(if (selectedName.isBlank()) "No file selected" else selectedName, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = {
                        picker.launch(arrayOf(
                            "text/*",
                            "application/json",
                            "application/pdf",
                            "image/*",
                            "application/octet-stream"
                        ))
                    }) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import File")
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = instruction,
                onValueChange = { instruction = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("AI instruction") },
                shape = RoundedCornerShape(18.dp)
            )
        }
        item {
            Button(
                onClick = {
                    val payload = if (fileText.isNotBlank()) {
                        "File: $selectedName\n\nInstruction: $instruction\n\nContent:\n$fileText"
                    } else {
                        "File selected: $selectedName\nURI: $selectedUri\n\nInstruction: $instruction"
                    }
                    onNavigateToChatWithFile(payload)
                },
                enabled = selectedUri != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze with ${selectedProvider.displayName}")
            }
        }
        if (fileText.isNotBlank()) item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Extracted preview", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(fileText.take(3000), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private data class ExtractionResult(val text: String, val status: String)

private fun queryDisplayName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else uri.lastPathSegment.orEmpty()
    } ?: uri.lastPathSegment.orEmpty()
}.getOrDefault(uri.lastPathSegment.orEmpty())

private fun extractSafeText(context: Context, uri: Uri, mimeType: String): ExtractionResult {
    if (mimeType.startsWith("image/")) {
        return ExtractionResult("", "Image selected • ready for a vision-capable provider")
    }
    if (mimeType == "application/pdf") {
        return ExtractionResult("", "PDF selected • direct text extraction is not enabled yet")
    }
    if (!isTextLike(mimeType, uri)) {
        return ExtractionResult("", "File selected • binary file content is not read as text")
    }

    return runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readNBytes(MAX_TEXT_BYTES + 1)
        } ?: return ExtractionResult("", "Unable to open file")

        if (bytes.size > MAX_TEXT_BYTES) {
            return ExtractionResult(
                bytes.copyOf(MAX_TEXT_BYTES).toString(Charsets.UTF_8),
                "Text extracted • limited to 2 MB for safety"
            )
        }
        val text = bytes.toString(Charsets.UTF_8)
        ExtractionResult(text, "Text extracted • ${text.length} characters")
    }.getOrElse {
        ExtractionResult("", "File selected • text extraction failed safely")
    }
}

private fun isTextLike(mimeType: String, uri: Uri): Boolean {
    if (mimeType.startsWith("text/")) return true
    if (mimeType == "application/json" || mimeType == "application/xml" || mimeType == "application/javascript") return true
    val name = uri.lastPathSegment.orEmpty().lowercase()
    return listOf(".kt", ".kts", ".java", ".py", ".js", ".ts", ".tsx", ".jsx", ".c", ".h", ".cpp", ".cs", ".go", ".rs", ".swift", ".xml", ".html", ".css", ".md", ".txt", ".sql", ".yaml", ".yml", ".toml", ".properties", ".gradle", ".gradle.kts").any(name::endsWith)
}
