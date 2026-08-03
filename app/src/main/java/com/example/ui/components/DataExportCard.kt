package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import com.example.data.repository.ImportSummary
import com.example.ui.ReviseViewModel
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OchreStreak
import com.example.ui.theme.RoseAlert
import java.io.File

@Composable
fun DataExportCard(
    viewModel: ReviseViewModel,
    modifier: Modifier = Modifier,
    specificDeckId: Long? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()

    var showExportModal by remember { mutableStateOf(false) }
    var showResetConfirmationModal by remember { mutableStateOf(false) }
    var showResetStreakModal by remember { mutableStateOf(false) }
    var showClearSessionsModal by remember { mutableStateOf(false) }
    var showDeleteAllDecksModal by remember { mutableStateOf(false) }
    var generatedJson by remember { mutableStateOf("") }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var isCopied by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportSummary?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    }
                    if (json.isNullOrBlank()) {
                        importError = "Selected file is empty or unreadable."
                    } else {
                        importResult = viewModel.importFromJson(json)
                        Toast.makeText(context, "Backup imported!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    importError = "Import failed: ${e.message ?: "could not read file"}"
                }
            }
        }
    }

    val deckCount = if (specificDeckId != null) 1 else decks.size
    val cardCount = if (specificDeckId != null) {
        allCards.count { it.deckId == specificDeckId }
    } else {
        allCards.size
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("data_export_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldMastery.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Backup JSON",
                            tint = EmeraldMastery,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (specificDeckId != null) "Backup Deck (JSON) 💾" else "Data & Reset Controls 💾",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$deckCount Decks • $cardCount Cards • Study Analytics",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            } catch (_: Throwable) {}
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            scope.launch {
                                generatedJson = viewModel.generateExportJson(specificDeckId)
                                exportedFile = viewModel.exportDataToFile(context, specificDeckId)
                                isCopied = false
                                showExportModal = true

                                Toast.makeText(
                                    context,
                                    "JSON backup file ready!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldMastery),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("export_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (specificDeckId == null) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                importLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("import_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = IndigoPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showResetConfirmationModal = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAlert),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("reset_all_data_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = RoseAlert
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoseAlert)
                        }
                    }
                }
            }

            if (specificDeckId == null) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Selective Reset",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                SelectiveResetRow(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Reset Streak Only",
                    subtitle = "Clears streak history, study logs & pomodoro counts",
                    onClick = { showResetStreakModal = true },
                    testTag = "reset_streak_only_button"
                )
                Spacer(modifier = Modifier.height(6.dp))
                SelectiveResetRow(
                    icon = Icons.Default.Schedule,
                    title = "Clear Scheduled Sessions",
                    subtitle = "Removes all planned sessions from the calendar",
                    onClick = { showClearSessionsModal = true },
                    testTag = "clear_sessions_button"
                )
                Spacer(modifier = Modifier.height(6.dp))
                SelectiveResetRow(
                    icon = Icons.Default.Folder,
                    title = "Delete All Decks & Cards",
                    subtitle = "Removes every deck, folder and flashcard",
                    onClick = { showDeleteAllDecksModal = true },
                    testTag = "delete_all_decks_button"
                )
            }
        }
    }

    // Confirmation Modal for Resetting All Data
    if (showResetConfirmationModal) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationModal = false },
            modifier = Modifier.testTag("reset_data_confirmation_dialog"),
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RoseAlert.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = RoseAlert,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Reset All App Data?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Text(
                    text = "This will permanently delete all your decks, flashcards, study logs, quiz results, and streaks so you can start completely fresh. Are you sure you want to proceed?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmationModal = false
                        viewModel.resetAllData()
                        Toast.makeText(context, "All data has been reset. Fresh start ready!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAlert),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Reset Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmationModal = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Selective reset dialogs
    if (showResetStreakModal) {
        AlertDialog(
            onDismissRequest = { showResetStreakModal = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = OchreStreak,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Reset Streak Only?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Text(
                    text = "Your decks, flashcards and folders stay untouched. Streak history, study logs and pomodoro counts will be cleared.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetStreakModal = false
                        viewModel.resetStreakOnly()
                        Toast.makeText(context, "Streak & study logs reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAlert),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset Streak", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStreakModal = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearSessionsModal) {
        AlertDialog(
            onDismissRequest = { showClearSessionsModal = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = OchreStreak,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Clear Scheduled Sessions?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Text(
                    text = "All planned study sessions will be removed from the calendar. Smart review sessions included.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearSessionsModal = false
                        viewModel.clearAllScheduledSessions()
                        Toast.makeText(context, "Scheduled sessions cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAlert),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Sessions", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessionsModal = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteAllDecksModal) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDecksModal = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = RoseAlert,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Delete All Decks?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Text(
                    text = "Every deck, folder and flashcard will be permanently removed. Your streak and stats stay intact.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDecksModal = false
                        viewModel.deleteAllDecksAndCards()
                        Toast.makeText(context, "All decks deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAlert),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDecksModal = false }) { Text("Cancel") }
            }
        )
    }

    // Export Success & Code Preview Modal
    if (showExportModal) {
        AlertDialog(
            onDismissRequest = { showExportModal = false },
            modifier = Modifier.testTag("export_preview_modal"),
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldMastery.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = EmeraldMastery,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Backup Ready! 📦",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = exportedFile?.name ?: "reviseiq_backup.json",
                            fontSize = 11.sp,
                            color = EmeraldMastery,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Your flashcards, SRS study states, quiz history, and weekly progress stats have been serialized into standard JSON.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Code snippet view
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = generatedJson,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy JSON Button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ReviseIQ JSON Backup", generatedJson)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                    Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("copy_json_button")
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isCopied) "Copied!" else "Copy Text", fontSize = 11.sp)
                        }

                        // Share / Save via Intent — attaches the REAL file through
                        // a FileProvider content URI (sharing raw text loses the file).
                        Button(
                            onClick = {
                                exportedFile?.let { file ->
                                    try {
                                        val fileUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "ReviseIQ Local Backup JSON")
                                            putExtra(Intent.EXTRA_STREAM, fileUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Save / Share JSON Backup"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("share_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share File", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExportModal = false },
                    modifier = Modifier.testTag("close_export_modal_button")
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Import result / error dialog
    if (importResult != null || importError != null) {
        val summary = importResult
        AlertDialog(
            onDismissRequest = {
                importResult = null
                importError = null
            },
            modifier = Modifier.testTag("import_result_dialog"),
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                (if (summary != null) EmeraldMastery else RoseAlert).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (summary != null) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (summary != null) EmeraldMastery else RoseAlert,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (summary != null) "Import Complete 🎉" else "Import Failed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                if (summary != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Your backup was restored. New rows were created for decks and cards; daily streaks were merged without losing local progress.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ImportStatRow("Decks imported", summary.decksImported)
                        ImportStatRow("Flashcards imported", summary.cardsImported)
                        ImportStatRow("Streak days merged", summary.streakDaysMerged)
                        ImportStatRow("Quiz results restored", summary.quizzesImported)
                    }
                } else {
                    Text(
                        text = importError ?: "Unknown error.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        importResult = null
                        importError = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (summary != null) EmeraldMastery else IndigoPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ImportStatRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SelectiveResetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
