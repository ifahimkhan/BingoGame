package com.fahim.bingonumbercaller.domain

import com.fahim.bingonumbercaller.model.GameState
import kotlin.random.Random

object NumberGenerator {
    fun freshGame(): GameState = GameState(
        calledNumbers = emptyList(),
        remainingPool = (1..90).toList()
    )

    fun drawNumber(current: GameState): GameState {
        if (current.remainingPool.isEmpty()) {
            return current
        }

        val randomIndex = Random.nextInt(current.remainingPool.size)
        val drawnNumber = current.remainingPool[randomIndex]
        val updatedPool = current.remainingPool.toMutableList().apply {
            removeAt(randomIndex)
        }
        val updatedCalledNumbers = current.calledNumbers + drawnNumber

        return current.copy(
            calledNumbers = updatedCalledNumbers,
            remainingPool = updatedPool
        )
    }
}

fun freshGame(): GameState = NumberGenerator.freshGame()

fun drawNumber(current: GameState): GameState = NumberGenerator.drawNumber(current)
