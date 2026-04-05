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
    private var isPaused  = false

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
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
            startTelemetry()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            startTelemetry()
        }
    }

    private fun startTelemetry() {
        affectiveManager.start()
        telemetryJob = lifecycleScope.launch {
            while (true) {
                delay(500)
                if (!isPaused && !gameEnded) {
                    viewModel.updateTelemetry(
                        smiling = affectiveManager.currentSmilingProbability,
                        eyeOpen = affectiveManager.currentEyeOpenProbability,
                        amplitude = affectiveManager.currentAudioAmplitude,
                        pitchHz = affectiveManager.currentPitchHz,
                        pitchVariability = affectiveManager.pitchVariability,
                        isSpeech = affectiveManager.isSpeechDetected
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

        // ── Cambio clave: se pasa null incluido para que el ViewModel
        //    pueda detener el movimiento continuo al soltar el botón ──
        binding.joystick.setOnDirectionListener { direction ->
            viewModel.onDirectionInput(direction)
        }

        binding.btnRestart.setOnClickListener { restartGame() }
        binding.btnPause.setOnClickListener   { togglePause() }
    }

    private fun setupObservers() {
        viewModel.gameState.observe(this) { state ->
            binding.gameBoard.setGameState(state)
            binding.tvMoves.text = getString(R.string.moves_count, state.moves)
            binding.tvTime.text  = getString(R.string.time_elapsed, formatElapsedTime(state.timeElapsed))

            binding.tvGameStatus.isVisible = state.isGameWon || state.isGameLost
            binding.tvGameStatus.text = when {
                state.isGameWon  -> getString(R.string.game_won)
                state.isGameLost -> getString(R.string.game_lost)
                else             -> getString(R.string.loading)
            }

            if (state.isGameWon  && !gameEnded) { gameEnded = true; viewModel.onGameWon()  }
            if (state.isGameLost && !gameEnded) { gameEnded = true; viewModel.onGameLost() }
        }

        viewModel.showVictoryDialog.observe(this) { result ->
            result?.let { showVictoryDialog(it) }
        }

        viewModel.showDefeatDialog.observe(this) { shouldShow ->
            if (shouldShow) showDefeatDialog()
        }
    }

    private fun showVictoryDialog(result: GameResult) {
        val timeStr = formatElapsedTime(result.timeElapsed)
        notificationHelper.showVictoryNotification(result.moves, timeStr)
        GameDialogs.showVictoryDialog(
            context     = this,
            moves       = result.moves,
            time        = timeStr,
            rank        = result.rank,
            totalRanked = result.totalRanked,
            onPlayAgain = { handleGameEnd(true,  "WON") },
            onMainMenu  = { handleGameEnd(false, "WON") }
        ).show()
    }

    private fun showDefeatDialog() {
        notificationHelper.showDefeatNotification()
        GameDialogs.showDefeatDialog(
            context     = this,
            onPlayAgain = { handleGameEnd(true,  "LOST") },
            onMainMenu  = { handleGameEnd(false, "LOST") }
        ).show()
    }

    private fun handleGameEnd(playAgain: Boolean, reason: String) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = viewModel.saveCurrentScore(reason)
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        notificationHelper.showCustomNotification(
                            "Partida Finalizada", "La telemetría fue enviada en segundo plano.")
                    },
                    onFailure = { error ->
                        notificationHelper.showCustomNotification(
                            "Error al Guardar", "No se pudo guardar: ${error.message}")
                    }
                )
            }
        }
        // Se ejecuta Inmediatamente (El finish nos devuelve el usuario al MainMenu al cerrar esta ventana)
        if (playAgain) {
            restartGame()
        } else {
            finish()
        }
    }

    private fun restartGame() {
        gameEnded = false
        isPaused  = false
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
        val total   = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
        val minutes = total / 60
        val seconds = total % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onSupportNavigateUp(): Boolean {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            viewModel.saveCurrentScore("QUIT_MIDGAME")
        }
        finish()
        return true
    }
}