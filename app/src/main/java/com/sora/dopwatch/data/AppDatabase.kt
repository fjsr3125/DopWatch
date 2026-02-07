package com.sora.dopwatch.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AppUsageEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 新テーブル作成（date + packageName が主キー）
                db.execSQL("""
                    CREATE TABLE app_usage_new (
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        usageTimeMs INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        PRIMARY KEY(date, packageName)
                    )
                """)
                // 旧データから重複排除して移行（同日・同パッケージはMAXの1行のみ）
                db.execSQL("""
                    INSERT OR REPLACE INTO app_usage_new (packageName, appName, usageTimeMs, date)
                    SELECT packageName, appName, MAX(usageTimeMs), date
                    FROM app_usage
                    GROUP BY date, packageName
                """)
                db.execSQL("DROP TABLE app_usage")
                db.execSQL("ALTER TABLE app_usage_new RENAME TO app_usage")
                db.execSQL("CREATE INDEX index_app_usage_date ON app_usage(date)")
            }
        }
    }
}
