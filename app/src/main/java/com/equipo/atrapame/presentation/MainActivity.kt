package com.equipo.atrapame.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.equipo.atrapame.databinding.ActivityMainBinding
import com.equipo.atrapame.presentation.config.ConfigActivity
import com.equipo.atrapame.presentation.game.GameActivity
import com.equipo.atrapame.presentation.score.ScoreActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnPlay.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        
        binding.btnConfig.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
        
        binding.btnScores.setOnClickListener {
            startActivity(Intent(this, ScoreActivity::class.java))
        }
        
        binding.btnExit.setOnClickListener {
            finish()
        }
    }
}