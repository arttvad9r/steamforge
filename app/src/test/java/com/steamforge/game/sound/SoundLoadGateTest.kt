package com.steamforge.game.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundLoadGateTest {

    @Test
    fun `request waits until sample load succeeds`() {
        val gate = SoundLoadGate()
        val playback = PendingSoundPlayback(volume = 0.8f, rate = 1.05f)

        assertNull(gate.request(sampleId = 7, playback = playback))
        assertEquals(playback, gate.markLoaded(sampleId = 7, successful = true))
        assertEquals(playback, gate.request(sampleId = 7, playback = playback))
    }

    @Test
    fun `pending request coalesces to latest playback`() {
        val gate = SoundLoadGate()
        val first = PendingSoundPlayback(volume = 0.5f, rate = 1f)
        val latest = PendingSoundPlayback(volume = 1f, rate = 1.075f)

        assertNull(gate.request(sampleId = 3, playback = first))
        assertNull(gate.request(sampleId = 3, playback = latest))
        assertEquals(latest, gate.markLoaded(sampleId = 3, successful = true))
    }

    @Test
    fun `failed load and pending clear never replay stale feedback`() {
        val gate = SoundLoadGate()
        val playback = PendingSoundPlayback(volume = 1f, rate = 1f)

        assertNull(gate.request(sampleId = 11, playback = playback))
        assertNull(gate.markLoaded(sampleId = 11, successful = false))

        assertNull(gate.request(sampleId = 12, playback = playback))
        gate.clearPending()
        assertNull(gate.markLoaded(sampleId = 12, successful = true))
    }
}
