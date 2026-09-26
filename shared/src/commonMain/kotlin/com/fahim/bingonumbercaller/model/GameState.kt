package com.fahim.bingonumbercaller.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val calledNumbers: List<Int> = emptyList(),
    val remainingPool: List<Int> = (1..90).toList()
) {
    val isGameComplete: Boolean
        get() = remainingPool.isEmpty()
}
