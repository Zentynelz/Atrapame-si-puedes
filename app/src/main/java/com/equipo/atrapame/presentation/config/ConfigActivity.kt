package com.equipo.atrapame.presentation.config

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.equipo.atrapame.R
import com.equipo.atrapame.data.models.Difficulty
import com.equipo.atrapame.data.models.PlayerConfig
import com.equipo.atrapame.data.repository.ConfigRepository
import com.equipo.atrapame.databinding.ActivityConfigBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityConfigBinding
    private lateinit var configRepository: ConfigRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        configRepository = ConfigRepository(this)
        
        setupUI()
        loadCurrentConfig()
        setupClickListeners()
    }
    
    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.config_title)
    }
    
    private fun loadCurrentConfig() {
        val config = configRepository.getPlayerConfig()
        
        binding.etPlayerName.setText(config.name)
        
        when (config.difficulty) {
            Difficulty.EASY -> binding.rbEasy.isChecked = true
            Difficulty.MEDIUM -> binding.rbMedium.isChecked = true
            Difficulty.HARD -> binding.rbHard.isChecked = true
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveConfiguration()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        // Botón temporal para probar Firebase - QUITAR EN PRODUCCIÓN
        binding.etPlayerName.setOnLongClickListener {
            testFirebase()
            true
        }
    }
    
    private fun testFirebase() {
        Toast.makeText(this, "Probando Firebase...", Toast.LENGTH_SHORT).show()
        
        // Usar corrutinas para probar Firebase
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            try {
                val result = com.equipo.atrapame.utils.FirebaseTestHelper.testFirebaseConnection(this@ConfigActivity)
                
                // Mostrar resultado en un diálogo
                androidx.appcompat.app.AlertDialog.Builder(this@ConfigActivity)
                    .setTitle("Resultado de Firebase")
                    .setMessage(result)
                    .setPositiveButton("Ver Scores") { _, _ ->
                        // Mostrar scores existentes
                        scope.launch {
                            val scores = com.equipo.atrapame.utils.FirebaseTestHelper.readAllScores()
                            androidx.appcompat.app.AlertDialog.Builder(this@ConfigActivity)
                                .setTitle("Scores en Firebase")
                                .setMessage(scores)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                    .setNegativeButton("OK", null)
                    .show()
                    
            } catch (e: Exception) {
                Toast.makeText(this@ConfigActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun saveConfiguration() {
        val playerName = binding.etPlayerName.text.toString().trim()
        
        if (playerName.isEmpty()) {
            binding.etPlayerName.error = "El nombre es requerido"
            return
        }
        
        val difficulty = when (binding.rgDifficulty.checkedRadioButtonId) {
            R.id.rbEasy -> Difficulty.EASY
            R.id.rbHard -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }
        
        val config = PlayerConfig(playerName, difficulty)
        configRepository.savePlayerConfig(config)
        
        Toast.makeText(this, getString(R.string.btn_save) + " ✓", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}