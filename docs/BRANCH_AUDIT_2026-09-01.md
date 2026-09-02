# Steamforge — Branch Audit 2026-09-01

Аудит выполнен перед консолидацией основной ветки. Принцип: **переносить полезное актуальное изменение, а не механически вливать всю историю ветки**.

| Ветка | Состояние относительно `master` | Решение |
|---|---|---|
| `master` | основная ветка | source of truth |
| `feat/steampunk-ui` | полностью содержится в `master` | не вливать повторно; historical |
| `fix/release-hardening` | полностью содержится в `master` | не вливать повторно; historical |
| `fix/2048-gameplay-feel` | stale/diverged; актуальные gameplay/icon fixes уже представлены в `master` | не вливать raw branch |
| `fix/launcher-icon-header-spacing` | superseded текущим `master` | не вливать raw branch |
| `ci/ui-emulator-smoke` | сильно отстал; содержит старый workflow и закоммиченные screenshot/XML артефакты | не вливать |
| `visual/premium-elements-audio` | ahead, но добавляет неподключённый legacy Android `View` + drawable assets | **не вливать as-is**; текущий gameplay — Compose/Canvas, а новый visual direction задаёт `VISUAL_BIBLE.md` |
| `chore/android-release-hardening-2026` | актуальный Android hardening | взят целиком как база consolidation |
| `fix/game-state-consistency` | полезные game-state fixes + sibling/older Android workflow | выборочно перенесены game code/tests/audit; workflow не переносился, чтобы не откатить hardening |

## Что вошло в consolidation

Из Android hardening:

- Android 17 / 16 KiB smoke workflow;
- более устойчивый emulator boot/wait;
- UI smoke hardening;
- manifest/navigation/accessibility/runtime compatibility tweaks;
- 16 KiB verification tooling.

Из game-state consistency:

- `GameSaveCodec` v4;
- сохранение session statistics через process death;
- корректировки `GameViewModel`;
- regression/unit tests для save/state consistency;
- `GAME_LOGIC_AUDIT_2026.md`.

## Что сознательно не вошло

- закоммиченные CI screenshots/XML dumps из старой CI-ветки;
- orphan `PremiumTileFace` legacy View и неиспользуемые element drawables;
- старые варианты workflow, если более новая hardening-версия уже существует;
- повторные коммиты веток, которые уже являются предками `master`.

## После merge

Старые ветки можно оставить как историю до следующей repository cleanup. Их наличие не означает, что они содержат незамёрженный production-код.
