package com.fahim.bingonumbercaller.data

import com.fahim.bingonumbercaller.model.GameState

/** Persists the host's last game snapshot so an app restart mid-game isn't catastrophic. */
interface GameStateStore {
    suspend fun save(state: GameState)
    suspend fun load(): GameState?
    suspend fun clear()
}
