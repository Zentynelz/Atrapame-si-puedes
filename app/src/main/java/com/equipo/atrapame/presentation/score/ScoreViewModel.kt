package com.equipo.atrapame.presentation.score

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipo.atrapame.data.models.Score
import com.equipo.atrapame.data.local.LocalGameRepository
import kotlinx.coroutines.launch

class ScoreViewModel(
    private val repository: LocalGameRepository
) : ViewModel() {

    private val _scores = MutableLiveData<List<Score>>()
    val scores: LiveData<List<Score>> = _scores

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadTopScores()
    }

    fun loadTopScores(limit: Int = 10) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val scoreList = repository.getTopScores()
                _scores.value = scoreList
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
                _scores.value = emptyList()
            }

            _loading.value = false
        }
    }

    fun loadPlayerScores(playerName: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val scoreList = repository.getPlayerScores(playerName)
                _scores.value = scoreList
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
                _scores.value = emptyList()
            }

            _loading.value = false
        }
    }

    fun retry() {
        loadTopScores()
    }
}
