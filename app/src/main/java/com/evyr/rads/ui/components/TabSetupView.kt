package com.evyr.rads.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.evyr.rads.BuildConfig
import com.evyr.rads.data.AppPrefs
import com.evyr.rads.data.Condition
import com.evyr.rads.data.SecureStore
import com.evyr.rads.data.Units
import com.evyr.rads.data.local.UserProfile
import com.evyr.rads.ui.theme.*

/**
 * Everything the user types lives in a local draft. Nothing touches the
 * database until SAVE, so typing can't be eaten by an async round trip and a
 * field can be cleared and retyped without snapping back.
 */
private data class SetupDraft(
    val imperial: Boolean,
    val name: String,
    val age: String,
    val sex: String,
    val feet: String,
    val inches: String,
    val heightCm: String,
    val weight: String,
    val goal: String,
    val goalWeight: String,
    val rate: String,
    val activity: String,
    val fatLimit: String,
    val conditions: Set<Condition>
) {
    companion object {
        fun from(p: UserProfile): SetupDraft {
            val (ft, inch) = Units.cmToFeetInches(p.heightCm)
            return SetupDraft(
                imperial = p.useImperial,
                name = p.name,
                age = if (p.age > 0) p.age.toString() else "",
                sex = p.sex,
                feet = if (p.heightCm > 0) ft.toString() else "",
                inches = if (p.heightCm > 0) inch.toString() else "",
                heightCm = if (p.heightCm > 0) trim(p.heightCm) else "",
                weight = Units.displayWeight(p.weightKg, p.useImperial),
                goal = p.goal,
                goalWeight = Units.displayWeight(p.goalWeightKg, p.useImperial),
                rate = trim(p.rateLbsPerWeek),
                activity = p.activityLevel,
                fatLimit = trim(p.fatWarnGramsPerMeal),
                conditions = p.conditionSet()
            )
        }
    }

    private fun heightCmValue(): Double? = if (imperial) {
        val f = if (feet.isBlank()) 0 else feet.trim().toIntOrNull() ?: return null
        val i = if (inches.isBlank()) 0 else inches.trim().toIntOrNull() ?: return null
        if (i !in 0..11) return null
        Units.feetInchesToCm(f, i)
    } else {
        if (heightCm.isBlank()) 0.0 else heightCm.trim().toDoubleOrNull()
    }

    private fun weightKgValue(text: String): Double? =
        if (text.isBlank()) 0.0 else Units.parseWeightToKg(text.trim(), imperial)

    /**
     * Builds the profile to save, or an error explaining which field is wrong.
     * Fields left as they were keep their stored value exactly, so unit
     * rounding (180 cm -> 5'11" -> 180.34 cm) never drifts untouched data.
     */
    fun toProfile(base: UserProfile, baseline: SetupDraft): Pair<UserProfile?, String?> {
        val ageV = if (age.isBlank()) 0 else age.trim().toIntOrNull()
            ?: return null to "Age must be a whole number."
        val heightV = heightCmValue()
            ?: return null to if (imperial) "Height: whole feet, inches 0-11." else "Height must be a number."
        val weightV = weightKgValue(weight) ?: return null to "Weight must be a number."
        val goalV = weightKgValue(goalWeight) ?: return null to "Target weight must be a number."
        val rateV = rate.trim().toDoubleOrNull() ?: return null to "Pick a rate."
        val fatV = fatLimit.trim().toDoubleOrNull()
            ?: return null to "Fat limit must be a number."
        if (fatV <= 0) return null to "Fat limit must be above zero."

        val sameUnits = imperial == baseline.imperial
        val heightUntouched = sameUnits && feet == baseline.feet &&
            inches == baseline.inches && heightCm == baseline.heightCm
        val weightUntouched = sameUnits && weight == baseline.weight
        val goalUntouched = sameUnits && goalWeight == baseline.goalWeight

        return base.copy(
            useImperial = imperial,
            name = name.trim(),
            age = ageV,
            sex = sex,
            heightCm = if (heightUntouched) base.heightCm else heightV,
            weightKg = if (weightUntouched) base.weightKg else weightV,
            goal = goal,
            goalWeightKg = if (goalUntouched) base.goalWeightKg else goalV,
            rateLbsPerWeek = rateV,
            activityLevel = activity,
            fatWarnGramsPerMeal = fatV,
            conditions = Condition.toCsv(conditions)
        ) to null
    }

    /** Switching units converts what's typed, rather than reinterpreting it. */
    fun withUnits(toImperial: Boolean): SetupDraft {
        if (toImperial == imperial) return this
        val hCm = heightCmValue()
        val wKg = weightKgValue(weight)
        val gKg = weightKgValue(goalWeight)
        val (ft, inch) = Units.cmToFeetInches(hCm ?: 0.0)
        return copy(
            imperial = toImperial,
            feet = if ((hCm ?: 0.0) > 0) ft.toString() else feet,
            inches = if ((hCm ?: 0.0) > 0) inch.toString() else inches,
            heightCm = if ((hCm ?: 0.0) > 0) trim(hCm!!) else heightCm,
            weight = wKg?.let { Units.displayWeight(it, toImperial) } ?: weight,
            goalWeight = gKg?.let { Units.displayWeight(it, toImperial) } ?: goalWeight
        )
    }
}

@Composable
fun TabSetupView(
    scrollState: ScrollState,
    profile: UserProfile?,
    onUpdate: (UserProfile) -> Unit
) {
    // Seed the draft once when the profile first arrives; never overwrite
    // it from the database while the user is editing.
    var draft by remember { mutableStateOf<SetupDraft?>(null) }
    var justSaved by remember { mutableStateOf(false) }
    LaunchedEffect(profile) {
        if (draft == null && profile != null) draft = SetupDraft.from(profile)
    }

    val d = draft
    if (profile == null || d == null) {
        Text(
            "LOADING PROFILE...",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = AmberDim
        )
        return
    }

    fun edit(change: (SetupDraft) -> SetupDraft) {
        draft = change(draft ?: d)
        justSaved = false
    }

    // Compare on-screen text, not converted numbers, so opening the tab
    // doesn't show phantom unsaved changes from rounding.
    val baseline = SetupDraft.from(profile)
    val (candidate, error) = d.toProfile(profile, baseline)
    val dirty = d != baseline && candidate != null
    val preview = candidate ?: profile
    val imp = d.imperial
    val wLabel = if (imp) "lb" else "kg"

    val save = {
        candidate?.let {
            onUpdate(it)
            draft = SetupDraft.from(it)
            justSaved = true
        }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(scrollState)) {

        SaveBar(dirty = dirty, justSaved = justSaved, error = error, onSave = { save() })

        SectionLabel("UNITS")
        ChoiceRow("MEASUREMENT", listOf("imperial", "metric"),
            if (imp) "imperial" else "metric") { choice ->
            edit { it.withUnits(choice == "imperial") }
        }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("OPERATOR")
        EditField("NAME", d.name) { v -> edit { it.copy(name = v) } }
        EditNumber("AGE", d.age) { v -> edit { it.copy(age = v) } }
        ChoiceRow("SEX", listOf("male", "female", "unspecified"), d.sex) { v ->
            edit { it.copy(sex = v) }
        }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("BODY")
        if (imp) {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    EditNumber("HEIGHT ft", d.feet) { v -> edit { it.copy(feet = v) } }
                }
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    EditNumber("in", d.inches) { v -> edit { it.copy(inches = v) } }
                }
            }
        } else {
            EditNumber("HEIGHT cm", d.heightCm) { v -> edit { it.copy(heightCm = v) } }
        }
        EditNumber("WEIGHT $wLabel", d.weight) { v -> edit { it.copy(weight = v) } }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("OBJECTIVE")
        ChoiceRow("GOAL", listOf("lose", "maintain", "gain"), d.goal) { v ->
            edit { it.copy(goal = v) }
        }
        if (d.goal != "maintain") {
            EditNumber("TARGET $wLabel", d.goalWeight) { v -> edit { it.copy(goalWeight = v) } }
            ChoiceRow("RATE lb/wk", listOf("0.5", "1", "1.5", "2"), d.rate) { v ->
                edit { it.copy(rate = v) }
            }
            preview.poundsToGoal()?.let { lbs ->
                StatRow("TO GOAL", "${trim(kotlin.math.abs(lbs))} lb")
            }
            preview.weeksToGoal()?.let { wk ->
                StatRow("ETA", if (wk == 0) "AT GOAL" else "$wk weeks")
            }
        }
        ChoiceRow(
            "ACTIVITY",
            listOf("sedentary", "light", "moderate", "active", "very_active"),
            d.activity
        ) { v -> edit { it.copy(activity = v) } }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("FAT LIMIT (PER MEAL)")
        Text(
            "Applied to each meal separately. Not a daily budget.",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = AmberFaint,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        EditNumber("GRAMS", d.fatLimit) { v -> edit { it.copy(fatLimit = v) } }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("HEALTH CONDITIONS")
        Text(
            "Tick any that apply. Foods get flagged for them before you log. " +
                "General guidance only — your doctor's instructions override these.",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = AmberFaint,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Condition.values().forEach { c ->
            val on = c in d.conditions
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        edit {
                            it.copy(conditions = if (on) it.conditions - c else it.conditions + c)
                        }
                    }
                    .background(if (on) RowHighlight else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Text(
                    (if (on) "[X] " else "[ ] ") + c.label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) AmberBright else AmberDim
                )
                Text(
                    "watches: " + c.watches,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = AmberFaint,
                    modifier = Modifier.padding(start = 28.dp, top = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel(if (dirty) "COMPUTED (UNSAVED)" else "COMPUTED")
        StatRow("BMR", preview.bmr().toInt().toString())
        StatRow("TDEE", preview.tdee().toInt().toString())
        StatRow(
            "ADJUST",
            preview.dailyAdjustment().let { if (it >= 0) "+$it" else "$it" } + " kcal"
        )
        StatRow("TARGET", "${preview.calorieTarget()} kcal", emphasize = true)

        SaveBar(dirty = dirty, justSaved = justSaved, error = error, onSave = { save() })

        // ---- Device settings: these save immediately, they aren't profile data ----
        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("INTERFACE")
        val bootCtx = LocalContext.current
        var bootOn by remember { mutableStateOf(AppPrefs.bootSequenceEnabled(bootCtx)) }
        ChoiceRow("BOOT SEQUENCE", listOf("on", "off"), if (bootOn) "on" else "off") {
            bootOn = it == "on"
            AppPrefs.setBootSequenceEnabled(bootCtx, bootOn)
        }

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("FOOD DATABASE")
        Text(
            "USDA key. Blank uses a shared demo key that rate limits quickly. Saves as you type.",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = AmberFaint,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        var usdaKey by remember { mutableStateOf(SecureStore.usdaKey(bootCtx)) }
        EditField("USDA KEY", usdaKey) {
            usdaKey = it
            SecureStore.setUsdaKey(bootCtx, it)
        }
        StatRow("DB KEY", if (usdaKey.isNotBlank()) "SET" else "DEMO (limited)")

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("AI FOOD DETECTION")
        Text(
            "Gemini key for photo and fallback estimates. Saves as you type.",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = AmberFaint,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        var apiKey by remember { mutableStateOf(SecureStore.geminiKey(bootCtx)) }
        var model by remember { mutableStateOf(SecureStore.geminiModel(bootCtx)) }
        EditField("API KEY", apiKey) {
            apiKey = it
            SecureStore.setGeminiKey(bootCtx, it)
        }
        EditField("MODEL", model) {
            model = it
            SecureStore.setGeminiModel(bootCtx, it)
        }
        StatRow("AI STATUS", if (apiKey.isNotBlank()) "ENABLED" else "OFF")

        Spacer(Modifier.height(4.dp))
        Hairline()
        SectionLabel("SYSTEM")
        StatRow("VERSION", BuildConfig.VERSION_NAME, emphasize = true)
        StatRow("BUILD", BuildConfig.VERSION_CODE.toString())
        StatRow("PACKAGE", BuildConfig.APPLICATION_ID)
        StatRow("AI MODEL", SecureStore.geminiModel(bootCtx))

        Spacer(Modifier.height(12.dp))
    }
}

/** Save button with dirty / saved / error feedback. */
@Composable
private fun SaveBar(
    dirty: Boolean,
    justSaved: Boolean,
    error: String?,
    onSave: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "[SAVE PROFILE]",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (dirty) AmberBright else AmberFaint,
            modifier = Modifier
                .background(if (dirty) RowHighlight else Color.Transparent)
                .clickable(enabled = dirty) { onSave() }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            when {
                error != null -> error
                dirty -> "UNSAVED CHANGES"
                justSaved -> "SAVED"
                else -> "UP TO DATE"
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = when {
                error != null -> AmberWarn
                dirty -> Amber
                else -> AmberFaint
            }
        )
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    FieldShell(label) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Amber
            ),
            cursorBrush = SolidColor(Amber),
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenInk, RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun EditNumber(label: String, value: String, onChange: (String) -> Unit) {
    FieldShell(label) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Amber
            ),
            cursorBrush = SolidColor(Amber),
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenInk, RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun FieldShell(label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AmberDim,
            modifier = Modifier.width(88.dp).padding(top = 5.dp)
        )
        Column(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = AmberDim)
        Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
            options.forEach { opt ->
                val isSel = opt == selected
                Text(
                    text = opt.replace('_', ' ').uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) AmberBright else AmberFaint,
                    modifier = Modifier
                        .clickable { onSelect(opt) }
                        .background(if (isSel) RowHighlight else Color.Transparent)
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(3.dp))
            }
        }
    }
}
