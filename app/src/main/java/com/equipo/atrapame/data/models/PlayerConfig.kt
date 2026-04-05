package com.equipo.atrapame.data.models

data class PlayerConfig(
    val name: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM
)

enum class Difficulty(val displayName: String, val enemySpeed: Int) {
    EASY("Fácil", 700), // Antes 1000, ahora mucho mas rápido
    MEDIUM("Medio", 500), // Antes 750
    HARD("Difícil", 350), // Antes 500
    DYNAMIC("Dinámico", 500), // Base speed
    IMPOSSIBLE("Imposible", 200) // Nueva dificultad super frenética
}