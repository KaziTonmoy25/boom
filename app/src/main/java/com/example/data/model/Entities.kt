package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val isPremium: Boolean = false,
    val dailyStreak: Int = 1,
    val lastActiveDate: String = "", // e.g. "2026-06-01"
    val xp: Int = 100,
    val isDarkMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val aiUsageCount: Int = 0
)

@Entity(tableName = "chat_threads")
data class ChatThread(
    @PrimaryKey val threadId: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val messageId: Int = 0,
    val threadId: String,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_histories")
data class QuizHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val questionsJson: String, // JSON serialization of QuizQuestions
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNote(
    @PrimaryKey(autoGenerate = true) val noteId: Int = 0,
    val title: String,
    val originalText: String,
    val summarizedText: String,
    val flashcardsJson: String, // JSON presentation of Flashcards
    val questionsJson: String, // JSON presentation of Study Questions
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_schedules")
data class StudySchedule(
    @PrimaryKey(autoGenerate = true) val scheduleId: Int = 0,
    val title: String,
    val subtitle: String,
    val timeLabel: String, // e.g. "14:00"
    val completed: Boolean = false,
    val dateString: String // "2026-06-01" or other dates
)
