package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getActiveUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_threads ORDER BY timestamp DESC")
    fun getAllThreadsFlow(): Flow<List<ChatThread>>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThreadFlow(threadId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    suspend fun getMessagesForThread(threadId: String): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThread)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_threads WHERE threadId = :threadId")
    suspend fun deleteThreadById(threadId: String)

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    suspend fun deleteMessagesByThreadId(threadId: String)

    @Transaction
    suspend fun deleteThreadAndMessages(threadId: String) {
        deleteMessagesByThreadId(threadId)
        deleteThreadById(threadId)
    }
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_histories ORDER BY timestamp DESC")
    fun getQuizHistoriesFlow(): Flow<List<QuizHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizHistory(quizHistory: QuizHistory)

    @Query("DELETE FROM quiz_histories WHERE id = :quizId")
    suspend fun deleteQuizById(quizId: Int)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM study_notes ORDER BY timestamp DESC")
    fun getAllNotesFlow(): Flow<List<StudyNote>>

    @Query("SELECT * FROM study_notes WHERE noteId = :noteId")
    suspend fun getNoteById(noteId: Int): StudyNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNote): Long

    @Query("DELETE FROM study_notes WHERE noteId = :noteId")
    suspend fun deleteNoteById(noteId: Int)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM study_schedules WHERE dateString = :dateString ORDER BY timeLabel ASC")
    fun getSchedulesForDateFlow(dateString: String): Flow<List<StudySchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: StudySchedule)

    @Update
    suspend fun updateSchedule(schedule: StudySchedule)

    @Query("DELETE FROM study_schedules WHERE scheduleId = :id")
    suspend fun deleteScheduleById(id: Int)
}
