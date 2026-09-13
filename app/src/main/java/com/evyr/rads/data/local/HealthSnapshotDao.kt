package com.evyr.rads.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: HealthSnapshot)

    @Query("SELECT * FROM health_snapshot WHERE dayKey = :dayKey")
    fun observeDay(dayKey: Long): Flow<HealthSnapshot?>
}
