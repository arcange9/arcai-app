package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val folder: String = "General",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val providerId: String,
    val modelId: String,
    val systemPrompt: String = "You are ArcAI Assistant, an enterprise AI powered by the user's own API key.",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val providerName: String = "",
    val modelUsed: String = "",
    val latencyMs: Long = 0L,
    val hasCodeBlock: Boolean = false,
    val imageAttachmentBase64: String? = null
)
