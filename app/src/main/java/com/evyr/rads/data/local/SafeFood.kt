package com.evyr.rads.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A food the person has marked as known-safe for them. It's a shortcut and a
 * reassurance, not an exemption: allergy and condition checks still run on
 * safe foods, since a "safe" item can change recipe or be mislabeled.
 *
 * Keyed by the lowercased, trimmed name so the same food logged with
 * different capitalisation maps to one entry.
 */
@Entity(tableName = "safe_food")
data class SafeFood(
    @PrimaryKey val foodKey: String,
    val name: String,
    val calories: Int,
    val fatGrams: Double,
    val proteinGrams: Double,
    val carbGrams: Double,
    val saturatedFatGrams: Double? = null,
    val sugarGrams: Double? = null,
    val fiberGrams: Double? = null,
    val sodiumMg: Double? = null,
    /** Comma-separated Trigger keys, carried over so checks match the original. */
    val triggers: String? = null,
    val source: String,
    val addedAt: Long
) {
    companion object {
        fun keyFor(name: String) = name.trim().lowercase()
    }
}

@Dao
interface SafeFoodDao {
    @Query("SELECT * FROM safe_food ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SafeFood>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(food: SafeFood)

    @Query("DELETE FROM safe_food WHERE foodKey = :foodKey")
    suspend fun delete(foodKey: String)
}
