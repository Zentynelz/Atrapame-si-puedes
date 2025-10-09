package com.equipo.atrapame.presentation.score

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.equipo.atrapame.R
import com.equipo.atrapame.data.repository.GameRepository
import com.equipo.atrapame.databinding.ActivityScoreBinding
import kotlinx.coroutines.launch

class ScoreActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityScoreBinding
    private lateinit var gameRepository: GameRepository
    private lateinit var scoreAdapter: ScoreAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        gameRepository = GameRepository()
        setupUI()
        setupRecyclerView()
        loadScores()
    }
    
    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.score_title)
    }
    
    private fun setupRecyclerView() {
        scoreAdapter = ScoreAdapter()
        binding.rvScores.apply {
            layoutManager = LinearLayoutManager(this@ScoreActivity)
            adapter = scoreAdapter
        }
    }
    
    private fun loadScores() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            gameRepository.getTopScores().fold(
                onSuccess = { scores ->
                    binding.progressBar.visibility = android.view.View.GONE
                    if (scores.isEmpty()) {
                        binding.tvNoScores.visibility = android.view.View.VISIBLE
                        binding.rvScores.visibility = android.view.View.GONE
                    } else {
                        binding.tvNoScores.visibility = android.view.View.GONE
                        binding.rvScores.visibility = android.view.View.VISIBLE
                        scoreAdapter.submitList(scores)
                    }
                },
                onFailure = { error ->
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.tvNoScores.visibility = android.view.View.VISIBLE
                    binding.tvNoScores.text = getString(R.string.error_connection)
                }
            )
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}