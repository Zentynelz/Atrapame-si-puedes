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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GameActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels { 
        GameViewModelFactory(this)
    }
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var affectiveManager: AffectiveManager
    private var telemetryJob: Job? = null
    
    private var gameEnded = false
    private var isPaused = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationHelper.requestNotificationPermission(this)
        }
        
        affectiveManager = AffectiveManager(this, this)
        requestAffectivePermissions()
        
        setupUI()
        setupObservers()
    }
    
    private fun requestAffectivePermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
            startTelemetry()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startTelemetry()
            }
        }
    }

    private fun startTelemetry() {
        affectiveManager.start()
        telemetryJob = lifecycleScope.launch {
            while (true) {
                delay(500)
                if (!isPaused && !gameEnded) {
                    viewModel.updateTelemetry(
                        affectiveManager.currentSmilingProbability,
                        affectiveManager.currentEyeOpenProbability,
                        affectiveManager.currentAudioAmplitude
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        telemetryJob?.cancel()
        affectiveManager.stop()
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
                showVictoryDialog(it)
            }
        }

        viewModel.showDefeatDialog.observe(this) { shouldShow ->
            if (shouldShow) {
                showDefeatDialog()
            }
        }
    }

    private fun showVictoryDialog(result: GameResult) {
        val timeStr = formatElapsedTime(result.timeElapsed)
        
        // Mostrar notificación de victoria
        notificationHelper.showVictoryNotification(result.moves, timeStr)

        GameDialogs.showVictoryDialog(
            context = this,
            moves = result.moves,
            time = timeStr,
            rank = result.rank,
            totalRanked = result.totalRanked,
            onPlayAgain = { handleGameEnd(playAgain = true, reason = "WON") },
            onMainMenu = { handleGameEnd(playAgain = false, reason = "WON") }
        ).show()
    }

    private fun showDefeatDialog() {
        // Mostrar notificación de derrota
        notificationHelper.showDefeatNotification()
        
        GameDialogs.showDefeatDialog(
            context = this,
            onPlayAgain = { handleGameEnd(playAgain = true, reason = "LOST") },
            onMainMenu = { handleGameEnd(playAgain = false, reason = "LOST") }
        ).show()
    }

    private fun handleGameEnd(playAgain: Boolean, reason: String) {
        // Guardar puntuación directamente SIN interrumpir con encuesta
        lifecycleScope.launch {
            val result = viewModel.saveCurrentScore(reason)
            result.fold(
                onSuccess = {
                    notificationHelper.showCustomNotification(
                        "Partida Finalizada", 
                        "La telemetría fue enviada en segundo plano."
                    )
                },
                onFailure = { error ->
                    notificationHelper.showCustomNotification(
                        "Error al Guardar", 
                        "No se pudo guardar la telemetría: ${error.message}"
                    )
                }
            )
            
            if (playAgain) {
                restartGame()
            } else {
                finish()
            }
        }
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
        lifecycleScope.launch {
            viewModel.saveCurrentScore("QUIT_MIDGAME")
            finish()
        }
        return true
    }
}