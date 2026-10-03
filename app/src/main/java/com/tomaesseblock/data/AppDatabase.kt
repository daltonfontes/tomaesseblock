package com.tomaesseblock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BlockRuleEntity::class, SpamReportEntity::class, CallEventEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun spamReportDao(): SpamReportDao
    abstract fun callEventDao(): CallEventDao

    companion object {
        /** v2: coluna `action` nas regras (lista de permitidos). Regras antigas continuam bloqueando. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE block_rules ADD COLUMN `action` TEXT NOT NULL DEFAULT 'BLOCK'")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "tomaesseblock.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
