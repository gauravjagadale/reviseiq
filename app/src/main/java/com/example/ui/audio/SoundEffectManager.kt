package com.example.ui.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Natural Tones Sound Effect Manager
 * Synthesizes organic, calming PCM audio feedback for flashcard flips, quiz completions,
 * and SRS review ratings without relying on external file assets.
 */
class SoundEffectManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("reviseiq_sound_prefs", Context.MODE_PRIVATE)

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("key_sound_effects_enabled", true)
        set(value) {
            prefs.edit().putBoolean("key_sound_effects_enabled", value).apply()
        }

    private val sampleRate = 44100

    /**
     * Subtle, gentle wooden tap / soft organic tone for card flipping
     */
    fun playCardFlipSound() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Short organic woodblock / warm click: 480Hz blending down to 280Hz over 65ms
                val durationMs = 65
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 480.0 - (200.0 * (i.toDouble() / numSamples))
                    val env = exp(-t * 60.0) // Soft exponential decay
                    val wave = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * (freq * 2.1) * t)
                    samples[i] = (wave * env * 12000.0).toInt().coerceIn(-32768, 32767).toShort()
                }

                playPcmSamples(samples)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Soft chime note for rating a card or submitting a correct question
     */
    fun playSuccessChime() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Gentle 528 Hz (Natural Solfeggio frequency) chime with soft attack & smooth decay
                val durationMs = 280
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 12.0) * (1.0 - exp(-t * 150.0)) // Smooth attack & decay
                    val wave = sin(2.0 * PI * 528.0 * t) + 0.25 * sin(2.0 * PI * 1056.0 * t)
                    samples[i] = (wave * env * 16000.0).toInt().coerceIn(-32768, 32767).toShort()
                }

                playPcmSamples(samples)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Calming major triad natural chord (A440 - C#554 - E659 - A880) for quiz/session completion
     */
    fun playCompletionArpeggio() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val freqs = doubleArrayOf(440.0, 554.37, 659.25, 880.0)
                val noteDurationMs = 140
                val numSamplesPerNote = (sampleRate * (noteDurationMs / 1000.0)).toInt()
                val totalSamples = numSamplesPerNote * freqs.size + (sampleRate * 0.35).toInt()
                val buffer = ShortArray(totalSamples)

                freqs.forEachIndexed { noteIndex, freq ->
                    val offset = noteIndex * numSamplesPerNote
                    val noteLen = totalSamples - offset
                    for (i in 0 until noteLen) {
                        val t = i.toDouble() / sampleRate
                        val env = exp(-t * 6.0) * (1.0 - exp(-t * 80.0))
                        val wave = sin(2.0 * PI * freq * t) + 0.2 * sin(2.0 * PI * (freq * 2.0) * t)
                        val sampleVal = (wave * env * 10000.0).toInt()
                        val currIndex = offset + i
                        if (currIndex < buffer.size) {
                            buffer[currIndex] = (buffer[currIndex] + sampleVal).coerceIn(-32768, 32767).toShort()
                        }
                    }
                }

                playPcmSamples(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playPcmSamples(samples: ShortArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()

        CoroutineScope(Dispatchers.Default).launch {
            delay((samples.size * 1000L / sampleRate) + 120L)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }
}
