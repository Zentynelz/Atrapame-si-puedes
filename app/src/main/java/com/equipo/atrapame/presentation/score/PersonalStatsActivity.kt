package com.equipo.atrapame.presentation.score

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.equipo.atrapame.data.local.LocalGameRepository
import com.equipo.atrapame.databinding.ActivityPersonalStatsBinding
import kotlinx.coroutines.launch

class PersonalStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalStatsBinding
    private lateinit var repository: LocalGameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = LocalGameRepository(applicationContext)

        binding.btnBack.setOnClickListener { finish() }

        loadPersonalData()
    }

    private fun loadPersonalData() {
        lifecycleScope.launch {
            binding.tvPlayerNameHeader.text = "Estadísticas de Este Dispositivo"

            val scores = repository.getAllLocalScores()
            
            if (scores.isEmpty()) {
                binding.tvTotalTime.text = "Aún no has jugado ninguna partida."
                return@launch
            }

            var totalTimeMs = 0L
            var wins = 0
            var quits = 0
            
            var happy = 0
            var stressed = 0
            var bored = 0
            var neutral = 0

            for (score in scores) {
                totalTimeMs += score.timeElapsed
                
                when (score.exitReason) {
                    "WON" -> wins++
                    "QUIT_MIDGAME" -> quits++
                }

                when (score.finalEmotion) {
                    "HAPPY_ENJOYING" -> happy++
                    "STRESSED_FRUSTRATED" -> stressed++
                    "BORED_SAD" -> bored++
                    else -> neutral++
                }
            }

            // Time config
            val seconds = totalTimeMs / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            val timeStr = String.format("%02d:%02d", minutes, remainingSeconds)

            binding.tvTotalTime.text = "Tiempo Total Jugado: $timeStr"
            binding.tvPlayedCount.text = "Partidas Totales: ${scores.size}"
            binding.tvWins.text = "Partidas Ganadas: $wins"
            binding.tvQuits.text = "Abandonadas o Salida Temprana: $quits"

            // Compute percentages
            val totalEmotions = happy + stressed + bored + neutral
            if (totalEmotions > 0) {
                val happyPct = (happy * 100) / totalEmotions
                val stressPct = (stressed * 100) / totalEmotions
                val boredPct = (bored * 100) / totalEmotions

                binding.tvHappyCount.text = "Diversión / Relajación ($happyPct%)"
                binding.progressHappy.progress = happyPct

                binding.tvStressedCount.text = "Tensión / Frustración ($stressPct%)"
                binding.progressStressed.progress = stressPct

                binding.tvBoredCount.text = "Aburrimiento / Tristeza ($boredPct%)"
                binding.progressBored.progress = boredPct
            }
        }
    }
}
