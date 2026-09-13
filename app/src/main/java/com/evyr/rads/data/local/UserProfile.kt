package com.evyr.rads.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val age: Int = 0,
    val sex: String = "unspecified",
    val goal: String = "maintain",
    val activityLevel: String = "moderate",
    val useImperial: Boolean = true,
    /**
     * Per-meal fat ceiling in grams. Deliberately per-meal, never a daily
     * budget — firm dietary requirement, do not aggregate.
     */
    val fatWarnGramsPerMeal: Double = 15.0,
    val onboarded: Boolean = false
) {
    /** Mifflin-St Jeor BMR. */
    fun bmr(): Double {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0) return 0.0
        val base = (10 * weightKg) + (6.25 * heightCm) - (5 * age)
        return when (sex) {
            "male" -> base + 5
            "female" -> base - 161
            else -> base - 78
        }
    }

    fun tdee(): Double {
        val multiplier = when (activityLevel) {
            "sedentary" -> 1.2
            "light" -> 1.375
            "moderate" -> 1.55
            "active" -> 1.725
            "very_active" -> 1.9
            else -> 1.55
        }
        return bmr() * multiplier
    }

    fun calorieTarget(): Int {
        val t = tdee()
        if (t <= 0) return 2000
        return when (goal) {
            "lose" -> (t - 500).toInt()
            "gain" -> (t + 300).toInt()
            else -> t.toInt()
        }
    }
}
