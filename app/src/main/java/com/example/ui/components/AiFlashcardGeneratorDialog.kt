package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DeckEntity
import com.example.data.db.FolderEntity
import com.example.ui.AiGenerationState
import com.example.ui.ReviseViewModel
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.VioletSecondary

/**
 * Gemini AI flashcard generator dialog, reusable from the Decks tab and the
 * Dashboard's "AI Generate" tile. The parent decides visibility; dismissal
 * resets the view model's AI state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFlashcardGeneratorDialog(
    viewModel: ReviseViewModel,
    decks: List<DeckEntity>,
    folders: List<FolderEntity>,
    aiState: AiGenerationState,
    onDismiss: () -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var selectedCardCount by remember { mutableStateOf(5) }
    var deckName by remember { mutableStateOf("") }
    var useExistingDeck by remember { mutableStateOf(false) }
    var selectedDeckId by remember { mutableStateOf(0L) }
    var deckDropdownExpanded by remember { mutableStateOf(false) }
    var folderDropdownExpanded by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf(0L) }
    var isCreatingNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val isSuccess = aiState is AiGenerationState.Success

    val dismiss = {
        viewModel.resetAiState()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = VioletSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini AI Flashcards", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter any topic, chapter summary, or paste study notes. Gemini AI will build flashcards from it.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic or Study Notes") },
                    placeholder = { Text("e.g. Operating Systems Paging and Virtual Memory") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Card count: quick chips + fine-tune stepper
                Text(
                    text = "Number of cards",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { n ->
                        FilterChip(
                            selected = selectedCardCount == n,
                            onClick = { selectedCardCount = n },
                            label = { Text("$n") }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedIconButton(
                        onClick = { selectedCardCount = (selectedCardCount - 1).coerceAtLeast(3) },
                        enabled = selectedCardCount > 3
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Fewer cards")
                    }
                    Text(
                        text = "$selectedCardCount",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                    OutlinedIconButton(
                        onClick = { selectedCardCount = (selectedCardCount + 1).coerceAtMost(30) },
                        enabled = selectedCardCount < 30
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "More cards")
                    }
                }

                // Destination: brand-new deck vs an existing one (never silent)
                Text(
                    text = "Save cards to",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useExistingDeck,
                        onClick = { useExistingDeck = false },
                        label = { Text("New deck") }
                    )
                    FilterChip(
                        selected = useExistingDeck,
                        onClick = { useExistingDeck = true },
                        label = { Text("Existing deck") },
                        enabled = decks.isNotEmpty()
                    )
                }

                if (useExistingDeck) {
                    ExposedDropdownMenuBox(
                        expanded = deckDropdownExpanded,
                        onExpandedChange = { deckDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = decks.find { it.id == selectedDeckId }?.title ?: "Choose a deck",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deckDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = deckDropdownExpanded,
                            onDismissRequest = { deckDropdownExpanded = false }
                        ) {
                            decks.forEach { deck ->
                                DropdownMenuItem(
                                    text = { Text(deck.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        selectedDeckId = deck.id
                                        deckDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = deckName,
                        onValueChange = { deckName = it },
                        label = { Text("Deck name") },
                        placeholder = { Text(topic.trim().take(40).ifBlank { "New AI deck" }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Save-to-folder picker (creates a subfolder when needed)
                    if (isCreatingNewFolder) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("New folder name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = { isCreatingNewFolder = false }) {
                            Text("Pick existing folder instead")
                        }
                    } else {
                        FolderPicker(
                            folders = folders,
                            selectedFolderId = selectedFolderId,
                            onFolderSelected = { selectedFolderId = it },
                            expanded = folderDropdownExpanded,
                            onExpandedChange = { folderDropdownExpanded = it }
                        )
                        TextButton(onClick = { isCreatingNewFolder = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create new folder for this deck")
                        }
                    }
                }

                when (aiState) {
                    is AiGenerationState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = VioletSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Generating cards with Gemini AI...", fontSize = 13.sp)
                        }
                    }
                    is AiGenerationState.Success -> {
                        val count = (aiState as AiGenerationState.Success).cards.size
                        Text(
                            text = "✨ Generated $count flashcards successfully!",
                            fontSize = 13.sp,
                            color = EmeraldMastery,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is AiGenerationState.Error -> {
                        Text(
                            text = (aiState as AiGenerationState.Error).message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Once generated, the button becomes "Done" and MUST NOT re-generate.
                    if (isSuccess) {
                        dismiss()
                    } else if (topic.isNotBlank()) {
                        if (useExistingDeck) {
                            val existingId = if (selectedDeckId > 0) selectedDeckId else decks.firstOrNull()?.id
                            if (existingId != null && existingId > 0) {
                                viewModel.generateAiFlashcards(topic.trim(), existingId, selectedCardCount)
                            }
                        } else {
                            val folderIdToUse = when {
                                isCreatingNewFolder && newFolderName.isNotBlank() -> {
                                    viewModel.createFolder(newFolderName)
                                }
                                else -> selectedFolderId
                            }
                            val finalDeckName = deckName.trim().take(40)
                                .ifBlank { topic.trim().take(40) }
                                .ifBlank { "New AI deck" }
                            val newDeckId = viewModel.createDeck(
                                finalDeckName,
                                "Generated by Gemini AI",
                                "AI Created",
                                "#7C3AED",
                                folderIdToUse
                            )
                            viewModel.generateAiFlashcards(topic.trim(), newDeckId, selectedCardCount)
                        }
                    }
                },
                enabled = aiState !is AiGenerationState.Loading && (isSuccess || topic.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = VioletSecondary)
            ) {
                Text(if (isSuccess) "Done" else "Generate Cards")
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPicker(
    folders: List<FolderEntity>,
    selectedFolderId: Long,
    onFolderSelected: (Long) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Save to Folder",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = if (selectedFolderId == 0L) "Uncategorized" else (folders.find { it.id == selectedFolderId }?.name ?: "Uncategorized"),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Uncategorized") },
                    onClick = {
                        onFolderSelected(0L)
                        onExpandedChange(false)
                    }
                )
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = { Text(folder.name) },
                        onClick = {
                            onFolderSelected(folder.id)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}
