package com.equipo.atrapame.presentation.game

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.equipo.atrapame.R

object GameDialogs {
    
    fun showVictoryDialog(
        context: Context,
        moves: Int,
        time: String,
        rank: Int,
        totalRanked: Int,
        onPlayAgain: () -> Unit,
        onMainMenu: () -> Unit
    ): AlertDialog {
        
        var baseMessage = context.getString(R.string.dialog_victory_message, moves, time)
        
        if (totalRanked > 0) {
            val percentile = 100 - ((rank.toFloat() / totalRanked.toFloat()) * 100).toInt()
            baseMessage += "\n\n🏆 ¡Eres el #$rank de $totalRanked en esta dificultad!"
            if (percentile > 0) {
                baseMessage += "\n(Mejor que el $percentile% de los jugadores locales)"
            }
        }

        return AlertDialog.Builder(context)
            .setTitle(R.string.dialog_victory_title)
            .setMessage(baseMessage)
            .setPositiveButton(R.string.btn_play_again) { dialog, _ ->
                dialog.dismiss()
                onPlayAgain()
            }
            .setNegativeButton(R.string.btn_main_menu) { dialog, _ ->
                dialog.dismiss()
                onMainMenu()
            }
            .setCancelable(false)
            .create()
    }
    
    fun showDefeatDialog(
        context: Context,
        onPlayAgain: () -> Unit,
        onMainMenu: () -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.dialog_defeat_title)
            .setMessage(R.string.dialog_defeat_message)
            .setPositiveButton(R.string.btn_try_again) { dialog, _ ->
                dialog.dismiss()
                onPlayAgain()
            }
            .setNegativeButton(R.string.btn_main_menu) { dialog, _ ->
                dialog.dismiss()
                onMainMenu()
            }
            .setCancelable(false)
            .create()
    }
    
    fun showSurveyDialog(
        context: Context,
        onSubmit: (stressScore: Int) -> Unit
    ): AlertDialog {
        val view = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_survey, null)
        val seekBar = view.findViewById<android.widget.SeekBar>(R.id.seekStress)
        val tvValue = view.findViewById<android.widget.TextView>(R.id.tvStressValue)

        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                // Rango 1 a 10
                val actualValue = progress + 1
                tvValue.text = actualValue.toString()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        return AlertDialog.Builder(context)
            .setTitle("Evaluación de Estrés")
            .setView(view)
            .setPositiveButton("Enviar") { dialog, _ ->
                dialog.dismiss()
                val score = seekBar.progress + 1
                onSubmit(score)
            }
            .setCancelable(false)
            .create()
    }
}
