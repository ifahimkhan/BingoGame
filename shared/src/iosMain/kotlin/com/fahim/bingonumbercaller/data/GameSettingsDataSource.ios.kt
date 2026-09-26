package com.fahim.bingonumbercaller.data

import com.fahim.bingonumbercaller.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual class GameSettingsDataSource actual constructor() {
    companion object {
        private const val KEY_GAME_STATE = "bingo_game_state"
    }

    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    actual suspend fun save(state: GameState) {
        withContext(Dispatchers.IO) {
            val serialized = json.encodeToString(state)
            userDefaults.setObject(serialized, forKey = KEY_GAME_STATE)
        }
    }

    actual suspend fun load(): GameState? {
        return withContext(Dispatchers.IO) {
            val serialized = userDefaults.stringForKey(KEY_GAME_STATE) ?: return@withContext null
            runCatching {
                json.decodeFromString<GameState>(serialized)
            }.getOrNull()
        }
    }

    actual suspend fun clear() {
        withContext(Dispatchers.IO) {
            userDefaults.removeObjectForKey(KEY_GAME_STATE)
        }
    }
}
