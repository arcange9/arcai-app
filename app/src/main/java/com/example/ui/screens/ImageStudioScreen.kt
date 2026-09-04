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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.entity.ImageHistoryEntity
import com.example.model.AiProvider

@Composable
fun ImageStudioScreen(
    imageHistory: List<ImageHistoryEntity>,
    selectedProvider: AiProvider,
    isGenerating: Boolean,
    onGenerateImage: (String, AiProvider, String) -> Unit,
    onDeleteHistoryItem: (ImageHistoryEntity) -> Unit,
    onOpenApiKeys: () -> Unit,
    modifier: Modifier = Modifier
) {
    var prompt by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("1024x1024") }

    LazyColumn(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Image Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Generate real images through a configured provider.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onOpenApiKeys) { Icon(Icons.Default.VpnKey, null); Spacer(Modifier.width(6.dp)); Text("Keys") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Create image", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), minLines = 5, label = { Text("Prompt") }, placeholder = { Text("Describe exactly what you want ArcAI to create…") }, shape = RoundedCornerShape(18.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Provider: OpenAI • Model: ${AiProvider.OPENAI.defaultModel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1024x1024", "1024x1536", "1536x1024").forEach { option ->
                            FilterChip(selected = size == option, onClick = { size = option }, label = { Text(option) })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { onGenerateImage(prompt.trim(), AiProvider.OPENAI, AiProvider.OPENAI.defaultModel); prompt = "" }, enabled = prompt.isNotBlank() && !isGenerating, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        if (isGenerating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp)); Text(if (isGenerating) "Generating…" else "Generate Image")
                    }
                }
            }
        }
        item { Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (imageHistory.isEmpty()) item { Text("No generated images yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(imageHistory, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.providerId} • ${item.modelId}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { onDeleteHistoryItem(item) }) { Icon(Icons.Default.DeleteOutline, "Delete") }
                    }
                    Text(item.prompt, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(model = item.imageUrlOrBase64, contentDescription = item.prompt, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(260.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)))
                }
            }
        }
    }
}
