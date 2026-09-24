package com.evyr.rads.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Insert
    suspend fun insert(entry: FoodLogEntry): Long

    @Delete
    suspend fun delete(entry: FoodLogEntry)

    @Query("SELECT * FROM food_log_entry WHERE timestamp BETWEEN :dayStart AND :dayEnd ORDER BY timestamp ASC")
    fun getEntriesForDay(dayStart: Long, dayEnd: Long): Flow<List<FoodLogEntry>>

    /** Everything logged since a moment, newest first -- for trends and recents. */
    @Query("SELECT * FROM food_log_entry WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEntriesSince(since: Long): Flow<List<FoodLogEntry>>

    @Query("SELECT * FROM food_log_entry WHERE mealSlot = :mealSlot AND timestamp BETWEEN :dayStart AND :dayEnd ORDER BY timestamp ASC")
    fun getEntriesForMealSlot(mealSlot: String, dayStart: Long, dayEnd: Long): Flow<List<FoodLogEntry>>
}
