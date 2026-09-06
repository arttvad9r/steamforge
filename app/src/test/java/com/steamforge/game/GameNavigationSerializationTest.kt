package com.steamforge.game

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GameNavigationSerializationTest {
    @Test
    fun `game route round trips every run mode`() {
        GameRunMode.entries.forEach { mode ->
            val route = Game(mode)
            val encoded = Json.encodeToString(route)
            val restored = Json.decodeFromString<Game>(encoded)

            assertEquals(route, restored)
            assertEquals(mode, restored.mode)
        }
    }

    @Test
    fun `run mode enum uses stable wire names`() {
        GameRunMode.entries.forEach { mode ->
            assertEquals("\"${mode.wireName}\"", Json.encodeToString(mode))
        }
    }
}
