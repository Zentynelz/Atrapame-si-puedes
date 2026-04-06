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
                val diff = configRepository.getPlayerConfig().difficulty
                if (state != null && !state.isGameWon && !state.isGameLost && !isPaused) {
                    val elapsed = System.currentTimeMillis() - gameStartTime - totalPausedTime
                    
                    val isTimeLimited = diff == com.equipo.atrapame.data.models.Difficulty.EASY || diff == com.equipo.atrapame.data.models.Difficulty.MEDIUM
                    if (isTimeLimited && elapsed >= 60000L) {
                        _gameState.postValue(state.copy(timeElapsed = 60000L))
                        onGameLost() // Perder automáticamente tras 1 minuto solo en modos bajos
                    } else {
                        _gameState.postValue(state.copy(timeElapsed = elapsed))
                    }
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
            com.equipo.atrapame.data.models.Difficulty.EASY -> 0.10
            com.equipo.atrapame.data.models.Difficulty.MEDIUM,
            com.equipo.atrapame.data.models.Difficulty.DYNAMIC -> 0.15
            com.equipo.atrapame.data.models.Difficulty.HARD -> 0.20
            com.equipo.atrapame.data.models.Difficulty.IMPOSSIBLE -> 0.25
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
        val safeSmile = if (smiling.isNaN()) 0f else smiling
        val safePitch = if (pitchHz.isNaN()) 0f else pitchHz
        
        telemetryCount++
        totalSmilingProb += safeSmile
        if (amplitude > maxAmplitude) maxAmplitude = amplitude

        // 1. VOZ: Si hay un ruido notorio (quejido, bufido), sube drásticamente el estrés
        val voiceStressComponent = if (amplitude > 20) {
            (amplitude / 100f) * 60f
        } else {
            0f
        }

        // 2. CARA: Personas con gafas fallan en la detección de ojos (RightEye = 0.0 constante).
        // Las cejas juntas (frustración) reducen drásticamente la "sonrisa" a 0.0.
        // Asignaremos estrés basal alto cuando la persona está con la ceja fruncida / seria (0% sonrisa).
        val nonSmileFactor = (1f - safeSmile.coerceIn(0f, 1f)) * 50f

        val stress = (nonSmileFactor + voiceStressComponent).coerceIn(0f, 100f)
        val stressInt = stress.toInt()
        
        _currentStressLevel.postValue(stressInt)

        // Timeline para gráfica
        val timeMs = System.currentTimeMillis() - gameStartTime - totalPausedTime
        emotionTimeline.add(mapOf(
            "timeMs" to timeMs,
            "stressLevel" to stressInt,
            "smiling" to safeSmile,
            "pitchHz" to safePitch,
            "isSpeech" to isSpeech,
            "difficultyDelayMs" to currentEnemySpeedDelay
        ))

        if (configRepository.getPlayerConfig().difficulty == com.equipo.atrapame.data.models.Difficulty.DYNAMIC) {
            adjustDifficultyDynamically(stressInt, safeSmile)
        }
    }

    private fun adjustDifficultyDynamically(stressLevel: Int, smiling: Float) {
        when {
            // Jugador hiper-estresado y frustrado (aliviarle muchísimo quitando velocidad el enemigo)
            stressLevel > 65                      -> currentEnemySpeedDelay += 150L
            stressLevel in 45..65                 -> currentEnemySpeedDelay += 75L
            // Jugador disfrutando un montón (subir dificultad drásticamente para agobiarlo)
            smiling > 0.35f                       -> currentEnemySpeedDelay -= 120L
            // Jugador aburrido o hiper-relajado (subir la velocidad sigilosamente)
            stressLevel < 20                      -> currentEnemySpeedDelay -= 120L
        }
        // Permitirle llegar hasta unos infernales 80ms de persecución si va muy feliz 
        currentEnemySpeedDelay = currentEnemySpeedDelay.coerceIn(80L, 1800L)
    }

    fun setPerceivedStress(score: Int) { finalPerceivedStress = score }

    private fun calculateFinalEmotion(avgSmile: Float, avgEye: Float, maxAmp: Int): String {
        // En vez de usar el ojo (que falla con las gafas) nos basamos fuertemente en la falta de sonrisa 
        // y los quejidos audibles (amplitud).
        return when {
            avgSmile > 0.35f -> "HAPPY_ENJOYING" // Claramente el tester se estaba riendo o pasando bien
            avgSmile < 0.15f && maxAmp >= 45 -> "STRESSED_FRUSTRATED" // Estaba serio/fruncido y bufó o exclamó
            avgSmile < 0.10f && maxAmp < 20 -> "BORED_SAD" // Muy serio todo el tiempo y en completo silencio
            else -> "NEUTRAL_FOCUSED"
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
