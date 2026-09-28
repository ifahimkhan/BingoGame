package com.fahim.bingonumbercaller.data

import com.fahim.bingonumbercaller.model.GameState

expect class GameSettingsDataSource() : GameStateStore {
    override suspend fun save(state: GameState)
    override suspend fun load(): GameState?
    override suspend fun clear()
}
