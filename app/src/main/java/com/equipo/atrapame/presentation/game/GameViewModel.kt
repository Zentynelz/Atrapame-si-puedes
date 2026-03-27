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

data class GameResult(val moves: Int, val timeElapsed: Long, val rank: Int = 0, val totalRanked: Int = 0)

class GameViewModel(
    private val gameRepository: LocalGameRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {
    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _currentStressLevel = MutableLiveData<Int>()
    val currentStressLevel: LiveData<Int> = _currentStressLevel

    private val _showVictoryDialog = MutableLiveData<GameResult?>()
    val showVictoryDialog: LiveData<GameResult?> = _showVictoryDialog

    private val _showDefeatDialog = MutableLiveData<Boolean>()
    val showDefeatDialog: LiveData<Boolean> = _showDefeatDialog

    private var enemyMovementJob: Job? = null
    private var timerJob: Job? = null

    // ─── Movimiento continuo ──────────────────────────────────────────────────
    private var heldDirection: Direction? = null
    private var movementJob: Job? = null
    private val MOVEMENT_REPEAT_DELAY_MS = 160L
    // ──────────────────────────────────────────────────────────────────────────

    private var gameStartTime: Long = 0L
    private var pauseStartTime: Long = 0L
    private var totalPausedTime: Long = 0L

    private var isPaused: Boolean = false

    // DDA Tracking
    private var currentEnemySpeedDelay: Long = 750L

    // Telemetry tracking
    private var telemetryCount = 0
    private var totalSmilingProb = 0f
    private var totalEyeOpenProb = 0f
    private var maxAmplitude = 0
    private var finalPerceivedStress = 0

    init {
        initializeGame()
    }

    fun initializeGame(rows: Int = GameState.DEFAULT_ROWS, cols: Int = GameState.DEFAULT_COLS) {
        timerJob?.cancel()
        enemyMovementJob?.cancel()
        stopMovementLoop()

        val obstacles = createDefaultObstacles(rows, cols)
        _gameState.value = GameState.createInitialState(rows, cols, obstacles)

        gameStartTime = System.currentTimeMillis()
        totalPausedTime = 0L
        isPaused = false

        telemetryCount = 0
        totalSmilingProb = 0f
        totalEyeOpenProb = 0f
        maxAmplitude = 0
        finalPerceivedStress = 0

        currentEnemySpeedDelay = configRepository.getPlayerConfig().difficulty.enemySpeed.toLong()

        startTimerLoop()
        startEnemyMovementLoop()
    }

    // ─── Input del D-Pad ──────────────────────────────────────────────────────

    fun onDirectionInput(direction: Direction?) {
        if (isPaused) return

        heldDirection = direction

        if (direction != null) {
            performMove(direction)

            if (movementJob?.isActive != true) {
                movementJob = viewModelScope.launch {
                    delay(MOVEMENT_REPEAT_DELAY_MS)
                    while (isActive) {
                        val currentDir = heldDirection ?: break
                        performMove(currentDir)
                        delay(MOVEMENT_REPEAT_DELAY_MS)
                    }
                }
            }
        } else {
            stopMovementLoop()
        }
    }

    private fun performMove(direction: Direction) {
        val current = _gameState.value ?: return
        if (current.isGameWon || current.isGameLost) return
        val newState = runCatching { current.movePlayer(direction) }.getOrNull() ?: return
        if (newState != current) _gameState.value = newState
    }

    private fun stopMovementLoop() {
        movementJob?.cancel()
        movementJob = null
        heldDirection = null
    }

    // ─── Pausa ────────────────────────────────────────────────────────────────

    fun pauseGame() {
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
        stopMovementLoop()
    }

    fun resumeGame() {
        if (isPaused) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime
            isPaused = false
        }
    }

    // ─── Loops internos ───────────────────────────────────────────────────────

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
        enemyMovementJob = viewModelScope.launch {
            while (isActive) {
                val shouldContinue = stepEnemy()
                if (!shouldContinue) break
                delay(currentEnemySpeedDelay)
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

    // ─── Mapas ────────────────────────────────────────────────────────────────

    private fun createDefaultObstacles(rows: Int, cols: Int): List<Position> {
        if (rows < 3 || cols < 3) return emptyList()
        return when (configRepository.getPlayerConfig().difficulty) {
            com.equipo.atrapame.data.models.Difficulty.EASY    -> createEasyMap(rows, cols)
            com.equipo.atrapame.data.models.Difficulty.MEDIUM,
            com.equipo.atrapame.data.models.Difficulty.DYNAMIC -> createMediumMap(rows, cols)
            com.equipo.atrapame.data.models.Difficulty.HARD    -> createHardMap(rows, cols)
        }
    }

    private fun createEasyMap(rows: Int, cols: Int): List<Position> {
        val positions = mutableSetOf<Position>()
        val centerRow = rows / 2; val centerCol = cols / 2
        if (rows >= 5 && cols >= 5) {
            positions.add(Position(centerRow, centerCol))
            if (centerRow + 1 < rows - 1) positions.add(Position(centerRow + 1, centerCol - 1))
            if (centerCol + 1 < cols - 1) positions.add(Position(centerRow - 1, centerCol + 1))
        }
        clearStartAndEndPositions(positions, rows, cols)
        return positions.toList()
    }

    private fun createMediumMap(rows: Int, cols: Int): List<Position> {
        val positions = mutableSetOf<Position>()
        for (row in 2 until rows - 2 step 2) {
            for (col in 2 until cols - 2 step 2) {
                positions.add(Position(row, col))
                if (row + 1 < rows - 1) positions.add(Position(row + 1, col))
                if (col + 1 < cols - 1) positions.add(Position(row, col + 1))
            }
        }
        for (row in 1 until rows - 1 step 3) {
            val col = (rows - row) % (cols - 2) + 1
            if (col < cols - 1) positions.add(Position(row, col))
        }
        clearStartAndEndPositions(positions, rows, cols)
        return positions.toList()
    }

    private fun createHardMap(rows: Int, cols: Int): List<Position> {
        val positions = mutableSetOf<Position>()
        for (row in 1 until rows - 1) {
            for (col in 1 until cols - 1) {
                if ((row + col) % 2 == 0 || (row % 3 == 1 && col % 3 == 1))
                    positions.add(Position(row, col))
            }
        }
        val cr = rows / 2; val cc = cols / 2
        for (i in 1 until rows - 1) { if (i != cr) positions.add(Position(i, cc)) }
        for (j in 1 until cols - 1) { if (j != cc) positions.add(Position(cr, j)) }
        for (i in 0 until minOf(rows, cols)) positions.remove(Position(i, i))
        for (i in 0 until rows) { if (i < cols) positions.remove(Position(i, 0)) }
        for (j in 0 until cols) { if (j < rows) positions.remove(Position(rows - 1, j)) }
        clearStartAndEndPositions(positions, rows, cols)
        return positions.toList()
    }

    private fun clearStartAndEndPositions(positions: MutableSet<Position>, rows: Int, cols: Int) {
        positions.remove(Position(0, 0));         positions.remove(Position(0, 1))
        positions.remove(Position(1, 0));         positions.remove(Position(1, 1))
        positions.remove(Position(rows-1, cols-1)); positions.remove(Position(rows-2, cols-1))
        positions.remove(Position(rows-1, cols-2)); positions.remove(Position(rows-2, cols-2))
    }

    // ─── Victoria / Derrota ───────────────────────────────────────────────────

    fun onGameWon() {
        val state = _gameState.value ?: return
        viewModelScope.launch {
            val difficulty = configRepository.getPlayerConfig().difficulty
            val (rank, total) = gameRepository.getPerformanceRank(difficulty, state.timeElapsed)
            _showVictoryDialog.value = GameResult(state.moves, state.timeElapsed, rank, total + 1)
        }
    }

    fun onGameLost() {
        _showDefeatDialog.value = true
    }

    // ─── Telemetría / DDA ─────────────────────────────────────────────────────

    fun updateTelemetry(smiling: Float, eyeOpen: Float, amplitude: Int) {
        if (isPaused) return
        telemetryCount++
        totalSmilingProb += smiling
        totalEyeOpenProb += eyeOpen
        if (amplitude > maxAmplitude) maxAmplitude = amplitude

        var stress = (eyeOpen * 50f) + (amplitude / 100f) - (smiling * 50f)
        stress = stress.coerceIn(0f, 100f)
        _currentStressLevel.postValue(stress.toInt())

        if (configRepository.getPlayerConfig().difficulty == com.equipo.atrapame.data.models.Difficulty.DYNAMIC) {
            adjustDifficultyDynamically(stress.toInt(), smiling)
        }
    }

    private fun adjustDifficultyDynamically(stressLevel: Int, smiling: Float) {
        when {
            stressLevel > 70                      -> currentEnemySpeedDelay += 100L
            stressLevel in 50..70                 -> currentEnemySpeedDelay += 50L
            smiling > 0.4f && stressLevel < 30   -> currentEnemySpeedDelay -= 100L
            stressLevel < 10                      -> currentEnemySpeedDelay -= 50L
        }
        currentEnemySpeedDelay = currentEnemySpeedDelay.coerceIn(400L, 1500L)
    }

    fun setPerceivedStress(score: Int) { finalPerceivedStress = score }

    private fun calculateFinalEmotion(avgSmile: Float, avgEye: Float, maxAmp: Int): String {
        return when {
            avgSmile > 0.4f                                     -> "HAPPY_ENJOYING"
            avgSmile < 0.1f && avgEye > 0.5f && maxAmp >= 2000 -> "STRESSED_FRUSTRATED"
            avgSmile < 0.1f && avgEye < 0.3f && maxAmp < 1000  -> "BORED_SAD"
            else                                                -> "NEUTRAL_FOCUSED"
        }
    }

    // ✔ Ahora usa el repositorio que se pasó al constructor
    // ─── Puntuación ───────────────────────────────────────────────────────────

    suspend fun saveCurrentScore(exitReason: String = "FINISHED"): Result<String> {
        val state  = _gameState.value ?: return Result.failure(Exception("No game state"))
        val config = configRepository.getPlayerConfig()

        val avgSmile        = if (telemetryCount > 0) totalSmilingProb / telemetryCount else 0f
        val avgEye          = if (telemetryCount > 0) totalEyeOpenProb / telemetryCount else 0f
        val emotionDetected = calculateFinalEmotion(avgSmile, avgEye, maxAmplitude)

        val score = Score(
            playerName           = config.name,
            moves                = state.moves,
            timeElapsed          = state.timeElapsed,
            difficulty           = config.difficulty,
            timestamp            = System.currentTimeMillis(),
            avgSmilingProb       = avgSmile,
            avgRightEyeOpenProb  = avgEye,
            maxAudioAmplitude    = maxAmplitude,
            perceivedStressScore = finalPerceivedStress,
            finalEmotion         = emotionDetected,
            exitReason           = exitReason
        )

        return try {
            gameRepository.saveScore(score)
            Result.success("Puntuación guardada correctamente")
        } catch (e: Exception) {
            Result.failure(Exception("Error al guardar: ${e.message}"))
        }
    }

    suspend fun testFirebaseConnection(): Result<String> {
        return try {
            gameRepository.saveScore(Score(
                playerName = "TEST_USER", moves = 999, timeElapsed = 60000L,
                difficulty = com.equipo.atrapame.data.models.Difficulty.EASY
            ))
            Result.success("Firebase funciona correctamente")
        } catch (e: Exception) {
            Result.failure(Exception("Firebase falló: ${e.message}"))
        }
    }

    fun resetDialogEvents() {
        _showVictoryDialog.value = null
        _showDefeatDialog.value  = false
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        enemyMovementJob?.cancel()
        timerJob?.cancel()
        movementJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_ENEMY_MOVE_INTERVAL_MS = 750L
    }
}
