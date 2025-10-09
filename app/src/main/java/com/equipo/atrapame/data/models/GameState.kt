package com.equipo.atrapame.data.models

data class GameState(
    val board: Array<Array<CellType>>,
    val playerPosition: Position,
    val enemyPosition: Position,
    val obstacles: List<Position>,
    val moves: Int = 0,
    val isGameWon: Boolean = false,
    val isGameLost: Boolean = false,
    val timeElapsed: Long = 0L
) {
    companion object {
        const val BOARD_SIZE = 8
        
        fun createInitialState(): GameState {
            val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { CellType.EMPTY } }
            val playerPos = Position(0, 0)
            val enemyPos = Position(BOARD_SIZE - 1, BOARD_SIZE - 1)
            
            // Set initial positions
            board[playerPos.row][playerPos.col] = CellType.PLAYER
            board[enemyPos.row][enemyPos.col] = CellType.ENEMY
            
            return GameState(
                board = board,
                playerPosition = playerPos,
                enemyPosition = enemyPos,
                obstacles = emptyList()
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameState
        return board.contentDeepEquals(other.board) &&
                playerPosition == other.playerPosition &&
                enemyPosition == other.enemyPosition &&
                obstacles == other.obstacles &&
                moves == other.moves &&
                isGameWon == other.isGameWon &&
                isGameLost == other.isGameLost
    }
    
    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + playerPosition.hashCode()
        result = 31 * result + enemyPosition.hashCode()
        result = 31 * result + obstacles.hashCode()
        result = 31 * result + moves
        result = 31 * result + isGameWon.hashCode()
        result = 31 * result + isGameLost.hashCode()
        return result
    }
}