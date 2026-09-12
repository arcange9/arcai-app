package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.StoredKeyInfo

@Composable
fun VoiceAssistantScreen(
    allProviderKeys: Map<String, StoredKeyInfo>,
    onOpenApiKeys: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var speechText by remember { mutableStateOf("Hello! I am ArcAI. How can I help you?") }
    var status by remember { mutableStateOf("Ready") }
    var speaking by remember { mutableStateOf(false) }

    val tts = remember { TextToSpeech(context) { } }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val values = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        transcript = values?.firstOrNull().orEmpty()
        status = if (transcript.isBlank()) "No speech detected" else "Transcription complete"
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchSpeechRecognizer(speechLauncher) else status = "Microphone permission denied"
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecognizer(speechLauncher)
        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Voice Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Native Android speech recognition and text-to-speech", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onOpenApiKeys) { Text("API Keys") }
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(112.dp).clip(CircleShape).background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))), contentAlignment = Alignment.Center) {
                    IconButton(onClick = ::startListening, Modifier.size(96.dp)) {
                        Icon(Icons.Default.Mic, "Start listening", Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(status, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(transcript, { transcript = it }, Modifier.fillMaxWidth(), minLines = 4, label = { Text("Transcription") }, shape = RoundedCornerShape(18.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(speechText, { speechText = it }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("Text to speak") }, shape = RoundedCornerShape(18.dp))
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            if (speaking) { tts.stop(); speaking = false; status = "Stopped" }
            else { tts.language = java.util.Locale.getDefault(); tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "arcai-tts"); speaking = true; status = "Speaking..." }
        }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Icon(if (speaking) Icons.Default.Stop else Icons.Default.VolumeUp, null)
            Spacer(Modifier.width(8.dp))
            Text(if (speaking) "Stop Voice" else "Speak Response")
        }
    }
}

private fun launchSpeechRecognizer(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to ArcAI")
    }
    launcher.launch(intent)
}
