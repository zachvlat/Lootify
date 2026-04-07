package com.zachvlat.lootify.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zachvlat.lootify.data.FreeGame
import com.zachvlat.lootify.data.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)

    private val _selectedFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilters: StateFlow<Set<String>> = _selectedFilters.asStateFlow()

    val games: StateFlow<List<FreeGame>> = repository.games
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun toggleFilter(source: String) {
        val current = _selectedFilters.value.toMutableSet()
        if (source in current) {
            current.remove(source)
        } else {
            current.add(source)
        }
        _selectedFilters.value = current
    }

    fun clearFilters() {
        _selectedFilters.value = emptySet()
    }

    init {
        refreshGames()
    }

    fun refreshGames() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.fetchGames()
            _isRefreshing.value = false
        }
    }

    fun getNewGamesForNotification(): List<FreeGame> {
        return repository.getNewGamesForNotification(games.value)
    }

    fun markGamesAsKnown(games: List<FreeGame>) {
        repository.markGamesAsKnown(games)
    }
}
