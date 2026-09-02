from pathlib import Path


def once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)

repo_path = Path('app/src/main/java/com/steamforge/game/data/SteamforgeRepository.kt')
repo = repo_path.read_text()
method_anchor = '''    override suspend fun applyGameFinish(
        record: FinishedGameRecord,
        finisher: (PlayerProgress) -> Pair<PlayerProgress, com.steamforge.game.progression.FinishEffects>,
    ) {
        context.dataStore.edit { prefs ->
'''
if repo.count(method_anchor) != 1:
    raise SystemExit(f'repository method anchor mismatch: {repo.count(method_anchor)}')
repo = repo.replace(
    method_anchor,
    method_anchor + '''            val existingFinished = prefs[Keys.finishedGame]?.let(FinishedGameCodec::decode)
            if (existingFinished?.id == record.id) {
                prefs.remove(Keys.game)
                return@edit
            }
''',
    1,
)
repo_path.write_text(repo)

fake_path = Path('app/src/test/java/com/steamforge/game/data/FakeDataRepo.kt')
fake = fake_path.read_text()
fake = once(
    fake,
    '''    ) {
        val (updated, effects) = finisher(currentProgress)
        currentProgress = updated
        currentFinished = record.withEffects(effects)
        currentGame = null
    }

    override suspend fun claimDoubleReward''',
    '''    ) {
        if (currentFinished?.id == record.id) {
            currentGame = null
            return
        }
        val (updated, effects) = finisher(currentProgress)
        currentProgress = updated
        currentFinished = record.withEffects(effects)
        currentGame = null
    }

    override suspend fun claimDoubleReward''',
    'fake finish idempotency',
)
fake_path.write_text(fake)

vm_path = Path('app/src/main/java/com/steamforge/game/ui/game/GameViewModel.kt')
vm = vm_path.read_text()
vm = once(
    vm,
    '''                repo.applyGameFinish(pending.record) { latest ->
                    val (updated, e) = applyGameFinished(latest, pending.summary, cfg)
                    eff = e
                    val withAchievementDays = updated.copy(
                        achievementDays = updated.achievementDays + e.newAchievements.associate { it.id to pending.day },
                    )
                    finalGemBalance = withAchievementDays.gems
                    withAchievementDays to e
                }
                if (discardFinishedRecord) repo.clearFinishedGame()

                finishWriteInFlight = false''',
    '''                repo.applyGameFinish(pending.record) { latest ->
                    val (updated, e) = applyGameFinished(latest, pending.summary, cfg)
                    eff = e
                    val withAchievementDays = updated.copy(
                        achievementDays = updated.achievementDays + e.newAchievements.associate { it.id to pending.day },
                    )
                    finalGemBalance = withAchievementDays.gems
                    withAchievementDays to e
                }
                val committedRecord = repo.finishedGame.first()?.takeIf { it.id == pending.record.id }
                if (committedRecord != null) eff = committedRecord.toEffects()
                finalGemBalance = repo.progress.first().gems
                if (discardFinishedRecord) repo.clearFinishedGame()

                finishWriteInFlight = false''',
    'recover committed effects after ambiguous io',
)
vm_path.write_text(vm)

test_path = Path('app/src/test/java/com/steamforge/game/ui/game/FinishPersistenceRetryTest.kt')
test = test_path.read_text()
test = once(
    test,
    '''        var remainingFinishFailures = 1
        var finishAttempts = 0
        val attemptedIds = mutableListOf<String>()''',
    '''        var remainingFinishFailures = 1
        var commitBeforeFailure = false
        var finishAttempts = 0
        val attemptedIds = mutableListOf<String>()''',
    'flaky repo mode',
)
test = once(
    test,
    '''            if (remainingFinishFailures > 0) {
                remainingFinishFailures--
                throw IOException("ENOSPC")
            }
            delegate.applyGameFinish(record, finisher)''',
    '''            if (remainingFinishFailures > 0) {
                remainingFinishFailures--
                if (commitBeforeFailure) delegate.applyGameFinish(record, finisher)
                throw IOException("ENOSPC")
            }
            delegate.applyGameFinish(record, finisher)''',
    'ambiguous commit failure',
)
insert_before = '''    private fun finishingSavedGame(seed: Long = 17L): SavedGame {'''
if test.count(insert_before) != 1:
    raise SystemExit('fixture marker mismatch')
new_test = '''    @Test
    fun `ambiguous io after durable commit retries idempotently`() = runTest(dispatcher) {
        val initial = finishingSavedGame()
        val repo = FlakyFinishRepo(FakeDataRepo(initialGame = initial)).apply { commitBeforeFailure = true }
        val analytics = RecordingAnalytics()
        val model = GameViewModel(
            repo = repo,
            analytics = analytics,
            seedProvider = { 17L },
            savedGameProvider = { initial },
        )
        advanceUntilIdle()

        model.onMove(Move.LEFT)
        advanceUntilIdle()

        assertTrue(model.ui.value.finishPersistenceFailed)
        assertFalse(model.ui.value.finished)
        assertEquals(1, repo.currentProgress.stats.gamesPlayed)
        val durable = requireNotNull(repo.currentFinished)
        assertEquals(1, repo.finishAttempts)

        model.retryFinishPersistence()
        advanceUntilIdle()

        assertTrue(model.ui.value.finished)
        assertFalse(model.ui.value.finishPersistenceFailed)
        assertEquals(2, repo.finishAttempts)
        assertEquals(1, repo.attemptedIds.toSet().size)
        assertEquals(durable.id, repo.currentFinished?.id)
        assertEquals(1, repo.currentProgress.stats.gamesPlayed)
        assertEquals(durable.xpGained, model.ui.value.effects?.xpGained)
        assertEquals(durable.gemsGained, model.ui.value.effects?.gemsGained)
        assertEquals(1, analytics.names.count { it == "game_finished" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_failed" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_retry" })
        assertEquals(1, analytics.names.count { it == "game_finish_save_recovered" })
    }

'''
test = test.replace(insert_before, new_test + insert_before, 1)
test_path.write_text(test)
