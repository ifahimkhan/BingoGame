package com.fahim.bingonumbercaller.data

import com.fahim.bingonumbercaller.model.GameState

expect class GameSettingsDataSource() {
    suspend fun save(state: GameState)
    suspend fun load(): GameState?
    suspend fun clear()
}
