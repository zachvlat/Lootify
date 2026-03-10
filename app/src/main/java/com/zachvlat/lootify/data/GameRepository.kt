package com.zachvlat.lootify.data

import android.content.Context
import android.content.SharedPreferences
import com.zachvlat.lootify.network.RssService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository(context: Context) {

    private val rssService = RssService()
    private val prefs: SharedPreferences = context.getSharedPreferences("lootify_prefs", Context.MODE_PRIVATE)
    
    private val _games = MutableStateFlow<List<FreeGame>>(emptyList())
    val games: StateFlow<List<FreeGame>> = _games.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun fetchGames() {
        _isLoading.value = true
        _error.value = null
        
        val result = rssService.fetchFreeGames()
        
        result.onSuccess { newGames ->
            _games.value = newGames
        }.onFailure { e ->
            _error.value = e.message ?: "Unknown error"
        }
        
        _isLoading.value = false
    }

    fun getKnownGameIds(): Set<String> {
        val idsString = prefs.getString("known_game_ids", "") ?: ""
        return if (idsString.isEmpty()) {
            emptySet()
        } else {
            idsString.split(",").toSet()
        }
    }

    private fun saveKnownGameIds(ids: Set<String>) {
        prefs.edit().putString("known_game_ids", ids.joinToString(",")).apply()
    }

    fun getNewGamesForNotification(currentGames: List<FreeGame>): List<FreeGame> {
        val knownIds = getKnownGameIds()
        return currentGames.filter { it.id !in knownIds }
    }

    fun markGamesAsKnown(games: List<FreeGame>) {
        val allIds = getKnownGameIds().toMutableSet()
        allIds.addAll(games.map { it.id })
        saveKnownGameIds(allIds)
    }
}
