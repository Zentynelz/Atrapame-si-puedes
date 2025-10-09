package com.equipo.atrapame.data.models

data class Position(
    val row: Int,
    val col: Int
) {
    fun isValid(boardSize: Int = GameState.BOARD_SIZE): Boolean {
        return row in 0 until boardSize && col in 0 until boardSize
    }
    
    fun getAdjacentPositions(): List<Position> {
        return listOf(
            Position(row - 1, col), // Up
            Position(row + 1, col), // Down
            Position(row, col - 1), // Left
            Position(row, col + 1)  // Right
        ).filter { it.isValid() }
    }
    
    fun distanceTo(other: Position): Int {
        return kotlin.math.abs(row - other.row) + kotlin.math.abs(col - other.col)
    }
}