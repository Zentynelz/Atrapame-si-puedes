package com.equipo.atrapame.presentation.game

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.equipo.atrapame.R
import com.equipo.atrapame.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGameBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.game_title)
        
        // TODO: Implementar lógica del juego
        // Por ahora solo mostramos un mensaje
        binding.tvGameStatus.text = "Juego en desarrollo..."
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}