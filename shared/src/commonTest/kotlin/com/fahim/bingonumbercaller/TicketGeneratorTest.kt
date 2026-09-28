package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.domain.TicketGenerator
import com.fahim.bingonumbercaller.model.allNumbers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TicketGeneratorTest {

    @Test
    fun testGenerate100Tickets_allValidPerRules() {
        for (i in 1..100) {
            val ticket = TicketGenerator.generateTicket()

            // 1. Grid structure is 3 rows x 9 columns
            assertEquals(3, ticket.cells.size, "Ticket $i must have 3 rows")
            for (row in ticket.cells) {
                assertEquals(9, row.size, "Ticket $i rows must have 9 columns")
            }

            // 2. Exactly 15 numbers total
            val allNumbers = ticket.allNumbers
            assertEquals(15, allNumbers.size, "Ticket $i must have exactly 15 numbers")
            assertEquals(15, allNumbers.toSet().size, "Ticket $i must have 15 distinct numbers")

            // 3. Exactly 5 numbers per row (4 blanks per row)
            for ((rowIndex, row) in ticket.cells.withIndex()) {
                val nonNullCount = row.filterNotNull().size
                assertEquals(5, nonNullCount, "Ticket $i row $rowIndex must have 5 numbers")
            }

            // 4. Columns: 1..3 numbers each, within range, strictly ascending top to bottom
            for (col in 0 until 9) {
                val colNumbers = (0 until 3).mapNotNull { r -> ticket.cells[r][col] }
                assertTrue(
                    colNumbers.size in 1..3,
                    "Ticket $i col $col count must be 1..3, but was ${colNumbers.size}"
                )

                val expectedRange = when (col) {
                    0 -> 1..9
                    1 -> 10..19
                    2 -> 20..29
                    3 -> 30..39
                    4 -> 40..49
                    5 -> 50..59
                    6 -> 60..69
                    7 -> 70..79
                    8 -> 80..90
                    else -> 0..0
                }

                for (num in colNumbers) {
                    assertTrue(
                        num in expectedRange,
                        "Ticket $i col $col number $num must be in range $expectedRange"
                    )
                }

                // Check strictly ascending
                for (k in 0 until colNumbers.size - 1) {
                    assertTrue(
                        colNumbers[k] < colNumbers[k + 1],
                        "Ticket $i col $col numbers must be strictly ascending: ${colNumbers[k]} < ${colNumbers[k+1]}"
                    )
                }
            }
        }
    }
}
