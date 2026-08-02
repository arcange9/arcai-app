package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ExportProjectDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val tabs = listOf("Overview & ZIP Export", "Phase 1-10 Roadmap", "FastAPI & PostgreSQL Schema", "API Keys List (24)")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ArcAI Enterprise Architecture & Export",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> OverviewAndZipTab(context)
                        1 -> PhaseRoadmapTab(context)
                        2 -> FastApiSchemaTab(context)
                        3 -> ApiKeysListTab(context)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("Close Documentation")
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewAndZipTab(context: Context) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "Exporting Code as ZIP & Generating APK/AAB",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "• To export this full enterprise project as a ZIP archive, use the Google AI Studio settings menu -> Export Project -> Download ZIP.\n" +
                        "• To build production APKs/AABs for Google Play Store, click Generate APK or build with Gradle: `./gradlew assembleRelease`.\n" +
                        "• All 24 AI providers (OpenAI, Claude, Gemini, DeepSeek, Groq, Mistral, Ollama, OpenRouter, etc.) are built-in with zero markup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text(
                text = "Clean Architecture & Feature Modules",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "1. com.example.model: AiProvider enum (24 providers, categories, auth types, models)\n" +
                        "2. com.example.data: Encrypted-style DataStore preferences for API keys & Room SQLite Database (Chat, Study, Code, Images)\n" +
                        "3. com.example.service: UnifiedAiClient multi-provider engine supporting streaming, reasoning, & coding\n" +
                        "4. com.example.ui.screens: Feature screens (Chat, Study, Code, Voice, Image Studio, API Key Manager)\n" +
                        "5. com.example.ui.theme: Material 3 Dark/Light Glassmorphism theme",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PhaseRoadmapTab(context: Context) {
    val roadmapText = """
# ArcAI Assistant - Phase 1-10 Implementation Guide

Phase 1: Architecture & Folder Structure
- Clean Architecture layered pattern with SOLID principles, repository pattern, and reactive StateFlow/Riverpod-style state.
- Package Name: com.arcai.assistant (in manifest/metadata com.example).

Phase 2: FastAPI Backend Design (Optional Server-side sync)
- Complete FastAPI python 3.13+ REST endpoints for user auth, provider sync, chat logs, and analytics.

Phase 3: Database Implementation
- Offline-first Room SQLite with complete tables: chats, messages, study_notes, code_snippets, image_history.

Phase 4: Authentication System
- Bring-Your-Own-Key (BYOK) local authentication with encrypted device DataStore and JSON backup export/import.

Phase 5: Flutter / Kotlin Compose UI Implementation
- Material 3 Glassmorphism dark/light dynamic theming, responsive side rail for tablets, and smooth animations.

Phase 6: 24 AI Provider Integration
- Unified abstraction layer for OpenAI, Anthropic Claude, Google AI Studio, Mistral, Cohere, AI21 Labs, Hugging Face, Groq, Together AI, Anyscale, Replicate, DeepInfra, Fireworks AI, Amazon Bedrock, Azure AI, Stability AI, Midjourney, ElevenLabs, Deepgram, AssemblyAI, Runway, OpenRouter, Martian, Ollama, and xAI Grok.

Phase 7: Advanced Features
- AI Chat (Markdown, syntax highlighting, copy, delete, retry)
- Study Assistant (Summarize notes, 5 Flashcards, practice quizzes)
- Coding Assistant (Multi-language code generator, debugger, SQL query builder)
- Voice Assistant (ElevenLabs TTS & Deepgram Nova-2 STT)
- Image Studio (Prompt enhancer & SD 3.5 / Midjourney generation)

Phase 8: Testing
- Unit testing with Robolectric, UI verification with Roborazzi screenshot tests.

Phase 9: Deployment
- Android APK & App Bundle (AAB) release signing configuration ready for Google Play Store.

Phase 10: Final Production Optimization
- ProGuard rules, Coil image caching, background coroutines, and APK size optimization.
    """.trimIndent()

    LazyColumn {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Phase 1-10 Development Guide", fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ArcAI Roadmap", roadmapText)
                        clipboard.setPrimaryClip(clip)
                    }
                ) {
                    Text("Copy Roadmap")
                }
            }
            Text(roadmapText, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun FastApiSchemaTab(context: Context) {
    val schemaCode = """
# FastAPI & PostgreSQL Schema Architecture for ArcAI Assistant
# SQL DDL for Server-side Sync (Optional Backend)

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    role VARCHAR(50) DEFAULT 'registered',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE api_keys (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    provider_id VARCHAR(50) NOT NULL,
    encrypted_key VARCHAR(500) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chats (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    folder VARCHAR(100) DEFAULT 'General',
    is_pinned BOOLEAN DEFAULT FALSE,
    provider_id VARCHAR(50) NOT NULL,
    model_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    chat_id INT REFERENCES chats(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    provider_name VARCHAR(100),
    model_used VARCHAR(100),
    latency_ms INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
    """.trimIndent()

    LazyColumn {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PostgreSQL DDL & FastAPI Schemas", fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ArcAI Schema", schemaCode)
                        clipboard.setPrimaryClip(clip)
                    }
                ) {
                    Text("Copy Schema DDL")
                }
            }
            Text(schemaCode, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ApiKeysListTab(context: Context) {
    val providersList = """
1. OpenAI (sk-...)
2. Anthropic Claude (sk-ant-...)
3. Google AI Studio (AIzaSy...)
4. Mistral AI (mistral_...)
5. Cohere (cohere_key_...)
6. AI21 Labs (ai21-key-...)
7. Hugging Face (hf_...)
8. Groq (gsk_...)
9. Together AI (together_...)
10. Anyscale / Ray (esecrety_...)
11. Replicate (r8_...)
12. DeepInfra (deepinfra-...)
13. Fireworks AI (fw_...)
14. Amazon Bedrock (AWS Secret Key)
15. Microsoft Azure AI (Azure API Key)
16. Stability AI (sk-...)
17. Midjourney (mj_api_token_...)
18. ElevenLabs (xi-api-key...)
19. Deepgram (Token ...)
20. AssemblyAI (assembly_...)
21. Runway (rw_...)
22. OpenRouter (sk-or-v1-...)
23. Martian Model Router (mr_...)
24. Ollama (http://localhost:11434 - Local)
25. xAI Grok (xai-...)
    """.trimIndent()

    LazyColumn {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All 24 Supported BYOK Providers", fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ArcAI Providers", providersList)
                        clipboard.setPrimaryClip(clip)
                    }
                ) {
                    Text("Copy Providers List")
                }
            }
            Text(providersList, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}
