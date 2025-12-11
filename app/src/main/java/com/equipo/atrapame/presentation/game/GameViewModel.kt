package com.equipo.atrapame.presentation.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipo.atrapame.data.models.Direction
import com.equipo.atrapame.data.models.GameState
import com.equipo.atrapame.data.models.Position
import com.equipo.atrapame.data.models.Score
import com.equipo.atrapame.data.repository.ConfigRepository
import com.equipo.atrapame.data.local.LocalGameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameResult(val moves: Int, val timeElapsed: Long)

class GameViewModel(
    private val gameRepository: LocalGameRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {
    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _showVictoryDialog = MutableLiveData<GameResult?>()
    val showVictoryDialog: LiveData<GameResult?> = _showVictoryDialog

    private val _showDefeatDialog = MutableLiveData<Boolean>()
    val showDefeatDialog: LiveData<Boolean> = _showDefeatDialog

    private var enemyMovementJob: Job? = null
    private var timerJob: Job? = null

    private var gameStartTime: Long = 0L
    private var pauseStartTime: Long = 0L
    private var totalPausedTime: Long = 0L

    private var isPaused: Boolean = false

    init {
        initializeGame()
    }

    fun initializeGame(rows: Int = GameState.DEFAULT_ROWS, cols: Int = GameState.DEFAULT_COLS) {

        timerJob?.cancel()
        enemyMovementJob?.cancel()

        val obstacles = createDefaultObstacles(rows, cols)

        _gameState.value = GameState.createInitialState(rows, cols, obstacles)

        gameStartTime = System.currentTimeMillis()
        totalPausedTime = 0L
        isPaused = false

        startTimerLoop()
        startEnemyMovementLoop()
    }

    fun onDirectionInput(direction: Direction?) {
        if (direction == null || isPaused) return
        val current = _gameState.value ?: return

        val result = runCatching { current.movePlayer(direction) }
        if (result.isFailure) return

        val newState = result.getOrNull()!!
        if (newState != current) _gameState.value = newState
    }

    fun pauseGame() {
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
    }

    fun resumeGame() {
        if (isPaused) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime
            isPaused = false
        }
    }

    private fun startTimerLoop() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                val state = _gameState.value
                if (state != null && !state.isGameWon && !state.isGameLost && !isPaused) {
                    val elapsed = System.currentTimeMillis() - gameStartTime - totalPausedTime
                    _gameState.postValue(state.copy(timeElapsed = elapsed))
                }
                delay(100)
            }
        }
    }

    private fun startEnemyMovementLoop() {
        val enemySpeed = configRepository.getPlayerConfig().difficulty.enemySpeed.toLong()

        enemyMovementJob = viewModelScope.launch {
            while (isActive) {
                val shouldContinue = stepEnemy()
                if (!shouldContinue) break
                delay(enemySpeed)
            }
        }
    }

    private fun stepEnemy(): Boolean {
        val current = _gameState.value ?: return true

        if (isPaused || current.isGameWon || current.isGameLost) return true

        val result = runCatching { current.advanceEnemy() }
        if (result.isFailure) return false

        val updated = result.getOrNull()!!
        _gameState.value = updated

        return !(updated.isGameWon || updated.isGameLost)
    }

    private fun createDefaultObstacles(rows: Int, cols: Int): List<Position> {
        if (rows < 3 || cols < 3) return emptyList()

        val difficulty = configRepository.getPlayerConfig().difficulty
        
        return when (difficulty) {
            com.equipo.atrapame.data.models.Difficulty.EASY -> createEasyMap(rows, cols)
            com.equipo.atrapame.data.models.Difficulty.MEDIUM -> createMediumMap(rows, cols)
            com.equipo.atrapame.data.models.Difficulty.HARD -> createHardMap(rows, cols)
        }
    }
    
    private fun createEasyMap(rows: Int, cols: Int): List<Position> {
        val positions = mutableSetOf<Position>()
        
        // Muy pocos obstáculos dispersos - solo algunos en el centro
        val centerRow = rows / 2
        val centerCol = cols / 2
        
        // Agregar 2-3 obstáculos simples
        if (rows >= 5 && cols >= 5) {
            positions.add(Position(centerRow, centerCol))
            if (centerRow + 1 < rows - 1) {
                positions.add(Position(centerRow + 1, centerCol - 1))
            }
            if (centerCol + 1 < cols - 1) {
                positions.add(Position(centerRow - 1, centerCol + 1))
            }
        }
        
        // Asegurar que inicio y fin estén libres
        clearStartAndEndPositions(positions, rows, cols)
        
        return positions.toList()
    }
    
    private fun createMediumMap(rows: Int, cols: Int): List<Position> {
        val positions = mutableSetOf<Position>()
        
        // Patrón de laberinto moderado - paredes en forma de L
        for (row in 2 until rows - 2 step 2) {
            for (col in 2 until cols - 2 step 2) {
                // Crear formas de L
                positions.add(Position(row, col))
                if (row + 1 < rows - 1) {
                    positions.add(Position(row + 1, col))
                }
                if (col + 1 < cols - 1) {
                    positions.add(Position(row, col + 1))
                }
            }
        }
        
        // Agregar algunas paredes verticales
        for (row in 1 until rows - 1 step 3) {
            val col = (rows - row) % (cols - 2) + 1
            if (col < cols - 1) {
                positions.add(Position(row, col))
            }
        }
        
        clearStartAndEndPositions(positions, rows, cols)
        
        return positions.toList()
    }
    
    private fun createHardMap(rows: Int, cols: Int): List<Position> {
        val positions = mutableSetOf<Position>()
        
        // Laberinto muy denso con múltiples patrones
        for (row in 1 until rows - 1) {
            for (col in 1 until cols - 1) {
                // Patrón de cuadrícula muy densa
                if ((row + col) % 2 == 0 || (row % 3 == 1 && col % 3 == 1)) {
                    positions.add(Position(row, col))
                }
            }
        }
        
        // Agregar más obstáculos en patrón de cruz
        val centerRow = rows / 2
        val centerCol = cols / 2
        for (i in 1 until rows - 1) {
            if (i != centerRow) {
                positions.add(Position(i, centerCol))
            }
        }
        for (j in 1 until cols - 1) {
            if (j != centerCol) {
                positions.add(Position(centerRow, j))
            }
        }
        
        // Crear corredores mínimos para que sea jugable
        // Corredor diagonal principal
        for (i in 0 until minOf(rows, cols)) {
            positions.remove(Position(i, i))
        }
        
        // Corredor alternativo
        for (i in 0 until rows) {
            if (i < cols) {
                positions.remove(Position(i, 0))
            }
        }
        for (j in 0 until cols) {
            if (j < rows) {
                positions.remove(Position(rows - 1, j))
            }
        }
        
        clearStartAndEndPositions(positions, rows, cols)
        
        return positions.toList()
    }
    
    private fun clearStartAndEndPositions(positions: MutableSet<Position>, rows: Int, cols: Int) {
        // Limpiar área alrededor del inicio (0,0)
        positions.remove(Position(0, 0))
        positions.remove(Position(0, 1))
        positions.remove(Position(1, 0))
        positions.remove(Position(1, 1))
        
        // Limpiar área alrededor del final (rows-1, cols-1)
        positions.remove(Position(rows - 1, cols - 1))
        positions.remove(Position(rows - 2, cols - 1))
        positions.remove(Position(rows - 1, cols - 2))
        positions.remove(Position(rows - 2, cols - 2))
    }

    fun onGameWon() {
        val state = _gameState.value ?: return
        _showVictoryDialog.value = GameResult(state.moves, state.timeElapsed)
    }

    fun onGameLost() {
        _showDefeatDialog.value = true
    }

    // ✔ Ahora usa el repositorio que se pasó al constructor
    suspend fun saveCurrentScore(): Result<String> {
        val state = _gameState.value ?: return Result.failure(Exception("No game state"))
        val config = configRepository.getPlayerConfig()

        val score = Score(
            playerName = config.name,
            moves = state.moves,
            timeElapsed = state.timeElapsed,
            difficulty = config.difficulty
        )

        return try {
            gameRepository.saveScore(score)
            Result.success("Puntuación guardada correctamente")
        } catch (e: Exception) {
            Result.failure(Exception("Error al guardar: ${e.message}"))
        }
    }
    
    // Función para probar Firebase directamente
    suspend fun testFirebaseConnection(): Result<String> {
        return try {
            val testScore = Score(
                playerName = "TEST_USER",
                moves = 999,
                timeElapsed = 60000L,
                difficulty = com.equipo.atrapame.data.models.Difficulty.EASY
            )
            gameRepository.saveScore(testScore)
            Result.success("Firebase funciona correctamente")
        } catch (e: Exception) {
            Result.failure(Exception("Firebase falló: ${e.message}"))
        }
    }

    fun resetDialogEvents() {
        _showVictoryDialog.value = null
        _showDefeatDialog.value = false
    }

    override fun onCleared() {
        enemyMovementJob?.cancel()
        timerJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_ENEMY_MOVE_INTERVAL_MS = 750L
    }
}
