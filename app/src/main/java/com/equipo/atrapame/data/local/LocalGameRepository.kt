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
        android.util.Log.d("LocalGameRepository", "=== GUARDANDO SCORE ===")
        android.util.Log.d("LocalGameRepository", "Score: ${score.playerName}, ${score.moves} movimientos, ${score.difficulty}")
        
        // Primero intentar guardar directamente en Firebase
        try {
            android.util.Log.d("LocalGameRepository", "Intentando guardar en Firebase...")
            
            val dataToSave = mapOf(
                "playerName" to score.playerName,
                "moves" to score.moves,
                "timeElapsed" to score.timeElapsed,
                "difficulty" to score.difficulty.name,
                "timestamp" to score.timestamp
            )
            
            android.util.Log.d("LocalGameRepository", "Datos a guardar: $dataToSave")
            
            val firebaseDoc = firestore.collection("scores")
                .add(dataToSave)
                .await()

            android.util.Log.d("LocalGameRepository", "✅ Firebase guardado exitosamente! ID: ${firebaseDoc.id}")

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
            
            android.util.Log.d("LocalGameRepository", "✅ También guardado localmente con sync=true")

        } catch (e: Exception) {
            android.util.Log.e("LocalGameRepository", "❌ Error guardando en Firebase: ${e.message}", e)
            
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
            
            android.util.Log.d("LocalGameRepository", "Guardado localmente con sync=false, intentando sincronizar...")
            
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
        android.util.Log.d("LocalGameRepository", "Sincronizando ${pending.size} scores pendientes...")

        for (item in pending) {
            try {
                android.util.Log.d("LocalGameRepository", "Sincronizando score ID ${item.localId}: ${item.playerName}")
                
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
                android.util.Log.d("LocalGameRepository", "✅ Score ${item.localId} sincronizado con Firebase ID: ${firebaseDoc.id}")

            } catch (e: Exception) {
                android.util.Log.e("LocalGameRepository", "❌ Error sincronizando score ${item.localId}: ${e.message}")
                // No se marca como synced, se intentará luego automáticamente.
            }
        }
    }
}