package com.rotask.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `enabled` INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks_new` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `weight` REAL NOT NULL,
                `enabled` INTEGER NOT NULL,
                `debtSeconds` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `tasks_new` (`id`, `name`, `description`, `weight`, `enabled`, `debtSeconds`)
            SELECT `id`, `name`, `description`, CAST(`weight` AS REAL), `enabled`, `debtSeconds` FROM `tasks`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `tasks`")
        db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
    }
}
