package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.db.DeckEntity
import com.example.data.db.FolderEntity
import com.example.ui.AiGenerationState
import com.example.ui.ReviseViewModel
import com.example.ui.components.AiFlashcardGeneratorDialog
import com.example.ui.components.FolderPicker
import com.example.ui.theme.CyanAI
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    viewModel: ReviseViewModel,
    onNavigateToDeckDetail: (Long) -> Unit,
    onNavigateToReview: (Long) -> Unit,
    onNavigateToQuiz: (Long) -> Unit
) {
    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val context = LocalContext.current

    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderPendingAction by remember { mutableStateOf<FolderEntity?>(null) }
    var deckPendingDelete by remember { mutableStateOf<DeckEntity?>(null) }
    var selectedFolderId by remember { mutableStateOf(0L) }

    // Per-deck card counts cached once per data change
    val deckCardCounts = remember(allCards) {
        allCards.groupingBy { it.deckId }.eachCount()
    }
    val deckDueCounts = remember(allCards) {
        val now = System.currentTimeMillis()
        allCards.filter { it.nextReviewDate <= now }.groupingBy { it.deckId }.eachCount()
    }

    val visibleDecks = if (selectedFolderId == 0L) decks else decks.filter { it.folderId == selectedFolderId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Flashcard Decks",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${decks.size} decks • ${folders.size} folders",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAiGeneratorDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletSecondary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Generate",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Deck", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Folder chips row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FolderChip(
                        label = "All Decks",
                        isSelected = selectedFolderId == 0L,
                        onClick = { selectedFolderId = 0L },
                        icon = Icons.Default.FolderOpen
                    )
                }
                items(folders, key = { it.id }) { folder ->
                    FolderChip(
                        label = folder.name,
                        isSelected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        icon = Icons.Default.Folder,
                        onLongClick = { folderPendingAction = folder }
                    )
                }
                item {
                    FolderChip(
                        label = "+ New",
                        isSelected = false,
                        onClick = { showCreateFolderDialog = true },
                        icon = Icons.Default.CreateNewFolder
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (visibleDecks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedFolderId == 0L) "No decks found. Tap + to add one!"
                            else "No decks in this folder yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Group decks by folder (0 = Uncategorized) when viewing all
                val grouped = if (selectedFolderId == 0L) {
                    visibleDecks.groupBy { it.folderId }
                } else {
                    visibleDecks.groupBy { selectedFolderId }
                }
                val folderNameById = folders.associateBy { it.id }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    grouped.forEach { (folderId, groupDecks) ->
                        if (selectedFolderId == 0L && groupDecks.isNotEmpty()) {
                            item(key = "header_$folderId") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = folderNameById[folderId]?.name ?: "Uncategorized",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${groupDecks.size}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(groupDecks, key = { it.id }) { deck ->
                            val cardCount = deckCardCounts[deck.id] ?: 0
                            val dueCount = deckDueCounts[deck.id] ?: 0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToDeckDetail(deck.id) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        try {
                                                            Color(android.graphics.Color.parseColor(deck.colorHex))
                                                        } catch (e: Exception) {
                                                            IndigoPrimary
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = "Deck",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column {
                                                Text(
                                                    text = deck.title,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${deck.category} • $cardCount cards",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Open Deck",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (deck.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = deck.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = { onNavigateToReview(deck.id) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Review",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = if (dueCount > 0) "Review ($dueCount)" else "Study", fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = { onNavigateToQuiz(deck.id) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldMastery)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Quiz,
                                                    contentDescription = "Quiz",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = "Quiz", fontSize = 12.sp)
                                            }
                                        }

                                        IconButton(
                                            onClick = { deckPendingDelete = deck }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Deck",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB to add deck
        FloatingActionButton(
            onClick = { showCreateDeckDialog = true },
            containerColor = IndigoPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Deck")
        }
    }

    // Create Deck Dialog
    if (showCreateDeckDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("General") }
        var description by remember { mutableStateOf("") }
        var folderDropdownExpanded by remember { mutableStateOf(false) }
        var selectedFolderId by remember { mutableStateOf(0L) }

        AlertDialog(
            onDismissRequest = { showCreateDeckDialog = false },
            title = { Text("Create New Deck", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Deck Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (e.g., Computer Science)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FolderPicker(
                        folders = folders,
                        selectedFolderId = selectedFolderId,
                        onFolderSelected = { selectedFolderId = it },
                        expanded = folderDropdownExpanded,
                        onExpandedChange = { folderDropdownExpanded = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createDeck(title, description, category, "#385A43", selectedFolderId)
                            showCreateDeckDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDeckDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    placeholder = { Text("e.g. Semester 3, Exams, Biology Unit 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName)
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Folder rename/delete dialog (long-press a folder chip)
    folderPendingAction?.let { folder ->
        var renameMode by remember { mutableStateOf(false) }
        var newName by remember { mutableStateOf(folder.name) }
        if (renameMode) {
            AlertDialog(
                onDismissRequest = { folderPendingAction = null },
                title = { Text("Rename Folder", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameFolder(folder.id, newName)
                            folderPendingAction = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { folderPendingAction = null }) { Text("Cancel") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { folderPendingAction = null },
                title = { Text("\"${folder.name}\"", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { renameMode = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rename Folder")
                        }
                        Button(
                            onClick = {
                                viewModel.deleteFolder(folder.id, deleteDecksInside = false)
                                folderPendingAction = null
                                Toast.makeText(context, "Folder deleted — decks moved to Uncategorized", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Folder (keep decks)")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { folderPendingAction = null }) { Text("Close") }
                }
            )
        }
    }

    // Delete Deck Confirmation Dialog
    deckPendingDelete?.let { deck ->
        AlertDialog(
            onDismissRequest = { deckPendingDelete = null },
            title = { Text("Delete Deck?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "\"${deck.title}\" and all its flashcards will be permanently removed. This cannot be undone.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDeck(deck)
                        deckPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deckPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Flashcard Deck Generator Dialog
    if (showAiGeneratorDialog) {
        AiFlashcardGeneratorDialog(
            viewModel = viewModel,
            decks = decks,
            folders = folders,
            aiState = aiState,
            onDismiss = { showAiGeneratorDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable { onClick() }
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
