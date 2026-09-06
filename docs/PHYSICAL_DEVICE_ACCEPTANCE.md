# Steamforge 1.0 — Physical Device Release Acceptance

**Статус:** шаблон ручного production gate.  
**Важно:** наличие этого файла не означает, что проверки выполнены. Релиз допускается только после заполнения фактическими результатами для **точного подписанного APK**, созданного `tools/build-rustore-release.sh`.

## 1. Release artifact identity

Перед установкой на устройство:

```bash
bash tools/verify-rustore-release-artifact.sh dist/Steamforge-<version>-vc<code>-rustore.apk
```

Перенесите значения из verifier/preflight в локальную копию этого record:

- Source commit: `[FILL]`
- APK file: `[FILL]`
- APK SHA-256: `[FILL]`
- Signing certificate SHA-256: `[FILL]`
- Package: `[FILL]`
- Version: `[FILL]`
- Inventory report: `[FILL]`
- Test date/time: `[FILL]`
- Tester: `[FILL]`

**Правило:** после начала device acceptance APK не пересобирать и не заменять. Если SHA-256 меняется, весь acceptance относится к старому artifact и должен быть повторён для нового.

## 2. Device matrix

Заполняйте только реально использованные устройства.

| ID | Manufacturer / model | Android / API | ABI | Screen / density | Build fingerprint or build ID | Role |
|---|---|---|---|---|---|---|
| D1 | `[FILL]` | `[FILL]` | `[FILL]` | `[FILL]` | `[FILL]` | core/lifecycle |
| D2 | `[FILL if used]` | `[FILL]` | `[FILL]` | `[FILL]` | `[FILL]` | performance/accessibility |
| D3 | `[FILL if used]` | `[FILL]` | `[FILL]` | `[FILL]` | `[FILL]` | additional low/mid/high coverage |

Не записывайте в публичный record серийный номер устройства, Android ID или другие устойчивые персональные идентификаторы.

## 3. Core / persistence / offline smoke

Для каждого пункта укажите `PASS`, `FAIL` или `N/A` с пояснением. `N/A` допустим только когда проверка действительно неприменима и причина записана.

| Check | Device | Result | Evidence / notes |
|---|---|---|---|
| Clean install opens Home directly, без ad/analytics consent gate | `[FILL]` | `[FILL]` | `[FILL]` |
| New normal run starts and accepts swipe input | `[FILL]` | `[FILL]` | `[FILL]` |
| Active run survives Home/background/resume | `[FILL]` | `[FILL]` | `[FILL]` |
| Active run survives screen-off/wake | `[FILL]` | `[FILL]` | `[FILL]` |
| Active run restores after process kill / force-stop + relaunch | `[FILL]` | `[FILL]` | `[FILL]` |
| Offline startup works | `[FILL]` | `[FILL]` | `[FILL]` |
| Offline gameplay + autosave + relaunch restore works | `[FILL]` | `[FILL]` | `[FILL]` |
| Game Over result persists; retry/relaunch does not duplicate reward/result | `[FILL]` | `[FILL]` | `[FILL]` |
| Play Again starts a clean next run | `[FILL]` | `[FILL]` | `[FILL]` |
| Daily / Contracts / Workshop / Collection open and basic actions work | `[FILL]` | `[FILL]` | `[FILL]` |
| Settings controls work | `[FILL]` | `[FILL]` | `[FILL]` |
| Reset progress behaves as documented | `[FILL]` | `[FILL]` | `[FILL]` |
| No AppMetrica/ad/consent/rewarded-video UI appears | `[FILL]` | `[FILL]` | `[FILL]` |
| With blank Remote Config endpoint, no unexpected product network dependency blocks startup/gameplay | `[FILL]` | `[FILL]` | `[FILL]` |

## 4. Accessibility / layout spot-check

| Check | Device | Result | Evidence / notes |
|---|---|---|---|
| TalkBack can reach and identify primary Home actions | `[FILL]` | `[FILL]` | `[FILL]` |
| TalkBack can reach gameplay controls without an unusable focus trap | `[FILL]` | `[FILL]` | `[FILL]` |
| Board/tile semantics remain understandable with TalkBack | `[FILL]` | `[FILL]` | `[FILL]` |
| Large text does not hide critical navigation/actions | `[FILL]` | `[FILL]` | `[FILL]` |
| System bars/cutout/safe-area do not obscure critical UI | `[FILL]` | `[FILL]` | `[FILL]` |
| Portrait and supported landscape/expanded layout remain usable | `[FILL]` | `[FILL]` | `[FILL]` |

## 5. Performance / thermal acceptance

Hosted CI diagnostics are not a substitute for these measurements.

| Check | Device | Result | Measurement / notes |
|---|---|---|---|
| Release-like gameplay frame timing recorded | `[FILL]` | `[FILL]` | `[FILL metrics]` |
| Input/merge response has no reproducible long stalls | `[FILL]` | `[FILL]` | `[FILL]` |
| 30–60 minute gameplay session completed | `[FILL]` | `[FILL]` | `[FILL duration]` |
| No unacceptable sustained thermal throttling / device heating for this game class | `[FILL]` | `[FILL]` | `[FILL]` |
| Battery drain observed and judged acceptable for the tested session/device | `[FILL]` | `[FILL]` | `[FILL]` |
| Menus/background do not retain obvious heavy animation workload | `[FILL]` | `[FILL]` | `[FILL]` |

Если release plan требует low/mid/high device coverage, перечислите фактические устройства и измерения выше; не заменяйте отсутствующий класс устройства emulator-result'ом.

## 6. Failures / deviations

- Open defects found: `[FILL or NONE]`
- Accepted deviations: `[FILL or NONE]`
- Retest required: `[YES/NO]`
- Retest artifact SHA-256 (если другой): `[FILL if applicable]`

Любой crash, state loss/duplication, broken reset, критическая accessibility/layout проблема или воспроизводимый performance blocker должен быть исправлен и перепроверен до release acceptance.

## 7. Exact-artifact re-verification after device smoke

После завершения тестов, перед загрузкой:

```bash
bash tools/verify-rustore-release-artifact.sh dist/Steamforge-<version>-vc<code>-rustore.apk
```

- Post-smoke verifier: `[PASS/FAIL]`
- Recomputed APK SHA-256: `[FILL]`
- Matches section 1 SHA-256: `[YES/NO]`

## 8. Manual release decision

Отдельно должны быть закрыты signing-key backups и фактическая privacy/store disclosure; этот record их не заменяет.

- Core/lifecycle/offline: `[PASS/FAIL]`
- Accessibility/layout: `[PASS/FAIL]`
- Performance/thermal: `[PASS/FAIL]`
- Exact artifact unchanged: `[PASS/FAIL]`
- Signing key + independent backups verified: `[PASS/FAIL]`
- Privacy/store disclosure published and matches production network configuration: `[PASS/FAIL]`
- **Final release decision:** `[ACCEPT / REJECT]`
- Decision date: `[FILL]`
- Decision owner: `[FILL]`
