package com.equipo.atrapame.presentation.score

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.equipo.atrapame.R
import com.equipo.atrapame.data.local.LocalGameRepository
import com.equipo.atrapame.databinding.ActivityScoreBinding
import com.equipo.atrapame.presentation.game.GameDialogs

class ScoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScoreBinding
    private lateinit var scoreAdapter: ScoreAdapter

    // ✅ Pasamos el applicationContext al repositorio
    private val viewModel: ScoreViewModel by viewModels {
        ScoreViewModelFactory(
            LocalGameRepository(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.score_title)
        
        val difficulties = listOf("Todas", "Fácil", "Medio", "Difícil", "Dinámico", "Imposible")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinDifficulty.adapter = adapter
        
        binding.spinDifficulty.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                // Adjust text color dynamically for dark mode
                (parent?.getChildAt(0) as? android.widget.TextView)?.setTextColor(android.graphics.Color.CYAN)
                
                val hardcodedEnum = when(pos) {
                    1 -> "EASY"
                    2 -> "MEDIUM"
                    3 -> "HARD"
                    4 -> "DYNAMIC"
                    5 -> "IMPOSSIBLE"
                    else -> null
                }
                viewModel.loadTopScores(hardcodedEnum)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        scoreAdapter = ScoreAdapter(isInteractive = false)
        binding.rvScores.apply {
            layoutManager = LinearLayoutManager(this@ScoreActivity)
            adapter = scoreAdapter
        }
    }

    private fun setupObservers() {
        viewModel.scores.observe(this) { scores ->
            if (scores.isEmpty()) {
                binding.tvNoScores.isVisible = true
                binding.rvScores.isVisible = false
            } else {
                binding.tvNoScores.isVisible = false
                binding.rvScores.isVisible = true
                scoreAdapter.submitList(scores)
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                binding.tvNoScores.isVisible = true
                binding.tvNoScores.text = errorMessage
                binding.rvScores.isVisible = false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
