package com.sora.dopwatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageTimeMs DESC")
    fun getUsageByDate(date: String): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageTimeMs DESC")
    suspend fun getUsageByDateOnce(date: String): List<AppUsageEntity>

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("SELECT SUM(usageTimeMs) FROM app_usage WHERE date = :date")
    suspend fun getTotalUsageByDate(date: String): Long?
}
