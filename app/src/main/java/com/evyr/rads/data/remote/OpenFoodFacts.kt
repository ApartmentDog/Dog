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

    suspend fun lookup(barcode: String): Result = withContext(Dispatchers.IO) {        try {
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
                        basisGrams = if (useServing) null else 100.0,
                        source = "barcode"
                    )
                )
            }
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Network error.")
        }
    }

    /** Text search across packaged products — secondary to USDA. */
    suspend fun search(query: String): List<ScannedFood> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url =
                "https://world.openfoodfacts.org/cgi/search.pl" +
                    "?search_terms=$encoded&search_simple=1&action=process&json=1" +
                    "&page_size=15" +
                    "&fields=product_name,brands,serving_size,serving_quantity,nutriments"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RADS/0.1 (personal diet tracker)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext emptyList()

                val products = JSONObject(body).optJSONArray("products")
                    ?: return@withContext emptyList()

                buildList {
                    for (i in 0 until products.length()) {
                        val product = products.optJSONObject(i) ?: continue
                        val n = product.optJSONObject("nutriments") ?: continue
                        val rawName = product.optString("product_name", "").trim()
                        if (rawName.isBlank()) continue

                        val brand = product.optString("brands", "")
                            .substringBefore(",").trim()
                        val servingQty = product.optDouble("serving_quantity", Double.NaN)
                        val servingText = product.optString("serving_size", "").trim()
                        val scalable = !servingQty.isNaN() && servingQty > 0
                        val factor = if (scalable) servingQty / 100.0 else 1.0

                        fun v(key: String): Double {
                            val perServing = n.optDouble("${key}_serving", Double.NaN)
                            if (!perServing.isNaN()) return perServing
                            val per100 = n.optDouble("${key}_100g", Double.NaN)
                            if (per100.isNaN()) return 0.0
                            return per100 * factor
                        }

                        val energy = v("energy-kcal")
                        if (energy <= 0.0) continue

                        add(
                            ScannedFood(
                                name = listOf(brand, rawName)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                    .take(80),
                                calories = energy.roundToInt(),
                                fatGrams = round1(v("fat")),
                                proteinGrams = round1(v("proteins")),
                                carbGrams = round1(v("carbohydrates")),
                                saturatedFatGrams = round1(v("saturated-fat")).takeIf { it > 0 },
                                servingNote = when {
                                    scalable && servingText.isNotBlank() ->
                                        "Per serving ($servingText)."
                                    scalable -> "Per serving."
                                    else -> "Values are per 100 g."
                                },
                                basisGrams = if (scalable) null else 100.0,
                                source = "off"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun round1(v: Double): Double = (v * 10).roundToInt() / 10.0
}
