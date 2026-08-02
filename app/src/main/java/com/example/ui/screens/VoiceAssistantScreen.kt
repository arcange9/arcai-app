package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.data.StoredKeyInfo
import com.example.model.AiProvider

@Composable
fun VoiceAssistantScreen(
    allProviderKeys: Map<String, StoredKeyInfo>,
    onOpenApiKeys: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isListening by remember { mutableStateOf(false) }
    var isPlayingTts by remember { mutableStateOf(false) }
    var sttTranscript by remember { mutableStateOf("Ready to transcribe speech via Deepgram Nova-2 / AssemblyAI Universal-1...") }
    var ttsText by remember { mutableStateOf("Hello! I am your ArcAI Voice Assistant powered by ElevenLabs Multilingual v2.") }

    val elevenKey = allProviderKeys[AiProvider.ELEVENLABS.id]?.apiKey
    val deepgramKey = allProviderKeys[AiProvider.DEEPGRAM.id]?.apiKey

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
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
                            text = "AI Voice & Audio Studio",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "ElevenLabs TTS • Deepgram Nova-2 STT • AssemblyAI Universal-1",
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
                        Text("Configure Voice Keys")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hands-Free Microphone Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isListening) "Listening to Voice Command..." else "Hands-Free Voice Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(16.dp))

                        // Mic Button
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) Color.Red.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                .clickable { isListening = !isListening },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Outlined.MicNone,
                                contentDescription = "Toggle Mic",
                                tint = if (isListening) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = if (isListening) "Tap to stop recording" else "Tap microphone to speak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Speech to Text Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Speech-to-Text Transcription",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Deepgram Nova-2",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = sttTranscript,
                            onValueChange = { sttTranscript = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                sttTranscript = "Transcribed voice audio: 'What is the architectural pattern for Riverpod and Clean Architecture in Flutter?'"
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Simulate STT Transcription")
                        }
                    }
                }
            }

            // Text to Speech Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ElevenLabs Text-to-Speech",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Multilingual v2",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = ttsText,
                            onValueChange = { ttsText = it },
                            label = { Text("Text to synthesize into voice") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { isPlayingTts = !isPlayingTts },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlayingTts) Color.Red else MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingTts) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (isPlayingTts) "Stop Audio" else "Synthesize & Play Voice")
                            }
                        }
                    }
                }
            }
        }
    }
}
