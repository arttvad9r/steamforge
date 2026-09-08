package com.steamforge.game.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.steamforge.game.R

enum class Sfx { MOVE, UNDO, MERGE_LOW, MERGE_MID, MERGE_HIGH, OVERDRIVE, GAME_OVER, WIN, COIN, LEVEL_UP }

/** SoundPool — официальный API для коротких игровых звуков. */
class SfxPlayer(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loadGate = SoundLoadGate()

    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var released: Boolean = false

    private val ids: Map<Sfx, Int>

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            val pending = loadGate.markLoaded(sampleId, successful = status == 0)
            if (pending != null) playLoaded(sampleId, pending)
        }

        ids = mapOf(
            Sfx.MOVE to pool.load(context, R.raw.sfx_move, 1),
            Sfx.UNDO to pool.load(context, R.raw.sfx_undo, 1),
            Sfx.MERGE_LOW to pool.load(context, R.raw.sfx_merge_low, 1),
            Sfx.MERGE_MID to pool.load(context, R.raw.sfx_merge_mid, 1),
            Sfx.MERGE_HIGH to pool.load(context, R.raw.sfx_merge_high, 1),
            Sfx.OVERDRIVE to pool.load(context, R.raw.sfx_overdrive, 1),
            Sfx.GAME_OVER to pool.load(context, R.raw.sfx_gameover, 1),
            Sfx.WIN to pool.load(context, R.raw.sfx_win, 1),
            Sfx.COIN to pool.load(context, R.raw.sfx_coin, 1),
            Sfx.LEVEL_UP to pool.load(context, R.raw.sfx_levelup, 1),
        )
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) loadGate.clearPending()
    }

    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled || released) return
        val sampleId = ids[sfx] ?: return
        val playback = PendingSoundPlayback(
            volume = volume.coerceIn(0f, 1f),
            rate = rate.coerceIn(0.5f, 2f),
        )
        loadGate.request(sampleId, playback)?.let { ready ->
            playLoaded(sampleId, ready)
        }
    }

    private fun playLoaded(sampleId: Int, playback: PendingSoundPlayback) {
        if (!enabled || released) return
        pool.play(
            sampleId,
            playback.volume,
            playback.volume,
            1,
            0,
            playback.rate,
        )
    }

    fun release() {
        if (released) return
        released = true
        loadGate.clear()
        pool.setOnLoadCompleteListener(null)
        pool.release()
    }
}
