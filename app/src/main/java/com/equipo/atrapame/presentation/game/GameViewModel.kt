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

        val positions = mutableSetOf<Position>()

        for (row in 1 until rows - 1 step 2) {
            val startCol = (row / 2) % cols
            positions.add(Position(row, startCol))

            val secondaryCol = startCol + 2
            if (secondaryCol in 0 until cols) {
                positions.add(Position(row, secondaryCol))
            }
        }

        positions.remove(Position(0, 0))
        positions.remove(Position(rows - 1, cols - 1))

        return positions.toList()
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

        gameRepository.saveScore(score)

        return Result.success("guardado-local")
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
