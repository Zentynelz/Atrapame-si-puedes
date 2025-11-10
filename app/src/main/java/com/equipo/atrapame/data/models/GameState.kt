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

    val rows: Int get() = board.size
    val cols: Int get() = board.firstOrNull()?.size ?: 0
    companion object {
        const val DEFAULT_ROWS = 8
        const val DEFAULT_COLS = 8

        fun createInitialState(
            rows: Int = DEFAULT_ROWS,
            cols: Int = DEFAULT_COLS,
            obstacles: List<Position> = emptyList()
        ): GameState {
            require(rows > 0 && cols > 0) { "Board must have positive dimensions" }

            val board = Array(rows) { Array(cols) { CellType.EMPTY } }
            val playerPos = Position(0, 0)


            val enemyPos = Position(rows - 1, cols - 1)

            board[playerPos.row][playerPos.col] = CellType.PLAYER
            board[enemyPos.row][enemyPos.col] = CellType.ENEMY

            val filteredObstacles = obstacles
                .filter { it.isValid(rows, cols) && it != playerPos && it != enemyPos }
                .distinct()

            filteredObstacles.forEach { position ->
                board[position.row][position.col] = CellType.OBSTACLE
            }


            return GameState(
                board = board,
                playerPosition = playerPos,
                enemyPosition = enemyPos,
                obstacles = filteredObstacles
            )
        }
    }

    fun movePlayer(direction: Direction): GameState {
        val targetPosition = playerPosition.move(direction)
        if (!targetPosition.isValid(rows, cols)) return this

        val targetCell = board[targetPosition.row][targetPosition.col]
        if (targetCell == CellType.OBSTACLE) return this

        val newBoard = Array(rows) { row -> board[row].clone() }
        newBoard[playerPosition.row][playerPosition.col] = CellType.EMPTY

        val hasCapturedEnemy = targetPosition == enemyPosition || targetCell == CellType.ENEMY

        newBoard[targetPosition.row][targetPosition.col] = CellType.PLAYER

        return copy(
            board = newBoard,
            playerPosition = targetPosition,
            moves = moves + 1,
            enemyPosition = if (hasCapturedEnemy) targetPosition else enemyPosition,
            isGameWon = hasCapturedEnemy,
            isGameLost = if (hasCapturedEnemy) false else isGameLost
        )
    }


    fun advanceEnemy(): GameState {
        if (isGameWon || isGameLost) return this

        val path = findEscapePath() ?: return this
        if (path.size < 2) return this

        val nextPosition = path[1]

        val newBoard = Array(rows) { row -> board[row].clone() }
        newBoard[enemyPosition.row][enemyPosition.col] = CellType.EMPTY

        val capturesPlayer = nextPosition == playerPosition
        if (capturesPlayer) {
            newBoard[playerPosition.row][playerPosition.col] = CellType.ENEMY
        } else {
            newBoard[nextPosition.row][nextPosition.col] = CellType.ENEMY
            newBoard[playerPosition.row][playerPosition.col] = CellType.PLAYER
        }

        return copy(
            board = newBoard,
            enemyPosition = nextPosition,
            isGameLost = capturesPlayer,
            isGameWon = if (capturesPlayer) false else isGameWon
        )
    }

    private fun findEscapePath(): List<Position>? {
        var bestPath: List<Position>? = null
        var bestDistance = -1
        var bestPathLength = Int.MAX_VALUE

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val candidate = Position(row, col)
                if (!isTraversable(candidate) || candidate == enemyPosition) continue

                val path = aStar(enemyPosition, candidate) ?: continue
                if (path.size < 2) continue

                val distanceFromPlayer = candidate.distanceTo(playerPosition)
                val shouldReplace = distanceFromPlayer > bestDistance ||
                        (distanceFromPlayer == bestDistance && path.size < bestPathLength)

                if (shouldReplace) {
                    bestDistance = distanceFromPlayer
                    bestPathLength = path.size
                    bestPath = path
                }
            }
        }

        return bestPath
    }

    private fun isTraversable(position: Position): Boolean {
        if (!position.isValid(rows, cols)) return false
        if (position == playerPosition) return false

        return when (board[position.row][position.col]) {
            CellType.OBSTACLE -> false
            else -> true
        }
    }

    private fun aStar(start: Position, goal: Position): List<Position>? {
        if (start == goal) return listOf(start)

        data class Node(
            val position: Position,
            val gScore: Int,
            val hScore: Int
        ) : Comparable<Node> {
            private val fScore: Int get() = gScore + hScore

            override fun compareTo(other: Node): Int {
                val fComparison = fScore.compareTo(other.fScore)
                return if (fComparison != 0) fComparison else hScore.compareTo(other.hScore)
            }
        }

        val openSet = java.util.PriorityQueue<Node>()
        val cameFrom = mutableMapOf<Position, Position>()
        val gScore = mutableMapOf(start to 0)
        val closedSet = mutableSetOf<Position>()

        openSet.add(Node(start, gScore.getValue(start), heuristic(start, goal)))

        while (openSet.isNotEmpty()) {
            val currentNode = openSet.poll()
            val current = currentNode.position

            if (current == goal) {
                return reconstructPath(cameFrom, current)
            }

            if (!closedSet.add(current)) continue

            for (neighbor in current.getAdjacentPositions(rows, cols)) {
                if (neighbor in closedSet) continue
                if (neighbor != goal && !isTraversable(neighbor)) continue

                val tentativeG = gScore.getValue(current) + 1
                if (tentativeG >= gScore.getOrDefault(neighbor, Int.MAX_VALUE)) continue

                cameFrom[neighbor] = current
                gScore[neighbor] = tentativeG
                val hScore = heuristic(neighbor, goal)
                openSet.add(Node(neighbor, tentativeG, hScore))
            }
        }

        return null
    }

    private fun heuristic(from: Position, to: Position): Int {
        return from.distanceTo(to)
    }

    private fun reconstructPath(
        cameFrom: Map<Position, Position>,
        current: Position
    ): List<Position> {
        val path = mutableListOf(current)
        var cursor = current
        while (cameFrom.containsKey(cursor)) {
            cursor = cameFrom.getValue(cursor)
            path.add(cursor)
        }
        path.reverse()
        return path
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