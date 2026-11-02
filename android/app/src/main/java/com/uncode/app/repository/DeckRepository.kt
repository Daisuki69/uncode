package com.uncode.app.repository

import com.uncode.app.data.AppDatabase
import com.uncode.app.data.Deck
import com.uncode.app.data.Flashcard
import com.uncode.app.data.QuizSchedule
import com.uncode.app.data.QuizSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DeckRepository(private val database: AppDatabase) {
    private val deckDao = database.deckDao()
    private val flashcardDao = database.flashcardDao()
    private val sessionDao = database.quizSessionDao()
    private val scheduleDao = database.quizScheduleDao()

    val allDecks: Flow<List<Deck>> = deckDao.getAllDecks()
    val allFlashcards: Flow<List<Flashcard>> = flashcardDao.getAllFlashcards()
    val recentSessions: Flow<List<QuizSession>> = sessionDao.getRecentSessions()
    val allSessions: Flow<List<QuizSession>> = sessionDao.getAllSessions()
    val allSchedules: Flow<List<QuizSchedule>> = scheduleDao.getAllSchedules()
    val upcomingSchedules: Flow<List<QuizSchedule>> = scheduleDao.getUpcomingSchedules()

    fun getDeckFlow(id: Long): Flow<Deck?> = deckDao.getDeckByIdFlow(id)
    suspend fun getDeck(id: Long): Deck? = withContext(Dispatchers.IO) { deckDao.getDeckById(id) }

    fun getFlashcards(deckId: Long): Flow<List<Flashcard>> = flashcardDao.getFlashcardsForDeck(deckId)
    suspend fun getFlashcardsSync(deckId: Long): List<Flashcard> = withContext(Dispatchers.IO) {
        flashcardDao.getFlashcardsForDeckSync(deckId)
    }

    suspend fun getFlashcardsForDecksSync(deckIds: List<Long>): List<Flashcard> = withContext(Dispatchers.IO) {
        flashcardDao.getFlashcardsForDecksSync(deckIds)
    }

    suspend fun allFlashcardsSync(): List<Flashcard> = withContext(Dispatchers.IO) {
        flashcardDao.getAllFlashcardsSync()
    }

    suspend fun insertDeck(deck: Deck): Long = withContext(Dispatchers.IO) {
        deckDao.insertDeck(deck)
    }

    suspend fun updateDeck(deck: Deck) = withContext(Dispatchers.IO) {
        deckDao.updateDeck(deck)
    }

    suspend fun deleteDeck(id: Long) = withContext(Dispatchers.IO) {
        flashcardDao.deleteFlashcardsByDeckId(id)
        deckDao.deleteDeckById(id)
    }

    suspend fun insertFlashcard(flashcard: Flashcard): Long = withContext(Dispatchers.IO) {
        flashcardDao.insertFlashcard(flashcard)
    }

    suspend fun insertFlashcards(flashcards: List<Flashcard>): List<Long> = withContext(Dispatchers.IO) {
        flashcardDao.insertFlashcards(flashcards)
    }

    suspend fun updateFlashcard(flashcard: Flashcard) = withContext(Dispatchers.IO) {
        flashcardDao.updateFlashcard(flashcard)
    }

    suspend fun deleteFlashcard(id: Long) = withContext(Dispatchers.IO) {
        flashcardDao.deleteFlashcardById(id)
    }

    suspend fun saveDeckFlashcards(
        deckId: Long,
        cardsToInsert: List<Flashcard>,
        cardsToUpdate: List<Flashcard>,
        cardIdsToDelete: List<Long>
    ) = withContext(Dispatchers.IO) {
        if (cardIdsToDelete.isNotEmpty()) {
            cardIdsToDelete.forEach { flashcardDao.deleteFlashcardById(it) }
        }
        if (cardsToUpdate.isNotEmpty()) {
            cardsToUpdate.forEach { flashcardDao.updateFlashcard(it) }
        }
        if (cardsToInsert.isNotEmpty()) {
            flashcardDao.insertFlashcards(cardsToInsert)
        }
    }

    suspend fun recordCardReview(cardId: Long, isCorrect: Boolean) = withContext(Dispatchers.IO) {
        val card = flashcardDao.getFlashcardById(cardId) ?: return@withContext
        val newReviewed = card.timesReviewed + 1
        val newCorrect = card.timesCorrect + (if (isCorrect) 1 else 0)
        val accuracy = newCorrect.toFloat() / newReviewed.toFloat()
        val newMastery = when {
            newReviewed >= 3 && accuracy >= 0.8f -> 2
            newReviewed >= 1 -> 1
            else -> 0
        }
        flashcardDao.updateCardStats(cardId, if (isCorrect) 1 else 0, newMastery)
        deckDao.updateLastStudied(card.deckId, System.currentTimeMillis())
    }

    suspend fun saveQuizSession(session: QuizSession): Long = withContext(Dispatchers.IO) {
        sessionDao.insertSession(session)
    }

    suspend fun getSession(id: Long): QuizSession? = withContext(Dispatchers.IO) {
        sessionDao.getSessionById(id)
    }

    suspend fun insertSchedule(schedule: QuizSchedule): Long = withContext(Dispatchers.IO) {
        scheduleDao.insertSchedule(schedule)
    }

    suspend fun setScheduleCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        scheduleDao.setScheduleCompleted(id, completed)
    }

    suspend fun deleteSchedule(id: Long) = withContext(Dispatchers.IO) {
        scheduleDao.deleteScheduleById(id)
    }

    suspend fun getSchedule(id: Long): QuizSchedule? = withContext(Dispatchers.IO) {
        scheduleDao.getScheduleById(id)
    }
}

