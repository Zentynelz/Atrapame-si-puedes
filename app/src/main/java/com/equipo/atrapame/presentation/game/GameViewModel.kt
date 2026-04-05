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
    
    // Timeline para la graficación en Firebase
    private val emotionTimeline = mutableListOf<Map<String, Any>>()

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
        emotionTimeline.clear()

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
        val difficulty = configRepository.getPlayerConfig().difficulty
        return createProceduralSolvableMap(rows, cols, difficulty)
    }

    private fun createProceduralSolvableMap(rows: Int, cols: Int, difficulty: com.equipo.atrapame.data.models.Difficulty): List<Position> {
        var attempts = 0
        while (attempts < 20) { // Limit attempts to prevent infinite loops
            val positions = generateRandomObstacles(rows, cols, difficulty)
            if (hasPathFromStartToEnd(rows, cols, positions)) {
                return positions.toList()
            }
            attempts++
        }
        return emptyList() // Fallback to an empty map if it fails too many times
    }

    private fun generateRandomObstacles(rows: Int, cols: Int, difficulty: com.equipo.atrapame.data.models.Difficulty): Set<Position> {
        val positions = mutableSetOf<Position>()
        val density = when(difficulty) {
            com.equipo.atrapame.data.models.Difficulty.EASY -> 0.15
            com.equipo.atrapame.data.models.Difficulty.MEDIUM,
            com.equipo.atrapame.data.models.Difficulty.DYNAMIC -> 0.25
            com.equipo.atrapame.data.models.Difficulty.HARD -> 0.35
        }
        val maxObstacles = (rows * cols * density).toInt()
        val random = java.util.Random()
        var placed = 0

        while (placed < maxObstacles) {
            val r = random.nextInt(rows)
            val c = random.nextInt(cols)
            val p = Position(r, c)
            
            // Do not block start and end areas completely
            if (p == Position(0,0) || p == Position(0,1) || p == Position(1,0)) continue
            if (p == Position(rows-1, cols-1) || p == Position(rows-1, cols-2) || p == Position(rows-2, cols-1)) continue
            
            if (positions.add(p)) {
                placed++
            }
        }
        return positions
    }

    private fun hasPathFromStartToEnd(rows: Int, cols: Int, obstacles: Set<Position>): Boolean {
        val visited = Array(rows) { BooleanArray(cols) }
        val queue = java.util.LinkedList<Position>()
        val start = Position(0,0)
        
        queue.add(start)
        visited[0][0] = true

        // Directions: Right, Down, Left, Up
        val dirs = arrayOf(Position(0,1), Position(1,0), Position(0,-1), Position(-1,0))

        while (queue.isNotEmpty()) {
            val curr = queue.poll()!!
            if (curr.row == rows - 1 && curr.col == cols - 1) return true

            for (d in dirs) {
                val nr = curr.row + d.row
                val nc = curr.col + d.col
                val nPos = Position(nr, nc)
                
                if (nr in 0 until rows && nc in 0 until cols && !obstacles.contains(nPos) && !visited[nr][nc]) {
                    visited[nr][nc] = true
                    queue.add(nPos)
                }
            }
        }
        return false
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

    fun updateTelemetry(
        smiling: Float,
        eyeOpen: Float,
        amplitude: Int,
        pitchHz: Float = 0f,
        pitchVariability: Float = 0f,
        isSpeech: Boolean = false
    ) {
        if (isPaused) return
        telemetryCount++
        totalSmilingProb += smiling
        totalEyeOpenProb += eyeOpen
        if (amplitude > maxAmplitude) maxAmplitude = amplitude

        // Solo considerar datos de voz cuando hay habla real
        val voiceStressComponent = if (isSpeech && pitchHz > 0f) {
            val pitchFactor = ((pitchHz - 120f) / 200f).coerceIn(0f, 1f)
            val variabilityFactor = (pitchVariability / 40f).coerceIn(0f, 1f)
            val volumeFactor = (amplitude / 100f).coerceIn(0f, 1f)
            (pitchFactor * 0.4f + variabilityFactor * 0.35f + volumeFactor * 0.25f) * 100f
        } else {
            0f
        }
        // Combinar cara + voz
        val faceStress = (eyeOpen * 40f) - (smiling * 40f)
        val stress = (faceStress + (voiceStressComponent * 0.6f)).coerceIn(0f, 100f)
        val stressInt = stress.toInt()
        _currentStressLevel.postValue(stressInt)

        // Timeline para gráfica
        val timeMs = System.currentTimeMillis() - gameStartTime - totalPausedTime
        emotionTimeline.add(mapOf(
            "timeMs" to timeMs,
            "stressLevel" to stressInt,
            "smiling" to smiling,
            "pitchHz" to pitchHz,
            "isSpeech" to isSpeech,
            "difficultyDelayMs" to currentEnemySpeedDelay
        ))

        if (configRepository.getPlayerConfig().difficulty == com.equipo.atrapame.data.models.Difficulty.DYNAMIC) {
            adjustDifficultyDynamically(stressInt, smiling)
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
            exitReason           = exitReason,
            emotionTimeline      = emotionTimeline.toList()
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
