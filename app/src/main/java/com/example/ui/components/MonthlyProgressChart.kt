package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OchreStreak
import com.example.ui.theme.VioletSecondary
import java.util.Locale

data class MonthlyStudyDataPoint(
    val dayNumber: Int,
    val dateLabel: String,
    val actualHours: Float,
    val goalHours: Float,
    val cardsReviewed: Int = 0,
    val quizzesCompleted: Int = 0
)

@Composable
fun MonthlyProgressChart(
    dataPoints: List<MonthlyStudyDataPoint>,
    modifier: Modifier = Modifier,
    actualColor: Color = IndigoPrimary,
    goalColor: Color = AmberStreak
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    if (dataPoints.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(dataPoints.size - 1) }
    val selectedPoint = dataPoints.getOrNull(selectedIndex) ?: dataPoints.last()

    // Calculate aggregated stats
    val totalActualHours = remember(dataPoints) { dataPoints.sumOf { it.actualHours.toDouble() }.toFloat() }
    val totalGoalHours = remember(dataPoints) { dataPoints.sumOf { it.goalHours.toDouble() }.toFloat() }
    val goalMetCount = remember(dataPoints) { dataPoints.count { it.actualHours >= it.goalHours && it.goalHours > 0 } }
    val completionPct = if (totalGoalHours > 0) ((totalActualHours / totalGoalHours) * 100).toInt() else 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_progress_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title & D3 Path Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Progress Chart",
                            tint = actualColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "30-Day Study Trajectory",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Actual Hours Studied vs. Planned Goals",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(actualColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$completionPct% Goal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = actualColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Badges Overview Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Studied", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.1fh", totalActualHours),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = actualColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Planned Goal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.1fh", totalGoalHours),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = goalColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Goals Met", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$goalMetCount / ${dataPoints.size} d",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldMastery
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(actualColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hours Studied", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(goalColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Planned Goal", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = goalColor)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // D3-Inspired Custom Path Canvas Drawing
            val maxVal = remember(dataPoints) {
                val maxObserved = dataPoints.maxOf { maxOf(it.actualHours, it.goalHours) }
                (maxOf(maxObserved, 3.0f) * 1.15f)
            }
            val textMeasurer = rememberTextMeasurer()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataPoints) {
                            detectTapGestures { offset ->
                                val paddingLeft = 40f
                                val paddingRight = 20f
                                val chartWidth = size.width - paddingLeft - paddingRight
                                val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

                                val clickedIdx = ((offset.x - paddingLeft + stepX / 2f) / stepX)
                                    .toInt()
                                    .coerceIn(0, dataPoints.size - 1)

                                if (clickedIdx != selectedIndex) {
                                    selectedIndex = clickedIdx
                                    try {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    } catch (_: Throwable) {}
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                        .pointerInput(dataPoints) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val paddingLeft = 40f
                                val paddingRight = 20f
                                val chartWidth = size.width - paddingLeft - paddingRight
                                val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

                                val draggedIdx = ((change.position.x - paddingLeft + stepX / 2f) / stepX)
                                    .toInt()
                                    .coerceIn(0, dataPoints.size - 1)

                                if (draggedIdx != selectedIndex) {
                                    selectedIndex = draggedIdx
                                    try {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    } catch (_: Throwable) {}
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 40f
                    val paddingRight = 20f
                    val paddingTop = 20f
                    val paddingBottom = 30f

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

                    // Draw Y-Axis Gridlines & Labels
                    val gridSteps = 3
                    val yLabelColor = Color.Gray
                    for (i in 0..gridSteps) {
                        val gridVal = (maxVal / gridSteps) * i
                        val y = height - paddingBottom - (gridVal / maxVal) * chartHeight

                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // Calculate Points for Actual Hours & Goal Hours
                    val actualPoints = mutableListOf<Offset>()
                    val goalPoints = mutableListOf<Offset>()

                    dataPoints.forEachIndexed { i, dp ->
                        val x = paddingLeft + i * stepX
                        val yActual = height - paddingBottom - (dp.actualHours / maxVal) * chartHeight
                        val yGoal = height - paddingBottom - (dp.goalHours / maxVal) * chartHeight

                        actualPoints.add(Offset(x, yActual))
                        goalPoints.add(Offset(x, yGoal))
                    }

                    // 1. Draw Goal Line (Dashed D3 Spline or Smooth Path)
                    if (goalPoints.isNotEmpty()) {
                        val goalPath = Path().apply {
                            moveTo(goalPoints[0].x, goalPoints[0].y)
                            for (i in 0 until goalPoints.size - 1) {
                                val p0 = goalPoints[i]
                                val p1 = goalPoints[i + 1]
                                val controlX = (p0.x + p1.x) / 2f
                                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            }
                        }

                        drawPath(
                            path = goalPath,
                            color = goalColor,
                            style = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // 2. Draw Actual Hours Area Gradient Fill & Cubic Bezier Curve (D3 Monotone Curve style)
                    if (actualPoints.isNotEmpty()) {
                        val actualPath = Path().apply {
                            moveTo(actualPoints[0].x, actualPoints[0].y)
                            for (i in 0 until actualPoints.size - 1) {
                                val p0 = actualPoints[i]
                                val p1 = actualPoints[i + 1]
                                val controlX = (p0.x + p1.x) / 2f
                                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(actualPath)
                            lineTo(actualPoints.last().x, height - paddingBottom)
                            lineTo(actualPoints.first().x, height - paddingBottom)
                            close()
                        }

                        // Gradient Area Fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    actualColor.copy(alpha = 0.35f),
                                    actualColor.copy(alpha = 0.02f)
                                ),
                                startY = paddingTop,
                                endY = height - paddingBottom
                            )
                        )

                        // Smooth Curve Line
                        drawPath(
                            path = actualPath,
                            color = actualColor,
                            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                        )
                    }

                    // 3. Draw Selected Indicator Vertical Crosshair & Highlight Nodes
                    if (selectedIndex in actualPoints.indices) {
                        val selActualPos = actualPoints[selectedIndex]
                        val selGoalPos = goalPoints[selectedIndex]

                        // Vertical dashed indicator line
                        drawLine(
                            color = actualColor.copy(alpha = 0.5f),
                            start = Offset(selActualPos.x, paddingTop),
                            end = Offset(selActualPos.x, height - paddingBottom),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )

                        // Outer Pulse Halo on Actual Node
                        drawCircle(
                            color = actualColor.copy(alpha = 0.25f),
                            radius = 14f,
                            center = selActualPos
                        )

                        // Inner Filled Circle on Actual Node
                        drawCircle(
                            color = Color.White,
                            radius = 8f,
                            center = selActualPos
                        )
                        drawCircle(
                            color = actualColor,
                            radius = 5f,
                            center = selActualPos
                        )

                        // Goal Node Highlight
                        drawCircle(
                            color = goalColor,
                            radius = 4f,
                            center = selGoalPos
                        )
                    }

                    // 4. Draw X-Axis Labels (Sampled for cleanliness)
                    val labelInterval = maxOf(1, dataPoints.size / 6)
                    dataPoints.forEachIndexed { i, dp ->
                        if (i % labelInterval == 0 || i == dataPoints.size - 1) {
                            val x = paddingLeft + i * stepX
                            val label = dp.dateLabel
                            val measured = textMeasurer.measure(
                                text = label,
                                style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                            )
                            // Keep labels inside the plot area (first/last would overflow)
                            val clampedX = x.coerceIn(
                                paddingLeft + measured.size.width / 2f,
                                width - paddingRight - measured.size.width / 2f
                            )
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(
                                    clampedX - measured.size.width / 2f,
                                    height - paddingBottom + 8f
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selected Data Point Interactive Tooltip Detail Panel
            selectedPoint?.let { point ->
                val variance = point.actualHours - point.goalHours
                val isGoalMet = point.actualHours >= point.goalHours

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = point.dateLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isGoalMet) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Goal Met",
                                        tint = EmeraldMastery,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = when {
                                    variance > 0 -> "+${String.format(Locale.US, "%.1f", variance)}h ahead of target 🎉"
                                    variance == 0f -> "Exactly matched planned goal 👍"
                                    else -> "${String.format(Locale.US, "%.1f", variance)}h behind planned goal"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isGoalMet) EmeraldMastery else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Studied", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(Locale.US, "%.1f hrs", point.actualHours),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = actualColor
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Goal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(Locale.US, "%.1f hrs", point.goalHours),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = goalColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
