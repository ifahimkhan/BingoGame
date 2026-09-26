package com.fahim.bingonumbercaller.data

import android.content.Context
import android.content.SharedPreferences
import com.fahim.bingonumbercaller.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual class GameSettingsDataSource(private val context: Context? = null) {
    actual constructor() : this(null)

    companion object {
        private const val PREFS_NAME = "bingo_game_prefs"
        private const val KEY_GAME_STATE = "bingo_game_state"

        var appContext: Context? = null
        private var inMemoryBackupState: String? = null

        private fun resolveApplicationContext(): Context? {
            return appContext ?: runCatching {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val method = activityThreadClass.getMethod("currentApplication")
                method.invoke(null) as? Context
            }.getOrNull()
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getPreferences(): SharedPreferences? {
        val targetContext = context ?: resolveApplicationContext()
        return targetContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual suspend fun save(state: GameState) {
        withContext(Dispatchers.IO) {
            val serialized = json.encodeToString(state)
            val preferences = getPreferences()
            if (preferences != null) {
                preferences.edit().putString(KEY_GAME_STATE, serialized).apply()
            } else {
                inMemoryBackupState = serialized
            }
        }
    }

    actual suspend fun load(): GameState? {
        return withContext(Dispatchers.IO) {
            val preferences = getPreferences()
            val serialized = if (preferences != null) {
                preferences.getString(KEY_GAME_STATE, null)
            } else {
                inMemoryBackupState
            } ?: return@withContext null

            runCatching {
                json.decodeFromString<GameState>(serialized)
            }.getOrNull()
        }
    }

    actual suspend fun clear() {
        withContext(Dispatchers.IO) {
            val preferences = getPreferences()
            if (preferences != null) {
                preferences.edit().remove(KEY_GAME_STATE).apply()
            } else {
                inMemoryBackupState = null
            }
        }
    }
}
