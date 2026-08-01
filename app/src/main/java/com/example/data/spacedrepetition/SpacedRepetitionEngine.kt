package com.example.data.spacedrepetition

import com.example.data.db.FlashcardEntity
import java.util.concurrent.TimeUnit

enum class ReviewRating {
    AGAIN, HARD, GOOD, EASY
}

data class SpacedRepetitionResult(
    val updatedCard: FlashcardEntity,
    val nextIntervalDays: Int
)

object SpacedRepetitionEngine {

    /**
     * Calculates the next review date and spaced repetition parameters based on SM-2 algorithm.
     */
    fun calculateNextReview(
        card: FlashcardEntity,
        rating: ReviewRating,
        nowMillis: Long = System.currentTimeMillis()
    ): SpacedRepetitionResult {
        var repetitions = card.repetitions
        var easeFactor = card.easeFactor
        var intervalDays = card.intervalDays
        var boxLevel = card.boxLevel

        val q = when (rating) {
            ReviewRating.AGAIN -> 0
            ReviewRating.HARD -> 3
            ReviewRating.GOOD -> 4
            ReviewRating.EASY -> 5
        }

        // Calculate new Ease Factor (EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
        easeFactor += (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (easeFactor < 1.3f) easeFactor = 1.3f

        if (q < 3) {
            // Failed recall
            repetitions = 0
            intervalDays = 1
            boxLevel = 1
        } else {
            // Successful recall
            repetitions += 1
            boxLevel = (boxLevel + 1).coerceAtMost(5)

            intervalDays = when (repetitions) {
                1 -> 1
                2 -> 3
                else -> {
                    val baseInterval = (intervalDays * easeFactor).toInt()
                    if (rating == ReviewRating.EASY) {
                        (baseInterval * 1.3f).toInt()
                    } else {
                        baseInterval
                    }
                }
            }
        }

        // Bound max interval to 180 days for reasonable recall checks
        intervalDays = intervalDays.coerceIn(1, 180)

        val nextReviewDate = nowMillis + TimeUnit.DAYS.toMillis(intervalDays.toLong())

        val updatedCard = card.copy(
            boxLevel = boxLevel,
            intervalDays = intervalDays,
            repetitions = repetitions,
            easeFactor = easeFactor,
            lastReviewed = nowMillis,
            nextReviewDate = nextReviewDate
        )

        return SpacedRepetitionResult(updatedCard, intervalDays)
    }
}
