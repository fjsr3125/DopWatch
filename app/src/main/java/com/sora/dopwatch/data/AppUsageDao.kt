package com.sora.dopwatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(usages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageTimeMs DESC")
    abstract fun getUsageByDate(date: String): Flow<List<AppUsageEntity>>

    @Query("DELETE FROM app_usage WHERE date = :date")
    abstract suspend fun deleteByDate(date: String)

    @Query("SELECT SUM(usageTimeMs) FROM app_usage WHERE date = :date")
    abstract suspend fun getTotalUsageByDate(date: String): Long?

    @Transaction
    open suspend fun replaceByDate(date: String, usages: List<AppUsageEntity>) {
        deleteByDate(date)
        insertAll(usages)
    }
}
