package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import com.example.data.db.FlashcardEntity
import com.example.ui.ReviseViewModel
import com.example.ui.followUpAfterQuiz
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.ForestMasteryContainer
import com.example.ui.theme.ForestMasteryContainerDark
import com.example.ui.theme.OchreStreakContainer
import com.example.ui.theme.OchreStreakContainerDark
import com.example.ui.theme.SagePrimaryContainer
import com.example.ui.theme.SagePrimaryContainerDark
import com.example.ui.theme.TerracottaSecondaryContainer
import com.example.ui.theme.TerracottaSecondaryContainerDark
import com.example.ui.theme.adaptiveAlertAccent
import com.example.ui.theme.adaptiveContainer
import com.example.ui.theme.adaptiveMasteryAccent
import com.example.ui.theme.adaptivePrimaryAccent
import com.example.ui.theme.adaptiveSecondaryAccent
import com.example.ui.theme.adaptiveStreakAccent

import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.platform.LocalContext
import com.example.ui.audio.SoundEffectManager
import com.example.ui.theme.EmeraldMastery

data class QuizQuestion(
    val card: FlashcardEntity,
    val options: List<String>,
    val correctIndex: Int
)

@Composable
fun QuizScreen(
    deckId: Long,
    viewModel: ReviseViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundEffectManager(context) }
    var isSoundOn by remember { mutableStateOf(soundManager.isSoundEnabled) }

    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()

    val deck = decks.find { it.id == deckId }
    val deckCards = remember(allCards, deckId) {
        if (deckId > 0) allCards.filter { it.deckId == deckId } else allCards
    }

    var quizSeed by remember { mutableIntStateOf(0) }
    var startTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Randomize flashcards selection & option generation based on quizSeed
    val questions = remember(deckCards, allCards, quizSeed) {
        if (deckCards.isEmpty()) emptyList()
        else {
            val shuffledCards = deckCards.shuffled()
            val allOtherBacks = allCards.map { it.back }.distinct()

            shuffledCards.map { currentCard ->
                val correct = currentCard.back
                val wrongFromDeck = deckCards.filter { it.id != currentCard.id && it.back != correct }.map { it.back }
                val wrongFromAll = allOtherBacks.filter { it != correct && !wrongFromDeck.contains(it) }

                val combinedWrong = (wrongFromDeck.shuffled() + wrongFromAll.shuffled()).distinct().take(3)

                val wrongOptions = if (combinedWrong.size < 3) {
                    val needed = 3 - combinedWrong.size
                    val fallbackPool = listOf("None of the above", "All of the above", "Cannot be determined", "Not applicable")
                    val fallbacks = fallbackPool.filter { it != correct && !combinedWrong.contains(it) }.take(needed)
                    combinedWrong + fallbacks
                } else {
                    combinedWrong
                }

                val allOptions = (wrongOptions + correct).distinct().shuffled()
                val correctIdx = allOptions.indexOf(correct)

                QuizQuestion(currentCard, allOptions, correctIdx)
            }
        }
    }

    var currentQuestionIdx by remember { mutableIntStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isQuizFinished) {
        if (isQuizFinished) {
            soundManager.playCompletionArpeggio()
        }
    }

    val currentQuestion = questions.getOrNull(currentQuestionIdx)
    val progress = if (questions.isNotEmpty()) ((currentQuestionIdx + 1).toFloat() / questions.size.toFloat()) else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (questions.isEmpty()) {
            // Empty State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(adaptiveContainer(SagePrimaryContainer, SagePrimaryContainerDark)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = "Empty Quiz",
                        tint = adaptivePrimaryAccent(),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "No Flashcards Available",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add flashcards to this deck or create cards with AI to start a practice quiz.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }

                        Text(
                            text = deck?.title ?: "Practice Quiz",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(adaptiveContainer(ForestMasteryContainer, ForestMasteryContainerDark))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = adaptiveMasteryAccent(),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$correctCount / ${questions.size}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = adaptiveMasteryAccent()
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
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                if (isQuizFinished || currentQuestion == null) {
                    // Evaluation / Score Screen
                    val total = questions.size
                    val scorePct = if (total > 0) (correctCount * 100) / total else 0
                    val durationSec = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt().coerceAtLeast(1)

                    val (performanceTitle, performanceColor, performanceMsg) = when {
                        scorePct >= 80 -> Triple("Outstanding!", adaptiveMasteryAccent(), "You've mastered these concepts! Excellent job.")
                        scorePct >= 50 -> Triple("Great Effort!", adaptiveStreakAccent(), "Solid performance! A quick review will get you to 100%.")
                        else -> Triple("Keep Practicing!", adaptiveSecondaryAccent(), "Don't give up! Review the flashcards and try again.")
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(adaptiveContainer(OchreStreakContainer, OchreStreakContainerDark)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trophy",
                                    tint = adaptiveStreakAccent(),
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = performanceTitle,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = performanceColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = performanceMsg,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Score breakdown stats card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Score", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$scorePct%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = performanceColor)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Correct", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$correctCount / $total", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${durationSec}s", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Follow-up window the review scheduler will book
                            // when the results are saved.
                            val followUp = followUpAfterQuiz(scorePct)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Saving results schedules a follow-up in ${followUp.first} day" +
                                            (if (followUp.first == 1) "" else "s"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = followUp.second,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Reset quiz state and seed to reshuffle
                                        quizSeed++
                                        currentQuestionIdx = 0
                                        selectedOptionIdx = null
                                        correctCount = 0
                                        isQuizFinished = false
                                        startTimeMs = System.currentTimeMillis()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.recordQuizCompletion(
                                            deckId = deckId,
                                            deckTitle = deck?.title ?: "Quiz",
                                            totalQuestions = questions.size,
                                            correctAnswers = correctCount,
                                            durationSeconds = durationSec
                                        )
                                        onNavigateBack()
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Save Results", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Active Question View
                    AnimatedContent(
                        targetState = currentQuestionIdx,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut())
                        },
                        label = "quiz_question_transition"
                    ) { questionIndex ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            val question = questions.getOrNull(questionIndex) ?: return@AnimatedContent
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "QUESTION ${questionIndex + 1} OF ${questions.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = question.card.front,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 24.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Options List
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                question.options.forEachIndexed { idx, optionText ->
                                    val isSelected = selectedOptionIdx == idx
                                    val isCorrect = idx == question.correctIndex

                                val cardBg = when {
                                    selectedOptionIdx != null && isCorrect -> adaptiveContainer(ForestMasteryContainer, ForestMasteryContainerDark)
                                    selectedOptionIdx != null && isSelected && !isCorrect -> adaptiveContainer(TerracottaSecondaryContainer, TerracottaSecondaryContainerDark)
                                    isSelected -> adaptiveContainer(SagePrimaryContainer, SagePrimaryContainerDark)
                                    else -> MaterialTheme.colorScheme.surface
                                }

                                val borderColor = when {
                                    selectedOptionIdx != null && isCorrect -> adaptiveMasteryAccent()
                                    selectedOptionIdx != null && isSelected && !isCorrect -> adaptiveAlertAccent()
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                }

                                val textColor = when {
                                    selectedOptionIdx != null && isCorrect -> adaptiveMasteryAccent()
                                    selectedOptionIdx != null && isSelected && !isCorrect -> adaptiveAlertAccent()
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                        .clickable(enabled = selectedOptionIdx == null) {
                                            try {
                                                if (isCorrect) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                } else {
                                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                }
                                            } catch (_: Throwable) {}
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                            if (isCorrect) {
                                                soundManager.playSuccessChime()
                                                correctCount++
                                            } else {
                                                soundManager.playCardFlipSound()
                                            }
                                            selectedOptionIdx = idx
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = optionText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (selectedOptionIdx != null) {
                                            if (isCorrect) {
                                                Icon(Icons.Default.Check, contentDescription = "Correct", tint = adaptiveMasteryAccent())
                                            } else if (isSelected) {
                                                Icon(Icons.Default.Close, contentDescription = "Wrong", tint = adaptiveAlertAccent())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    // Bottom Navigation / Next Question Button
                    Button(
                        onClick = {
                            try {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            } catch (_: Throwable) {}
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            selectedOptionIdx = null
                            if (currentQuestionIdx < questions.size - 1) {
                                currentQuestionIdx++
                            } else {
                                isQuizFinished = true
                            }
                        },
                        enabled = selectedOptionIdx != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (currentQuestionIdx < questions.size - 1) "Next Question →" else "Finish Quiz 🎉",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

