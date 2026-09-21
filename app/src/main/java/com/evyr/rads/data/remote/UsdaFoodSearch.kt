package com.evyr.rads.data.remote

import com.evyr.rads.data.FoodPortion
import com.evyr.rads.data.ScannedFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * USDA FoodData Central — the US government nutrition database.
 * Covers whole foods (Foundation/SR Legacy) and branded packaged goods.
 * DEMO_KEY works without signup but is heavily rate limited; a free key
 * from fdc.nal.usda.gov lifts that.
 */
object UsdaFoodSearch {

    const val DEMO_KEY = "DEMO_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // FDC nutrient numbers
    private const val N_ENERGY = "208"
    private const val N_PROTEIN = "203"
    private const val N_FAT = "204"
    private const val N_CARBS = "205"
    private const val N_SATFAT = "606"
    private const val N_SUGAR = "269"
    private const val N_FIBER = "291"
    private const val N_SODIUM = "307"

    sealed class Result {
        data class Found(val foods: List<ScannedFood>) : Result()
        object Empty : Result()
        data class Failed(val message: String) : Result()
    }

    suspend fun search(query: String, apiKey: String): Result = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.Empty
        try {
            val url = "https://api.nal.usda.gov/fdc/v1/foods/search".toHttpUrl()
                .newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("pageSize", "25")
                .addQueryParameter("api_key", apiKey.ifBlank { DEMO_KEY })
                .addQueryParameter(
                    "dataType",
                    "Foundation,SR Legacy,Branded,Survey (FNDDS)"
                )
                .build()

            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 429) {
                    return@withContext Result.Failed(
                        "USDA rate limit hit. Add a free API key in SETUP."
                    )
                }
                if (!response.isSuccessful) {
                    return@withContext Result.Failed("Search failed (${response.code}).")
                }

                val root = JSONObject(body)
                val arr = root.optJSONArray("foods")
                    ?: return@withContext Result.Empty
                if (arr.length() == 0) return@withContext Result.Empty

                val foods = buildList {
                    for (i in 0 until arr.length()) {
                        val f = arr.optJSONObject(i) ?: continue
                        parseFood(f)?.let { add(it) }
                    }
                }
                if (foods.isEmpty()) Result.Empty else Result.Found(foods)
            }
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Network error.")
        }
    }

    private fun parseFood(f: JSONObject): ScannedFood? {
        val description = f.optString("description", "").trim()
        if (description.isBlank()) return null

        val brand = f.optString("brandOwner", "").trim()
            .ifBlank { f.optString("brandName", "").trim() }

        val nutrients = f.optJSONArray("foodNutrients") ?: return null

        fun nutrientOrNull(number: String): Double? {
            for (i in 0 until nutrients.length()) {
                val n = nutrients.optJSONObject(i) ?: continue
                if (n.optString("nutrientNumber", "") == number) {
                    return n.optDouble("value", Double.NaN).takeIf { !it.isNaN() }
                }
            }
            return null
        }

        fun nutrient(number: String): Double = nutrientOrNull(number) ?: 0.0

        // Foundation/SR values are per 100 g. Branded also reports per 100 g
        // alongside a serving size, so scale when we have one.
        val servingSize = f.optDouble("servingSize", Double.NaN)
        val servingUnit = f.optString("servingSizeUnit", "").lowercase()
        val household = f.optString("householdServingFullText", "").trim()

        val scalable = !servingSize.isNaN() && servingSize > 0 &&
            (servingUnit == "g" || servingUnit == "ml")
        val factor = if (scalable) servingSize / 100.0 else 1.0

        val note = when {
            scalable && household.isNotBlank() ->
                "Per serving ($household, ${servingSize.roundToInt()}$servingUnit)."
            scalable -> "Per serving (${servingSize.roundToInt()}$servingUnit)."
            else -> "Values are per 100 g/ml."
        }

        val name = if (brand.isNotBlank()) "$brand — $description" else description

        return ScannedFood(
            name = name.take(80),
            calories = (nutrient(N_ENERGY) * factor).roundToInt(),
            fatGrams = round1(nutrient(N_FAT) * factor),
            proteinGrams = round1(nutrient(N_PROTEIN) * factor),
            carbGrams = round1(nutrient(N_CARBS) * factor),
            saturatedFatGrams = round1(nutrient(N_SATFAT) * factor).takeIf { it > 0 },
            sugarGrams = nutrientOrNull(N_SUGAR)?.let { round1(it * factor) },
            fiberGrams = nutrientOrNull(N_FIBER)?.let { round1(it * factor) },
            sodiumMg = nutrientOrNull(N_SODIUM)?.let { (it * factor).roundToInt().toDouble() },
            ingredients = f.optString("ingredients", "").ifBlank { null },
            servingNote = note,
            basisGrams = if (scalable) null else 100.0,
            servingGrams = servingSize.takeIf { scalable },
            sourceId = f.optLong("fdcId", 0L).takeIf { it > 0 }?.toString(),
            source = "usda"
        )
    }

    /**
     * Real portions published for a food — "1 biscuit", "1 sandwich", "1 cup".
     * Used instead of asking someone to weigh a fast-food item.
     */
    suspend fun portions(fdcId: String, apiKey: String): List<FoodPortion> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.nal.usda.gov/fdc/v1/food/$fdcId".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("api_key", apiKey.ifBlank { DEMO_KEY })
                    .build()

                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) return@withContext emptyList()

                    val food = JSONObject(body)
                    val arr = food.optJSONArray("foodPortions") ?: return@withContext emptyList()

                    val out = mutableListOf<FoodPortion>()
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val grams = o.optDouble("gramWeight", 0.0)
                        if (grams <= 0) continue

                        val amount = o.optDouble("amount", 1.0)
                        val unit = o.optJSONObject("measureUnit")?.optString("name", "").orEmpty()
                        val modifier = o.optString("modifier", "").trim()
                        val described = o.optString("portionDescription", "").trim()

                        val label = when {
                            described.isNotBlank() && described != "Quantity not specified" ->
                                described
                            unit.isNotBlank() && unit != "undetermined" ->
                                "${trimNum(amount)} $unit" +
                                    if (modifier.isNotBlank()) " $modifier" else ""
                            modifier.isNotBlank() -> "${trimNum(amount)} $modifier"
                            else -> "${grams.roundToInt()} g"
                        }
                        out += FoodPortion(label.take(46), grams)
                    }
                    out.distinctBy { it.label }.take(8)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun trimNum(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)

    private fun round1(v: Double): Double = (v * 10).roundToInt() / 10.0
}
