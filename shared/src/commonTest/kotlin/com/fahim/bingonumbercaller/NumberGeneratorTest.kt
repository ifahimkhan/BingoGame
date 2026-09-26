package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.domain.NumberGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberGeneratorTest {

    @Test
    fun testFreshGameInitialState() {
        val game = NumberGenerator.freshGame()
        assertTrue(game.calledNumbers.isEmpty(), "Initial called numbers should be empty")
        assertEquals(90, game.remainingPool.size, "Initial pool should contain 90 numbers")
        assertEquals((1..90).toList(), game.remainingPool, "Pool should contain numbers 1 to 90")
        assertFalse(game.isGameComplete, "Game should not be complete initially")
    }

    @Test
    fun testFullGameSequentialDrawsNoDuplicates() {
        var current = NumberGenerator.freshGame()

        for (step in 1..90) {
            current = NumberGenerator.drawNumber(current)
            assertEquals(step, current.calledNumbers.size, "Called count should match step $step")
            assertEquals(90 - step, current.remainingPool.size, "Remaining count should be 90 - step")
        }

        assertTrue(current.isGameComplete, "Game should be marked complete after 90 draws")
        assertEquals(0, current.remainingPool.size, "Remaining pool must be empty")

        // Confirm exactly numbers 1..90 are drawn with no duplicates
        val sortedCalled = current.calledNumbers.sorted()
        assertEquals((1..90).toList(), sortedCalled, "All numbers 1-90 must be drawn exactly once")
        assertEquals(90, current.calledNumbers.toSet().size, "All 90 called numbers must be unique")

        // Verify randomness: drawn list should not be identical to simple sorted order 1..90
        val isNotSortedTrivially = current.calledNumbers != (1..90).toList()
        assertTrue(isNotSortedTrivially, "Draw order should be random, not trivially sequential")
    }

    @Test
    fun testDrawAfterCompleteIsSafeNoOp() {
        var current = NumberGenerator.freshGame()
        repeat(90) {
            current = NumberGenerator.drawNumber(current)
        }

        assertTrue(current.isGameComplete)
        val completedState = current

        // 91st draw must return current unchanged and never throw
        val after91stDraw = NumberGenerator.drawNumber(current)
        assertEquals(completedState, after91stDraw, "91st draw should return state unchanged")
        assertEquals(90, after91stDraw.calledNumbers.size)
        assertEquals(0, after91stDraw.remainingPool.size)
    }

    @Test
    fun testNewGameResetsState() {
        var current = NumberGenerator.freshGame()
        repeat(30) {
            current = NumberGenerator.drawNumber(current)
        }
        assertEquals(30, current.calledNumbers.size)

        val resetGame = NumberGenerator.freshGame()
        assertEquals(0, resetGame.calledNumbers.size)
        assertEquals(90, resetGame.remainingPool.size)
        assertFalse(resetGame.isGameComplete)
    }
}
