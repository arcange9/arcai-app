package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CodeSnippetEntity
import com.example.model.AiProvider
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodingAssistantScreen(
    snippets: List<CodeSnippetEntity>,
    selectedProvider: AiProvider,
    isGenerating: Boolean,
    onGenerateCode: (String, String, String, String) -> Unit,
    onDeleteSnippet: (CodeSnippetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var titleInput by remember { mutableStateOf("") }
    var instructionInput by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var selectedTaskType by remember { mutableStateOf("generate") }
    var selectedSnippet by remember { mutableStateOf<CodeSnippetEntity?>(null) }

    val languages = listOf("Kotlin", "Python", "Flutter / Dart", "TypeScript / React", "SQL / PostgreSQL", "Rust", "Go")
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "AI Coding & Engineering Assistant",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Generate code, debug bugs, refactor for Clean Architecture/SOLID, and write SQL queries using ${selectedProvider.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Code Specification / Debug Request",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Snippet Title") },
                            placeholder = { Text("e.g. JWT Auth Interceptor") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Language Pills
                        Text("Language:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            languages.forEach { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang },
                                    label = { Text(lang, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Task Type Pills
                        Text("Task Type:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val types = listOf(
                                "generate" to "Generate Code",
                                "debug" to "Debug & Fix Bug",
                                "refactor" to "Refactor (SOLID)",
                                "sql" to "SQL Query",
                                "explain" to "Explain Line-by-Line"
                            )
                            types.forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedTaskType == key,
                                    onClick = { selectedTaskType = key },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = instructionInput,
                            onValueChange = { instructionInput = it },
                            label = { Text("Instructions / Paste Code to Debug") },
                            placeholder = { Text("Describe the function, class, endpoint, or paste the code snippet to debug...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        val canGen = titleInput.isNotBlank() && instructionInput.isNotBlank() && !isGenerating
                        Button(
                            onClick = {
                                if (canGen) {
                                    onGenerateCode(
                                        titleInput.trim(),
                                        selectedLanguage,
                                        instructionInput.trim(),
                                        selectedTaskType
                                    )
                                    titleInput = ""
                                    instructionInput = ""
                                }
                            },
                            enabled = canGen,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("AI Generating Code...")
                            } else {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Execute Code Generation")
                            }
                        }
                    }
                }
            }

            // Saved Snippets List
            item {
                Text(
                    text = "Generated Code Snippets (${snippets.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (snippets.isEmpty()) {
                item {
                    Text(
                        text = "No code snippets saved yet. Enter instructions above to generate code!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(snippets, key = { it.id }) { snippet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSnippet = snippet },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = snippet.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Surface(
                                        color = DeepSeekCyan.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = snippet.language,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepSeekCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("ArcAI Code", snippet.explanation)
                                            clipboard.setPrimaryClip(clip)
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { onDeleteSnippet(snippet) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            FormattedCodeContent(
                                text = snippet.explanation,
                                onCopyClick = { code ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ArcAI Code", code)
                                    clipboard.setPrimaryClip(clip)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedSnippet?.let { snippet ->
        AlertDialog(
            onDismissRequest = { selectedSnippet = null },
            title = {
                Text(
                    text = "${snippet.title} (${snippet.language})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn {
                    item {
                        FormattedCodeContent(
                            text = snippet.explanation,
                            onCopyClick = { code ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ArcAI Code", code)
                                clipboard.setPrimaryClip(clip)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedSnippet = null }) {
                    Text("Close")
                }
            }
        )
    }
}
