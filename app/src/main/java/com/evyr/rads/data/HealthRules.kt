package com.evyr.rads.data

import com.evyr.rads.data.local.FoodLogEntry

/**
 * Things in food that commonly aggravate specific conditions. Detected from a
 * food's name and ingredient text, plus tags an AI estimate reports directly.
 * Keyword detection is a heuristic: it catches what the name or ingredient
 * list says, not what a restaurant quietly puts in the fryer.
 */
enum class Trigger(val key: String, val label: String) {
    CAFFEINE("caffeine", "caffeine"),
    ALCOHOL("alcohol", "alcohol"),
    SPICY("spicy", "spicy"),
    FRIED("fried", "fried / greasy"),
    ACIDIC("acidic", "acidic (tomato / citrus)"),
    CARBONATED("carbonated", "carbonated"),
    CHOCOLATE_MINT("chocolate_mint", "chocolate / mint"),
    DAIRY("dairy", "dairy"),
    GLUTEN("gluten", "gluten"),
    HIGH_PURINE("high_purine", "high purine"),
    HIGH_FODMAP("high_fodmap", "high FODMAP"),
    FRUCTOSE("fructose", "added fructose / sugary drink");

    companion object {
        fun fromKey(k: String): Trigger? =
            values().firstOrNull { it.key == k.trim().lowercase() }

        fun parse(csv: String?): Set<Trigger> =
            csv.orEmpty().split(',').mapNotNull { fromKey(it) }.toSet()
    }
}

/**
 * Conditions the user can switch on in Settings. Each lists the triggers it
 * cares about and how seriously to treat them. Numeric checks (sodium, sugar,
 * carbs, saturated fat, meal size) live in VerdictRules.
 */
enum class Condition(
    val key: String,
    val label: String,
    val watches: String,
    val triggers: Map<Trigger, Verdict>
) {
    REFLUX(
        "reflux", "ACID REFLUX / GERD",
        "caffeine, alcohol, spicy, fried, acidic, carbonated, chocolate/mint, large meals",
        mapOf(
            Trigger.CAFFEINE to Verdict.CAUTION,
            Trigger.ALCOHOL to Verdict.CAUTION,
            Trigger.SPICY to Verdict.CAUTION,
            Trigger.FRIED to Verdict.CAUTION,
            Trigger.ACIDIC to Verdict.CAUTION,
            Trigger.CARBONATED to Verdict.CAUTION,
            Trigger.CHOCOLATE_MINT to Verdict.CAUTION
        )
    ),
    HIATAL_HERNIA(
        "hiatal_hernia", "HIATAL HERNIA",
        "large meals, carbonated, fried, spicy, caffeine, alcohol, acidic",
        mapOf(
            Trigger.CARBONATED to Verdict.CAUTION,
            Trigger.FRIED to Verdict.CAUTION,
            Trigger.SPICY to Verdict.CAUTION,
            Trigger.CAFFEINE to Verdict.CAUTION,
            Trigger.ALCOHOL to Verdict.CAUTION,
            Trigger.ACIDIC to Verdict.CAUTION
        )
    ),
    IBS(
        "ibs", "IBS",
        "high FODMAP, fried, spicy, caffeine, alcohol, carbonated",
        mapOf(
            Trigger.HIGH_FODMAP to Verdict.CAUTION,
            Trigger.FRIED to Verdict.CAUTION,
            Trigger.SPICY to Verdict.CAUTION,
            Trigger.CAFFEINE to Verdict.CAUTION,
            Trigger.ALCOHOL to Verdict.CAUTION,
            Trigger.CARBONATED to Verdict.CAUTION
        )
    ),
    FATTY_LIVER(
        "fatty_liver", "FATTY LIVER",
        "alcohol, added sugar and fructose, saturated fat, fried",
        mapOf(
            Trigger.ALCOHOL to Verdict.OVER_LIMIT,
            Trigger.FRUCTOSE to Verdict.CAUTION,
            Trigger.FRIED to Verdict.CAUTION
        )
    ),
    DIABETES(
        "diabetes", "DIABETES / PREDIABETES",
        "carbs and sugar per meal, sugary drinks",
        mapOf(Trigger.FRUCTOSE to Verdict.CAUTION)
    ),
    HYPERTENSION(
        "hypertension", "HIGH BLOOD PRESSURE",
        "sodium per meal, alcohol",
        mapOf(Trigger.ALCOHOL to Verdict.CAUTION)
    ),
    CHOLESTEROL(
        "cholesterol", "HIGH CHOLESTEROL",
        "saturated fat per meal, fried food",
        mapOf(Trigger.FRIED to Verdict.CAUTION)
    ),
    GOUT(
        "gout", "GOUT",
        "high-purine foods (organ meat, some seafood), alcohol especially beer, sugary drinks",
        mapOf(
            Trigger.HIGH_PURINE to Verdict.CAUTION,
            Trigger.ALCOHOL to Verdict.CAUTION,
            Trigger.FRUCTOSE to Verdict.CAUTION
        )
    ),
    LACTOSE(
        "lactose", "LACTOSE INTOLERANCE",
        "dairy",
        mapOf(Trigger.DAIRY to Verdict.CAUTION)
    ),
    CELIAC(
        "celiac", "CELIAC / GLUTEN-FREE",
        "gluten — wheat, barley, rye, most breads, pasta and batters",
        mapOf(Trigger.GLUTEN to Verdict.OVER_LIMIT)
    );

    companion object {
        fun fromKey(k: String): Condition? =
            values().firstOrNull { it.key == k.trim().lowercase() }

        fun parse(csv: String?): Set<Condition> =
            csv.orEmpty().split(',').mapNotNull { fromKey(it) }.toSet()

        /** Stable, ordinal-ordered CSV for storage. */
        fun toCsv(set: Set<Condition>): String =
            values().filter { it in set }.joinToString(",") { it.key }
    }
}

/**
 * Per-meal thresholds, including what's already logged in the meal.
 * These are general-guidance defaults, not anyone's prescription.
 */
object Thresholds {
    const val SODIUM_CAUTION_MG = 600.0
    const val SODIUM_OVER_MG = 900.0
    const val SUGAR_CAUTION_G = 15.0
    const val SUGAR_OVER_G = 25.0
    const val CARB_CAUTION_G = 45.0
    const val CARB_OVER_G = 60.0
    const val SATFAT_CAUTION_G = 5.0
    const val SATFAT_OVER_G = 10.0
    const val REFLUX_MEAL_KCAL_CAUTION = 700
    const val REFLUX_MEAL_KCAL_OVER = 950
    const val HERNIA_MEAL_KCAL_CAUTION = 600
    const val HERNIA_MEAL_KCAL_OVER = 850
}

object TriggerDetector {

    private fun rx(pattern: String) =
        Regex("""\b(?:$pattern)\b""", RegexOption.IGNORE_CASE)

    // Shared exclusions: soft drinks named "beer" and plant milks trip several checks.
    private const val SOFT_BEERS = """root beer|ginger beer|ginger ale"""
    private const val PLANT_MILK = """(?:oat|almond|soy|coconut|rice|cashew|hemp|pea) milk(?:\s+\w+)?"""
    private const val LOW_FODMAP_MILK = """(?:oat|almond|coconut|rice|hemp|pea) milk(?:\s+\w+)?"""

    /** Phrases removed before matching, so "peanut butter" isn't dairy. */
    private val strip: Map<Trigger, Regex> = mapOf(
        Trigger.CAFFEINE to rx("""decaf(?:feinated)?(?:\s+\w+)?|caffeine[- ]free(?:\s+\w+)?|herbal teas?|(?:chamomile|rooibos|peppermint|ginger|hibiscus) teas?"""),
        Trigger.ALCOHOL to rx("""non[- ]?alcoholic(?:\s+\w+)?|alcohol[- ]free(?:\s+\w+)?|(?:red |white |rice )?wine vinegar|$SOFT_BEERS|beer[- ]battered|scotch eggs?|butterscotch"""),
        Trigger.SPICY to rx("""buffalo mozzarella|mild(?:\s+\w+)?"""),
        Trigger.FRIED to rx("""air[- ]fried(?:\s+\w+)?|baked (?:chips|fries)"""),
        Trigger.DAIRY to rx("""(?:peanut|almond|cashew|apple|cocoa|shea|sunflower|seed|nut) butter|$PLANT_MILK|dairy[- ]free(?:\s+\w+)?|non[- ]?dairy(?:\s+\w+)?|lactose[- ]free(?:\s+\w+)?|vegan (?:cheese|butter|cream|milk)|coconut cream|cream of tartar|cream soda"""),
        Trigger.GLUTEN to rx("""$SOFT_BEERS|gluten[- ]free(?:\s+\w+)?|corn tortillas?|rice (?:noodles?|cakes?|crackers?|paper|bread)|glass noodles?|zucchini noodles?|lettuce wraps?"""),
        Trigger.HIGH_PURINE to rx("""$SOFT_BEERS|beer[- ]battered|kidney beans?"""),
        Trigger.HIGH_FODMAP to rx("""$LOW_FODMAP_MILK|green beans?|coffee beans?|jelly beans?|garlic[- ]infused oil"""),
        Trigger.FRUCTOSE to rx("""diet (?:soda|coke|pepsi|cola|drink)s?|(?:coke|pepsi) zero|zero sugar(?:\s+\w+)?|sugar[- ]free(?:\s+\w+)?|unsweetened(?:\s+\w+)?"""),
        Trigger.CARBONATED to rx("""beer[- ]battered""")
    )

    private val patterns: Map<Trigger, Regex> = mapOf(
        Trigger.CAFFEINE to rx("""coffee|espresso|lattes?|cappuccinos?|americanos?|mochas?|macchiatos?|frappuccinos?|cold brew|energy drinks?|red bull|monster energy|colas?|coke|pepsi|dr\.? ?pepper|mountain dew|teas?|matcha|chai|yerba mate|guarana|caffeine|pre[- ]?workout"""),
        Trigger.ALCOHOL to rx("""beers?|wines?|vodka|whiske?y|bourbon|scotch|rum|tequila|mezcal|gin|brandy|cognac|liqueurs?|sake|soju|hard (?:seltzer|cider|lemonade)s?|margaritas?|mimosas?|sangria|cocktails?|martinis?|mojitos?|daiquiris?|pina coladas?|bloody marys?|ipas?|lagers?|stouts?|pilsners?|champagne|prosecco|spiked"""),
        Trigger.SPICY to rx("""spicy|hot sauce|hot wings|hot chicken|nashville hot|flamin'?g? hot|sriracha|jalape(?:ñ|n)os?|habaneros?|serranos?|chipotles?|cayenne|chil(?:i|e|li)(?:e?s)?|buffalo|ghost peppers?|red pepper flakes|crushed red pepper|curry|curries|vindaloo|gochujang|harissa|wasabi|pepper jack|kimchi|szechuan|sichuan|jerk"""),
        Trigger.FRIED to rx("""fried|deep[- ]fried|crispy|tempura|fries|french fries|tater tots?|hash ?browns?|nuggets?|onion rings?|battered|breaded|doughnuts?|donuts?|fritters?|funnel cakes?|tenders|chicken (?:strips|fingers)|egg rolls?|spring rolls?|corn dogs?|churros?|chips|mozzarella sticks|schnitzel|katsu|fish and chips|hush ?puppies|chimichangas?|taquitos?|empanadas?|samosas?|pakoras?|wings?"""),
        Trigger.ACIDIC to rx("""tomato(?:es)?|marinara|pizza|salsa|ketchup|bbq|barbecue|oranges?|orange juice|oj|lemons?|lemonade|limes?|limeade|grapefruits?|citrus|pineapples?|vinegar|vinaigrette|cranberr(?:y|ies)|pickles?|sauerkraut|hot sauce"""),
        Trigger.CARBONATED to rx("""sodas?|colas?|coke|pepsi|sprite|fanta|7[- ]?up|dr\.? ?pepper|mountain dew|root beer|ginger ale|ginger beer|cream soda|sparkling|seltzers?|club soda|tonic|carbonated|energy drinks?|red bull|monster energy|kombucha|beers?|champagne|prosecco|la ?croix|bubly"""),
        Trigger.CHOCOLATE_MINT to rx("""chocolate|cocoa|cacao|fudge|brownies?|mochas?|nutella|peppermint|spearmint|mints?"""),
        Trigger.DAIRY to rx("""milk|milkshakes?|shakes?|cheeses?|cheesy|cheddar|mozzarella|parmesan|parmigiano|provolone|pepper jack|brie|gouda|feta|ricotta|mascarpone|queso|cream|creamer|sour cream|ice cream|whipped cream|cream cheese|half and half|butter|buttercream|buttermilk|buttery|yogh?urts?|kefir|whey|casein|lactose|alfredo|custard|pudding|froyo|lattes?|cappuccinos?|frappuccinos?|mac (?:and|&|n) cheese|gelato|cheesecake|frosty|blizzard|mcflurry"""),
        Trigger.GLUTEN to rx("""wheat|flour|breads?|breaded|breadsticks?|buns?|bagels?|biscuits?|croissants?|muffins?|pancakes?|waffles?|french toast|toast|tortillas?|wraps?|pitas?|naan|flatbreads?|crackers?|cookies?|cakes?|cupcakes?|pastr(?:y|ies)|pies?|pretzels?|pasta|spaghetti|linguine|fettuccine|penne|macaroni|lasagna|ravioli|noodles?|ramen|udon|dumplings?|couscous|bulgur|farro|spelt|semolina|barley|rye|malt|seitan|croutons?|sandwich(?:es)?|burgers?|subs?|hoagies?|pizza|calzones?|stromboli|beers?|soy sauce|teriyaki|gravy|roux|battered|tempura|doughnuts?|donuts?|churros?|cereal|granola|gluten|panko|brioche|sourdough|(?:dinner|egg|cinnamon|spring|crescent) rolls?"""),
        Trigger.HIGH_PURINE to rx("""anchov(?:y|ies)|sardines?|herring|mackerel|mussels?|scallops?|trout|organ meats?|liver|livers|liverwurst|pate|pâté|kidneys?|sweetbreads?|tripe|venison|goose|gravy|bouillon|beers?|shellfish|shrimp|lobster|crab|clams?|oysters?|roe|caviar"""),
        Trigger.HIGH_FODMAP to rx("""onions?|garlic|beans?|refried beans|baked beans|lentils?|chickpeas?|hummus|falafel|apples?|apple juice|pears?|mangos?|mangoes|watermelon|cherr(?:y|ies)|peach(?:es)?|plums?|prunes|dates|figs|honey|agave|high fructose corn syrup|hfcs|sorbitol|xylitol|mannitol|maltitol|isomalt|cauliflower|mushrooms?|asparagus|artichokes?|leeks?|shallots?|wheat|rye|barley|inulin|chicory|milk|ice cream|yogh?urts?|ricotta|cottage cheese|cashews?|pistachios?"""),
        Trigger.FRUCTOSE to rx("""high fructose corn syrup|hfcs|corn syrup|fructose|agave|sodas?|colas?|coke|pepsi|sprite|fanta|7[- ]?up|dr\.? ?pepper|mountain dew|root beer|ginger ale|lemonade|limeade|sweet tea|fruit punch|punch|juices?|energy drinks?|red bull|sports drinks?|gatorade|powerade|slush(?:y|ies)|slurpee|icee|frappuccinos?|milkshakes?|syrup|candy|candies""")
    )

    fun detect(name: String, ingredients: String?, tags: Set<String>): Set<Trigger> {
        val text = (name + " " + ingredients.orEmpty()).lowercase()
        val found = mutableSetOf<Trigger>()
        for ((trigger, regex) in patterns) {
            val cleaned = strip[trigger]?.replace(text, " ") ?: text
            if (regex.containsMatchIn(cleaned)) found += trigger
        }
        tags.mapNotNullTo(found) { Trigger.fromKey(it) }
        return found
    }
}

data class ConditionWarning(
    val condition: Condition,
    val severity: Verdict,
    val details: List<String>
)

data class VerdictResult(
    val verdict: Verdict,
    val headline: String,
    val reasons: List<String>,
    val warnings: List<ConditionWarning> = emptyList(),
    val triggers: Set<Trigger> = emptySet()
)

object VerdictRules {

    private fun worst(a: Verdict, b: Verdict) = if (b.ordinal > a.ordinal) b else a

    /**
     * Fat is judged against the per-meal ceiling. Condition checks add their
     * own flags; the stamp shows the worst of everything. Totals always
     * include what's already logged in this meal.
     */
    fun evaluate(
        food: ScannedFood,
        mealEntries: List<FoodLogEntry>,
        fatLimitPerMeal: Double,
        conditions: Set<Condition>
    ): VerdictResult {
        val reasons = mutableListOf<String>()

        // ---- Fat, per meal ----
        val fatAlready = mealEntries.sumOf { it.fatGrams }
        val projectedFat = fatAlready + food.fatGrams
        val fatVerdict = when {
            projectedFat >= fatLimitPerMeal -> Verdict.OVER_LIMIT
            projectedFat >= fatLimitPerMeal * 0.7 -> Verdict.CAUTION
            else -> Verdict.PASS
        }
        reasons += "Meal fat would reach ${fmt(projectedFat)}g of ${fmt(fatLimitPerMeal)}g."
        if (fatAlready > 0) reasons += "${fmt(fatAlready)}g already logged this meal."
        if (food.calories >= 700) reasons += "Large single item at ${food.calories} kcal."
        food.servingNote?.let { reasons += it }
        if (food.confidence == "low") reasons += "AI estimate — numbers are approximate."

        // ---- Conditions ----
        val triggers = TriggerDetector.detect(food.name, food.ingredients, food.tags)
        val warnings = mutableListOf<ConditionWarning>()

        for (c in Condition.values().filter { it in conditions }) {
            var sev = Verdict.PASS
            val details = mutableListOf<String>()

            c.triggers.forEach { (t, s) ->
                if (t in triggers) {
                    sev = worst(sev, s)
                    details += t.label
                }
            }

            fun numeric(
                label: String,
                unit: String,
                item: Double?,
                pick: (FoodLogEntry) -> Double?,
                caution: Double,
                over: Double
            ) {
                if (item == null) {
                    details += "no $label data for this item"
                    return
                }
                val prior = mealEntries.map(pick)
                val total = prior.sumOf { it ?: 0.0 } + item
                val partial = prior.any { it == null }
                val level = when {
                    total >= over -> Verdict.OVER_LIMIT
                    total >= caution -> Verdict.CAUTION
                    else -> Verdict.PASS
                }
                if (level != Verdict.PASS) {
                    sev = worst(sev, level)
                    details += "$label ${fmt(total)}$unit this meal (limit ~${fmt(over)}$unit)" +
                        if (partial) ", earlier items lack data" else ""
                }
            }

            fun mealSize(caution: Int, over: Int) {
                val total = mealEntries.sumOf { it.calories } + food.calories
                val level = when {
                    total >= over -> Verdict.OVER_LIMIT
                    total >= caution -> Verdict.CAUTION
                    else -> Verdict.PASS
                }
                if (level != Verdict.PASS) {
                    sev = worst(sev, level)
                    details += "large meal, $total kcal"
                }
            }

            when (c) {
                Condition.REFLUX -> mealSize(
                    Thresholds.REFLUX_MEAL_KCAL_CAUTION, Thresholds.REFLUX_MEAL_KCAL_OVER
                )
                Condition.HIATAL_HERNIA -> mealSize(
                    Thresholds.HERNIA_MEAL_KCAL_CAUTION, Thresholds.HERNIA_MEAL_KCAL_OVER
                )
                Condition.FATTY_LIVER -> {
                    numeric("sugar", "g", food.sugarGrams, { it.sugarGrams },
                        Thresholds.SUGAR_CAUTION_G, Thresholds.SUGAR_OVER_G)
                    numeric("sat fat", "g", food.saturatedFatGrams, { it.saturatedFatGrams },
                        Thresholds.SATFAT_CAUTION_G, Thresholds.SATFAT_OVER_G)
                }
                Condition.DIABETES -> {
                    numeric("carbs", "g", food.carbGrams, { it.carbGrams },
                        Thresholds.CARB_CAUTION_G, Thresholds.CARB_OVER_G)
                    numeric("sugar", "g", food.sugarGrams, { it.sugarGrams },
                        Thresholds.SUGAR_CAUTION_G, Thresholds.SUGAR_OVER_G)
                }
                Condition.HYPERTENSION ->
                    numeric("sodium", "mg", food.sodiumMg, { it.sodiumMg },
                        Thresholds.SODIUM_CAUTION_MG, Thresholds.SODIUM_OVER_MG)
                Condition.CHOLESTEROL ->
                    numeric("sat fat", "g", food.saturatedFatGrams, { it.saturatedFatGrams },
                        Thresholds.SATFAT_CAUTION_G, Thresholds.SATFAT_OVER_G)
                else -> Unit
            }

            if (details.isNotEmpty()) warnings += ConditionWarning(c, sev, details)
        }

        val flagged = warnings.filter { it.severity != Verdict.PASS }
        val overall = flagged.fold(fatVerdict) { acc, w -> worst(acc, w.severity) }

        val parts = mutableListOf<String>()
        when (fatVerdict) {
            Verdict.OVER_LIMIT -> parts += "EXCEEDS MEAL FAT LIMIT"
            Verdict.CAUTION -> parts += "APPROACHING FAT LIMIT"
            Verdict.PASS -> Unit
        }
        if (flagged.isNotEmpty()) {
            parts += "${flagged.size} CONDITION FLAG" + if (flagged.size > 1) "S" else ""
        }

        return VerdictResult(
            verdict = overall,
            headline = if (parts.isEmpty()) "WITHIN LIMITS" else parts.joinToString(" / "),
            reasons = reasons,
            warnings = warnings,
            triggers = triggers
        )
    }

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
}
