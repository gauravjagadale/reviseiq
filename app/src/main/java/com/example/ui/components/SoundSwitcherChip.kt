package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.audio.SoundEffectManager
import com.example.ui.theme.ForestMastery

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SoundSwitcherChip(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundEffectManager(context) }
    var isSoundOn by remember { mutableStateOf(soundManager.isSoundEnabled) }
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val containerBg = if (isSoundOn) ForestMastery.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSoundOn) ForestMastery.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val iconTint = if (isSoundOn) ForestMastery else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .testTag("sound_switcher_chip")
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable {
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                } catch (_: Throwable) {}
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                isSoundOn = !isSoundOn
                soundManager.isSoundEnabled = isSoundOn
                if (isSoundOn) {
                    soundManager.playSuccessChime()
                }
            },
        color = containerBg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = isSoundOn,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                label = "sound_icon_transition"
            ) { soundState ->
                Box(contentAlignment = Alignment.Center) {
                    if (soundState) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Natural Tones Sound On",
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Sound Muted",
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
