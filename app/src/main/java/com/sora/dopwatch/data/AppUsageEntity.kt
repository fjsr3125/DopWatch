package com.sora.dopwatch.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "app_usage",
    primaryKeys = ["date", "packageName"],
    indices = [Index("date")]
)
data class AppUsageEntity(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val date: String // yyyy-MM-dd
)
