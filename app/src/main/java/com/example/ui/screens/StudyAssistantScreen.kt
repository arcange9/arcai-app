package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StudyNoteEntity
import com.example.model.AiProvider
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyAssistantScreen(
    studyNotes: List<StudyNoteEntity>,
    selectedProvider: AiProvider,
    isGenerating: Boolean,
    onGenerateHelp: (String, String, String, String) -> Unit,
    onDeleteNote: (StudyNoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var titleInput by remember { mutableStateOf("") }
    var subjectInput by remember { mutableStateOf("") }
    var rawNotesInput by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("summarize") }
    var selectedNote by remember { mutableStateOf<StudyNoteEntity?>(null) }

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
                Text(
                    text = "AI Study & Exam Assistant",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Summarize notes, generate interactive flashcards, explain concepts, and create practice quizzes using ${selectedProvider.displayName}",
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
            // New Study Generation Card
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
                            text = "Create Study Tool",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("Topic Title") },
                                placeholder = { Text("e.g. Photosynthesis") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = subjectInput,
                                onValueChange = { subjectInput = it },
                                label = { Text("Subject") },
                                placeholder = { Text("Biology") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = rawNotesInput,
                            onValueChange = { rawNotesInput = it },
                            label = { Text("Paste Study Notes / Concept Description") },
                            placeholder = { Text("Paste your lecture notes, textbook excerpt, or topic summary here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Action Selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudyActionChip("summarize", "Summarize", selectedAction == "summarize") { selectedAction = "summarize" }
                            StudyActionChip("flashcards", "5 Flashcards", selectedAction == "flashcards") { selectedAction = "flashcards" }
                            StudyActionChip("quiz", "3-Q Quiz", selectedAction == "quiz") { selectedAction = "quiz" }
                            StudyActionChip("explain", "Explain Simple", selectedAction == "explain") { selectedAction = "explain" }
                        }

                        Spacer(Modifier.height(14.dp))

                        val canGenerate = rawNotesInput.isNotBlank() && titleInput.isNotBlank() && !isGenerating
                        Button(
                            onClick = {
                                if (canGenerate) {
                                    onGenerateHelp(
                                        titleInput.trim(),
                                        if (subjectInput.isBlank()) "General" else subjectInput.trim(),
                                        rawNotesInput.trim(),
                                        selectedAction
                                    )
                                    titleInput = ""
                                    subjectInput = ""
                                    rawNotesInput = ""
                                }
                            },
                            enabled = canGenerate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("AI Generating Study Material...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Study Material")
                            }
                        }
                    }
                }
            }

            // Saved Study Notes Header
            item {
                Text(
                    text = "Saved Study Materials (${studyNotes.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (studyNotes.isEmpty()) {
                item {
                    Text(
                        text = "No study notes generated yet. Paste notes above and click Generate!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(studyNotes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedNote = note },
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
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = note.subject,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteNote(note) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = note.aiSummary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            TextButton(onClick = { selectedNote = note }) {
                                Text("View Full Study Note & Flashcards →", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedNote?.let { note ->
        AlertDialog(
            onDismissRequest = { selectedNote = null },
            title = {
                Text(
                    text = "${note.title} (${note.subject})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn {
                    item {
                        Text(
                            text = note.aiSummary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedNote = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StudyActionChip(
    id: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
        } else null
    )
}
