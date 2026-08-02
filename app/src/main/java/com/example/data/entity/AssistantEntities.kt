package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val rawText: String,
    val aiSummary: String,
    val flashcardsJson: String = "[]", // JSON array of {question, answer}
    val quizJson: String = "[]", // JSON array of {question, options, answer}
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "code_snippets")
data class CodeSnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val language: String,
    val code: String,
    val explanation: String,
    val providerId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "image_history")
data class ImageHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val providerId: String,
    val modelId: String,
    val imageUrlOrBase64: String,
    val createdAt: Long = System.currentTimeMillis()
)
