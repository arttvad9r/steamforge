package com.steamforge.game.core

import kotlin.random.Random

/**
 * Детерминированный PRNG с сериализуемой позицией. Используется обычными seeded runs и verifier'ами,
 * чтобы одна и та же пара seed + последовательность команд всегда давала тот же spawn stream.
 */
class ReplayableRandom(
    private val seed: Long,
    initialDraws: Long = 0L,
) : Random() {
    var draws: Long = initialDraws.coerceIn(0L, MAX_DRAWS)
        private set

    override fun nextBits(bitCount: Int): Int {
        require(bitCount in 0..32)
        if (bitCount == 0) return 0
        val index = draws++
        var z = seed + GOLDEN_GAMMA * (index + 1L)
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        z = z xor (z ushr 31)
        return (z ushr (64 - bitCount)).toInt()
    }

    private companion object {
        const val MAX_DRAWS = 1_000_000L
        const val GOLDEN_GAMMA = -7046029254386353131L
        const val MIX_1 = -4658895280553007687L
        const val MIX_2 = -7723592293110705685L
    }
}
