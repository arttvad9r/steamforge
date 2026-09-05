package com.steamforge.game

/** Stable product-level identity for gameplay sessions. */
enum class GameRunMode(val wireName: String) {
    NORMAL("normal"),
    DAILY("daily"),
    WEEKLY("weekly"),
    ;

    val isDaily: Boolean
        get() = this == DAILY
}
