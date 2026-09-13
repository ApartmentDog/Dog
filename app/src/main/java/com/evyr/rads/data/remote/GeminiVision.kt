package com.evyr.rads.data.remote

import android.util.Base64
import com.evyr.rads.data.ScannedFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Sends a photo of a plate or menu to Gemini and asks for structured macros.
 * Optional — barcode scanning works without any of this.
 */
object GeminiVision {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Found(val foods: List<ScannedFood>) : Result()
        data class Failed(val message: String) : Result()
    }

    private const val PROMPT = """
You are a nutrition estimator. Look at the image and identify the food items.

If it is a MENU, list the dishes shown and estimate typical restaurant portions.
If it is a PLATE or PACKAGE, estimate what is actually present.

Respond with ONLY a JSON array, no markdown fences, no commentary:
[
  {
    "name": "short dish name",
    "calories": 0,
    "fat_g": 0,
    "saturated_fat_g": 0,
    "protein_g": 0,
    "carbs_g": 0,
    "portion": "what portion this assumes",
    "confidence": "high" | "medium" | "low"
  }
]

Rules:
- Numbers only, no units inside the numeric fields.
- Estimate one entry per distinct dish, at most 8 entries.
- If you cannot identify any food, return [].
"""

    suspend fun analyze(
        apiKey: String,
        model: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.Failed("No API key set.")
        try {
            val b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray()
                        .put(JSONObject().put("text", PROMPT))
                        .put(JSONObject().put("inline_data", JSONObject()
                            .put("mime_type", mimeType)
                            .put("data", b64)))
                    )
                ))
                put("generationConfig", JSONObject().put("temperature", 0.2))
            }

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    return@withContext Result.Failed(
                        msg ?: "Vision request failed (${response.code})."
                    )
                }

                val text = runCatching {
                    JSONObject(body)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                }.getOrNull() ?: return@withContext Result.Failed("Unexpected response shape.")

                val cleaned = text
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val arr = runCatching { JSONArray(cleaned) }.getOrNull()
                    ?: return@withContext Result.Failed("Could not read the estimate.")

                if (arr.length() == 0) {
                    return@withContext Result.Failed("No food identified in that image.")
                }

                val foods = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        add(
                            ScannedFood(
                                name = o.optString("name", "Unknown item"),
                                calories = o.optInt("calories", 0),
                                fatGrams = o.optDouble("fat_g", 0.0),
                                proteinGrams = o.optDouble("protein_g", 0.0),
                                carbGrams = o.optDouble("carbs_g", 0.0),
                                saturatedFatGrams =
                                    o.optDouble("saturated_fat_g", 0.0).takeIf { it > 0 },
                                servingNote = o.optString("portion", "").ifBlank { null },
                                source = "vision",
                                confidence = o.optString("confidence", "medium")
                            )
                        )
                    }
                }
                Result.Found(foods)
            }
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Vision error.")
        }
    }
}
