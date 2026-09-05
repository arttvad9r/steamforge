package com.steamforge.game.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplayableRandomTest {
    @Test
    fun `seed zero keeps stable 32 bit golden vector`() {
        val rng = ReplayableRandom(seed = 0L)

        val values = List(5) { rng.nextBits(32) }

        assertEquals(
            listOf(-501176263, 1853398634, 113532184, -125060952, 456755562),
            values,
        )
        assertEquals(5L, rng.draws)
    }

    @Test
    fun `serialized draw position resumes the same stream`() {
        val resumed = ReplayableRandom(seed = 0L, initialDraws = 2L)

        assertEquals(113532184, resumed.nextBits(32))
        assertEquals(3L, resumed.draws)
    }
}
