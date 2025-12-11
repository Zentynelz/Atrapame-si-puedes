package com.equipo.atrapame.presentation.game

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.equipo.atrapame.R
import com.equipo.atrapame.databinding.ActivityGameBinding
import com.equipo.atrapame.presentation.NotificationHelper
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class GameActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels { 
        GameViewModelFactory(this)
    }
    private lateinit var notificationHelper: NotificationHelper
    private var gameEnded = false
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationHelper.requestNotificationPermission(this)
        }
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.game_title)

        binding.tvGameStatus.isVisible = false

        binding.joystick.setOnDirectionListener { direction ->
            if (direction != null) {
                viewModel.onDirectionInput(direction)
            }
        }
        
        // Configurar botón de reinicio
        binding.btnRestart.setOnClickListener {
            restartGame()
        }
        
        // Configurar botón de pausa
        binding.btnPause.setOnClickListener {
            togglePause()
        }
    }

    private fun setupObservers() {
        viewModel.gameState.observe(this) { state ->
            binding.gameBoard.setGameState(state)
            binding.tvMoves.text = getString(R.string.moves_count, state.moves)
            binding.tvTime.text = getString(R.string.time_elapsed, formatElapsedTime(state.timeElapsed))

            binding.tvGameStatus.isVisible = state.isGameWon || state.isGameLost
            binding.tvGameStatus.text = when {
                state.isGameWon -> getString(R.string.game_won)
                state.isGameLost -> getString(R.string.game_lost)
                else -> getString(R.string.loading)
            }

            // Detectar victoria
            if (state.isGameWon && !gameEnded) {
                gameEnded = true
                viewModel.onGameWon()
            }

            // Detectar derrota
            if (state.isGameLost && !gameEnded) {
                gameEnded = true
                viewModel.onGameLost()
            }
        }

        viewModel.showVictoryDialog.observe(this) { result ->
            result?.let {
                showVictoryDialog(it.moves, it.timeElapsed)
            }
        }

        viewModel.showDefeatDialog.observe(this) { shouldShow ->
            if (shouldShow) {
                showDefeatDialog()
            }
        }
    }

    private fun showVictoryDialog(moves: Int, timeElapsed: Long) {
        val timeStr = formatElapsedTime(timeElapsed)
        
        // Mostrar notificación de victoria
        notificationHelper.showVictoryNotification(moves, timeStr)

        GameDialogs.showVictoryDialog(
            context = this,
            moves = moves,
            time = timeStr,
            onPlayAgain = { restartGame() },
            onMainMenu = { finish() }
        ).show()

        // Guardar puntuación
        lifecycleScope.launch {
            val result = viewModel.saveCurrentScore()
            result.fold(
                onSuccess = { message ->
                    // Mostrar notificación de éxito
                    notificationHelper.showCustomNotification(
                        "Puntuación Guardada", 
                        "Tu puntuación se guardó correctamente en Firebase"
                    )
                },
                onFailure = { error ->
                    // Mostrar notificación de error
                    notificationHelper.showCustomNotification(
                        "Error al Guardar", 
                        "No se pudo guardar en Firebase: ${error.message}"
                    )
                }
            )
        }
    }

    private fun showDefeatDialog() {
        // Mostrar notificación de derrota
        notificationHelper.showDefeatNotification()
        
        GameDialogs.showDefeatDialog(
            context = this,
            onPlayAgain = { restartGame() },
            onMainMenu = { finish() }
        ).show()
    }

    private fun restartGame() {
        gameEnded = false
        isPaused = false
        binding.btnPause.text = getString(R.string.btn_pause)
        viewModel.resetDialogEvents()
        viewModel.initializeGame()
    }
    
    private fun togglePause() {
        isPaused = !isPaused
        if (isPaused) {
            viewModel.pauseGame()
            binding.btnPause.text = getString(R.string.btn_resume)
        } else {
            viewModel.resumeGame()
            binding.btnPause.text = getString(R.string.btn_pause)
        }
    }

    private fun formatElapsedTime(elapsedMillis: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}