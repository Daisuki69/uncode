package com.uncode.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun getDeckById(id: Long): Deck?

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    fun getDeckByIdFlow(id: Long): Flow<Deck?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteDeckById(id: Long)

    @Query("UPDATE decks SET lastStudiedAt = :timestamp WHERE id = :id")
    suspend fun updateLastStudied(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun getDeckCount(): Int
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    fun getFlashcardsForDeck(deckId: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getFlashcardsForDeckSync(deckId: Long): List<Flashcard>

    @Query("SELECT * FROM flashcards WHERE deckId IN (:deckIds)")
    suspend fun getFlashcardsForDecksSync(deckIds: List<Long>): List<Flashcard>

    @Query("SELECT * FROM flashcards")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards")
    suspend fun getAllFlashcardsSync(): List<Flashcard>

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getFlashcardById(id: Long): Flashcard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<Flashcard>): List<Long>

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: Long)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteFlashcardsByDeckId(deckId: Long)

    @Query("UPDATE flashcards SET timesReviewed = timesReviewed + 1, timesCorrect = timesCorrect + :correctDelta, masteryLevel = :mastery WHERE id = :id")
    suspend fun updateCardStats(id: Long, correctDelta: Int, mastery: Int)
}

@Dao
interface QuizSessionDao {
    @Query("SELECT * FROM quiz_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<QuizSession>>

    @Query("SELECT * FROM quiz_sessions ORDER BY timestamp DESC LIMIT 5")
    fun getRecentSessions(): Flow<List<QuizSession>>

    @Query("SELECT * FROM quiz_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): QuizSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: QuizSession): Long

    @Query("DELETE FROM quiz_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}

@Dao
interface QuizScheduleDao {
    @Query("SELECT * FROM quiz_schedules ORDER BY scheduledTimeEpoch ASC")
    fun getAllSchedules(): Flow<List<QuizSchedule>>

    @Query("SELECT * FROM quiz_schedules WHERE isCompleted = 0 ORDER BY scheduledTimeEpoch ASC")
    fun getUpcomingSchedules(): Flow<List<QuizSchedule>>

    @Query("SELECT * FROM quiz_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): QuizSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: QuizSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: QuizSchedule)

    @Query("UPDATE quiz_schedules SET isCompleted = :completed WHERE id = :id")
    suspend fun setScheduleCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM quiz_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)
}

