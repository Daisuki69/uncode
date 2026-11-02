package com.uncode.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String = "",
    val colorHex: String = "#3F51B5",
    val createdAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long? = null
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val termOrQuestion: String,
    val definitionOrAnswer: String,
    val photoUri: String? = null,
    val isAnnotated: Boolean = false,
    val annotationsJson: String = "[]",
    val masteryLevel: Int = 0,
    val timesReviewed: Int = 0,
    val timesCorrect: Int = 0
)

data class AnnotationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetX: Float,
    val targetY: Float,
    val boxX: Float = (targetX + 0.18f).coerceIn(0.05f, 0.85f),
    val boxY: Float = (targetY - 0.1f).coerceIn(0.05f, 0.85f),
    val labelAnswer: String = "",
    val connectorStyle: String = "ARROW",
    val isBoxAttached: Boolean = true,
    val isCornerInverted: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("targetX", targetX.toDouble())
        json.put("targetY", targetY.toDouble())
        json.put("boxX", boxX.toDouble())
        json.put("boxY", boxY.toDouble())
        json.put("labelAnswer", labelAnswer)
        json.put("connectorStyle", connectorStyle)
        json.put("isBoxAttached", isBoxAttached)
        json.put("isCornerInverted", isCornerInverted)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): AnnotationItem {
            val isCornerInverted = json.optBoolean("isCornerInverted", false)
            return AnnotationItem(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                targetX = json.optDouble("targetX", 0.5).toFloat(),
                targetY = json.optDouble("targetY", 0.5).toFloat(),
                boxX = json.optDouble("boxX", 0.68).toFloat(),
                boxY = json.optDouble("boxY", 0.4).toFloat(),
                labelAnswer = json.optString("labelAnswer", ""),
                connectorStyle = json.optString("connectorStyle", "ARROW"),
                isBoxAttached = json.optBoolean("isBoxAttached", json.optString("labelAnswer", "").isNotBlank()),
                isCornerInverted = isCornerInverted
            )
        }

        fun parseList(jsonString: String): List<AnnotationItem> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<AnnotationItem>()
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun toJsonString(list: List<AnnotationItem>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}

@Entity(tableName = "quiz_schedules")
data class QuizSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val deckIdsJson: String,
    val deckNames: String,
    val combinationMode: String = "SINGLE",
    val scheduledTimeEpoch: Long,
    val targetQuestionCount: Int = 10,
    val repeatType: String = "ONCE",
    val reminderNote: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_sessions")
data class QuizSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckIdsJson: String,
    val deckNames: String,
    val combinationMode: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val scorePercent: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val answerLogsJson: String = "[]"
)

data class QuestionReviewLog(
    val cardId: Long,
    val question: String,
    val correctAnswer: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val deckName: String,
    val photoUri: String? = null,
    val isAnnotated: Boolean = false,
    val annotationsJson: String = "[]"
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("cardId", cardId)
        json.put("question", question)
        json.put("correctAnswer", correctAnswer)
        json.put("userAnswer", userAnswer)
        json.put("isCorrect", isCorrect)
        json.put("deckName", deckName)
        json.put("photoUri", photoUri ?: "")
        json.put("isAnnotated", isAnnotated)
        json.put("annotationsJson", annotationsJson)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): QuestionReviewLog {
            return QuestionReviewLog(
                cardId = json.optLong("cardId"),
                question = json.optString("question"),
                correctAnswer = json.optString("correctAnswer"),
                userAnswer = json.optString("userAnswer"),
                isCorrect = json.optBoolean("isCorrect"),
                deckName = json.optString("deckName"),
                photoUri = json.optString("photoUri").takeIf { it.isNotBlank() },
                isAnnotated = json.optBoolean("isAnnotated"),
                annotationsJson = json.optString("annotationsJson", "[]")
            )
        }

        fun parseList(jsonString: String): List<QuestionReviewLog> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<QuestionReviewLog>()
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun toJsonString(list: List<QuestionReviewLog>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}

