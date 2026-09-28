package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.domain.PlayerNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerNamesTest {

    @Test
    fun `trims and collapses inner whitespace`() {
        assertEquals("Ann Lee", PlayerNames.sanitize("   Ann \t  Lee  "))
    }

    @Test
    fun `strips control characters`() {
        assertEquals("Bob", PlayerNames.sanitize("B\u0000o\u001Bb\n"))
    }

    @Test
    fun `truncates to the maximum length`() {
        val long = "x".repeat(PlayerNames.MAX_LENGTH + 10)
        assertEquals(PlayerNames.MAX_LENGTH, PlayerNames.sanitize(long)?.length)
    }

    @Test
    fun `blank or missing names give null so the server can assign a default`() {
        assertNull(PlayerNames.sanitize(""))
        assertNull(PlayerNames.sanitize("   \n\t "))
        assertNull(PlayerNames.sanitize(null))
    }

    @Test
    fun `unique name adds a numeric suffix when taken, ignoring case`() {
        val taken = setOf("Ann", "ann (2)")
        assertEquals("Ann (3)", PlayerNames.unique("Ann", taken))
        assertEquals("Bob", PlayerNames.unique("Bob", taken))
    }

    @Test
    fun `unique name with suffix still fits the maximum length`() {
        val base = "y".repeat(PlayerNames.MAX_LENGTH)
        val result = PlayerNames.unique(base, setOf(base))
        assertEquals(PlayerNames.MAX_LENGTH, result.length)
        assertEquals(true, result.endsWith(" (2)"))
    }
}
