package com.sora.dopwatch.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val refreshMutex = Mutex()

    fun getTodayString(): String = dateFormat.format(System.currentTimeMillis())

    /**
     * UsageEvents APIでフォアグラウンド時間を正確に計算する。
     * ACTIVITY_RESUMED → ACTIVITY_PAUSED の差分のみをカウント。
     * これにより画面OFF中やバックグラウンド時間は含まれない。
     */
    fun queryTodayUsage(): List<AppUsageInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()

        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()

        // パッケージごとのフォアグラウンド開始時刻を追跡
        val foregroundStart = mutableMapOf<String, Long>()
        // パッケージごとの累計フォアグラウンド時間
        val usageMap = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // フォアグラウンドに来た
                    foregroundStart[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    // バックグラウンドに移った
                    val startTime = foregroundStart.remove(pkg)
                    if (startTime != null) {
                        val duration = event.timeStamp - startTime
                        if (duration > 0) {
                            usageMap[pkg] = (usageMap[pkg] ?: 0L) + duration
                        }
                    }
                }
            }
        }

        // まだフォアグラウンドにいるアプリ（現在使用中）は「今」までカウント
        for ((pkg, startTime) in foregroundStart) {
            val duration = end - startTime
            if (duration > 0) {
                usageMap[pkg] = (usageMap[pkg] ?: 0L) + duration
            }
        }

        val pm = context.packageManager
        return usageMap
            .filter { it.value > 60_000 } // 1分以上のみ
            .map { (packageName, totalMs) ->
                val appName = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(packageName, 0)
                    ).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName
                }
                AppUsageInfo(
                    packageName = packageName,
                    appName = appName,
                    usageTimeMs = totalMs
                )
            }
            .sortedByDescending { it.usageTimeMs }
    }

    suspend fun refreshAndSave() = refreshMutex.withLock {
        val today = getTodayString()
        val usages = queryTodayUsage().map { info ->
            AppUsageEntity(
                packageName = info.packageName,
                appName = info.appName,
                usageTimeMs = info.usageTimeMs,
                date = today
            )
        }
        dao.replaceByDate(today, usages)
    }

    fun getTodayUsageFlow(): Flow<List<AppUsageEntity>> {
        return dao.getUsageByDate(getTodayString())
    }

    suspend fun getTodayTotalMs(): Long {
        return dao.getTotalUsageByDate(getTodayString()) ?: 0L
    }
}
