package com.example.ui

import com.example.data.db.DailyStreakEntity
import com.example.data.spacedrepetition.ReviewRating
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Follow-up windows (days) per dominant rating: Hard/Again=1d, Good/Medium=2d, Easy=3d
private val followUpWindows: Map<ReviewRating, Int> = mapOf(
    ReviewRating.AGAIN to 1,
    ReviewRating.HARD to 1,
    ReviewRating.GOOD to 2,
    ReviewRating.EASY to 3
)

/**
 * Picks the dominant rating from a review session and maps it to a follow-up
 * window + focus topic. Returns null when there are no ratings (nothing to schedule).
 */
internal fun followUpAfterRatings(ratings: List<ReviewRating>): Pair<Int, String>? {
    if (ratings.isEmpty()) return null
    val dominant = ratings
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key ?: ReviewRating.GOOD
    val days = followUpWindows[dominant] ?: 2
    val focusTopic = when (dominant) {
        ReviewRating.AGAIN, ReviewRating.HARD -> "Re-review hard/again cards (smart schedule)"
        ReviewRating.EASY -> "Light review of easy cards (smart schedule)"
        else -> "Standard spaced review (smart schedule)"
    }
    return days to focusTopic
}

/**
 * Follow-up window + focus topic after a quiz, keyed on score: <60% → 1 day, else 2 days.
 */
internal fun followUpAfterQuiz(scorePct: Int): Pair<Int, String> =
    if (scorePct < 60) {
        1 to "Re-practice quiz concepts (scored $scorePct%)"
    } else {
        2 to "Solidify quiz topics (scored $scorePct%)"
    }

data class ReviewSessionSummary(
    val totalReviewed: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int,
    val followUpDays: Int?
)

/**
 * Builds a review-session summary from the session's ratings.
 * Returns null when nothing was rated (no session happened).
 */
internal fun reviewSessionSummaryOf(ratings: List<ReviewRating>): ReviewSessionSummary? {
    if (ratings.isEmpty()) return null
    val followUpDays = followUpAfterRatings(ratings)?.first
    return ReviewSessionSummary(
        totalReviewed = ratings.size,
        againCount = ratings.count { it == ReviewRating.AGAIN },
        hardCount = ratings.count { it == ReviewRating.HARD },
        goodCount = ratings.count { it == ReviewRating.GOOD },
        easyCount = ratings.count { it == ReviewRating.EASY },
        followUpDays = followUpDays
    )
}

/**
 * Total studied hours for the current Mon-Sun week only, so the weekly target
 * resets every Monday. Rounds to 0.1h.
 *
 * Uses an explicit Monday-first week so Sundays stay in the current week
 * regardless of the device locale's first day of week.
 */
internal fun calculateWeeklyStudyHours(
    streaks: List<DailyStreakEntity>,
    nowMillis: Long
): Float {
    val weekStart = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
    val weekKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val weekDays = mutableSetOf<String>()
    var c = weekStart.clone() as Calendar
    while (c.before(weekEnd)) {
        weekDays.add(weekKeyFormat.format(c.time))
        c.add(Calendar.DAY_OF_YEAR, 1)
    }
    val totalMins = streaks
        .filter { weekDays.contains(it.dateString) }
        .sumOf { it.studyDurationMinutes }
    return (kotlin.math.round((totalMins / 60.0f) * 10f) / 10f)
}
