package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.ImageHistoryEntity
import com.example.model.AiProvider
import com.example.ui.theme.*

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
    var promptInput by remember { mutableStateOf("") }
    var selectedImageProvider by remember { mutableStateOf(AiProvider.STABILITY_AI) }
    var selectedImageModel by remember { mutableStateOf(AiProvider.STABILITY_AI.defaultModel) }

    val imageProviders = listOf(AiProvider.STABILITY_AI, AiProvider.MIDJOURNEY, AiProvider.REPLICATE, AiProvider.RUNWAY)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Image Studio & Prompt Enhancer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Stability AI (SD 3.5) • Midjourney v6.1 • Replicate FLUX.1 • Runway Gen-3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenApiKeys,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("API Keys")
                    }
                }
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
                            text = "Generate or Enhance Image Prompt",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(10.dp))

                        // Provider Pills
                        Text("Image Provider:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            imageProviders.forEach { p ->
                                FilterChip(
                                    selected = selectedImageProvider == p,
                                    onClick = {
                                        selectedImageProvider = p
                                        selectedImageModel = p.defaultModel
                                    },
                                    label = { Text(p.displayName, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            label = { Text("Image Prompt / Idea") },
                            placeholder = { Text("e.g. Cyberpunk city at twilight with neon rain and futuristic cars...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        val canGen = promptInput.isNotBlank() && !isGenerating
                        Button(
                            onClick = {
                                if (canGen) {
                                    onGenerateImage(
                                        promptInput.trim(),
                                        selectedImageProvider,
                                        selectedImageModel
                                    )
                                    promptInput = ""
                                }
                            },
                            enabled = canGen,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Enhancing & Generating Image...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Enhance Prompt & Generate (${selectedImageProvider.displayName})")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Generated Image History (${imageHistory.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (imageHistory.isEmpty()) {
                item {
                    Text(
                        text = "No image generation history yet. Enter a prompt above to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(imageHistory, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = AnthropicOrange.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${item.providerId.uppercase()} • ${item.modelId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AnthropicOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(onClick = { onDeleteHistoryItem(item) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = item.prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(12.dp))

                            AsyncImage(
                                model = item.imageUrlOrBase64,
                                contentDescription = item.prompt,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
