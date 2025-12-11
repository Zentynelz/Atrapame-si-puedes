package com.equipo.atrapame.utils

import android.content.Context
import android.util.Log
import com.equipo.atrapame.data.models.Difficulty
import com.equipo.atrapame.data.models.Score
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseTestHelper {
    
    companion object {
        private const val TAG = "FirebaseTest"
        
        suspend fun testFirebaseConnection(context: Context): String {
            return try {
                val firestore = FirebaseFirestore.getInstance()
                
                // Crear un score de prueba
                val testScore = Score(
                    playerName = "PRUEBA_FIREBASE",
                    moves = 42,
                    timeElapsed = 30000L,
                    difficulty = Difficulty.EASY,
                    timestamp = System.currentTimeMillis()
                )
                
                Log.d(TAG, "Intentando guardar en Firebase...")
                
                // Intentar guardar en Firebase
                val docRef = firestore.collection("scores")
                    .add(
                        mapOf(
                            "playerName" to testScore.playerName,
                            "moves" to testScore.moves,
                            "timeElapsed" to testScore.timeElapsed,
                            "difficulty" to testScore.difficulty.name,
                            "timestamp" to testScore.timestamp
                        )
                    )
                    .await()
                
                Log.d(TAG, "Guardado exitoso con ID: ${docRef.id}")
                
                // Intentar leer de Firebase para confirmar
                val savedDoc = firestore.collection("scores")
                    .document(docRef.id)
                    .get()
                    .await()
                
                if (savedDoc.exists()) {
                    val playerName = savedDoc.getString("playerName")
                    val moves = savedDoc.getLong("moves")
                    
                    Log.d(TAG, "Lectura exitosa: $playerName con $moves movimientos")
                    
                    "✅ Firebase funciona correctamente!\n" +
                    "ID del documento: ${docRef.id}\n" +
                    "Jugador: $playerName\n" +
                    "Movimientos: $moves"
                } else {
                    "❌ Error: No se pudo leer el documento guardado"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error en Firebase", e)
                "❌ Error de Firebase: ${e.message}\n" +
                "Tipo: ${e.javaClass.simpleName}\n" +
                "Verifica tu conexión a internet y configuración de Firebase"
            }
        }
        
        suspend fun readAllScores(): String {
            return try {
                val firestore = FirebaseFirestore.getInstance()
                
                Log.d(TAG, "Leyendo todos los scores de Firebase...")
                
                val querySnapshot = firestore.collection("scores")
                    .orderBy("timestamp")
                    .limit(10)
                    .get()
                    .await()
                
                if (querySnapshot.isEmpty) {
                    "📋 No hay puntuaciones guardadas en Firebase"
                } else {
                    val scores = querySnapshot.documents.mapIndexed { index, doc ->
                        val playerName = doc.getString("playerName") ?: "Desconocido"
                        val moves = doc.getLong("moves") ?: 0
                        val timeElapsed = doc.getLong("timeElapsed") ?: 0
                        val difficulty = doc.getString("difficulty") ?: "MEDIUM"
                        
                        "${index + 1}. $playerName - $moves movimientos - ${timeElapsed/1000}s - $difficulty"
                    }
                    
                    "📋 Puntuaciones en Firebase (${scores.size}):\n" + scores.joinToString("\n")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo scores", e)
                "❌ Error leyendo Firebase: ${e.message}"
            }
        }
    }
}