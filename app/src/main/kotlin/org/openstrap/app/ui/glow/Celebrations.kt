package org.openstrap.app.ui.glow

import org.openstrap.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.openstrap.app.data.Rewards
import org.openstrap.app.ui.theme.AppIcons
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.MetricColors
import org.openstrap.app.ui.theme.ScreenLight
import org.openstrap.app.ui.theme.screenLight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import org.openstrap.app.presentation.RewardsCopy
import org.openstrap.app.presentation.dotFormat
import org.openstrap.app.ui.glow.draw.DotSize

/*
 * The celebrations (workouts spec 3.4, boards GoalMoment and BadgeUnlocked): the full screen goal moment, once a week,
 * the full screen card for a personal best (never for a milestone), the badges past history earned, shown once on
 * one screen, and the two switches that turn the goal moment and the weekly recap off. No other pop ups.
 */

/** Each group of badges in its own colour, so the art tells them apart. */
internal fun badgeColor(group: Rewards.Group): Color = when (group) {
    Rewards.Group.Firsts -> MetricColors.steps
    Rewards.Group.Milestones -> MetricColors.calories
    Rewards.Group.Weekly -> Blends.Goals.first
    // Something new: white glass, no colour of its own (decision 8).
    Rewards.Group.Kinds -> Color.White
}

/** A personal best's colour: the calories' gold. */
internal val BestColor = MetricColors.calories

/** The close button in a celebration's top corner. */
@Composable
private fun CloseRow(title: String?, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(title.orEmpty(), Modifier.weight(1f))
        RoundIconButton(AppIcons.X, stringResource(R.string.glow_close), onClose, size = 40.dp)
    }
}

/**
 * The full screen card for a personal best (board BadgeUnlocked): the best's art with its value, what it is, how far it
 * beat the last one and when it was set. Share comes with the share screen; Continue goes on.
 */
@Composable
fun BestCardScreen(best: Rewards.NewBest, sportName: String, contentPadding: PaddingValues, onContinue: () -> Unit) {
    BackHandler(onBack = onContinue)
    val now = System.currentTimeMillis() / 1000
    LightPage(
        Modifier.screenLight(ScreenLight(Blends.Calories, 270f, ScreenLight.DAY, 260f)), contentPadding,
        pinned = { WorkoutButton(stringResource(R.string.glow_continue), Color.White, Color.Black, onContinue) },
    ) {
        CloseRow(stringResource(R.string.glow_celebrate_personal_best), onContinue)
        Spacer(Modifier.height(24.dp))
        Medal(bestLook(best.best.kind), 200.dp, Modifier.align(Alignment.CenterHorizontally), lit = rememberLightRise(true))
        Text(
            RewardsCopy.bestValue(best.best.kind, best.best.value), color = Color.White, style = GlowType.goalValue, maxLines = 1,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(RewardsCopy.bestTitle(best.best.kind), color = Color.White, style = GlowType.pageTitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(RewardsCopy.beatLine(best, now), color = Color.White, style = GlowType.sentence, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(
            RewardsCopy.whenSet(best.best.date, sportName), color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The goal moment (board GoalMoment), once a week after the summary of the workout that met the goal: "4/4" in dots,
 * the weeks in a row, the week's days, and Continue, with Celebrations settings under it.
 */
@Composable
fun GoalMomentScreen(count: Int, goal: Int, inARow: Int, weekStarts: List<Long>, contentPadding: PaddingValues, onSettings: () -> Unit, onContinue: () -> Unit) {
    BackHandler(onBack = onContinue)
    LightPage(
        Modifier.screenLight(ScreenLight(Blends.Goals, 270f, ScreenLight.DAY, 190f)), contentPadding,
        pinned = {
            WorkoutButton(stringResource(R.string.glow_continue), Color.White, Color.Black, onContinue)
            PageTextButton(stringResource(R.string.glow_celebrate_settings), onSettings, Modifier.align(Alignment.CenterHorizontally))
        },
    ) {
        CloseRow(null, onContinue)
        Spacer(Modifier.height(12.dp))
        // The number sits in a glass disc ringed in the Goals blend (board WorkoutCelebration).
        Box(
            Modifier.align(Alignment.CenterHorizontally).size(220.dp).drawBehind {
                val ring = 1.5.dp.toPx()
                val brush = Brush.linearGradient(listOf(Blends.Goals.first, Blends.Goals.second), Offset(size.width * 0.2f, 0f), Offset(size.width * 0.8f, size.height))
                drawCircle(Brush.verticalGradient(listOf(org.openstrap.app.ui.theme.DarkGlowFill.copy(alpha = 0.92f), org.openstrap.app.ui.theme.DarkGlowFill.copy(alpha = 0.6f), org.openstrap.app.ui.theme.DarkGlowFill.copy(alpha = 0.2f))), size.minDimension / 2)
                drawCircle(brush.let { Brush.linearGradient(listOf(Blends.Goals.first.copy(alpha = 0.18f), Blends.Goals.second.copy(alpha = 0.3f))) }, size.minDimension / 2)
                drawCircle(brush, size.minDimension / 2 - ring / 2, style = Stroke(ring))
            },
            contentAlignment = Alignment.Center,
        ) {
            DotMatrixText(dotFormat("%d/%d", count, goal), dot = DotSize.Hero)
        }
        Text(
            stringResource(R.string.glow_celebrate_goal_met), color = Color.White, style = GlowType.pageTitle, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )
        Text(RewardsCopy.momentLine(inARow), color = Color.White, style = GlowType.sentence, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        WeekDaysPanel(weekStarts, note = stringResource(R.string.glow_celebrate_goal_note))
    }
}

/**
 * The celebration settings and the week the goal moment last showed for, with what changes them, handed to the
 * summary so it can follow itself with the goal moment.
 */
class Celebrations(
    val goalMoment: Boolean,
    val weeklyRecap: Boolean,
    /** The week (epoch day) the goal moment last showed for. */
    val momentWeek: Long?,
    val setGoalMoment: (Boolean) -> Unit,
    val setWeeklyRecap: (Boolean) -> Unit,
    val markMoment: (Long) -> Unit,
)

/** The two celebration switches (workouts spec 3.4), in a sheet. */
@Composable
fun CelebrationsSheet(goalMoment: Boolean, weeklyRecap: Boolean, onGoalMoment: (Boolean) -> Unit, onWeeklyRecap: (Boolean) -> Unit, onDismiss: () -> Unit) {
    PageSheet(onDismiss, stringResource(R.string.glow_celebrate_title), note = stringResource(R.string.glow_celebrate_sheet_note)) {
        PageRows {
            PageSwitchRow(
                stringResource(R.string.glow_celebrate_goal_moment), stringResource(R.string.glow_celebrate_goal_moment_line), goalMoment,
                onGoalMoment, divider = false,
            )
            PageSwitchRow(
                stringResource(R.string.glow_celebrate_recap), stringResource(R.string.glow_celebrate_recap_line), weeklyRecap,
                onWeeklyRecap,
            )
        }
        PageButton(stringResource(R.string.glow_done), onDismiss, Modifier.fillMaxWidth(), primary = true)
    }
}

/**
 * The badges past history earned, shown once on one screen when the app first works them out (requests spec 7.2),
 * rather than one by one.
 */
@Composable
fun HistoryBadgesScreen(badges: List<Rewards.Badge>, contentPadding: PaddingValues, onContinue: () -> Unit, face: (Rewards.Badge) -> String = { it.short }) {
    BackHandler(onBack = onContinue)
    LightPage(
        Modifier.screenLight(ScreenLight(Blends.Goals, 270f, ScreenLight.DAY, 190f)), contentPadding,
        pinned = { WorkoutButton(stringResource(R.string.glow_continue), Color.White, Color.Black, onContinue) },
    ) {
        CloseRow(null, onContinue)
        Text(
            stringResource(R.string.glow_celebrate_history_title), color = Color.White, style = GlowType.pageTitle,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )
        Text(
            pluralStringResource(R.plurals.glow_celebrate_history_badges, badges.size, badges.size),
            color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.body, modifier = Modifier.padding(start = 4.dp),
        )
        QuietPanel {
            badges.chunked(HISTORY_PER_ROW).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { b ->
                        Column(
                            Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Medal(badgeLook(b, face(b)), 72.dp)
                            Text(b.title, color = Color.White, style = GlowType.secondary, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                    repeat(HISTORY_PER_ROW - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** The badges history earned, three to a row. */
private const val HISTORY_PER_ROW = 3
