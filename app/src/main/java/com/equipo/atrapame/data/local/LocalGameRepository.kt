package com.equipo.atrapame.data.local

import android.content.Context
import androidx.room.Room
import com.equipo.atrapame.data.models.Score
import com.equipo.atrapame.data.models.Difficulty
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocalGameRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "local_game_db"
    ).fallbackToDestructiveMigration() // Para desarrollo, en producción usar migraciones apropiadas
    .build()

    private val scoreDao = db.scoreDao()
    private val firestore = FirebaseFirestore.getInstance()

    // ----------- GUARDAR LOCAL & SINCRONIZAR ----------
    suspend fun saveScore(score: Score) = withContext(Dispatchers.IO) {
        // Primero intentar guardar directamente en Firebase
        try {
            val firebaseDoc = firestore.collection("scores")
                .add(
                    mapOf(
                        "playerName" to score.playerName,
                        "moves" to score.moves,
                        "timeElapsed" to score.timeElapsed,
                        "difficulty" to score.difficulty.name,
                        "timestamp" to score.timestamp
                    )
                )
                .await()

            // Si Firebase funciona, guardar local con ID de Firebase
            val localEntity = LocalScoreEntity(
                idFirebase = firebaseDoc.id,
                playerName = score.playerName,
                moves = score.moves,
                timeElapsed = score.timeElapsed,
                difficulty = score.difficulty.name,
                timestamp = score.timestamp,
                synced = true
            )
            scoreDao.insert(localEntity)

        } catch (e: Exception) {
            // Si Firebase falla, guardar solo local para sincronizar después
            val localEntity = LocalScoreEntity(
                playerName = score.playerName,
                moves = score.moves,
                timeElapsed = score.timeElapsed,
                difficulty = score.difficulty.name,
                timestamp = score.timestamp,
                synced = false
            )
            scoreDao.insert(localEntity)
            
            // Intentar sincronizar scores pendientes
            syncPendingScores()
        }
    }

    // ----------- OBTENER DESDE LOCAL ----------
    suspend fun getTopScores(): List<Score> = withContext(Dispatchers.IO) {
        scoreDao.getTopScores(10).map { e ->
            Score(
                id = e.idFirebase ?: e.localId.toString(),
                playerName = e.playerName,
                moves = e.moves,
                timeElapsed = e.timeElapsed,
                difficulty = try { 
                    com.equipo.atrapame.data.models.Difficulty.valueOf(e.difficulty) 
                } catch (_: Exception) { 
                    com.equipo.atrapame.data.models.Difficulty.MEDIUM 
                },
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
                difficulty = try { 
                    com.equipo.atrapame.data.models.Difficulty.valueOf(e.difficulty) 
                } catch (_: Exception) { 
                    com.equipo.atrapame.data.models.Difficulty.MEDIUM 
                },
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
                            "difficulty" to item.difficulty,
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