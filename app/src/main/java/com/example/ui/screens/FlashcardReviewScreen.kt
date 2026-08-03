package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import com.example.data.spacedrepetition.ReviewRating
import com.example.ui.ReviseViewModel
import com.example.ui.ReviewSessionSummary
import com.example.ui.components.Flashcard3DCard
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanAI
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseAlert

import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.ui.audio.SoundEffectManager

@Composable
fun FlashcardReviewScreen(
    deckId: Long,
    viewModel: ReviseViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundEffectManager(context) }
    var isSoundOn by remember { mutableStateOf(soundManager.isSoundEnabled) }

    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val allCards by viewModel.allCards.collectAsState()
    val aiExplanation by viewModel.aiExplanation.collectAsState()

    val deckTitle = viewModel.decks.collectAsState().value.firstOrNull { it.id == deckId }?.title
        ?: if (deckId > 0) "Deck $deckId" else "All Cards"

    val sessionStartMs = remember { System.currentTimeMillis() }
    var sessionSummary by remember { mutableStateOf<ReviewSessionSummary?>(null) }

    fun finishReviewSession() {
        val elapsedMinutes = ((System.currentTimeMillis() - sessionStartMs) / 60000L).toInt().coerceAtLeast(1)
        val summary = viewModel.smartScheduleReviewSession(deckId, deckTitle, elapsedMinutes)
        if (summary == null) {
            onNavigateBack()
        } else {
            sessionSummary = summary
        }
    }

    // System back button must end the session the same way the UI back arrow does.
    BackHandler { finishReviewSession() }

    // Only cards due at or before NOW are part of the session; future-dated
    // cards stay hidden so a deck review doesn't preview tomorrow's cards.
    val reviewCards = remember(allCards, deckId) {
        val now = System.currentTimeMillis()
        allCards.filter {
            it.nextReviewDate <= now && (deckId <= 0 || it.deckId == deckId)
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isCardFlipped by remember { mutableStateOf(false) }
    var showAiExplanationModal by remember { mutableStateOf(false) }

    val currentCard = reviewCards.getOrNull(currentIndex)
    val progress = if (reviewCards.isNotEmpty()) (currentIndex.toFloat() / reviewCards.size.toFloat()) else 1f

    // Play completion arpeggio when user finishes reviewing all cards in session
    LaunchedEffect(reviewCards, currentIndex) {
        if (reviewCards.isNotEmpty() && currentIndex >= reviewCards.size) {
            soundManager.playCompletionArpeggio()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { finishReviewSession() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    Text(
                        text = "Card ${if (reviewCards.isNotEmpty()) currentIndex + 1 else 0} of ${reviewCards.size}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isSoundOn = !isSoundOn
                                soundManager.isSoundEnabled = isSoundOn
                                if (isSoundOn) soundManager.playSuccessChime()
                            }
                        ) {
                            Icon(
                                imageVector = if (isSoundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Toggle Audio Effects",
                                tint = if (isSoundOn) EmeraldMastery else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (currentCard != null) {
                            IconButton(
                                onClick = {
                                    viewModel.explainConceptWithAi(currentCard.front, currentCard.back)
                                    showAiExplanationModal = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Explain",
                                    tint = CyanAI,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = IndigoPrimary,
                    trackColor = Color(0x336366F1)
                )
            }

            // Main Flashcard View
            if (reviewCards.isEmpty()) {
                // Nothing is due right now — this is NOT a completed session.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "All caught up",
                            tint = EmeraldMastery,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Caught Up!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No cards are due for review right now. Check back later, or take a quiz to keep your streak going!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { finishReviewSession() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Back to Dashboard")
                        }
                    }
                }
            } else if (currentCard == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = EmeraldMastery,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Revision Session Complete!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You've reviewed all scheduled cards for this session. Excellent work!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { finishReviewSession() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Back to Dashboard")
                        }
                    }
                }
            } else {
                Flashcard3DCard(
                    frontText = currentCard.front,
                    backText = currentCard.back,
                    hintText = currentCard.hint,
                    isFlipped = isCardFlipped,
                    onFlip = { isCardFlipped = !isCardFlipped }
                )

                // Rating Buttons (SM-2 Spaced Repetition controls)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isCardFlipped) {
                        Button(
                            onClick = {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isCardFlipped = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Reveal Answer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "How easily did you recall this?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RatingButton(
                                label = "Again",
                                intervalText = "1d",
                                color = RoseAlert,
                                modifier = Modifier.weight(1f)
                            ) {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.playSuccessChime()
                                viewModel.reviewCard(currentCard, ReviewRating.AGAIN)
                                isCardFlipped = false
                                if (currentIndex < reviewCards.size - 1) currentIndex++ else currentIndex = reviewCards.size
                            }

                            RatingButton(
                                label = "Hard",
                                intervalText = "${cardInterval(currentCard, ReviewRating.HARD)}d",
                                color = AmberStreak,
                                modifier = Modifier.weight(1f)
                            ) {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.playSuccessChime()
                                viewModel.reviewCard(currentCard, ReviewRating.HARD)
                                isCardFlipped = false
                                if (currentIndex < reviewCards.size - 1) currentIndex++ else currentIndex = reviewCards.size
                            }

                            RatingButton(
                                label = "Good",
                                intervalText = "${cardInterval(currentCard, ReviewRating.GOOD)}d",
                                color = IndigoPrimary,
                                modifier = Modifier.weight(1f)
                            ) {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.playSuccessChime()
                                viewModel.reviewCard(currentCard, ReviewRating.GOOD)
                                isCardFlipped = false
                                if (currentIndex < reviewCards.size - 1) currentIndex++ else currentIndex = reviewCards.size
                            }

                            RatingButton(
                                label = "Easy",
                                intervalText = "${cardInterval(currentCard, ReviewRating.EASY)}d",
                                color = EmeraldMastery,
                                modifier = Modifier.weight(1f)
                            ) {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager.playSuccessChime()
                                viewModel.reviewCard(currentCard, ReviewRating.EASY)
                                isCardFlipped = false
                                if (currentIndex < reviewCards.size - 1) currentIndex++ else currentIndex = reviewCards.size
                            }
                        }
                    }
                }
            }
        }
    }

    // AI Tutor / Explainer Modal
    if (showAiExplanationModal) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearAiExplanation()
                showAiExplanationModal = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = CyanAI,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini AI Tutor Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (aiExplanation == null || aiExplanation?.startsWith("Asking") == true) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = CyanAI)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Gemini AI is analyzing this card...", fontSize = 13.sp)
                        }
                    } else {
                        Text(
                            text = aiExplanation ?: "",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAiExplanation()
                        showAiExplanationModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Got it!")
                }
            }
        )
    }

    // Session summary — shown once after the user finishes reviewing cards.
    sessionSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = {
                sessionSummary = null
                onNavigateBack()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = EmeraldMastery,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Session Complete", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "You reviewed ${summary.totalReviewed} " +
                            if (summary.totalReviewed == 1) "card" else "cards",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryCount(label = "Again", count = summary.againCount, color = RoseAlert)
                        SummaryCount(label = "Hard", count = summary.hardCount, color = AmberStreak)
                        SummaryCount(label = "Good", count = summary.goodCount, color = IndigoPrimary)
                        SummaryCount(label = "Easy", count = summary.easyCount, color = EmeraldMastery)
                    }
                    summary.followUpDays?.let { days ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Follow-up session scheduled in " +
                                if (days == 1) "1 day" else "$days days",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionSummary = null
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SummaryCount(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$count", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun cardInterval(card: com.example.data.db.FlashcardEntity, rating: ReviewRating): Int {
    val res = com.example.data.spacedrepetition.SpacedRepetitionEngine.calculateNextReview(card, rating)
    return res.nextIntervalDays
}

@Composable
fun RatingButton(
    label: String,
    intervalText: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "+$intervalText", fontSize = 10.sp, color = Color(0xEEFFFFFF))
        }
    }
}
