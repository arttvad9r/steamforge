package com.steamforge.game.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFeatureVisibilityTest {

    @Test
    fun `fresh profile exposes only core home shell`() {
        assertEquals(
            HomeFeatureVisibility(),
            homeFeatureVisibility(
                gamesPlayed = 0,
                activeRunMerges = 0,
                hasBlueprintPieces = false,
            ),
        )
    }

    @Test
    fun `starting first run does not dump meta destinations`() {
        assertEquals(
            HomeFeatureVisibility(),
            homeFeatureVisibility(
                gamesPlayed = 0,
                activeRunMerges = 0,
                hasBlueprintPieces = false,
            ),
        )
    }

    @Test
    fun `first merge reveals status rail and workshop only`() {
        assertEquals(
            HomeFeatureVisibility(
                showStatusRail = true,
                showWorkshop = true,
            ),
            homeFeatureVisibility(
                gamesPlayed = 0,
                activeRunMerges = 1,
                hasBlueprintPieces = false,
            ),
        )
    }

    @Test
    fun `completed first run reveals daily contracts and persistent meta`() {
        assertEquals(
            HomeFeatureVisibility(
                showStatusRail = true,
                showWorkshop = true,
                showContracts = true,
                showDaily = true,
                showCollection = true,
            ),
            homeFeatureVisibility(
                gamesPlayed = 1,
                activeRunMerges = 0,
                hasBlueprintPieces = false,
            ),
        )
    }

    @Test
    fun `existing blueprint state never becomes unreachable`() {
        assertEquals(
            HomeFeatureVisibility(
                showStatusRail = true,
                showWorkshop = true,
                showCollection = true,
            ),
            homeFeatureVisibility(
                gamesPlayed = 0,
                activeRunMerges = 0,
                hasBlueprintPieces = true,
            ),
        )
    }
}
