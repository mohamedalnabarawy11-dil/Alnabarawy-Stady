package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceText: String,
    val cramMode: Boolean,
    val simplifiedExplanation: String,
    val outsideConnections: String,
    val importantQuestionsRadar: String,
    val mustMemorize: String,
    val isUnderstood: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val question: String,
    val studentAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckTitle: String,
    val front: String,
    val back: String,
    val isMastered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "busy_slots")
data class BusySlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val startMinutes: Int, // minutes from midnight (0..1439)
    val endMinutes: Int    // minutes from midnight (0..1439)
)

@Entity(tableName = "subject_prefs")
data class SubjectPrefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val difficulty: String, // EASY, MEDIUM, HARD
    val isSelected: Boolean = true
)

@Entity(tableName = "schedule_sessions")
data class ScheduleSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: String,
    val endTime: String,
    val title: String,
    val subjectName: String,
    val difficulty: String,
    val durationMinutes: Int,
    val isBreak: Boolean,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)
