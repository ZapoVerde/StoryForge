package com.example.storyforge.utils

import kotlin.random.Random

object DiceRoller {

    private val diceRegex = Regex("""^\s*(\d+)d(\d+)([+-]\d+)?(!)?\s*$""")

    data class RollResult(
        val total: Int,
        val rolls: List<Int>,
        val modifier: Int,
        val formula: String,
        val error: String? = null
    )

    fun roll(formula: String): RollResult {
        val trimmed = formula.trim()

        val match = diceRegex.matchEntire(trimmed)
            ?: return RollResult(
                total = 0,
                rolls = emptyList(),
                modifier = 0,
                formula = trimmed,
                error = "Invalid format. Try formats like: 2d6, 1d20+3, 3d4-1, 2d6!"
            )

        val (countStr, sidesStr, modStr, verboseFlag) = match.destructured
        val count = countStr.toInt()
        val sides = sidesStr.toInt()
        val modifier = modStr.toIntOrNull() ?: 0
        val verbose = verboseFlag == "!"

        val rolls = List(count) { Random.nextInt(1, sides + 1) }
        val total = rolls.sum() + modifier

        return RollResult(
            total = total,
            rolls = if (verbose) rolls else emptyList(),
            modifier = modifier,
            formula = trimmed,
            error = null
        )
    }

    fun format(result: RollResult): String {
        if (result.error != null) return result.error

        return if (result.rolls.isNotEmpty()) {
            val rollStr = result.rolls.joinToString(", ")
            val modStr = if (result.modifier != 0)
                " ${if (result.modifier > 0) "+" else "-"} ${kotlin.math.abs(result.modifier)}"
            else ""
            "Rolled ${result.formula}: [$rollStr]$modStr = ${result.total}"
        } else {
            "Rolled ${result.formula}: ${result.total}"
        }
    }
}
