package com.equipo.atrapame.data.models

/**
 * Represents the four cardinal directions the player can move on the logical grid.
 */
enum class Direction(val deltaRow: Int, val deltaCol: Int) {
    UP(deltaRow = -1, deltaCol = 0),
    DOWN(deltaRow = 1, deltaCol = 0),
    LEFT(deltaRow = 0, deltaCol = -1),
    RIGHT(deltaRow = 0, deltaCol = 1);
}