package org.openstrap.app.ui.device

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import org.openstrap.app.ui.glow.LocalPageColumn
import org.openstrap.app.ui.components.PinnedFade
import org.openstrap.app.ui.components.pinnedFade
import org.openstrap.app.ui.dashboard.ScreenList
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import org.openstrap.app.R
import org.openstrap.app.i18n.Strings
import org.openstrap.app.data.settings.Units
import org.openstrap.app.data.settings.UserProfile
import androidx.compose.foundation.layout.height
import org.openstrap.app.ui.glow.GlowLayout
import org.openstrap.app.ui.glow.GlowType
import org.openstrap.app.ui.glow.PageButton
import org.openstrap.app.ui.glow.PageConfirm
import org.openstrap.app.ui.glow.PageNumberField
import org.openstrap.app.ui.glow.PageHeader
import org.openstrap.app.ui.glow.PageNote
import org.openstrap.app.ui.glow.PagePanel
import org.openstrap.app.ui.glow.PageRow
import org.openstrap.app.ui.glow.PageSwitchRow
import java.time.LocalDate
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.openstrap.app.ui.glow.PageRows
import org.openstrap.app.ui.theme.AppIcons
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.clearGlass
import org.openstrap.app.ui.theme.darkGlow

/** One line for the settings row: "34 years, male, 72 kg, 178 cm", or a prompt when empty. */
fun UserProfile.summary(strings: Strings = Strings.app): String =
    if (isEmpty) strings.get(R.string.you_profile_empty)
    else listOfNotNull(
        age()?.let { strings.plural(R.plurals.you_profile_age, it, it) },
        sex?.let {
            strings.get(
                when (it) {
                    UserProfile.Sex.Female -> R.string.you_profile_summary_female
                    UserProfile.Sex.Male -> R.string.you_profile_summary_male
                },
            )
        },
        weightKg?.let { Units.formatWeight(it, usesImperial) },
        heightCm?.let { Units.formatHeight(it, usesImperial) },
    ).joinToString(", ")

/**
 * Age, sex, weight and height. Age sets the maximum heart rate (and so every effort calculation);
 * sex picks the training-load curve. Kept on this phone only. A full page over the You list (or beside it, when
 * wide); back leaves without saving, asking first if anything was changed.
 */
@Composable
fun ProfileScreen(profile: UserProfile, contentPadding: PaddingValues, onSave: (UserProfile) -> Unit, onBack: () -> Unit, onHeartZones: () -> Unit = {}) {
    var age by rememberSaveable { mutableStateOf(profile.age()?.toString().orEmpty()) }
    var sex by rememberSaveable { mutableIntStateOf(when (profile.sex) { UserProfile.Sex.Female -> 0; UserProfile.Sex.Male -> 1; null -> 2 }) }
    var imperial by rememberSaveable { mutableStateOf(profile.usesImperial) }
    // Entry text in the chosen units; converting on a unit switch keeps what the user typed.
    var weight by rememberSaveable { mutableStateOf(profile.weightKg?.let { formatNumber(if (imperial) Units.kgToLb(it) else it) }.orEmpty()) }
    var heightCm by rememberSaveable { mutableStateOf(profile.heightCm?.toString().orEmpty()) }
    val (ft0, in0) = profile.heightCm?.let { Units.cmToFeetInches(it) } ?: (null to null)
    var feet by rememberSaveable { mutableStateOf(ft0?.toString().orEmpty()) }
    var inches by rememberSaveable { mutableStateOf(in0?.toString().orEmpty()) }
    var activity by rememberSaveable { mutableIntStateOf(profile.activityLevel ?: -1) }
    var betaBlocker by rememberSaveable { mutableStateOf(profile.betaBlocker) }
    var pregnant by rememberSaveable { mutableStateOf(profile.pregnant) }
    var sleepNeed by rememberSaveable { mutableStateOf(profile.sleepNeedMinutes?.let { formatNumber(it / 60f) }.orEmpty()) }

    val ageValue = age.trim().toIntOrNull()?.takeIf { it in 10..100 }
    val weightNumber = weight.trim().replace(',', '.').toFloatOrNull()
    val weightValue = weightNumber?.let { if (imperial) Units.lbToKg(it) else it }?.takeIf { it in 25f..300f }
    val heightValue = if (imperial) {
        val f = feet.trim().toIntOrNull()
        val i = inches.trim().ifEmpty { "0" }.toIntOrNull()?.takeIf { it in 0..11 }
        if (f != null && i != null) Units.feetInchesToCm(f, i).takeIf { it in 100..250 } else null
    } else heightCm.trim().toIntOrNull()?.takeIf { it in 100..250 }
    val heightBlank = if (imperial) feet.isBlank() && inches.isBlank() else heightCm.isBlank()
    // Hours, so "7.5" is 450 min; only 7-9 h is accepted (Recovery.SLEEP_NEED_MIN..MAX).
    val sleepNeedValue = sleepNeed.trim().replace(',', '.').toFloatOrNull()?.let { Math.round(it * 60) }
        ?.takeIf { it in org.openstrap.app.data.Recovery.SLEEP_NEED_MIN..org.openstrap.app.data.Recovery.SLEEP_NEED_MAX }
    val valid = (age.isBlank() || ageValue != null) && (weight.isBlank() || weightValue != null) && (heightBlank || heightValue != null) &&
        (sleepNeed.isBlank() || sleepNeedValue != null)

    fun switchUnits(toImperial: Boolean) {
        if (toImperial == imperial) return
        // Carry valid entries across so switching never loses what was typed.
        weightValue?.let { weight = formatNumber(if (toImperial) Units.kgToLb(it) else it) }
        heightValue?.let { cm ->
            heightCm = cm.toString()
            Units.cmToFeetInches(cm).let { (f, i) -> feet = f.toString(); inches = i.toString() }
        }
        imperial = toImperial
    }

    // What the fields held when the page opened, so back knows whether there is anything to lose.
    val initial = rememberSaveable { listOf(age, sex, imperial, weight, heightCm, feet, inches, activity, betaBlocker, pregnant, sleepNeed).joinToString("\u0000") }
    val dirty = listOf(age, sex, imperial, weight, heightCm, feet, inches, activity, betaBlocker, pregnant, sleepNeed).joinToString("\u0000") != initial
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val back: () -> Unit = { if (dirty) confirmDiscard = true else onBack() }
    BackHandler(onBack = back)
    if (confirmDiscard) {
        PageConfirm(
            stringResource(R.string.you_profile_discard_title), stringResource(R.string.you_profile_discard_text),
            confirm = stringResource(R.string.you_profile_discard), onConfirm = { confirmDiscard = false; onBack() },
            onDismiss = { confirmDiscard = false },
            dismiss = stringResource(R.string.you_profile_keep_editing),
        )
    }
    val save: () -> Unit = {
        onSave(
            // A copy, so fields this page doesn't edit are kept.
            profile.copy(
                birthYear = ageValue?.let { LocalDate.now().year - it },
                sex = when (sex) { 0 -> UserProfile.Sex.Female; 1 -> UserProfile.Sex.Male; else -> null },
                weightKg = weightValue,
                heightCm = heightValue,
                imperial = imperial,
                sleepNeedMinutes = sleepNeedValue,
                activityLevel = activity.takeIf { it in 0..4 },
                // Saving confirms the answers, so the fitness estimate's freshness checks restart.
                activityAnsweredAt = if (activity in 0..4) System.currentTimeMillis() / 1000 else null,
                bodyMeasuredAt = if (weightValue != null && heightValue != null) System.currentTimeMillis() / 1000 else null,
                betaBlocker = betaBlocker,
                pregnant = pregnant,
            ),
        )
    }

    // The keyboard shrinks the page rather than covering it, so a focused field scrolls into view above it;
    // while it's up the nav bar is hidden behind it and needs no room. Save is pinned at the bottom, over the
    // keyboard when it's up, like Start and Done on the workout pages (spec 6.3), and as wide as the fields: on a
    // wide screen both keep to the page's centred column (spec 2.4 Unfolded). Whether the keyboard takes room is
    // read from its inset, so a keyboard that floats (as it can on an unfolded screen) leaves Save above the tab bar.
    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val keyboard = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val bottom = if (keyboard) 0.dp else contentPadding.calculateBottomPadding()
        val side = GlowLayout.pageSide(maxWidth, LocalPageColumn.current, GlowLayout.Gutter)
        // The list ends clear of Save and the fade behind it (ScreenList adds its own 24 dp).
        org.openstrap.app.ui.components.PageLight(
            org.openstrap.app.ui.theme.ScreenLight(org.openstrap.app.ui.theme.Blends.You, angle = 30f, strength = 0.75f, heroCenterY = 165f),
        )
        ScreenList(PaddingValues(bottom = PinnedFade.listClearance(bottom + 12.dp + SaveHeight, listEnd = 24.dp)), gutter = GlowLayout.Gutter) {
            item { PageHeader(stringResource(R.string.you_profile_title), back) }
            item { PageNote(stringResource(R.string.you_profile_note), Color.White.copy(alpha = 0.65f)) }
            item {
                PagePanel {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PageNumberField(
                            age, { age = it }, stringResource(R.string.you_profile_age), stringResource(R.string.you_profile_age_unit),
                            age.isNotBlank() && ageValue == null,
                        )
                        // Sex as labelled segments like Units, not two bare symbols beside Age. Tapping the chosen one
                        // again clears it.
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PageNote(stringResource(R.string.you_profile_sex_note), Color.White.copy(alpha = 0.65f))
                            org.openstrap.app.ui.glow.GlowSegments(
                                listOf(stringResource(R.string.you_profile_sex_female), stringResource(R.string.you_profile_sex_male)),
                                if (sex in 0..1) sex else -1, { i -> sex = if (sex == i) 2 else i },
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PageNote(stringResource(R.string.you_profile_units), Color.White.copy(alpha = 0.65f))
                            org.openstrap.app.ui.glow.GlowSegments(
                                listOf(
                                    stringResource(R.string.you_profile_units_metric), stringResource(R.string.you_profile_units_imperial),
                                ),
                                if (imperial) 1 else 0, { switchUnits(it == 1) },
                            )
                        }
                    }
                }
            }
            // Weight and height as two big tiles (the board's 112 dp); imperial height is feet and inches, three across.
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BodyTile(
                        weight, { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5) }, stringResource(R.string.you_profile_weight),
                        stringResource(if (imperial) R.string.you_profile_unit_lb else R.string.you_profile_unit_kg),
                        weight.isNotBlank() && weightValue == null, Modifier.weight(if (imperial) 1.3f else 1f), decimal = true, big = !imperial,
                    )
                    if (imperial) {
                        val wrong = !heightBlank && heightValue == null
                        BodyTile(
                            feet, { feet = it.filter(Char::isDigit).take(1) }, stringResource(R.string.you_profile_feet),
                            stringResource(R.string.you_profile_unit_ft), wrong, Modifier.weight(1f), big = false,
                        )
                        BodyTile(
                            inches, { inches = it.filter(Char::isDigit).take(2) }, stringResource(R.string.you_profile_inches),
                            stringResource(R.string.you_profile_unit_in), wrong, Modifier.weight(1f), big = false,
                        )
                    } else {
                        BodyTile(
                            heightCm, { heightCm = it.filter(Char::isDigit).take(5) }, stringResource(R.string.you_profile_height),
                            stringResource(R.string.you_profile_unit_cm), !heightBlank && heightValue == null, Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                // The usual activity in a card lit from below in the copper blend, the five levels as segments.
                Column(
                    Modifier.fillMaxWidth().darkGlow(Blends.You, GlowShapes.Card).padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.you_profile_activity_note), color = Color.White.copy(alpha = 0.8f),
                        style = GlowType.body,
                    )
                    // Tapping the chosen level again clears it.
                    org.openstrap.app.ui.glow.GlowSegments((1..5).map { "$it" }, activity, { i -> activity = if (activity == i) -1 else i })
                    Text(
                        if (activity in 0..4) org.openstrap.app.data.Vo2max.ACTIVITY_LEVELS[activity]
                        else stringResource(R.string.you_profile_activity_pick),
                        color = Color.White, style = GlowType.body,
                    )
                }
            }
            item {
                PageRows {
                    PageSwitchRow(
                        stringResource(R.string.you_profile_beta_blockers), null, betaBlocker, { betaBlocker = it }, divider = false,
                    )
                    PageSwitchRow(stringResource(R.string.you_profile_pregnant), null, pregnant, { pregnant = it })
                }
            }
            item {
                // Your max is set on the Heart rate zones page, with the zones that follow it.
                OutlineRow(
                    AppIcons.Heartbeat, org.openstrap.app.ui.workout.HeartZonesCopy.MAX_ROW, onHeartZones,
                    note = org.openstrap.app.ui.workout.HeartZonesCopy.profileLine(profile.maxHeartRate),
                    iconFill = Blends.Heart.first.copy(alpha = 0.20f), iconTint = Blends.Heart.chart,
                )
            }
            item {
                SleepNeedRow(
                    sleepNeed, { sleepNeed = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5) },
                    stringResource(R.string.you_profile_sleep_need), stringResource(R.string.you_unit_h),
                    sleepNeed.isNotBlank() && sleepNeedValue == null,
                )
            }
        }
        // Black from the bottom of the screen up to Save, fading out above it, so the page scrolls away under it.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().pinnedFade().padding(start = side, end = side, bottom = bottom + 12.dp)) {
            PageButton(
                stringResource(R.string.you_action_save), save, Modifier.fillMaxWidth().height(SaveHeight), enabled = valid, primary = true,
            )
        }
    }
}

/** A big number field on a tile (Weight, Height): its [label] over the value in light type with the [unit] on its baseline. */
@Composable
private fun BodyTile(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    unit: String,
    error: Boolean,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    big: Boolean = true,
) {
    val shape = GlowShapes.Card
    Column(
        modifier.height(112.dp).clearGlass(shape).let { if (error) it.border(1.5.dp, Color.White.copy(alpha = 0.9f), shape) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.72f), style = GlowType.body)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicTextField(
                value, onChange, Modifier.width(IntrinsicSize.Min).widthIn(min = 18.dp, max = 110.dp),
                textStyle = (if (big) GlowType.goalValue else GlowType.smallValue).copy(color = Color.White),
                singleLine = true, cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            )
            Text(unit, color = Color.White.copy(alpha = 0.7f), style = GlowType.body, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

/** The sleep need as a 64 dp row on the board: the question, the hours on the right in 20 sp, then "h". */
@Composable
private fun SleepNeedRow(value: String, onChange: (String) -> Unit, label: String, unit: String, error: Boolean) {
    val shape = GlowShapes.Card
    Row(
        Modifier.fillMaxWidth().height(64.dp).clearGlass(shape).let { if (error) it.border(1.5.dp, Color.White.copy(alpha = 0.9f), shape) else it }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), color = Color.White.copy(alpha = 0.85f), style = GlowType.body)
        BasicTextField(
            value, onChange, Modifier.width(40.dp),
            textStyle = GlowType.rowValue.copy(color = Color.White, textAlign = TextAlign.End),
            singleLine = true, cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Text(unit, color = Color.White.copy(alpha = 0.6f), style = GlowType.body)
    }
}

/** The pinned Save button's height, as Start on Ready. */
private val SaveHeight = 56.dp

/** 72.0 -> "72", 158.7 -> "158.7". */
private fun formatNumber(v: Float): String = "%.1f".format(java.util.Locale.ROOT, v).removeSuffix(".0")
