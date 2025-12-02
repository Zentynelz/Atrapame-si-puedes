package com.equipo.atrapame.presentation.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.equipo.atrapame.data.local.LocalGameRepository
import com.equipo.atrapame.data.repository.ConfigRepository

class GameViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(
                gameRepository = LocalGameRepository(context),
                configRepository = ConfigRepository(context)
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
