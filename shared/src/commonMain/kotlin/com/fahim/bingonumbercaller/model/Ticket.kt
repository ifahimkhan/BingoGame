package com.fahim.bingonumbercaller.model

import kotlinx.serialization.Serializable

@Serializable
data class Ticket(
    val id: String,
    val cells: List<List<Int?>>
)

val Ticket.allNumbers: List<Int>
    get() = cells.flatten().filterNotNull()
