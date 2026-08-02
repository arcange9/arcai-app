package com.example.data.dao

import androidx.room.*
import com.example.data.entity.CodeSnippetEntity
import com.example.data.entity.ImageHistoryEntity
import com.example.data.entity.StudyNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    @Query("SELECT * FROM study_notes ORDER BY createdAt DESC")
    fun getAllStudyNotes(): Flow<List<StudyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyNote(note: StudyNoteEntity): Long

    @Delete
    suspend fun deleteStudyNote(note: StudyNoteEntity)

    @Query("SELECT * FROM code_snippets ORDER BY createdAt DESC")
    fun getAllCodeSnippets(): Flow<List<CodeSnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodeSnippet(snippet: CodeSnippetEntity): Long

    @Delete
    suspend fun deleteCodeSnippet(snippet: CodeSnippetEntity)

    @Query("SELECT * FROM image_history ORDER BY createdAt DESC")
    fun getAllImageHistory(): Flow<List<ImageHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageHistory(item: ImageHistoryEntity): Long

    @Delete
    suspend fun deleteImageHistory(item: ImageHistoryEntity)
}
