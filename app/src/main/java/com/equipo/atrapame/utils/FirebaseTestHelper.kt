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
                Log.d(TAG, "=== INICIANDO PRUEBA DE FIREBASE ===")
                
                val firestore = FirebaseFirestore.getInstance()
                Log.d(TAG, "FirebaseFirestore instance obtenida")
                
                // Crear un score de prueba
                val testScore = Score(
                    playerName = "PRUEBA_FIREBASE_${System.currentTimeMillis()}",
                    moves = 42,
                    timeElapsed = 30000L,
                    difficulty = Difficulty.EASY,
                    timestamp = System.currentTimeMillis()
                )
                
                Log.d(TAG, "Score de prueba creado: ${testScore.playerName}")
                
                val dataToSave = mapOf(
                    "playerName" to testScore.playerName,
                    "moves" to testScore.moves,
                    "timeElapsed" to testScore.timeElapsed,
                    "difficulty" to testScore.difficulty.name,
                    "timestamp" to testScore.timestamp
                )
                
                Log.d(TAG, "Datos a guardar: $dataToSave")
                Log.d(TAG, "Intentando guardar en colección 'scores'...")
                
                // Intentar guardar en Firebase
                val docRef = firestore.collection("scores")
                    .add(dataToSave)
                    .await()
                
                Log.d(TAG, "✅ Guardado exitoso con ID: ${docRef.id}")
                
                // Intentar leer de Firebase para confirmar
                Log.d(TAG, "Verificando que se guardó correctamente...")
                val savedDoc = firestore.collection("scores")
                    .document(docRef.id)
                    .get()
                    .await()
                
                if (savedDoc.exists()) {
                    val playerName = savedDoc.getString("playerName")
                    val moves = savedDoc.getLong("moves")
                    val difficulty = savedDoc.getString("difficulty")
                    
                    Log.d(TAG, "✅ Lectura exitosa: $playerName con $moves movimientos, dificultad: $difficulty")
                    
                    "✅ Firebase funciona PERFECTAMENTE!\n\n" +
                    "🔥 Proyecto ID: atrapame-si-puedes-30ab4\n" +
                    "📄 ID del documento: ${docRef.id}\n" +
                    "👤 Jugador: $playerName\n" +
                    "🎯 Movimientos: $moves\n" +
                    "⚡ Dificultad: $difficulty\n\n" +
                    "Ve a Firebase Console para verificar:\n" +
                    "https://console.firebase.google.com/project/atrapame-si-puedes-30ab4/firestore"
                } else {
                    Log.e(TAG, "❌ El documento no existe después de guardarlo")
                    "❌ Error: No se pudo leer el documento guardado"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error completo en Firebase", e)
                val errorDetails = """
                    ❌ Error de Firebase:
                    
                    Mensaje: ${e.message}
                    Tipo: ${e.javaClass.simpleName}
                    Causa: ${e.cause?.message ?: "No especificada"}
                    
                    Posibles soluciones:
                    1. Verifica tu conexión a internet
                    2. Revisa las reglas de Firestore
                    3. Confirma que el proyecto Firebase esté activo
                    4. Verifica que google-services.json sea correcto
                    
                    Stack trace completo en los logs (busca 'FirebaseTest')
                """.trimIndent()
                
                errorDetails
            }
        }
        
        suspend fun readAllScores(): String {
            return try {
                Log.d(TAG, "=== LEYENDO SCORES DE FIREBASE ===")
                val firestore = FirebaseFirestore.getInstance()
                
                Log.d(TAG, "Consultando colección 'scores'...")
                
                val querySnapshot = firestore.collection("scores")
                    .orderBy("timestamp")
                    .limit(20)
                    .get()
                    .await()
                
                Log.d(TAG, "Consulta completada. Documentos encontrados: ${querySnapshot.size()}")
                
                if (querySnapshot.isEmpty) {
                    Log.w(TAG, "No se encontraron documentos en la colección 'scores'")
                    """
                    📋 No hay puntuaciones en Firebase
                    
                    Esto puede significar:
                    1. Nunca se ha guardado ninguna puntuación
                    2. Las reglas de Firestore bloquean la lectura
                    3. Hay un problema de permisos
                    
                    Intenta jugar una partida y ganar para crear datos.
                    """.trimIndent()
                } else {
                    val scores = querySnapshot.documents.mapIndexed { index, doc ->
                        val playerName = doc.getString("playerName") ?: "Desconocido"
                        val moves = doc.getLong("moves") ?: 0
                        val timeElapsed = doc.getLong("timeElapsed") ?: 0
                        val difficulty = doc.getString("difficulty") ?: "MEDIUM"
                        val timestamp = doc.getLong("timestamp") ?: 0
                        
                        Log.d(TAG, "Score ${index + 1}: $playerName, $moves movimientos, ID: ${doc.id}")
                        
                        "${index + 1}. $playerName\n" +
                        "   🎯 $moves movimientos | ⏱️ ${timeElapsed/1000}s | ⚡ $difficulty\n" +
                        "   📄 ID: ${doc.id}\n"
                    }
                    
                    "📋 PUNTUACIONES EN FIREBASE (${scores.size}):\n\n" + 
                    scores.joinToString("\n") +
                    "\n🔗 Ver en consola: https://console.firebase.google.com/project/atrapame-si-puedes-30ab4/firestore"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo scores de Firebase", e)
                """
                ❌ Error leyendo Firebase: ${e.message}
                
                Tipo de error: ${e.javaClass.simpleName}
                
                Verifica:
                1. Conexión a internet
                2. Reglas de Firestore (deben permitir lectura)
                3. Configuración del proyecto
                """.trimIndent()
            }
        }
    }
}