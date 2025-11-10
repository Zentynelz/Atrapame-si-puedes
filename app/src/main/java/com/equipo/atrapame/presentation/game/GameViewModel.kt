package com.equipo.atrapame.presentation.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipo.atrapame.data.models.Direction
import com.equipo.atrapame.data.models.GameState
import com.equipo.atrapame.data.models.Position
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private var enemyMovementJob: Job? = null

    init {
        initializeGame()
    }

    fun initializeGame(rows: Int = GameState.DEFAULT_ROWS, cols: Int = GameState.DEFAULT_COLS) {
        val obstacles = createDefaultObstacles(rows, cols)
        _gameState.value = GameState.createInitialState(rows, cols, obstacles)
        startEnemyMovementLoop()
    }

    fun onDirectionInput(direction: Direction?) {
        if (direction == null) return
        val currentState = _gameState.value ?: return
        val newState = currentState.movePlayer(direction)
        if (newState != currentState) {
            _gameState.value = newState
        }
    }

    private fun startEnemyMovementLoop(intervalMillis: Long = DEFAULT_ENEMY_MOVE_INTERVAL_MS) {
        enemyMovementJob?.cancel()
        enemyMovementJob = viewModelScope.launch {
            while (isActive) {
                val shouldContinue = stepEnemy()
                if (!shouldContinue) break
                delay(intervalMillis)
            }
        }
    }

    private fun stepEnemy(): Boolean {
        val currentState = _gameState.value ?: return true
        if (currentState.isGameWon || currentState.isGameLost) {
            return false
        }

        val updatedState = currentState.advanceEnemy()
        if (updatedState != currentState) {
            _gameState.value = updatedState
        }

        return !(updatedState.isGameWon || updatedState.isGameLost)
    }


    private fun createDefaultObstacles(rows: Int, cols: Int): List<Position> {
        if (rows < 3 || cols < 3) return emptyList()

        val positions = mutableSetOf<Position>()
        for (row in 1 until rows - 1 step 2) {
            val startCol = (row / 2) % cols
            positions.add(Position(row, startCol))
            val secondaryCol = startCol + 2
            if (secondaryCol < cols) {
                positions.add(Position(row, secondaryCol))
            }
        }

        positions.remove(Position(0, 0))
        positions.remove(Position(rows - 1, cols - 1))

        return positions.toList()
    }

    override fun onCleared() {
        enemyMovementJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_ENEMY_MOVE_INTERVAL_MS = 800L
    }



}