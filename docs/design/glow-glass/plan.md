# Glow × Glass: build plan

Written 2026-11-13. The design is the Glow × Glass canvas, https://claude.ai/artifact/AqjcQrgM6ZEiAmoybqvN4f (71 boards in six sections). This plan moves the app from the v2 look to that canvas. It adds no features and changes no algorithms. Where a board and this plan disagree, the board wins, and the gap goes into "Open design calls" below.

## 1. What changes

| Area | Now (v2, develop 3.0.7) | Glow × Glass |
|---|---|---|
| Colour | One ink per metric (`MetricColors` in `ui/theme/Theme.kt`) | A two-stop blend per metric, plus three place blends (Strap, You, Goals) |
| Screen light | Card glows and edge light in the metric ink | The hero's blend, muted (60% saturation, 40% lightness), spilled behind the screen from where the hero's light comes from, at the mood's strength |
| Corners | `RoundedCornerShape`, 91 uses in 24 files, card corner 28 dp | Squircles from `com.stoyanvuchev:squircle-shape` 5.4.0, smoothing 35 |
| Materials | OLED black cards, glass only on the nav bar and popups | Four card materials (clear glass, tinted glass, solid light, dark glow) plus heavy glass for sheets |
| Numbers | Dotted numbers drawn by `ui/glow/draw/DotSpec.kt` (own 5×7 font) | Doto 700, four sizes by dot, groups split by a gap, not a comma |
| Type | Funnel Display and Funnel Sans | Same families. Titles in Display 600, body and labels in Sans 400 |
| Ground | #000000 | #030304 |

### Blends (Palette board)

Each entry is first stop → second stop, then the v2 ink it replaces.

| Blend | Stops | Replaces |
|---|---|---|
| Recovery | #7CF2C9 → #2FC79A | `readiness` #7CF2C9 |
| HRV | #2DD4BF → #1B8FA8 | `hrv` |
| Blood oxygen | #38BDF8 → #5AD1FF | `spo2` |
| Breathing rate | #E6ECF5 → #C9B8FF | shared Sleep light #8FA8FF (now its own) |
| Sleep | #4C6FFF → #2E3FC9 | `sleepDeep` as the metric light (stage colours stay flat) |
| Load | #A78BFA → #796AFF | `load` |
| Evening (moment) | #A274F2 → #5B2A86 | `evening` |
| Habit Age | #D86BEA → #6FE3F0 | `habitAge` |
| Stress | #F062C0 → #FF9AD5 | `stress` |
| Heart rate | #FF5A5F → #D9233F | `heart` |
| Energy | #FF7A2E → #B05CFF | `charge` #F66C28 |
| Skin temperature | #6FC3FF → #FF8A3D | `temperature` |
| Calories | #FFD23F → #F2A93B | `calories` (the shade stays for walking bars) |
| Steps | #C3F53C → #EEF04A | `steps` |
| Strap (place) | #AEB6C2 → #4A5262 | `StrapLight` #7DE0C0 in `ui/glow/StrapDrawing.kt`, also on setup |
| You (place) | #F0B48C → #A8613F | the grey on You pages |
| Goals (place) | #9BFFB0 → #7C6BFF | `good` #3DDC97 wherever it lights goals and rewards |
| Indoor cardio (family) | #C04FE0 → #6B3FD8 | zone blue and cyan |
| Strength (family) | #B98A4E → #5A4636 | zone amber |
| Intervals (family) | #FF44B0 → #0E8A87 | work red, rest teal |
| Games (family) | #4CB85C → #2F6B3F | sky into mint |

Rules kept and added:

- A blend says which metric, never how it is going. Values and dotted numbers stay white.
- Green #3DDC97 and amber #FFB547 stay, for the strap and sync status dots only. `good` keeps its name for that job and stops lighting goals.
- Strap, You and Goals are places, not metrics. They light their own pages and never a chart.
- A chart line or ring draws the blend along its length, and the second stop lands on the answer (now, the top, the value). A row icon uses the first stop.
- Chart marks on dark glass use a lighter version of each blend, with lightness raised until 4.5:1 (Charts board). The deep stop stays for the screen light.
- Categories (sleep stages, heart zones) keep their flat colours.
- Copy never uses em dash, semicolon or middle dot. Never two cards in a row with the same look.

### Corners

The library's API, from its source (`sv.lib.squircleshape`):

- `SquircleShape(percent: Int, smoothing: Int)`: percent runs 0 to 50 of the shortest side. Anything above 50 is clamped to a pill.
- `SquircleShape(radius: Dp, smoothing: Int)`: fixed radius, clamped to half the shortest side.
- Per-corner versions take Int, Dp or Float. `CornerSmoothing` presets are None 0, Small 20, Medium 48, High 67, Full 100.

Smoothing is 50 everywhere. Radii are fixed dp (decided 2026-11-13), not percent: a percent radius grows with the card, so a Small card and a tall hero in one column would have different corners. Built once in `ui/theme/Shapes.kt`:

| Surface | Radius |
|---|---|
| Cards, panels, strips | 48 dp |
| Sheets (top corners) | 48 dp |
| Tiles | 16 dp |
| Buttons, chips, tags, the tab pill | pill: `SquircleShape(percent = 50, smoothing = 50)`. The boards draw every text button this way (44, 52 and 56 dp tall at half-height radius). `percent = 100`, as in the library's sample, gives the same shape because it clamps at 50 |
| Square icon buttons, inputs | 14 dp |
| Small inner parts (bars, swatches) | 4 dp, plain rounded |

Shapes are `val`s, never built during composition. They also go into `MaterialTheme(shapes = …)` (small 14 dp, medium 16 dp, large 48 dp, extraLarge pill), so Material controls such as switches, menus and text fields pick them up too. Widgets (Glance) can't take a Compose shape, so they keep rounded corners from the launcher (section 4E).

### Materials (System and Elements boards)

| Material | Use | Build |
|---|---|---|
| Clear glass | Every card except the hero: lists, strips, picture cards | Haze blur of the screen light behind, white 7% fill, lit top edge white 14% |
| Tinted glass | The hero | The blend's first stop at low alpha over the blur |
| Solid light | Where the colour is the value (the Energy level card) | Solid blend fill, fading to dark behind the number. At most two per screen |
| Dark glow | Strips and moments that rise from below | The current GlowStyle core, lit from below |
| Heavy glass | Sheets and popups | `GlassPanel` with blur 28 dp and saturation 170% |

Blur cost: one `HazeState` source per screen (the screen light layer), with every glass card a child of it. Cards never blur one another. Android 11 (minSdk 30) has no RenderEffect blur, so glass falls back to the tinted fill, as `GlassPanelAlpha` does today.

## 2. Screens by section

| Canvas section | Boards | Code |
|---|---|---|
| 1 Foundations | Palette, Matrix, Elements, System, Charts, Numbers | `ui/theme/*`, `ui/glow/draw/*`, `ui/components/Glass.kt`, `ui/glow/GlowLayout.kt`, `GlowType.kt`, `GlowCharts.kt`, `pictures/*` |
| 2 Today, through the day | Morning, Midday, Workout time, During, After, Evening, Night | `GlowTodayScreen.kt`, `TodayCards.kt`, `dashboard/TodayLooks.kt`, `TodayPlan.kt`, `MomentHeader.kt`, `root/TodayMoment.kt` |
| 3 Tabs, details and sheets | Recovery, Sleep, Activity tabs, seven details, All metrics, Edit Today, Info sheets | `GlowRecoveryScreen.kt`, `GlowSleepScreen.kt`, `GlowActivityScreen.kt`, `ScoreDetail.kt`, `SleepDetail.kt`, `GlowMetricDetail.kt`, `ActivityDetail.kt`, `GlowHealthspan*.kt`, `dashboard/WeekStory.kt`, `AllMetricsScreen.kt`, `EditTodaySheet.kt`, `InfoSheet.kt` |
| 4 Workouts | Choose, Ready, Zones, Live (7), Summary (6), Celebration, Rewards, Share | `ChooseScreen.kt`, `Ready*.kt`, `GlowWorkoutMode.kt`, `live/*`, `GlowWorkoutSummary.kt`, `SummarySections.kt`, `Celebrations.kt`, `Medals.kt`, `Badge*.kt`, `GoalsAndBests.kt`, `ShareCards.kt`, `ShareScreen.kt`, `WorkoutCapsule.kt`, `HeartZonesScreen.kt` |
| 5 You and the strap | 8 You pages, 6 Strap pages | `ui/device/*`, `ui/alarms/*`, `ui/firmware/*`, `ui/zepp/*`, `StrapDrawing.kt`, `root/StrapHeader.kt`, `dashboard/StrapPill.kt` |
| 6 Setup, states, widgets, notifications | Sign in, Pair, Permissions, Lock screen, States, Widgets, Notification | `root/PermissionGates.kt`, `root/LockScreen.kt`, `device/SetupStep.kt`, `dashboard/DataStates.kt`, `glow/DataStateParts.kt`, `dashboard/SyncStates.kt`, `widget/*`, `device/*Notifier.kt`, `presentation/NotificationCopy.kt` |

## 3. Branch and rules

- `design/glow-glass` is cut from the latest `origin/develop` (3.0.7, d3151480), not main. Wave branches merge back into it, and it goes to `develop` by PR when the user says so.
- Wave tasks run in worktrees on `design/gg-w<wave>-<task>` branches, each owning its files exclusively (section 4).
- Every Gradle build goes through `.claude/tools/gradle-queue.sh`, one at a time on this machine.
- Agents write code and tests only. They never run adb. The coordinator merges a wave into `design/glow-glass`, then runs unit tests, lint and detekt once.
- The main thread does every phone and emulator check: build, install with `-PphoneApp`, and screenshot each screen next to its board. The emulator with the Fold database copy covers moments and states the phone isn't in.
- Each agent keeps a progress file in the scratchpad (Root cause, Decisions, Done, Next, Open questions).
- Nothing is pushed and no PR opens without asking.

## 4. Waves

### Wave 0: foundations (coordinator, alone)

Everything else builds on this, so it runs first and alone.

1. Add `squircle-shape` 5.4.0 to `libs.versions.toml` and the app, and add the Doto font (OFL) to `res/font` and `licenses/`.
2. `ui/theme/Blends.kt`: a `Blend(first, second, chart)` type and all 17 blends, with each chart colour computed to 4.5:1 on dark glass and checked by a unit test. `MetricColors` stays as a deprecated bridge to the first stops until wave 3.
3. `ui/theme/Shapes.kt`: the shapes in section 1, and `GlowLayout.CardShape`, `SheetShape` and `TileShape` pointed at them. That changes most cards at once.
4. `ui/theme/Materials.kt`: the five materials as modifiers, and a `ScreenLight` layer that takes the hero's blend, mood and light direction (Matrix board) and is the Haze source.
5. Ground #030304 in the colour scheme. Doto in `GlowType`, with `DotSpec`'s grouping (gap, no comma) and its four sizes.
6. Build Today morning on the new parts, as the proof screen.

**Gate 0 (user):** Today morning on the phone next to its board. Check the 28 dp squircle corners at smoothing 50 on the real screen, and decide whether Doto replaces `DotSpec` everywhere, widgets included (widgets draw on a Canvas, so they need the Doto typeface there too).

### Wave 1: shared parts (parallel, 4 agents)

| Task | Owns |
|---|---|
| 1A Cards | `GlowCard.kt`, `PictureCard.kt`, `CardLight.kt`, `CardLooks.kt`, `draw/LightSpec.kt`, `draw/EdgeSpec.kt`, `draw/PictureRim.kt`, `MovingBorder.kt` |
| 1B Charts | `GlowCharts.kt`, `draw/ChartSpec.kt`, `pictures/*`, `DetailPicture.kt`, `TrimBar.kt` (blend along length, second stop on the answer, ghost and band rules) |
| 1C Lists, strips, controls | `GlowList.kt`, `TabParts.kt`, `PageParts.kt`, `NewsPill.kt`, `GlowParts.kt`, `GlowShared.kt`, `root/TabsPager.kt` tab pill |
| 1D Glass and sheets | `components/Glass.kt` (heavy glass, sheet shape), `InfoSheet.kt`, `EditTodaySheet.kt`, `AllMetricsScreen.kt` |

Then the merge, one test run, and the main thread checks the Elements and Charts boards against the Live parts gallery (debug build).

### Wave 2: screens (parallel, 5 agents)

| Task | Owns | Boards |
|---|---|---|
| 2A Today | section 2 row 2 files | the 7 Today boards |
| 2B Tabs and details | section 2 row 3 files | 13 boards |
| 2C Workouts | section 2 row 4 files | 22 boards |
| 2D You and Strap | section 2 row 5 files | 14 boards (graphite and copper lights) |
| 2E Setup, states, widgets, notifications | section 2 row 6 files | 7 boards (setup in graphite) |

Each agent works through its boards one at a time, matching the copy as drawn (the copy was already aligned: HRV 44 Below normal, the run 17:20 to 17:58, 37:42). 2E keeps widgets on system sans and launcher corners (widgets spec), and only changes their colours and dotted numbers.

**Gate 2 (user):** every section on the phone, side by side with the canvas.

### Wave 3: clean-up (coordinator)

- Remove `MetricColors`' single inks, `StrapLight`, and every use of `good` that lights a goal. A lint check or test fails on any `Color(0x…)` literal in `ui/` outside `ui/theme`.
- Remove `RoundedCornerShape` from cards (20 files still use the old colours, 24 use rounded shapes).
- Baseline profile update (`baseline-prof.txt` names RoundedCornerShape), a frame-time check on Today scroll with glass on a mid-range phone, a TalkBack pass, and a font-scale 1.3 pass.
- `docs/design/v2/spec.md` sections 2.x are rewritten to point at the canvas, and the release notes are written.

## 5. Design calls (decided 2026-11-13)

All settled by the user. Where a call changes a board, the canvas is fixed in the same pass as the code, so board and app stay one.

1. **Row chevrons:** raised to pass 4.5:1, one alpha per material (45 to 67%), not decoration.
2. **Activity tab steps:** the tab reads after the run, 14,820 steps, with copy to match, as Today After does.
3. **Share card:** tells the same run, Sat 7 Nov evening, 6.42 km, 37:42.
4. **Load's second stop:** #796AFF, as the boards draw it. The Palette changes.
5. **Heart zones:** categories, like sleep stages, so they keep their flat colours, zone green and amber included. They're drawn small (a bar or chip), never as a large area. That applies to Today During and the workout notification.
6. **Widget status:** an amber dot with the words in white or grey, as in the app. No amber text.
7. **Rewards Goals fill:** fades out softly just past 3 of 4, like the Energy level card, instead of a hard stop.
8. **"Something new" badges:** white glass chips at 12 to 14%, no lavender, so nothing competes with Aurora.
9. **Strap page hero:** a small green status dot beside "Connected", amber when it isn't, matching the chip.
10. **Today During "Goal met":** in the hero only. The card below shows what's next instead (time left in zone, or distance).
11. **Sleep Deep stage:** the flat Deep colour is lightened just enough to reach 3:1 wherever it's a small mark. The bars stay the same.
12. **Today After "78":** the hero glow behind it is dimmed until 4.5:1, as on Waking, Recovery and Activity.
13. **Info sheet HRV range:** the board's 46 to 58 ms is only an example. The app shows the user's own range, as it does now.

The boards were updated to match on 2026-11-13 (canvas version 44). That pass turned up five more points, settled by the user the same night (canvas version 45):

14. **Workout families have their own blends.** The family's blend lights Ready, live, summary, share and the notification, and zones never light a screen. Four families had zone colours as their light, so they got new blends, each at least 0.10 OKLab from every other blend, zone and status colour: Indoor cardio #C04FE0 → #6B3FD8 (violet into indigo), Strength #B98A4E → #5A4636 (bronze into iron), Intervals #FF44B0 → #0E8A87 (flare pink for work, deep teal for rest), Games #4CB85C → #2F6B3F (turf into pitch). Distance keeps Steps' blend and Mind and body the Evening's.
15. **Share cards:** a heavier, darker glass block (dark gradient 0.46 to 0.74, blur 44) under the text on every lit share card, so the bright light stays and the text passes 4.5:1.
16. **Hero titles and notes on bright light:** a soft dark scrim behind them (hero title and chevron on Elements, Charts and States, the note on the solid Energy card, the Goals and Weekly recap heroes, the widget notes).
17. **Older contrast failures fixed:** DetailRecovery's 77 moved clear of the arc, DetailHabitAge and DetailMetric labels raised to 0.7 to 0.82 white, and similar raises on PageHeartZones, PageBackup, PageProfile and DetailWorkout.
18. **States is dated Sat 7 Nov**, like the rest of the canvas.

### Decided by the coordinator (2026-11-13 night, the user asleep, all recommended options)

The user asked for the waves to run through without them, picking the recommended option at each gate and reporting it.

- **Gate 0, numbers:** our own dot font (`DotSpec`) stays instead of Doto. It already draws round 5 by 7 dots like Doto, the widgets share its geometry (Canvas, no font needed there), and it can't drift between app and widgets. It takes the Numbers board's rules: a gap of half a digit before each group of three from five digits up, four digits with no gap, and grid cards at 3 dp (the Second size moves from 3.6 to 3).
- **Gate 0, corners:** 28 dp squircles at smoothing 50 at first. On 13 Nov, tried with the debug Corners panel on the Fold, cards and sheets went to 48 dp and every squircle to smoothing 35, because 28 at 50 looked too square. Tiles stay 16 dp and icon buttons 14. Widgets keep the launcher's corner.
- **Cards don't blur.** The boards' cards are a see-through fill with a lit edge over an already soft light, so only the nav bar, sheets and popups blur. That removes the blur-cost risk and makes Android 11 look the same.
- **The screen's light goes with the hero** (changed on the phone, 13 Nov, from fixed to the screen): it comes from behind the hero, moves up as the list scrolls and fades by 60% over 600 dp of scroll. Pages whose list isn't a `ScreenList` keep it fixed.
- **Gate 2** is the coordinator's emulator pass against every board, with the user's phone check left for the morning.

## 6. Risks

- **Blur cost.** Many glass cards on a long scroll. One Haze source per screen. Wave 3 measures frame time and drops to tinted glass if it janks.
- **Android 11.** No blur, so glass has to look right as a flat tint too. Check on the emulator at API 30.
- **Squircle borders.** Lit top edges and blend rims must follow the squircle path (`Modifier.border(shape = …)` with the same shape, or `drawSquircle` on Canvas), never a rounded rect drawn over it.
- **Doto metrics.** Its digits are wider than the 5×7 `DotSpec` font, so Small cards (173 dp) need a fit check at the largest dot size.
- **Merge drift.** develop moves on for protocol and fix work. Merge the latest develop into `design/glow-glass` once per wave, before the wave's branches are cut, not per task.
