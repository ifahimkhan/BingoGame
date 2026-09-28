package com.fahim.bingonumbercaller.domain

/**
 * Display-name rules shared by client (input limit) and server (authoritative cleanup).
 * Names are untrusted input from any device on the network.
 */
object PlayerNames {
    const val MAX_LENGTH = 20

    private val whitespace = Regex("\\s+")

    /** Cleaned name, or null when nothing usable remains (the server then assigns a default). */
    fun sanitize(raw: String?): String? {
        if (raw == null) return null
        val cleaned = raw
            .filterNot { it.isISOControl() && !it.isWhitespace() }
            .replace(whitespace, " ")
            .trim()
            .take(MAX_LENGTH)
            .trim()
        return cleaned.ifEmpty { null }
    }

    /** [name] if not already in [taken] (case-insensitive), else "name (2)", "name (3)", ... within [MAX_LENGTH]. */
    fun unique(name: String, taken: Set<String>): String {
        val takenLower = taken.mapTo(mutableSetOf()) { it.lowercase() }
        if (name.lowercase() !in takenLower) return name

        var n = 2
        while (true) {
            val suffix = " ($n)"
            val candidate = name.take(MAX_LENGTH - suffix.length).trimEnd() + suffix
            if (candidate.lowercase() !in takenLower) return candidate
            n++
        }
    }

    fun defaultName(seatNumber: Int): String = "Player $seatNumber"
}
