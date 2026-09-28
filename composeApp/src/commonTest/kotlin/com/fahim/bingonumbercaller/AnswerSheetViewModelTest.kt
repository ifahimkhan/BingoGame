package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.model.allNumbers
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnswerSheetViewModelTest {

    private fun createSampleTicket(): Ticket {
        val row1 = listOf(1, null, 20, null, 40, null, 60, null, 80)
        val row2 = listOf(null, 11, null, 31, 41, null, 61, null, 81)
        val row3 = listOf(2, null, 22, null, null, 52, null, 72, 82)
        return Ticket(id = "ticket-123", cells = listOf(row1, row2, row3))
    }

    @Test
    fun calledNumbers_doNotAutomaticallyMarkCells() {
        val ticket = createSampleTicket()
        // Server called 1, 20, and 40
        val state = AnswerSheetUiState(
            ticket = ticket,
            calledNumbers = listOf(1, 20, 40),
            userCheckedNumbers = emptySet() // User hasn't touched the screen
        )

        // markedNumbers must be empty because the user hasn't checked them manually
        assertTrue(state.markedNumbers.isEmpty())
        assertFalse(state.isTicketComplete)
    }

    @Test
    fun userCanCheckCalledNumber_andItBecomesMarked() {
        val ticket = createSampleTicket()
        val state = AnswerSheetUiState(
            ticket = ticket,
            calledNumbers = listOf(1, 20, 40),
            userCheckedNumbers = setOf(1, 20)
        )

        assertEquals(setOf(1, 20), state.markedNumbers)
        assertTrue(state.isNumberCalled(1))
        assertTrue(state.isNumberChecked(1))
        assertTrue(state.isNumberCalled(40))
        assertFalse(state.isNumberChecked(40)) // 40 is called but not checked yet
    }

    @Test
    fun checkingUncalledNumber_isNotMarked() {
        val ticket = createSampleTicket()
        // User attempted to check 80, but 80 has not been called yet
        val state = AnswerSheetUiState(
            ticket = ticket,
            calledNumbers = listOf(1, 20),
            userCheckedNumbers = setOf(1, 80) // 80 was checked by mistake
        )

        // 80 must NOT be in markedNumbers because markedNumbers is the intersection with calledNumbers
        assertEquals(setOf(1), state.markedNumbers)
        assertFalse(state.markedNumbers.contains(80))
    }

    @Test
    fun ticketComplete_onlyWhenAll15NumbersAreBothCalledAndChecked() {
        val ticket = createSampleTicket()
        val all15 = ticket.allNumbers
        assertEquals(15, all15.size)

        // Case 1: All 15 called, but only 14 checked by user
        val state1 = AnswerSheetUiState(
            ticket = ticket,
            calledNumbers = all15,
            userCheckedNumbers = all15.take(14).toSet()
        )
        assertFalse(state1.isTicketComplete)
        assertEquals(14, state1.markedNumbers.size)

        // Case 2: All 15 checked by user, but only 14 called by caller
        val state2 = AnswerSheetUiState(
            ticket = ticket,
            calledNumbers = all15.take(14),
            userCheckedNumbers = all15.toSet()
        )
        assertFalse(state2.isTicketComplete)
        assertEquals(14, state2.markedNumbers.size)

        // Case 3: All 15 called AND all 15 manually checked by user
        val state3 = AnswerSheetUiState(
            ticket = ticket,
            calledNumbers = all15,
            userCheckedNumbers = all15.toSet()
        )
        assertTrue(state3.isTicketComplete)
        assertEquals(15, state3.markedNumbers.size)
    }

    @Test
    fun uncheckingNumber_removesItFromMarkedNumbers() {
        val ticket = createSampleTicket()
        val state = AnswerSheetUiState(
            ticket = ticket,
            calledNumbers = listOf(1, 20),
            userCheckedNumbers = setOf(1, 20)
        )
        assertEquals(setOf(1, 20), state.markedNumbers)

        // User unchecks 20
        val updatedState = state.copy(
            userCheckedNumbers = state.userCheckedNumbers - 20
        )
        assertEquals(setOf(1), updatedState.markedNumbers)
        assertFalse(updatedState.isNumberChecked(20))
    }
}
