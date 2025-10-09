package com.equipo.atrapame.data.repository

import com.equipo.atrapame.data.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class GameRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val scoresCollection = firestore.collection("scores")
    
    suspend fun saveScore(score: Score): Result<String> {
        return try {
            val docRef = scoresCollection.add(score).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTopScores(limit: Int = 10): Result<List<Score>> {
        return try {
            val querySnapshot = scoresCollection
                .orderBy("moves", Query.Direction.ASCENDING)
                .orderBy("timeElapsed", Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val scores = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Score::class.java)?.copy(id = doc.id)
            }
            Result.success(scores)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlayerScores(playerName: String): Result<List<Score>> {
        return try {
            val querySnapshot = scoresCollection
                .whereEqualTo("playerName", playerName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val scores = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Score::class.java)?.copy(id = doc.id)
            }
            Result.success(scores)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}