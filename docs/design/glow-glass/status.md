# Glow × Glass: status, 2026-11-13 morning

All the waves ran overnight while you were away. The branch is `design/glow-glass` in the `openstrap-glowglass/` worktree, cut from develop 3.0.7. Everything is committed locally. Nothing was pushed and no PR was opened.

## Where it stands

- **Checks:** all 2,098 unit tests pass, along with `detektDebug`, `lintDebug` and the debug build. The final run was one queue call.
- **Emulator:** every section was checked against its board on the API 36 emulator, using the phone's database copy from 13 Nov. `emulator/contact-sheet.png` has 21 screens. Nothing was checked on the Fold.
- **Canvas:** version 45 adds the family blends, the share glass block, the scrims, the contrast fixes and Sat 7 Nov on States (plan section 5, decisions 14 to 18).

## Waves

| Wave | What landed |
|---|---|
| 0 | squircle-shape 5.4.0. `Blends.kt` has 17 blends, the 4 new family blends, chart tones and muted screen lights, checked by a test. `Shapes.kt` has squircles at smoothing 50 (cards 28, tiles 16, icon buttons 14, pills). `Materials.kt` has clear glass, tinted glass, solid light, dark glow, the lit edge and the screen light. The ground is #030304. Dotted numbers group with a gap. |
| 1 | Cards draw the materials. Charts draw blends along their length. Lists, rows, buttons, chips, switches and the battery chip are restyled. The tab bar is heavy glass, with the selected tab tinted by the screen's light. Sheets are heavy glass, and pages and details can set a light (`PageLight`). |
| 2 | Today: each moment has its light and hero. Tabs and details. Workouts: Choose, Ready and live in the family blends, with zones drawn small. Summaries, rewards and share. You and Strap in copper and graphite. Widgets and the workout notification. |
| 2, polish | A second pass from emulator screenshots: the Activity hero, Heart points, workout rows, the Recovery Energy number, the Strap tiles, tinted rows, Alarms, Profile, the Zepp sign-in, no amber text anywhere, the summary hero, glass secondary buttons. |
| 3 | v2 design-rule tests rewritten to the new rules (each cites its board). New detekt and lint findings fixed. zh strings for the new live strings, and unused strings removed. |

## Decisions made without you (recommended options)

1. **The dot font stays (`DotSpec`), not Doto.** It already draws round 5 by 7 dots, the widgets share its geometry, and it can't drift between app and widgets. It follows the Numbers board: grid cards at 3 dp, four digits with no gap, a half-digit gap from five digits up.
2. **Cards don't blur.** The boards' cards are a see-through fill with a lit edge over an already soft light. Only the nav bar, sheets and popups blur. That removes the blur-cost risk, and Android 11 looks the same.
3. **The screen light goes with the hero** (changed on 13 Nov, you overruled fixing it to the screen): it comes from behind the hero, moves up as the list scrolls and fades by 60% over 600 dp. Pages that don't scroll in a `ScreenList` keep it fixed.
4. **Sheets use the sheet boards' values** (blur 40, saturation 150%, fill 78%), not System's 28 and 170%. Four boards draw them that way.
5. **The workout detail page is lit by the family blend**, not the zone colours the old board shows (decision 14: zones never light a screen).
6. **The Energy level fill keeps its hard edge** at the level, as TodayMidday draws it. Only Rewards Goals fades (decision 7).
7. **The live zone line now takes the row's free width.** Two weights had cut "Zone 2, easy" to "Zone 2,".

## Known gaps

- **Today's hero light** switches between moments without the 300 ms crossfade. Animating it would recompose the whole screen every frame.
- **Not built:** there's no Calories card on the Activity tab, since its slots come from `activityCards` and the board's Calories card isn't a slot yet.
- **Can't be restyled:** the LockScreen board's PIN keypad is Android's own keyguard.
- **Not seen on the emulator:**
  - Setup screens: they need an unpaired strap.
  - The permission sheet.
  - Live strength, intervals, games and mind screens.
  - Celebrations.
  - Widgets: the gallery activity is commented out, see CLAUDE.md.
  - The run summary was checked with a 95 s simulated workout. The phone's database copy has no workouts, so past workout pages showed empty.
- **Plan items not done:** the frame-time check on a mid-range phone, a TalkBack pass, a font-scale 1.3 pass, the baseline profile refresh (it still names removed classes), and the `MetricColors` removal. `MetricColors` stays as a bridge through `Blends.forInk`.
- **Commit trailers:** some agent commits carry a Sonnet Co-Authored-By line.

## To try it

```
cd openstrap-glowglass
/Users/sayedameer/projects/software/zepp-alt/openstrap/.claude/tools/gradle-queue.sh :app:assembleDebug -PphoneApp -PappVersion=3.0.8
adb install -r --user 0 app/build/outputs/apk/debug/app-debug.apk
```

Back up the phone's database first, as CLAUDE.md says.
