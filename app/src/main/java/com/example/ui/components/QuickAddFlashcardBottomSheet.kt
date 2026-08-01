package com.example.ui.components

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReviseViewModel
import com.example.ui.audio.SoundEffectManager
import com.example.ui.theme.CyanAI
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddFlashcardBottomSheet(
    viewModel: ReviseViewModel,
    onDismiss: () -> Unit,
    initialDeckId: Long? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundEffectManager(context) }
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val decks by viewModel.decks.collectAsState()

    var selectedDeckId by remember(decks, initialDeckId) {
        val defaultId = initialDeckId ?: decks.firstOrNull()?.id ?: 0L
        mutableLongStateOf(defaultId)
    }

    var isCreatingNewDeck by remember { mutableStateOf(decks.isEmpty()) }
    var newDeckTitle by remember { mutableStateOf("") }
    var cardFront by remember { mutableStateOf("") }
    var cardBack by remember { mutableStateOf("") }
    var cardHint by remember { mutableStateOf("") }
    var isDeckDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("quick_add_card_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Quick Add Flashcard ⚡",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Instant creation from anywhere in ReviseIQ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Select or Create Deck Section
            Text(
                text = "TARGET DECK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (isCreatingNewDeck || decks.isEmpty()) {
                OutlinedTextField(
                    value = newDeckTitle,
                    onValueChange = { newDeckTitle = it },
                    placeholder = { Text("Enter new deck title (e.g. Science 101)") },
                    leadingIcon = {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = IndigoPrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_deck_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )
                if (decks.isNotEmpty()) {
                    TextButton(
                        onClick = { isCreatingNewDeck = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Choose existing deck instead", fontSize = 12.sp)
                    }
                }
            } else {
                val currentSelectedDeck = decks.find { it.id == selectedDeckId } ?: decks.firstOrNull()

                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { isDeckDropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currentSelectedDeck?.title ?: "Select Deck",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Change ▾",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = isDeckDropdownExpanded,
                        onDismissRequest = { isDeckDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        decks.forEach { deck ->
                            DropdownMenuItem(
                                text = { Text(deck.title, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = IndigoPrimary) },
                                onClick = {
                                    selectedDeckId = deck.id
                                    isDeckDropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Create New Deck", fontWeight = FontWeight.Bold, color = EmeraldMastery) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldMastery) },
                            onClick = {
                                isCreatingNewDeck = true
                                isDeckDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Card Front (Question / Term)
            Text(
                text = "FRONT (QUESTION / CONCEPT)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = cardFront,
                onValueChange = { cardFront = it },
                placeholder = { Text("What is Photosynthesis?") },
                leadingIcon = {
                    Icon(Icons.Default.Style, contentDescription = null, tint = IndigoPrimary)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_front_input"),
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card Back (Answer)
            Text(
                text = "BACK (ANSWER / EXPLANATION)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = cardBack,
                onValueChange = { cardBack = it },
                placeholder = { Text("The process by which plants convert light energy into chemical energy...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_back_input"),
                shape = RoundedCornerShape(14.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card Hint (Optional)
            Text(
                text = "HINT (OPTIONAL)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = cardHint,
                onValueChange = { cardHint = it },
                placeholder = { Text("e.g. Involves chlorophyll and sunlight") },
                leadingIcon = {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_hint_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF59E0B),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    if (cardFront.isBlank() || cardBack.isBlank()) {
                        Toast.makeText(context, "Please enter both front and back text", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    try {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } catch (_: Throwable) {}
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    soundManager.playSuccessChime()

                    scope.launch {
                        if (isCreatingNewDeck || decks.isEmpty()) {
                            val titleToUse = newDeckTitle.ifBlank { "General Deck" }
                            viewModel.createDeck(
                                title = titleToUse,
                                description = "Created via Quick Add",
                                category = "General",
                                colorHex = "#4F46E5"
                            )
                            // Retrieve updated decks or delay briefly
                            kotlinx.coroutines.delay(100)
                            val targetDeck = viewModel.decks.value.find { it.title == titleToUse }
                            val targetId = targetDeck?.id ?: 1L
                            viewModel.addFlashcard(targetId, cardFront.trim(), cardBack.trim(), cardHint.trim())
                        } else {
                            viewModel.addFlashcard(selectedDeckId, cardFront.trim(), cardBack.trim(), cardHint.trim())
                        }

                        Toast.makeText(context, "Flashcard created instantly! ✨", Toast.LENGTH_SHORT).show()
                        sheetState.hide()
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_quick_add_card_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Flashcard Now ⚡",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
