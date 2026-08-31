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

    @Volatile
    private var enabled: Boolean = true

    private val ids: Map<Sfx, Int> = mapOf(
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

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun play(sfx: Sfx, volume: Float = 1f) {
        if (!enabled) return
        ids[sfx]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    fun release() {
        pool.release()
    }
}
