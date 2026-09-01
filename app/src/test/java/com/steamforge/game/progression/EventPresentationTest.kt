package com.steamforge.game.progression

import com.steamforge.game.config.EventTemplateConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EventPresentationTest {

    @Test
    fun `foundry presentation lives in event definition`() {
        val event = LiveOpsCatalog.foundryWeek(25_000L)
        val theme = event.theme

        assertEquals("STEAM PRESSURE", theme.scoreLabel)
        assertEquals("pressure", theme.compactUnit)
        assertEquals("PRESSURE", theme.milestoneUnit)
        assertEquals("РУБЕЖИ ЛИТЕЙНОЙ", theme.milestonesTitle)
        assertEquals(true, theme.rulesText.contains("64+"))
    }

    @Test
    fun `generic event does not inherit foundry wording`() {
        val theme = EventTheme(
            id = "score-run",
            title = "SCORE SHIFT",
            subtitle = "Набирайте очки",
            accent = "steel-blue",
        )

        assertEquals("EVENT SCORE", theme.scoreLabel)
        assertEquals("очков", theme.compactUnit)
        assertEquals("POINTS", theme.milestoneUnit)
        assertEquals("РУБЕЖИ СОБЫТИЯ", theme.milestonesTitle)
        assertFalse(theme.rulesText.contains("Pressure", ignoreCase = true))
    }

    @Test
    fun `event template carries custom presentation without ui code`() {
        val customTheme = EventTheme(
            id = "maintenance",
            title = "MAINTENANCE SHIFT",
            subtitle = "Стабилизируйте линию",
            accent = "patina-teal",
            scoreLabel = "OUTPUT SCORE",
            compactUnit = "output",
            milestoneUnit = "OUTPUT",
            milestonesTitle = "РУБЕЖИ ЛИНИИ",
            rulesText = "Каждые 100 игровых очков дают 1 output.",
        )
        val template = EventTemplateConfig(
            idPrefix = "maintenance",
            durationDays = 5,
            scoringRule = EventScoringRule(EventMetric.SCORE, pointsPerUnit = 1, unitsPerStep = 100),
            milestones = listOf(EventMilestone("output-10", 10, EventReward(gems = 3))),
            theme = customTheme,
        )

        val event = template.instantiateForEpochDay(25_000L)

        assertEquals(EventMetric.SCORE, event.scoringRule.metric)
        assertEquals(customTheme, event.theme)
        assertEquals("OUTPUT", event.theme.milestoneUnit)
    }
}
