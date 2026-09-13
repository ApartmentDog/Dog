package com.evyr.rads.data.remote

import com.evyr.rads.data.ScannedFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Free, no-key product database. Nutriments are per 100g/ml; where a serving
 * size is published we scale to one serving and say so.
 */
object OpenFoodFacts {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Found(val food: ScannedFood) : Result()
        data class NotFound(val barcode: String) : Result()
        data class Failed(val message: String) : Result()
    }

    suspend fun lookup(barcode: String): Result = withContext(Dispatchers.IO) {
        try {
            val url =
                "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
                    "?fields=product_name,brands,serving_size,serving_quantity,nutriments"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RADS/0.1 (personal diet tracker)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.Failed("Lookup failed (${response.code}).")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext Result.Failed("Empty response.")

                val root = JSONObject(body)
                if (root.optInt("status", 0) != 1) {
                    return@withContext Result.NotFound(barcode)
                }

                val product = root.optJSONObject("product")
                    ?: return@withContext Result.NotFound(barcode)
                val n = product.optJSONObject("nutriments")
                    ?: return@withContext Result.Failed("No nutrition data published.")

                val brand = product.optString("brands", "").substringBefore(",").trim()
                val rawName = product.optString("product_name", "").trim()
                val name = listOf(brand, rawName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Item $barcode" }

                // Prefer per-serving values when present, else scale per-100g.
                val servingQty = product.optDouble("serving_quantity", Double.NaN)
                val servingText = product.optString("serving_size", "").trim()

                fun per100(key: String): Double =
                    n.optDouble("${key}_100g", Double.NaN)
                        .takeIf { !it.isNaN() } ?: 0.0

                fun perServing(key: String): Double? =
                    n.optDouble("${key}_serving", Double.NaN).takeIf { !it.isNaN() }

                val useServing = perServing("energy-kcal") != null ||
                    (!servingQty.isNaN() && servingQty > 0)

                val factor = if (!servingQty.isNaN() && servingQty > 0) servingQty / 100.0 else 1.0

                fun value(key: String): Double =
                    perServing(key) ?: (per100(key) * if (useServing) factor else 1.0)

                val note = when {
                    servingText.isNotBlank() && useServing -> "Per serving ($servingText)."
                    useServing -> "Per serving."
                    else -> "Per 100 g/ml — adjust if your portion differs."
                }

                Result.Found(
                    ScannedFood(
                        name = name,
                        calories = value("energy-kcal").roundToInt(),
                        fatGrams = round1(value("fat")),
                        proteinGrams = round1(value("proteins")),
                        carbGrams = round1(value("carbohydrates")),
                        saturatedFatGrams = round1(value("saturated-fat")).takeIf { it > 0 },
                        servingNote = note,
                        barcode = barcode,
                        source = "barcode"
                    )
                )
            }
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Network error.")
        }
    }

    private fun round1(v: Double): Double = (v * 10).roundToInt() / 10.0
}
