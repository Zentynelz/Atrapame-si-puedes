package com.equipo.atrapame.presentation

import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.equipo.atrapame.databinding.ActivityMainBinding
import com.equipo.atrapame.presentation.config.ConfigActivity
import com.equipo.atrapame.presentation.game.GameActivity
import com.equipo.atrapame.presentation.game.AffectiveManager
import com.equipo.atrapame.presentation.score.ScoreActivity
import com.equipo.atrapame.presentation.score.PersonalStatsActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var affectiveManager: AffectiveManager
    private var telemetryJob: Job? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1002
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        affectiveManager = AffectiveManager(this, this)
        
        setupClickListeners()
        requestAffectivePermissions()
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

        binding.btnMyStats.setOnClickListener {
            startActivity(Intent(this, PersonalStatsActivity::class.java))
        }
        
        binding.btnExit.setOnClickListener {
            finish()
        }
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
                val smiling = affectiveManager.currentSmilingProbability
                val eyeOpen = affectiveManager.currentEyeOpenProbability
                val amplitude = affectiveManager.currentAudioAmplitude
                val pitchHz = affectiveManager.currentPitchHz
                val pitchVar = affectiveManager.pitchVariability
                val isSpeech = affectiveManager.isSpeechDetected

                val voiceStress = if (isSpeech && pitchHz > 0f) {
                    val pitchFactor = ((pitchHz - 120f) / 200f).coerceIn(0f, 1f)
                    val varFactor = (pitchVar / 40f).coerceIn(0f, 1f)
                    val volFactor = (amplitude / 100f).coerceIn(0f, 1f)
                    (pitchFactor * 0.4f + varFactor * 0.35f + volFactor * 0.25f) * 100f
                } else 0f

                val faceStress = (eyeOpen * 40f) - (smiling * 40f)
                var stress = (faceStress + voiceStress * 0.6f).coerceIn(0f, 100f)

                binding.progressMainStress.progress = stress.toInt()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        telemetryJob?.cancel()
        affectiveManager.stop()
    }
}