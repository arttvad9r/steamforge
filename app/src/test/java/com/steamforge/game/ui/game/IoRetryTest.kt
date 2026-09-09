package com.steamforge.game.ui.game

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IoRetryTest {

    @Test
    fun `returns first successful result without retry`() = runBlocking {
        var attempts = 0

        val result = retryIoOnce {
            attempts++
            42
        }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrThrow())
        assertEquals(1, attempts)
    }

    @Test
    fun `retries one transient IOException`() = runBlocking {
        var attempts = 0

        val result = retryIoOnce {
            attempts++
            if (attempts == 1) throw IOException("transient")
            "ok"
        }

        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrThrow())
        assertEquals(2, attempts)
    }

    @Test
    fun `returns failure after second IOException`() = runBlocking {
        var attempts = 0

        val result = retryIoOnce<Unit> {
            attempts++
            throw IOException("still failing")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(2, attempts)
    }

    @Test(expected = IllegalStateException::class)
    fun `does not swallow non IO failures`() = runBlocking<Unit> {
        retryIoOnce<Unit> {
            throw IllegalStateException("programming error")
        }
    }
}
