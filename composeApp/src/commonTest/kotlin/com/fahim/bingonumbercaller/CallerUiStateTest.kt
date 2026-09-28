package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.protocol.ServerMessage
import com.fahim.bingonumbercaller.viewmodel.CallerUiState
import com.fahim.bingonumbercaller.viewmodel.isSingleNewDraw
import com.fahim.bingonumbercaller.viewmodel.withServerUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CallerUiStateTest {

    private fun stateWith(called: List<Int>, winner: String? = null) = CallerUiState(
        gameState = GameState(
            calledNumbers = called,
            remainingPool = (1..90).filterNot { it in called }
        ),
        winnerConnectionId = winner
    )

    private fun update(called: List<Int>, status: String = "IN_PROGRESS") =
        ServerMessage.GameStateUpdate(
            calledNumbers = called,
            currentNumber = called.lastOrNull(),
            remainingCount = 90 - called.size,
            status = status
        )

    @Test
    fun newGameUpdate_clearsWinner() {
        val afterWin = stateWith(listOf(5, 17, 42), winner = "conn-abc")

        val reset = afterWin.withServerUpdate(update(emptyList()))

        assertNull(reset.winnerConnectionId)
        assertTrue(reset.calledNumbers.isEmpty())
        assertEquals(90, reset.remainingPool.size)
    }

    @Test
    fun completeUpdate_keepsWinner() {
        val afterWin = stateWith(listOf(5, 17), winner = "conn-abc")

        val next = afterWin.withServerUpdate(update(listOf(5, 17), status = "COMPLETE"))

        assertEquals("conn-abc", next.winnerConnectionId)
    }

    @Test
    fun serverUpdate_rebuildsRemainingPool() {
        val next = stateWith(emptyList()).withServerUpdate(update(listOf(3, 88)))

        assertEquals(listOf(3, 88), next.calledNumbers)
        assertEquals(88, next.remainingPool.size)
        assertFalse(next.remainingPool.contains(3))
        assertFalse(next.remainingPool.contains(88))
    }

    @Test
    fun isSingleNewDraw_trueOnlyWhenUpdateExtendsByOne() {
        val state = stateWith(listOf(5, 17))

        assertTrue(state.isSingleNewDraw(update(listOf(5, 17, 42))))
        assertFalse(state.isSingleNewDraw(update(listOf(5, 17))), "unchanged state is not a draw")
        assertFalse(state.isSingleNewDraw(update(emptyList())), "reset is not a draw")
        assertFalse(state.isSingleNewDraw(update(listOf(9, 17, 42))), "different history is not a draw")
        assertFalse(state.isSingleNewDraw(update(listOf(5, 17, 42, 60))), "bulk sync is not a draw")
    }
}
