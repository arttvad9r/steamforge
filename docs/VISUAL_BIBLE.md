# Steamforge — Visual Bible / Design Brief

> **Status: APPROVED PRIMARY ART DIRECTION — 01.09.2026.**
>
> Этот документ имеет приоритет над ранее сгенерированными concept screens. Последняя серия экранов в HEXSTORM-like направлении сохраняется как **art-direction reference**, а не как pixel-perfect production spec. Если reference image конфликтует с правилами ниже — побеждает Visual Bible.

## 1. Основная формула

Steamforge — это **premium stylized industrial steampunk 2048**:

```text
clean puzzle UI
+
dark industrial-steampunk atmosphere
+
premium materials and lighting
+
painted/cinematic backgrounds
+
restrained ornament
```

Стиль должен быть атмосферным, взрослым, цельным и дорогим, но прежде всего удобным для длительной игры в 2048.

## 2. Что берём из HEXSTORM-подхода

`HEXSTORM: Tears of Arcadia` используется только как ориентир по:

- уровню полировки;
- мягкой painterly stylization;
- атмосферному свету;
- сочетанию тёмной среды с тёплыми металлическими акцентами;
- ощущению цельного иллюстративного мира.

Не копируем:

- конкретные композиции;
- официальный UI;
- логотипы/символы;
- ассеты;
- конкретные машины, корабли и иллюстрации.

Steamforge сохраняет собственный puzzle-first identity.

## 3. Главный принцип — restrained steampunk

Steampunk находится в **материале, свете, окружении и отдельных акцентах**, а не в количестве деталей.

Не перегружать интерфейс:

- шестерёнками на каждом блоке;
- рамками внутри рамок;
- болтами в каждом углу;
- трубками вокруг каждого числа;
- повторяющимися массивными plaques;
- сильным glow на всех элементах.

Premium feel создают material response, bevel, controlled highlights, depth, typography и motion.

## 4. Разная плотность разных экранов

### Showcase — можно богаче

- Home / Main Menu;
- Event;
- End-of-run / Reward Reveal.

### Meta — средняя насыщенность

- Workshop;
- Blueprints;
- Contracts;
- Profile;
- Shop.

### Gameplay / Utility — самые чистые

- основной 2048 screen;
- Settings;
- dialogs / confirmations.

Utility screen не должен иметь тот же уровень визуального пафоса, что Event или Home.

## 5. Палитра

### Base — Charcoal / Deep Navy

- `#10161D`
- `#16202A`
- `#1C2731`

### Primary metal — Brass / Warm Gold

- `#A9782E`
- `#C08A3E`
- `#D1A45A`

### Secondary metal — Steel / Gunmetal

- `#4A5560`
- `#5B6773`

### Accent — Muted Teal / Patina

- `#2C7F83`
- `#3B9A9E`
- `#63B7BA`

### Light — Forge Orange / Ember

- `#C76A2A`
- `#E08A3A`

Ориентир по визуальному весу:

```text
60% dark base
25% brass/warm metal
10% teal
5% glow / rare highlight
```

Не превращать постоянный UI в rainbow/RPG palette. Purple не является постоянным системным accent.

## 6. Материалы

Основные:

1. dark steel;
2. polished brass;
3. aged copper;
4. patina / teal metal.

Материал читается через:

- soft specular;
- controlled bevel;
- restrained roughness variation;
- edge highlights;
- лёгкий износ;
- убедительную тень.

Избегать plastic look, глобального noisy grunge и excessive bloom.

## 7. Typography

- Hero/display typography — только для логотипа, крупных score/reward/event моментов.
- Section headers — крупные, но спокойнее brand typography.
- Body/utility text — максимально простой и читаемый.
- Числа на tiles — главный readability element; декоративный serif не должен мешать мгновенному распознаванию.
- Огромный `STEAMFORGE` logo не повторяется на каждом meta/utility screen.

## 8. Gameplay screen — самый строгий экран

Иерархия первого взгляда:

1. board;
2. tiles;
3. numbers;
4. score / best;
5. одна current goal;
6. всё остальное.

### Board

- большой и центрированный;
- простая тёмная металлическая frame;
- без multiple nested borders;
- empty cells темнее и спокойнее active tiles;
- background за board приглушён и менее детализирован.

### Tiles

> **Tile first, steampunk second.**

Плитка:

- крупная;
- простой silhouette;
- очень большое читаемое число;
- soft bevel/material;
- максимум 1–2 decorative accents.

Нельзя делать мини-механизм внутри каждой плитки.

#### Tile progression

- `2–16`: простые metal plates, минимум различий;
- `32–128`: чуть богаче material/edge treatment;
- `256–1024`: дороже metal, controlled teal accent;
- `2048`: special tile, restrained premium glow;
- `4096+`: rare/premium, но не neon magical RPG.

### HUD

Минимум:

- compact logo/title;
- score;
- best;
- settings/back where needed;
- одна compact objective/progress strip.

Gameplay-clean pass обязателен перед production: HUD, board frame и tiles должны быть легче concept art.

## 9. Home

Home может быть самым cinematic:

- сильный hero background/illustration;
- полноценный Steamforge branding;
- одна dominant `Играть` CTA;
- 2–3 secondary entry points;
- compact navigation.

Здесь допустимы более богатые workshop/city/sky background details, если CTA остаётся очевидным.

## 10. Workshop

Workshop — визуальное сердце meta progression.

- машина/устройство — hero object;
- machines могут быть детализированными и painterly;
- UI вокруг них спокойнее самой сцены;
- requirements/progress/upgrade CTA считываются мгновенно;
- развитие должно визуально менять workshop, а не только число level.

## 11. Blueprints

Фирменный language:

- technical blueprint sheet;
- clean engineering linework;
- collected/missing parts;
- clear set progress;
- reward/unlock связан с Workshop.

Главный blueprint — hero; secondary rows/cards заметно проще.

## 12. Contracts

- clear task list;
- teal progress bars;
- compact reward preview;
- Daily и Weekly различаются hierarchy, а не количеством ornament;
- достаточно whitespace между cards.

## 13. Events

Event может быть визуально богаче:

- strong event identity;
- reward track;
- main prize;
- controlled special lighting.

Но milestone track остаётся функциональным и не превращается в decorative noise.

## 14. End-of-run

Приоритет:

1. final score;
2. max tile/new record;
3. earned rewards;
4. next actions.

Допустим short premium celebration, но не loot explosion.

## 15. Shop

Shop — один из главных рисков перегрузки.

- 1–2 hero offers максимум;
- остальные cards спокойнее;
- меньше текста;
- больше whitespace;
- монетизация визуально не должна становиться важнее игры.

## 16. Profile / Settings

Profile — calm meta utility.
Settings — самый спокойный screen системы.

Для Settings:

- thin frames;
- simple rows;
- меньше gold;
- restrained icons;
- functional switches/sliders;
- никакой showcase theatricality.

## 17. Navigation

Основные destinations:

- Игра;
- Мастерская;
- Контракты;
- Чертежи;
- Событие.

Bottom nav:

- compact;
- одинаковая grid;
- simple icons;
- active state = teal/brass highlight;
- не пять тяжёлых латунных plaques.

## 18. Backgrounds

Использовать painterly/cinematic industrial scenes:

- workshop;
- pipes;
- furnaces;
- industrial skyline;
- distant structures;
- steam;
- evening light.

На gameplay background contrast/detail обязательно ниже, чем на Home/Event.

## 19. VFX / Motion

VFX точечный и иерархичный.

Strong effects только для:

- high-tier merge;
- first 2048;
- completed blueprint;
- machine restoration;
- major reward/event milestone.

Motion:

- precise;
- weighted;
- slightly mechanical;
- short;
- never blocks fast play.

Merge = short impact + restrained scale pulse + optional tiny spark/steam + synchronized SFX/haptic.

## 20. DO / DON'T

### DO

- clean premium surfaces;
- large readable numbers;
- painterly atmosphere;
- brass as premium accent;
- teal as controlled secondary accent;
- one visual hero per screen;
- gameplay-first hierarchy.

### DON'T

- ornament everywhere;
- tiny gears on every tile;
- giant header/logo on every screen;
- nested frames everywhere;
- rainbow palette;
- heavy RPG HUD;
- chibi/mobile-cartoon look;
- magic/neon overload.

## 21. Final rule

```text
HEXSTORM-like premium mood
+
Steamforge industrial identity
+
cleaner puzzle readability
+
restrained ornament
+
strong material/light/motion quality
```

Если визуальное решение красиво в статичном concept art, но замедляет считывание 4×4 board или утомляет взгляд за длинную сессию — оно не подходит Steamforge.
