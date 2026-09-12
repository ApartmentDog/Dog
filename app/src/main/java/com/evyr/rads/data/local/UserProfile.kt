package com.evyr.rads.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val heightCm: Double,
    val weightKg: Double,
    val age: Int,
    val sex: String,
    val goal: String, // lose, maintain, gain
    val activityLevel: String,
    val useImperial: Boolean = false
)
