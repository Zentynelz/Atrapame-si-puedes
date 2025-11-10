package com.equipo.atrapame.presentation.game

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.equipo.atrapame.R
import com.equipo.atrapame.databinding.ActivityGameBinding
import java.util.Locale
import java.util.concurrent.TimeUnit

class GameActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
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