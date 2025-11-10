package com.equipo.atrapame.data.models

data class Position(
    val row: Int,
    val col: Int
) {
    fun isValid(rows: Int = GameState.DEFAULT_ROWS, cols: Int = GameState.DEFAULT_COLS): Boolean {
        return row in 0 until rows && col in 0 until cols
    }

    fun getAdjacentPositions(
        rows: Int = GameState.DEFAULT_ROWS,
        cols: Int = GameState.DEFAULT_COLS
    ): List<Position> {
        return listOf(
            Position(row - 1, col), // Up
            Position(row + 1, col), // Down
            Position(row, col - 1), // Left
            Position(row, col + 1)  // Right
        ).filter { it.isValid(rows, cols) }
    }
    
    fun distanceTo(other: Position): Int {
        return kotlin.math.abs(row - other.row) + kotlin.math.abs(col - other.col)
    }
    fun offset(deltaRow: Int, deltaCol: Int): Position {
        return Position(row + deltaRow, col + deltaCol)
    }

    fun move(direction: Direction): Position {
        return offset(direction.deltaRow, direction.deltaCol)
    }
}