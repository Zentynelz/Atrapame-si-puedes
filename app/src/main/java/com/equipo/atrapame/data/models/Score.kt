package com.equipo.atrapame.data.models

data class Score(
    val id: String = "",
    val playerName: String = "",
    val moves: Int = 0,
    val timeElapsed: Long = 0L,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val timestamp: Long = System.currentTimeMillis(),
    val avgSmilingProb: Float = 0f,
    val avgRightEyeOpenProb: Float = 0f,
    val maxAudioAmplitude: Int = 0,
    val perceivedStressScore: Int = 0,
    val finalEmotion: String = "NEUTRAL",
    val exitReason: String = "FINISHED",
    val emotionTimeline: List<Map<String, Any>> = emptyList()

) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this("", "", 0, 0L, Difficulty.MEDIUM, System.currentTimeMillis(), 0f, 0f, 0, 0, "NEUTRAL", "FINISHED", emptyList())

    fun getFormattedTime(): String {
        val seconds = timeElapsed / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}