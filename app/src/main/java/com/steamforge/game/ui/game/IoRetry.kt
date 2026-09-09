package com.steamforge.game.ui.game

import java.io.IOException

/**
 * Executes an idempotent I/O operation with one automatic retry for transient [IOException]s.
 * Non-I/O failures are deliberately not swallowed.
 */
internal suspend fun <T> retryIoOnce(block: suspend () -> T): Result<T> {
    var lastFailure: IOException? = null
    repeat(2) {
        try {
            return Result.success(block())
        } catch (error: IOException) {
            lastFailure = error
        }
    }
    return Result.failure(requireNotNull(lastFailure))
}
