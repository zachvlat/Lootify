package com.zachvlat.lootify.network

import com.zachvlat.lootify.data.FreeGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class RssService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val parser = RssParser()

    suspend fun fetchFreeGames(): Result<List<FreeGame>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://www.reddit.com/r/Freegamestuff/new.rss")
                .header("User-Agent", "Lootify/1.0")
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
            }

            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val games = parser.parse(body)
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
