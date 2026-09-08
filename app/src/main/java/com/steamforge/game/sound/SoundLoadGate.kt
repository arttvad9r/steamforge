package com.steamforge.game.sound

/** A playback request that can be replayed once an asynchronous SoundPool sample finishes loading. */
internal data class PendingSoundPlayback(
    val volume: Float,
    val rate: Float,
)

/**
 * Keeps SoundPool's asynchronous loading boundary out of gameplay code.
 *
 * Requests for an already loaded sample are returned immediately. While samples are still loading, only the latest
 * gameplay feedback is retained globally so a slow cold start can never build an obsolete audio backlog.
 */
internal class SoundLoadGate {
    private val loadedSampleIds = mutableSetOf<Int>()
    private var pending: Pair<Int, PendingSoundPlayback>? = null

    @Synchronized
    fun request(sampleId: Int, playback: PendingSoundPlayback): PendingSoundPlayback? {
        if (sampleId in loadedSampleIds) {
            // A newer request that can play now supersedes any older request still waiting for a different sample.
            pending = null
            return playback
        }
        pending = sampleId to playback
        return null
    }

    @Synchronized
    fun markLoaded(sampleId: Int, successful: Boolean): PendingSoundPlayback? {
        if (!successful) {
            if (pending?.first == sampleId) pending = null
            return null
        }
        loadedSampleIds += sampleId
        if (pending?.first != sampleId) return null
        return pending?.second.also { pending = null }
    }

    @Synchronized
    fun clearPending() {
        pending = null
    }

    @Synchronized
    fun clear() {
        pending = null
        loadedSampleIds.clear()
    }
}
