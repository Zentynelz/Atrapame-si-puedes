package com.equipo.atrapame.data.local

import android.content.Context
import androidx.room.Room
import android.provider.Settings
import com.equipo.atrapame.data.models.Score
import com.equipo.atrapame.data.models.Difficulty
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LocalGameRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "local_game_db"
    ).fallbackToDestructiveMigration() // Para desarrollo, en producción usar migraciones apropiadas
    .build()

    private val scoreDao = db.scoreDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "EMULATOR_OR_UNKNOWN"

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
                "timestamp" to score.timestamp,
                "avgSmilingProb" to score.avgSmilingProb,
                "avgRightEyeOpenProb" to score.avgRightEyeOpenProb,
                "maxAudioAmplitude" to score.maxAudioAmplitude,
                "perceivedStressScore" to score.perceivedStressScore,
                "finalEmotion" to score.finalEmotion,
                "exitReason" to score.exitReason,
                "emotionTimeline" to score.emotionTimeline,
                "deviceId" to deviceId
            )
            
            android.util.Log.d("LocalGameRepository", "Datos a guardar: $dataToSave")
            
            // Usamos un Timeout de 2.5s. Si Firebase no responde (como en el emulador sin wifi), 
            // no se quedará colgado para siempre y caerá en el catch para guardar offline.
            val firebaseDoc = kotlinx.coroutines.withTimeout(2500L) {
                firestore.collection("scores")
                    .add(dataToSave)
                    .await()
            }

            android.util.Log.d("LocalGameRepository", "✅ Firebase guardado exitosamente! ID: ${firebaseDoc.id}")

            // Si Firebase funciona, guardar local con ID de Firebase
            val localEntity = LocalScoreEntity(
                idFirebase = firebaseDoc.id,
                playerName = score.playerName,
                moves = score.moves,
                timeElapsed = score.timeElapsed,
                difficulty = score.difficulty.name,
                timestamp = score.timestamp,
                avgSmilingProb = score.avgSmilingProb,
                avgRightEyeOpenProb = score.avgRightEyeOpenProb,
                maxAudioAmplitude = score.maxAudioAmplitude,
                perceivedStressScore = score.perceivedStressScore,
                finalEmotion = score.finalEmotion,
                exitReason = score.exitReason,
                emotionTimelineJson = encodeTimeline(score.emotionTimeline),
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
                avgSmilingProb = score.avgSmilingProb,
                avgRightEyeOpenProb = score.avgRightEyeOpenProb,
                maxAudioAmplitude = score.maxAudioAmplitude,
                perceivedStressScore = score.perceivedStressScore,
                finalEmotion = score.finalEmotion,
                exitReason = score.exitReason,
                emotionTimelineJson = encodeTimeline(score.emotionTimeline),
                synced = false
            )
            scoreDao.insert(localEntity)
            
            android.util.Log.d("LocalGameRepository", "Guardado localmente con sync=false, intentando sincronizar...")
            // Intentar sincronizar scores pendientes en segundo plano sin bloquear
            kotlinx.coroutines.GlobalScope.launch {
                syncPendingScores()
            }
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
                timestamp = e.timestamp,
                avgSmilingProb = e.avgSmilingProb,
                avgRightEyeOpenProb = e.avgRightEyeOpenProb,
                maxAudioAmplitude = e.maxAudioAmplitude,
                perceivedStressScore = e.perceivedStressScore
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
                timestamp = e.timestamp,
                avgSmilingProb = e.avgSmilingProb,
                avgRightEyeOpenProb = e.avgRightEyeOpenProb,
                maxAudioAmplitude = e.maxAudioAmplitude,
                perceivedStressScore = e.perceivedStressScore
            )
        }
    }

    suspend fun getAllLocalScores(): List<Score> = withContext(Dispatchers.IO) {
        scoreDao.getAllLocalScores().map { e ->
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
                timestamp = e.timestamp,
                avgSmilingProb = e.avgSmilingProb,
                avgRightEyeOpenProb = e.avgRightEyeOpenProb,
                maxAudioAmplitude = e.maxAudioAmplitude,
                perceivedStressScore = e.perceivedStressScore,
                finalEmotion = e.finalEmotion,
                exitReason = e.exitReason,
                emotionTimeline = decodeTimeline(e.emotionTimelineJson)
            )
        }
    }

    suspend fun getPerformanceRank(difficulty: Difficulty, timeElapsed: Long): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val diffString = difficulty.name
        val betterCount = scoreDao.getBetterScoresCount(diffString, timeElapsed)
        val totalCount = scoreDao.getTotalWonScores(diffString)
        
        // Puesto = los que son mejores que yo + 1
        // Si acabamos de ganar y guardar, el totalCount nos incluye.
        val rank = betterCount + 1
        Pair(rank, totalCount)
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
                            "timestamp" to item.timestamp,
                            "avgSmilingProb" to item.avgSmilingProb,
                            "avgRightEyeOpenProb" to item.avgRightEyeOpenProb,
                            "maxAudioAmplitude" to item.maxAudioAmplitude,
                            "perceivedStressScore" to item.perceivedStressScore,
                            "finalEmotion" to item.finalEmotion,
                            "exitReason" to item.exitReason,
                            "emotionTimeline" to decodeTimeline(item.emotionTimelineJson), // Subir graph al recovery
                            "deviceId" to deviceId
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

    // ----------- ACTUALIZAR ENCUESTA ----------
    suspend fun updatePerceivedStress(scoreId: String, stressScore: Int) = withContext(Dispatchers.IO) {
        try {
            // Intenta en Firebase
            firestore.collection("scores").document(scoreId)
                .update("perceivedStressScore", stressScore)
                .await()
            android.util.Log.d("LocalGameRepository", "✅ Firebase Survey actualizado ID: $scoreId")
        } catch (e: Exception) {
            android.util.Log.e("LocalGameRepository", "❌ Error actualizando Firebase Survey: ${e.message}")
        }
        
        // Actualiza Local
        scoreDao.updateStressScore(scoreId, stressScore)
    }

    private fun encodeTimeline(timeline: List<Map<String, Any>>): String {
        val arr = org.json.JSONArray()
        for (m in timeline) {
            arr.put(org.json.JSONObject(m))
        }
        return arr.toString()
    }

    private fun decodeTimeline(json: String): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val map = mutableMapOf<String, Any>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = obj.get(key)
                }
                list.add(map)
            }
        } catch (e: Exception) {}
        return list
    }
}