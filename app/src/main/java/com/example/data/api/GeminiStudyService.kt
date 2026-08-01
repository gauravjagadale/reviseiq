package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class GeneratedCard(
    val front: String,
    val back: String,
    val hint: String = ""
)

data class GeneratedQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object GeminiStudyService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generatePomodoroSummary(
        deckTitle: String,
        focusTopic: String,
        durationMinutes: Int,
        sampleCardTexts: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is missing. Please configure it in the Secrets panel."))
        }

        val cardsSummary = if (sampleCardTexts.isNotEmpty()) {
            sampleCardTexts.take(6).joinToString("\n") { (front, back) -> "- Q: $front | A: $back" }
        } else {
            ""
        }

        val prompt = """
            You are an expert AI study coach summarizing a completed $durationMinutes-minute Pomodoro focus study session.
            Session Info:
            - Subject / Deck: $deckTitle
            - Focus Topic: $focusTopic
            ${if (cardsSummary.isNotBlank()) "Sample Cards Reviewed:\n$cardsSummary" else ""}

            Please generate a concise, structured study summary highlighting:
            1. 📌 **Key Concepts Reviewed**: 2-3 bullet points summarizing the core concepts covered in this focus topic.
            2. ⏳ **Suggested Next Study Interval**: Suggest the optimal next review time (e.g., in 24 hours, in 3 days, or in 1 week) based on Leitner spaced repetition principles.
            3. 💡 **Recall Tip**: 1 sentence tip for long-term retention.

            Keep the tone motivating, concise, and well-formatted. Do not add redundant chatter.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonRequest.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini request failed (${response.code})"))
            }

            val bodyString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "Session summary completed."

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateFlashcards(topicOrNotes: String, cardCount: Int = 5): Result<List<GeneratedCard>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is missing. Please configure it in the Secrets panel."))
        }

        val prompt = """
            You are an expert tutor creating study flashcards for spaced revision.
            Topic/Notes: $topicOrNotes
            Generate exactly $cardCount high quality flashcards.
            Respond ONLY in valid JSON format as a JSON array of objects with fields:
            "front": clear, concise question or term,
            "back": accurate, well-explained answer,
            "hint": brief memory anchor or mnemonic hint.

            Example format:
            [
              {"front": "What is clean architecture?", "back": "A software design pattern that separates code into distinct layers with clear dependency rules.", "hint": "Think concentric circles"},
              {"front": "What is SM-2?", "back": "An algorithm for spaced repetition learning developed by SuperMemo.", "hint": "Calculates intervals based on recall ease"}
            ]
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonRequest.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API request failed with code ${response.code}"))
            }

            val bodyString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val cardsArray = JSONArray(text)
            val list = mutableListOf<GeneratedCard>()
            for (i in 0 until cardsArray.length()) {
                val item = cardsArray.getJSONObject(i)
                list.add(
                    GeneratedCard(
                        front = item.optString("front", ""),
                        back = item.optString("back", ""),
                        hint = item.optString("hint", "")
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateAIExplanation(cardFront: String, cardBack: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is missing."))
        }

        val prompt = """
            Explain the following flashcard concept clearly for a student in simple terms with a real-world analogy and key takeaways:
            Question: $cardFront
            Answer: $cardBack
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonRequest.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to reach Gemini AI"))
            }

            val bodyString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "No response generated."

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
