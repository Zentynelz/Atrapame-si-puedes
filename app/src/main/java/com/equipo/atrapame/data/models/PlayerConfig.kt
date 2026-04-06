package com.equipo.atrapame.data.models

data class PlayerConfig(
    val name: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM
)

enum class Difficulty(val displayName: String, val enemySpeed: Int) {
    EASY("Fácil", 700),
    MEDIUM("Medio", 500),
    HARD("Difícil", 250), // Antes 350
    DYNAMIC("Dinámico", 500), 
    IMPOSSIBLE("Imposible", 80) // Extremadamente veloz (imposible ganar sin macros)
}