package com.fahim.bingonumbercaller.domain

import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.model.allNumbers
import kotlin.random.Random

object TicketGenerator {

    fun generateTicket(): Ticket {
        while (true) {
            val candidate = tryGenerateCandidate()
            if (candidate != null && isValid(candidate)) {
                return candidate
            }
        }
    }

    private fun tryGenerateCandidate(): Ticket? {
        val columnCounts = pickColumnCounts() ?: return null
        val rowAssignment = assignRows(columnCounts) ?: return null

        val cells = Array(3) { Array<Int?>(9) { null } }

        for (col in 0 until 9) {
            val range = columnRange(col)
            val count = columnCounts[col]
            val numbers = range.shuffled(Random).take(count).sorted()

            var numIndex = 0
            for (row in 0 until 3) {
                if (rowAssignment[row][col]) {
                    cells[row][col] = numbers[numIndex++]
                }
            }
        }

        val id = generateUuid()
        return Ticket(id = id, cells = cells.map { it.toList() })
    }

    private fun pickColumnCounts(): IntArray? {
        val counts = IntArray(9) { 1 }
        var remaining = 6
        val availableCols = (0 until 9).toMutableList()

        while (remaining > 0) {
            if (availableCols.isEmpty()) return null
            val col = availableCols.random(Random)
            counts[col]++
            if (counts[col] == 3) {
                availableCols.remove(col)
            }
            remaining--
        }
        return counts
    }

    private fun assignRows(columnCounts: IntArray): Array<BooleanArray>? {
        val grid = Array(3) { BooleanArray(9) }
        val rowSums = IntArray(3)

        // For columns with count 3, they MUST be in all 3 rows
        for (col in 0 until 9) {
            if (columnCounts[col] == 3) {
                for (row in 0 until 3) {
                    grid[row][col] = true
                    rowSums[row]++
                }
            }
        }

        if (rowSums.any { it > 5 }) return null

        val remainingCols = (0 until 9).filter { columnCounts[it] < 3 }.shuffled(Random)

        if (backtrackAssignment(remainingCols, 0, columnCounts, grid, rowSums)) {
            return grid
        }
        return null
    }

    private fun backtrackAssignment(
        columns: List<Int>,
        index: Int,
        columnCounts: IntArray,
        grid: Array<BooleanArray>,
        rowSums: IntArray
    ): Boolean {
        if (index == columns.size) {
            return rowSums[0] == 5 && rowSums[1] == 5 && rowSums[2] == 5
        }

        val col = columns[index]
        val count = columnCounts[col]

        val rowCombinations = when (count) {
            1 -> listOf(listOf(0), listOf(1), listOf(2)).shuffled(Random)
            2 -> listOf(listOf(0, 1), listOf(0, 2), listOf(1, 2)).shuffled(Random)
            else -> emptyList()
        }

        for (rows in rowCombinations) {
            if (rows.all { rowSums[it] < 5 }) {
                for (r in rows) {
                    grid[r][col] = true
                    rowSums[r]++
                }

                if (backtrackAssignment(columns, index + 1, columnCounts, grid, rowSums)) {
                    return true
                }

                for (r in rows) {
                    grid[r][col] = false
                    rowSums[r]--
                }
            }
        }
        return false
    }

    private fun columnRange(col: Int): List<Int> = when (col) {
        0 -> (1..9).toList()
        1 -> (10..19).toList()
        2 -> (20..29).toList()
        3 -> (30..39).toList()
        4 -> (40..49).toList()
        5 -> (50..59).toList()
        6 -> (60..69).toList()
        7 -> (70..79).toList()
        8 -> (80..90).toList()
        else -> emptyList()
    }

    private fun generateUuid(): String {
        val hexChars = "0123456789abcdef"
        val bytes = ByteArray(16)
        Random.nextBytes(bytes)
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()

        val sb = StringBuilder()
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xff
            sb.append(hexChars[b ushr 4])
            sb.append(hexChars[b and 0x0f])
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                sb.append('-')
            }
        }
        return sb.toString()
    }

    private fun isValid(ticket: Ticket): Boolean {
        if (ticket.cells.size != 3) return false
        if (ticket.cells.any { it.size != 9 }) return false

        for (row in ticket.cells) {
            if (row.filterNotNull().size != 5) return false
        }

        val all = ticket.allNumbers
        if (all.size != 15) return false
        if (all.toSet().size != 15) return false

        for (col in 0 until 9) {
            val colNumbers = (0 until 3).mapNotNull { row -> ticket.cells[row][col] }
            if (colNumbers.size !in 1..3) return false

            val expectedRange = columnRange(col)
            if (!colNumbers.all { it in expectedRange }) return false

            for (i in 0 until colNumbers.size - 1) {
                if (colNumbers[i] >= colNumbers[i + 1]) return false
            }
        }

        return true
    }
}

fun generateTicket(): Ticket = TicketGenerator.generateTicket()
