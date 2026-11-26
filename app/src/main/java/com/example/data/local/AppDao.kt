package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Lessons
    @Query("SELECT * FROM lessons ORDER BY createdAt DESC")
    fun observeLessons(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Query("UPDATE lessons SET isUnderstood = :understood WHERE id = :id")
    suspend fun updateLessonUnderstood(id: Long, understood: Boolean)

    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun deleteLesson(id: Long)

    // Mistakes Bank
    @Query("SELECT * FROM mistakes ORDER BY createdAt DESC")
    fun observeMistakes(): Flow<List<MistakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistakes(mistakes: List<MistakeEntity>)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistake(id: Long)

    // Flashcards
    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun observeFlashcards(): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<FlashcardEntity>)

    @Query("UPDATE flashcards SET isMastered = :mastered WHERE id = :id")
    suspend fun updateFlashcardMastered(id: Long, mastered: Boolean)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcard(id: Long)

    // Busy Slots
    @Query("SELECT * FROM busy_slots ORDER BY startMinutes ASC")
    fun observeBusySlots(): Flow<List<BusySlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusySlot(slot: BusySlotEntity): Long

    @Query("DELETE FROM busy_slots WHERE id = :id")
    suspend fun deleteBusySlot(id: Long)

    // Subjects
    @Query("SELECT * FROM subject_prefs ORDER BY id ASC")
    fun observeSubjects(): Flow<List<SubjectPrefEntity>>

    @Query("SELECT COUNT(*) FROM subject_prefs")
    suspend fun countSubjects(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectPrefEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectPrefEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectPrefEntity)

    @Query("DELETE FROM subject_prefs WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    // Schedule Sessions
    @Query("SELECT * FROM schedule_sessions ORDER BY sortOrder ASC")
    fun observeScheduleSessions(): Flow<List<ScheduleSessionEntity>>

    @Query("DELETE FROM schedule_sessions")
    suspend fun clearScheduleSessions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleSessions(sessions: List<ScheduleSessionEntity>)

    @Query("UPDATE schedule_sessions SET isCompleted = :completed WHERE id = :id")
    suspend fun updateSessionCompleted(id: Long, completed: Boolean)
}
