package com.steamforge.game.sound

/** A playback request that can be replayed once an asynchronous SoundPool sample finishes loading. */
internal data class PendingSoundPlayback(
    val volume: Float,
    val rate: Float,
)

/**
 * Keeps SoundPool's asynchronous loading boundary out of gameplay code.
 *
 * Requests for an already loaded sample are returned immediately. Requests that arrive while a sample is still
 * loading are coalesced to the latest request for that sample and returned from [markLoaded] once loading succeeds.
 */
internal class SoundLoadGate {
    private val loadedSampleIds = mutableSetOf<Int>()
    private val pendingBySampleId = mutableMapOf<Int, PendingSoundPlayback>()

    @Synchronized
    fun request(sampleId: Int, playback: PendingSoundPlayback): PendingSoundPlayback? {
        if (sampleId in loadedSampleIds) return playback
        pendingBySampleId[sampleId] = playback
        return null
    }

    @Synchronized
    fun markLoaded(sampleId: Int, successful: Boolean): PendingSoundPlayback? {
        if (!successful) {
            pendingBySampleId.remove(sampleId)
            return null
        }
        loadedSampleIds += sampleId
        return pendingBySampleId.remove(sampleId)
    }

    @Synchronized
    fun clearPending() {
        pendingBySampleId.clear()
    }

    @Synchronized
    fun clear() {
        pendingBySampleId.clear()
        loadedSampleIds.clear()
    }
}
