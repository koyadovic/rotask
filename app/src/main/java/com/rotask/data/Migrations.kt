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

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) Create the groups table.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `groups` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `name` TEXT NOT NULL,
                `dailyMinutes` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 2) Carry the previous global dailyMinutes into a default "General" group.
        val previousDailyMinutes = db.query("SELECT `dailyMinutes` FROM `settings` WHERE `id` = 1").use { c ->
            if (c.moveToFirst()) c.getInt(0) else 60
        }
        db.execSQL(
            "INSERT INTO `groups` (`name`, `dailyMinutes`) VALUES ('General', $previousDailyMinutes)"
        )

        // 3) Recreate tasks with a groupId column pointing at the General group.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks_new` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `groupId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `weight` REAL NOT NULL,
                `enabled` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `tasks_new` (`id`, `groupId`, `name`, `description`, `weight`, `enabled`)
            SELECT `id`,
                   (SELECT `id` FROM `groups` WHERE `name` = 'General' LIMIT 1),
                   `name`, `description`, `weight`, `enabled`
            FROM `tasks`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `tasks`")
        db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_groupId` ON `tasks` (`groupId`)")

        // 4) Drop the now-unused settings table.
        db.execSQL("DROP TABLE `settings`")
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks_new` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `weight` REAL NOT NULL,
                `enabled` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `tasks_new` (`id`, `name`, `description`, `weight`, `enabled`)
            SELECT `id`, `name`, `description`, `weight`, `enabled` FROM `tasks`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `tasks`")
        db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `settings_new` (
                `id` INTEGER NOT NULL PRIMARY KEY,
                `dailyMinutes` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `settings_new` (`id`, `dailyMinutes`)
            SELECT `id`, `dailyMinutes` FROM `settings`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `settings`")
        db.execSQL("ALTER TABLE `settings_new` RENAME TO `settings`")
    }
}
