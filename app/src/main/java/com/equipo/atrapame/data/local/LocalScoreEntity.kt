package com.equipo.atrapame.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class LocalScoreEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val idFirebase: String? = null,
    val playerName: String,
    val moves: Int,
    val timeElapsed: Long,
    val difficulty: String = "MEDIUM", // Guardamos como String para Room
    val timestamp: Long,
    val synced: Boolean = false // Indica si ya se envió a Firebase
)
