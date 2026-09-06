package com.steamforge.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Stable product-level identity for gameplay sessions and typed navigation. */
@Serializable
enum class GameRunMode(val wireName: String) {
    @SerialName("normal")
    NORMAL("normal"),

    @SerialName("daily")
    DAILY("daily"),

    @SerialName("weekly")
    WEEKLY("weekly"),
    ;

    val isDaily: Boolean
        get() = this == DAILY
}
