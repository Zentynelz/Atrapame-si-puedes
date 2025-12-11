package com.equipo.atrapame.presentation.score

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.equipo.atrapame.data.local.LocalGameRepository

class ScoreViewModelFactory(
    private val repository: LocalGameRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScoreViewModel::class.java)) {
            return ScoreViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

