package com.fahim.bingonumbercaller.audio

/**
 * What the caller says for each number, in the classic style: single digits are "on its own",
 * two-digit numbers are read digit by digit and then whole ("4 and 2, 42").
 * Digits are left as numerals so the speech engine reads them in the device language.
 */
object BingoCallPhrases {
    fun phraseFor(number: Int): String {
        require(number in 1..90) { "Bingo numbers are 1-90, got $number" }
        return if (number < 10) {
            "On its own, number $number"
        } else {
            "${number / 10} and ${number % 10}, $number"
        }
    }
}
