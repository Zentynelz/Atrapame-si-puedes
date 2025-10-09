package com.equipo.atrapame.data.models

data class Score(
    val id: String = "",
    val playerName: String,
    val moves: Int,
    val timeElapsed: Long,
    val difficulty: Difficulty,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        val seconds = timeElapsed / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}