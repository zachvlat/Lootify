package com.zachvlat.lootify.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zachvlat.lootify.data.GameRepository

class FreeGameWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = GameRepository(applicationContext)
            repository.fetchGames()

            val currentGames = repository.games.value
            val newGames = repository.getNewGamesForNotification(currentGames)
            
            if (newGames.isNotEmpty()) {
                val notificationHelper = NotificationHelper(applicationContext)
                val gameInfo = newGames.take(5).map { game -> game.title to game.gameLink }
                notificationHelper.showNewGamesNotification(gameInfo)
                
                repository.markGamesAsKnown(currentGames)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
