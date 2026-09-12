package com.evyr.rads.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_log_entry")
data class FoodLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mealSlot: String, // breakfast, lunch, dinner, snack
    val name: String,
    val calories: Int,
    val fatGrams: Double,
    val proteinGrams: Double,
    val carbGrams: Double,
    val source: String, // manual, barcode, search, menu
    val flagged: Boolean = false,
    val flagReason: String? = null
)
