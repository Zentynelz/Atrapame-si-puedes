package com.equipo.atrapame.data.models

data class PlayerConfig(
    val name: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM
)

enum class Difficulty(val displayName: String, val enemySpeed: Int) {
    EASY("Easy", 1000),
    MEDIUM("Medium", 750),
    HARD("Hard", 500)
}