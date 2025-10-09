package com.equipo.atrapame.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.equipo.atrapame.data.models.Difficulty
import com.equipo.atrapame.data.models.PlayerConfig

class ConfigRepository(context: Context) {
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences("game_config", Context.MODE_PRIVATE)
    
    fun savePlayerConfig(config: PlayerConfig) {
        sharedPrefs.edit()
            .putString("player_name", config.name)
            .putString("difficulty", config.difficulty.name)
            .apply()
    }
    
    fun getPlayerConfig(): PlayerConfig {
        val name = sharedPrefs.getString("player_name", "") ?: ""
        val difficultyName = sharedPrefs.getString("difficulty", Difficulty.MEDIUM.name) ?: Difficulty.MEDIUM.name
        val difficulty = try {
            Difficulty.valueOf(difficultyName)
        } catch (e: IllegalArgumentException) {
            Difficulty.MEDIUM
        }
        
        return PlayerConfig(name, difficulty)
    }
    
    fun isFirstTime(): Boolean {
        return sharedPrefs.getString("player_name", null) == null
    }
}