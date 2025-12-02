package com.equipo.atrapame.data.local

import androidx.room.*

@Dao
interface ScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: LocalScoreEntity)

    @Query("SELECT * FROM scores ORDER BY moves ASC, timeElapsed ASC LIMIT :limit")
    suspend fun getTopScores(limit: Int): List<LocalScoreEntity>

    @Query("SELECT * FROM scores WHERE playerName = :playerName ORDER BY timestamp DESC")
    suspend fun getPlayerScores(playerName: String): List<LocalScoreEntity>

    @Query("SELECT * FROM scores WHERE synced = 0")
    suspend fun getPendingSyncScores(): List<LocalScoreEntity>

    @Query("UPDATE scores SET synced = 1, idFirebase = :firebaseId WHERE localId = :localId")
    suspend fun markAsSynced(localId: Int, firebaseId: String)
}
