package com.example

import com.example.data.db.DailyStreakEntity
import com.example.data.spacedrepetition.ReviewRating
import com.example.ui.calculateWeeklyStudyHours
import com.example.ui.followUpAfterQuiz
import com.example.ui.followUpAfterRatings
import com.example.ui.reviewSessionSummaryOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StudySchedulingTest {

    private fun at(mondayDate: String): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.timeInMillis = fmt.parse(mondayDate)!!.time
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun streak(date: String, minutes: Int) =
        DailyStreakEntity(dateString = date, cardsReviewed = 0, studyDurationMinutes = minutes, targetMet = false)

    // --- Weekly Mon-Sun calculation ---

    @Test
    fun weeklyProgress_countsOnlyCurrentWeek() {
        val now = at("2026-07-27") // Monday
        val streaks = listOf(
            streak("2026-07-27", 30), // Monday (current week)
            streak("2026-08-01", 60), // Saturday (current week)
            streak("2026-07-26", 240), // Sunday (previous week) -> excluded
            streak("2026-08-09", 120) // Sunday (next week) -> excluded
        )
        assertEquals(1.5f, calculateWeeklyStudyHours(streaks, nowMillis = now), 0.001f)
    }

    @Test
    fun weeklyProgress_onSunday_includesSundayInCurrentWeek() {
        // Regression: with a Sunday-first locale, the week must still run Mon-Sun
        // and include the current Sunday's minutes.
        val now = at("2026-08-02") // Sunday
        val streaks = listOf(
            streak("2026-07-27", 30), // Monday of this week
            streak("2026-08-02", 30)  // Sunday of this week
        )
        assertEquals(1.0f, calculateWeeklyStudyHours(streaks, nowMillis = now), 0.001f)
    }

    @Test
    fun weeklyProgress_onSunday_excludesNextWeek() {
        val now = at("2026-08-02") // Sunday
        val streaks = listOf(
            streak("2026-08-03", 60) // Monday of the NEXT week -> excluded
        )
        assertEquals(0f, calculateWeeklyStudyHours(streaks, nowMillis = now), 0.001f)
    }

    @Test
    fun weeklyProgress_resetsToZeroOnMondayWithNoStreaks() {
        val now = at("2026-07-27")
        assertEquals(0f, calculateWeeklyStudyHours(emptyList(), nowMillis = now), 0.001f)
    }

    @Test
    fun weeklyProgress_roundsToOneDecimal() {
        val now = at("2026-07-27")
        assertEquals(0.8f, calculateWeeklyStudyHours(listOf(streak("2026-07-28", 47)), nowMillis = now), 0.001f)
    }

    // --- Smart review scheduling ---

    @Test
    fun smartSchedule_emptyRatingsSchedulesNothing() {
        assertNull(followUpAfterRatings(emptyList()))
    }

    @Test
    fun smartSchedule_goodDominant_schedulesTwoDays() {
        val (days, topic) = followUpAfterRatings(
            listOf(ReviewRating.GOOD, ReviewRating.GOOD, ReviewRating.HARD, ReviewRating.EASY)
        )!!
        assertEquals(2, days)
        assertEquals("Standard spaced review (smart schedule)", topic)
    }

    @Test
    fun smartSchedule_hardOrAgainDominant_schedulesOneDay() {
        val (days, topic) = followUpAfterRatings(
            listOf(ReviewRating.HARD, ReviewRating.HARD, ReviewRating.AGAIN, ReviewRating.GOOD)
        )!!
        assertEquals(1, days)
        assertEquals("Re-review hard/again cards (smart schedule)", topic)
    }

    @Test
    fun smartSchedule_easyDominant_schedulesThreeDays() {
        val (days, topic) = followUpAfterRatings(
            listOf(ReviewRating.EASY, ReviewRating.EASY, ReviewRating.GOOD)
        )!!
        assertEquals(3, days)
        assertEquals("Light review of easy cards (smart schedule)", topic)
    }

    @Test
    fun smartSchedule_againDominant_schedulesOneDay() {
        val (days, _) = followUpAfterRatings(listOf(ReviewRating.AGAIN))!!
        assertEquals(1, days)
    }

    // --- Quiz follow-up ---

    @Test
    fun quizFollowUp_belowSixty_schedulesOneDay() {
        val (days, topic) = followUpAfterQuiz(45)
        assertEquals(1, days)
        assertEquals("Re-practice quiz concepts (scored 45%)", topic)
    }

    @Test
    fun quizFollowUp_sixtyOrAbove_schedulesTwoDays() {
        val (days, topic) = followUpAfterQuiz(60)
        assertEquals(2, days)
        assertEquals("Solidify quiz topics (scored 60%)", topic)
    }

    @Test
    fun quizFollowUp_hundred_schedulesTwoDays() {
        val (days, _) = followUpAfterQuiz(100)
        assertEquals(2, days)
    }

    // --- Review session summary ---

    @Test
    fun reviewSummary_emptyRatings_returnsNull() {
        assertNull(reviewSessionSummaryOf(emptyList()))
    }

    @Test
    fun reviewSummary_countsRatingsAndFollowUp() {
        val summary = reviewSessionSummaryOf(
            listOf(
                ReviewRating.AGAIN, ReviewRating.HARD,
                ReviewRating.GOOD, ReviewRating.GOOD,
                ReviewRating.EASY
            )
        )!!
        assertEquals(5, summary.totalReviewed)
        assertEquals(1, summary.againCount)
        assertEquals(1, summary.hardCount)
        assertEquals(2, summary.goodCount)
        assertEquals(1, summary.easyCount)
        assertEquals(2, summary.followUpDays)
    }

    @Test
    fun reviewSummary_hardDominant_schedulesOneDay() {
        val summary = reviewSessionSummaryOf(
            listOf(ReviewRating.HARD, ReviewRating.HARD, ReviewRating.GOOD)
        )!!
        assertEquals(1, summary.followUpDays)
    }
}
