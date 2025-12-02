package com.equipo.atrapame.data.local

import android.content.Context
import androidx.room.Room
import com.equipo.atrapame.data.models.Score
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocalGameRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "local_game_db"
    ).build()

    private val scoreDao = db.scoreDao()
    private val firestore = FirebaseFirestore.getInstance()

    // ----------- GUARDAR LOCAL & SINCRONIZAR ----------
    suspend fun saveScore(score: Score) = withContext(Dispatchers.IO) {
        val localEntity = LocalScoreEntity(
            playerName = score.playerName,
            moves = score.moves,
            timeElapsed = score.timeElapsed,
            timestamp = score.timestamp
        )

        scoreDao.insert(localEntity)

        syncPendingScores()
    }

    // ----------- OBTENER DESDE LOCAL ----------
    suspend fun getTopScores(): List<Score> = withContext(Dispatchers.IO) {
        scoreDao.getTopScores(10).map { e ->
            Score(
                id = e.idFirebase ?: e.localId.toString(),
                playerName = e.playerName,
                moves = e.moves,
                timeElapsed = e.timeElapsed,
                timestamp = e.timestamp
            )
        }
    }

    suspend fun getPlayerScores(playerName: String): List<Score> = withContext(Dispatchers.IO) {
        scoreDao.getPlayerScores(playerName).map { e ->
            Score(
                id = e.idFirebase ?: e.localId.toString(),
                playerName = e.playerName,
                moves = e.moves,
                timeElapsed = e.timeElapsed,
                timestamp = e.timestamp
            )
        }
    }

    // ----------- SINCRONIZACIÓN AUTOMÁTICA A FIREBASE ----------
    private suspend fun syncPendingScores() = withContext(Dispatchers.IO) {
        val pending = scoreDao.getPendingSyncScores()

        for (item in pending) {
            try {
                val firebaseDoc = firestore.collection("scores")
                    .add(
                        mapOf(
                            "playerName" to item.playerName,
                            "moves" to item.moves,
                            "timeElapsed" to item.timeElapsed,
                            "timestamp" to item.timestamp
                        )
                    )
                    .await()

                scoreDao.markAsSynced(item.localId, firebaseDoc.id)

            } catch (_: Exception) {
                // No se marca como synced, se intentará luego automáticamente.
            }
        }
    }
}