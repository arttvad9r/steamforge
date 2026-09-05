package com.steamforge.game.config

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRemoteConfigProviderTest {
    private class MemoryCache(initial: String? = null) : RemoteConfigCache {
        var payload: String? = initial
        var writes: Int = 0

        override suspend fun read(): String? = payload

        override suspend fun write(payload: String) {
            this.payload = payload
            writes += 1
        }
    }

    @Test
    fun `cleartext endpoint is rejected before any network request`() = runTest {
        val fetcher = HttpsRemoteConfigFetcher("http://example.com/config.json")

        assertNull(fetcher.fetch())
    }

    @Test
    fun `valid remote payload becomes active and is cached`() = runTest {
        val cache = MemoryCache()
        val provider = CachingRemoteConfigProvider(
            cache = cache,
            fetcher = RemoteConfigFetcher {
                """
                {
                  "schemaVersion": 1,
                  "workshopUpgradeCosts": [7, 15, 30, 60],
                  "contractRewardMultiplier": 1.5,
                  "rewardMultiplier": 1.25,
                  "featureFlags": {"weeklyChallengeEnabled": true},
                  "futureField": "ignored"
                }
                """.trimIndent()
            },
        )

        val result = provider.refresh()

        assertEquals(RemoteConfigRefreshResult.UPDATED, result)
        assertEquals(RemoteConfigSource.REMOTE, provider.snapshot.value.source)
        assertEquals(listOf(7, 15, 30, 60), provider.snapshot.value.config.workshopUpgradeCosts)
        assertEquals(1.5, provider.snapshot.value.config.contractRewardMultiplier, 0.0)
        assertTrue(provider.snapshot.value.config.featureFlags.weeklyChallengeEnabled)
        assertEquals(1, cache.writes)
        assertNotNull(cache.payload)
    }

    @Test
    fun `valid cached payload stays active when remote fetch fails`() = runTest {
        val cache = MemoryCache(
            """
            {
              "schemaVersion": 1,
              "workshopUpgradeCosts": [8, 16, 32, 64],
              "contractRewardMultiplier": 1.0,
              "rewardMultiplier": 1.0
            }
            """.trimIndent(),
        )
        val provider = CachingRemoteConfigProvider(
            cache = cache,
            fetcher = RemoteConfigFetcher { null },
        )

        val result = provider.refresh()

        assertEquals(RemoteConfigRefreshResult.FAILED_USING_FALLBACK, result)
        assertEquals(RemoteConfigSource.CACHE, provider.snapshot.value.source)
        assertEquals(listOf(8, 16, 32, 64), provider.snapshot.value.config.workshopUpgradeCosts)
        assertEquals(0, cache.writes)
    }

    @Test
    fun `unsupported remote schema cannot replace last known good cache`() = runTest {
        val cache = MemoryCache(
            """
            {
              "schemaVersion": 1,
              "workshopUpgradeCosts": [9, 18, 36, 72]
            }
            """.trimIndent(),
        )
        val provider = CachingRemoteConfigProvider(
            cache = cache,
            fetcher = RemoteConfigFetcher {
                """
                {
                  "schemaVersion": 2,
                  "workshopUpgradeCosts": [1, 2, 3, 4]
                }
                """.trimIndent()
            },
        )

        val result = provider.refresh()

        assertEquals(RemoteConfigRefreshResult.FAILED_USING_FALLBACK, result)
        assertEquals(RemoteConfigSource.CACHE, provider.snapshot.value.source)
        assertEquals(listOf(9, 18, 36, 72), provider.snapshot.value.config.workshopUpgradeCosts)
        assertEquals(0, cache.writes)
    }

    @Test
    fun `invalid cache and malformed remote payload leave compiled defaults active`() = runTest {
        val cache = MemoryCache("not-json")
        val provider = CachingRemoteConfigProvider(
            cache = cache,
            fetcher = RemoteConfigFetcher { "{still-not-json" },
        )

        val result = provider.refresh()

        assertEquals(RemoteConfigRefreshResult.FAILED_USING_FALLBACK, result)
        assertEquals(RemoteConfigSource.LOCAL_DEFAULT, provider.snapshot.value.source)
        assertEquals(LocalDefaultConfig.value, provider.snapshot.value.config)
        assertEquals(0, cache.writes)
    }

    @Test
    fun `invalid remote fields are sanitized before cache write`() = runTest {
        val cache = MemoryCache()
        val provider = CachingRemoteConfigProvider(
            cache = cache,
            fetcher = RemoteConfigFetcher {
                """
                {
                  "schemaVersion": 1,
                  "workshopUpgradeCosts": [80, 10, -5, 2],
                  "contractRewardMultiplier": 99.0,
                  "rewardMultiplier": -1.0
                }
                """.trimIndent()
            },
        )

        val result = provider.refresh()

        assertEquals(RemoteConfigRefreshResult.UPDATED, result)
        assertEquals(LocalDefaultConfig.value.workshopUpgradeCosts, provider.snapshot.value.config.workshopUpgradeCosts)
        assertEquals(LocalDefaultConfig.value.contractRewardMultiplier, provider.snapshot.value.config.contractRewardMultiplier, 0.0)
        assertEquals(LocalDefaultConfig.value.rewardMultiplier, provider.snapshot.value.config.rewardMultiplier, 0.0)
        assertEquals(1, cache.writes)
        assertTrue(cache.payload!!.contains("20"))
        assertTrue(cache.payload!!.contains("1.0"))
    }
}
