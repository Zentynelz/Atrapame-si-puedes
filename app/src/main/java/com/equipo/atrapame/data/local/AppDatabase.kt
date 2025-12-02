package com.equipo.atrapame.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocalScoreEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
}
