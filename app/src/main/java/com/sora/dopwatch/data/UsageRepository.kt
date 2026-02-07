package com.sora.dopwatch.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long
)

@Singleton
class UsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppUsageDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayString(): String = dateFormat.format(System.currentTimeMillis())

    fun queryTodayUsage(): List<AppUsageInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            System.currentTimeMillis()
        )
        val pm = context.packageManager
        return stats
            .filter { it.totalTimeInForeground > 60_000 } // 1分以上のみ
            .map { stat ->
                val appName = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(stat.packageName, 0)
                    ).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    stat.packageName
                }
                AppUsageInfo(
                    packageName = stat.packageName,
                    appName = appName,
                    usageTimeMs = stat.totalTimeInForeground
                )
            }
            .sortedByDescending { it.usageTimeMs }
    }

    suspend fun refreshAndSave() {
        val today = getTodayString()
        val usages = queryTodayUsage()
        dao.deleteByDate(today)
        dao.insertAll(usages.map { info ->
            AppUsageEntity(
                packageName = info.packageName,
                appName = info.appName,
                usageTimeMs = info.usageTimeMs,
                date = today
            )
        })
    }

    fun getTodayUsageFlow(): Flow<List<AppUsageEntity>> {
        return dao.getUsageByDate(getTodayString())
    }

    suspend fun getTodayTotalMs(): Long {
        return dao.getTotalUsageByDate(getTodayString()) ?: 0L
    }
}
