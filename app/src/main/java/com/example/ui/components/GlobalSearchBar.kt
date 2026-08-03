package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import com.example.ui.ReviseViewModel
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OchreStreak
import com.example.ui.theme.VioletSecondary

enum class SearchFilterType {
    ALL, FLASHCARDS, DECKS
}

@Composable
fun GlobalSearchBar(
    viewModel: ReviseViewModel,
    onNavigateToReview: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val decks by viewModel.decks.collectAsState()

    GlobalSearchContent(
        searchQuery = searchQuery,
        allCards = allCards,
        decks = decks,
        onQueryChange = { viewModel.setSearchQuery(it) },
        onClearQuery = { viewModel.setSearchQuery("") },
        onNavigateToReview = onNavigateToReview,
        modifier = modifier
    )
}

@Composable
fun GlobalSearchContent(
    searchQuery: String,
    allCards: List<FlashcardEntity>,
    decks: List<DeckEntity>,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onNavigateToReview: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var selectedFilter by remember { mutableStateOf(SearchFilterType.ALL) }
    var selectedCardPreview by remember { mutableStateOf<FlashcardEntity?>(null) }

    val matchedDecks = remember(searchQuery, decks) {
        if (searchQuery.isBlank()) emptyList()
        else decks.filter { deck ->
            deck.title.contains(searchQuery, ignoreCase = true) ||
            deck.description.contains(searchQuery, ignoreCase = true) ||
            deck.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val matchedCards = remember(searchQuery, allCards) {
        if (searchQuery.isBlank()) emptyList()
        else allCards.filter { card ->
            card.front.contains(searchQuery, ignoreCase = true) ||
            card.back.contains(searchQuery, ignoreCase = true) ||
            card.hint.contains(searchQuery, ignoreCase = true)
        }
    }

    val deckMap = remember(decks) {
        decks.associateBy { it.id }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_search_bar_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input_field"),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                placeholder = {
                    Text(
                        text = "Search flashcards, topics, or decks...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                onClearQuery()
                            },
                            modifier = Modifier.testTag("search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                })
            )

            if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilter == SearchFilterType.ALL,
                        onClick = { selectedFilter = SearchFilterType.ALL },
                        label = {
                            Text("All (${matchedCards.size + matchedDecks.size})", fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == SearchFilterType.FLASHCARDS,
                        onClick = { selectedFilter = SearchFilterType.FLASHCARDS },
                        label = {
                            Text("Cards (${matchedCards.size})", fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == SearchFilterType.DECKS,
                        onClick = { selectedFilter = SearchFilterType.DECKS },
                        label = {
                            Text("Decks (${matchedDecks.size})", fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val showCards = selectedFilter == SearchFilterType.ALL || selectedFilter == SearchFilterType.FLASHCARDS
                val showDecks = selectedFilter == SearchFilterType.ALL || selectedFilter == SearchFilterType.DECKS

                val hasAnyResults = (showCards && matchedCards.isNotEmpty()) || (showDecks && matchedDecks.isNotEmpty())

                if (!hasAnyResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "No results",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No matches found for \"$searchQuery\"",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Try different keywords or check your deck names",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showDecks && matchedDecks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "DECKS & TOPICS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = IndigoPrimary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(matchedDecks, key = { "deck_${it.id}" }) { deck ->
                                DeckSearchResultItem(
                                    deck = deck,
                                    onStudyDeck = {
                                        onNavigateToReview(deck.id)
                                    }
                                )
                            }
                        }

                        if (showCards && matchedCards.isNotEmpty()) {
                            item {
                                Text(
                                    text = "MATCHED FLASHCARDS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = IndigoPrimary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            items(matchedCards, key = { "card_${it.id}" }) { card ->
                                val parentDeck = deckMap[card.deckId]
                                CardSearchResultItem(
                                    card = card,
                                    parentDeck = parentDeck,
                                    onCardClick = {
                                        selectedCardPreview = card
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCardPreview?.let { card ->
        val parentDeck = deckMap[card.deckId]
        Dialog(onDismissRequest = { selectedCardPreview = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("flashcard_detail_dialog")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = parentDeck?.title ?: "Flashcard Detail",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { selectedCardPreview = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close dialog"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "QUESTION / FRONT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = card.front,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ANSWER / BACK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldMastery
                    )
                    Text(
                        text = card.back,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    if (card.hint.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Hint: ${card.hint}",
                            fontSize = 13.sp,
                            color = OchreStreak
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedCardPreview = null }) {
                            Text("Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val deckId = card.deckId
                                selectedCardPreview = null
                                onNavigateToReview(deckId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Review Deck")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckSearchResultItem(
    deck: DeckEntity,
    onStudyDeck: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onStudyDeck() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Deck",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = deck.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${deck.category} • ${deck.description}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onStudyDeck,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Study", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CardSearchResultItem(
    card: FlashcardEntity,
    parentDeck: DeckEntity?,
    onCardClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VioletSecondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = parentDeck?.title ?: "Deck",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = VioletSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "Box ${card.boxLevel}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = card.front,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "A: ${card.back}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
