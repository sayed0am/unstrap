package org.openstrap.app.ui.glow

import org.openstrap.app.presentation.DayWords
import org.openstrap.app.presentation.Ranges
import org.openstrap.app.presentation.Units
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle as DayStyle
import java.util.Locale
import org.openstrap.app.R
import org.openstrap.app.data.Rewards
import org.openstrap.app.i18n.Strings
import org.openstrap.app.ui.dashboard.ScreenList
import org.openstrap.app.ui.theme.AppIcons
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.DarkGlowFill
import org.openstrap.app.ui.theme.litEdge
import org.openstrap.app.ui.theme.GlowShapes
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import org.openstrap.app.presentation.RewardsCopy
import org.openstrap.app.presentation.RewardsView
import org.openstrap.app.ui.glow.draw.GlowStyle
import org.openstrap.app.ui.glow.draw.level

/*
 * The Goals and bests page from You (workouts spec 3.5, board AchievementsV1), All badges, and the weekly recap the
 * Monday notification opens (board WeeklyRecap). The pages show each best and badge as its medal ([Medal]) with only
 * its name and figure; a tap opens its sheet, with what it is, when it was earned and how far along a locked one is.
 */

/** The goal colour: met weeks, the goal moment. */
private val GoalColor = Blends.Goals.first

/** The days of [first]'s week, each a letter and whether a workout in [view] counted on it. */
private fun weekDots(view: RewardsView, first: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Pair<String, Boolean>> {
    val days = view.counted.map { Instant.ofEpochSecond(it.shownStart).atZone(zone).toLocalDate() }.toSet()
    return (0L until 7L).map { i ->
        first.plusDays(i).let { d -> d.dayOfWeek.getDisplayName(DayStyle.NARROW, Locale.getDefault()) to (d in days) }
    }
}

/**
 * The Goals blend's page light (boards RewardsGoals, RewardsBadges, RewardsWeeklyRecap, WorkoutCelebration): the second
 * stop big at the upper left, the first small at the lower right, both muted, over the ground. [top] and [bottom] are
 * the two blobs' strengths.
 */
internal fun Modifier.goalsLight(top: Float = 0.5f, bottom: Float = 0.22f, topY: Float = 200f, bottomY: Float = 680f): Modifier = drawBehind {
    drawRect(org.openstrap.app.ui.theme.Ground)
    fun blob(c: Offset, rx: Float, ry: Float, color: Color, alpha: Float) {
        val brush = Brush.radialGradient(
            0f to color.copy(alpha = alpha), 0.35f to color.copy(alpha = alpha * 0.9f), 0.6f to color.copy(alpha = alpha * 0.55f),
            0.8f to color.copy(alpha = alpha * 0.2f), 1f to Color.Transparent, center = c, radius = rx,
        )
        scale(1f, ry / rx, pivot = c) { drawCircle(brush, rx, c) }
    }
    val w = size.width
    blob(Offset(w * 0.3f, topY.dp.toPx()), w * 0.82f + 100.dp.toPx(), 240.dp.toPx() + 100.dp.toPx(), Blends.Goals.lightSecond, top)
    blob(Offset(w * 0.8f, bottomY.dp.toPx()), w * 0.5f + 100.dp.toPx(), 170.dp.toPx() + 100.dp.toPx(), Blends.Goals.light, bottom)
}

/**
 * The Goals hero's fill (board RewardsGoals, decision 7): the Goals blend left to right as far as [level] of the goal,
 * then a soft fade into the dark just past it (not a hard stop), with three dark pools behind the text.
 */
private fun Modifier.goalsFill(level: Float): Modifier = drawWithCache {
    val dark = DarkGlowFill
    val l = level.coerceIn(0f, 1f)
    val fade = (l * 1.2f).coerceAtMost(1f)
    val fill = Brush.horizontalGradient(
        0f to Blends.Goals.first.copy(alpha = 0.78f), (l * 0.96f) to Blends.Goals.second.copy(alpha = 0.62f),
        fade to DarkGlowFill.copy(alpha = 0.88f), 1f to DarkGlowFill.copy(alpha = 0.88f),
    )
    onDrawBehind {
        drawRect(fill)
        fun pool(cx: Float, cy: Float, rx: Float, ry: Float, a: Float) {
            val c = Offset(size.width * cx, size.height * cy)
            val r = size.width * rx
            val b = Brush.radialGradient(0f to dark.copy(alpha = a), 0.5f to dark.copy(alpha = a * 0.66f), 1f to Color.Transparent, center = c, radius = r)
            scale(1f, size.height * ry / r, pivot = c) { drawCircle(b, r, c) }
        }
        pool(0.18f, 0.10f, 0.6f, 0.30f, 0.6f)
        pool(0.38f, 0.94f, 0.9f, 0.32f, 0.82f)
        pool(0.14f, 0.46f, 0.4f, 0.46f, 0.9f)
    }
}

/**
 * A week against its goal on a glow filled as far as the count is toward it, fading out softly just past it: the count
 * in dots, "of 4 workouts", its days, and [line] under them. [trailing] goes on the title row.
 */
@Composable
private fun GoalWeekCard(
    title: String, count: Int, goal: Int, dots: List<Pair<String, Boolean>>, line: String,
    /** The week as a bar of the goal's workouts (board AchievementsV1) rather than its days (WeeklyRecap). */
    bar: Boolean = false, trailing: @Composable (() -> Unit)? = null,
) {
    val shape = GlowShapes.Card
    val level = (count.toFloat() / goal).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().clip(shape).goalsFill(level).litEdge(shape, top = 0.3f, ring = 0.1f).padding(GlowLayout.CardPadding)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, style = GlowType.cardTitle, maxLines = 1, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            HeroValue(
                "$count", pluralStringResource(R.plurals.glow_goals_of_workouts, goal, goal), Modifier.padding(top = GlowLayout.TitleGap),
            )
            // The week's workouts against the goal as a bar of [goal] parts, [count] of them white.
            val described = pluralStringResource(R.plurals.glow_goals_count_of_goal, goal, count, goal)
            if (bar) SegmentBar(
                List(goal.coerceIn(1, MAX_GOAL_SHOWN)) { i -> Segment(1f, Color.White.copy(alpha = if (i < count) 1f else 0.2f)) },
                Modifier.padding(top = 18.dp).semantics { contentDescription = described }, height = 8.dp, gap = 6.dp,
            ) else WeekDots(dots, Color.White, Modifier.padding(top = 18.dp))
            Text(line, color = Color.White, style = GlowType.body, modifier = Modifier.padding(top = 14.dp))
        }
    }
}

/** The most workouts the goal bar splits into, so each part stays wide enough to read. */
private const val MAX_GOAL_SHOWN = 14

/** A week's distance shows from a tenth of a km, the first the figure can write. */
private const val MIN_WEEK_METERS = 100f

/** All badges: three medals to a row, 8 dp apart. */
private const val ALL_PER_ROW = 3
private val ALL_GAP = 8.dp

/**
 * The share card for the week from [first]: the count against the goal, its days, the hours, km and active kcal, and
 * chips for the weeks in a row and the week's first best.
 */
private fun weekStory(view: RewardsView, first: LocalDate, strings: Strings = Strings.app): StoryCard.Week {
    val week = view.week(first)
    val count = week?.count ?: 0
    val goal = week?.goal ?: view.goal
    val zone = ZoneId.systemDefault()
    val from = first.atStartOfDay(zone).toEpochSecond()
    val to = first.plusDays(7).atStartOfDay(zone).toEpochSecond()
    val mine = view.all.filter { it.startedByYou && it.shownStart in from until to }
    val seconds = mine.sumOf { it.activeSeconds }
    val meters = mine.sumOf { (it.meters ?: 0f).toDouble() }.toFloat()
    val kcal = Math.round(mine.mapNotNull { it.activeKcal }.sum())
    val totals = if (mine.isEmpty()) emptyList() else listOfNotNull(
        StoryStat(Units.clockDuration(seconds), null, strings.get(R.string.glow_goals_hours)),
        meters.takeIf { it >= MIN_WEEK_METERS }?.let {
            StoryStat(Units.km(it, Units.Precision.Day), strings.get(R.string.glow_unit_km), strings.get(R.string.glow_goals_distance))
        },
        kcal.takeIf { it > 0 }?.let { StoryStat(Units.count(it), null, strings.get(R.string.glow_goals_active_kcal)) },
    )
    val run = Rewards.inARow(view.weeks, first)
    val best = view.bests.firstOrNull { it.date in from until to }
    val chips = listOfNotNull(
        if (week?.met == true && run > 0) {
            StoryChip(strings.plural(R.plurals.glow_goals_weeks_in_a_row, run, run), best = false, met = true)
        } else {
            null
        },
        best?.let {
            val text = strings.format(R.string.glow_goals_best, RewardsCopy.bestTitle(it.kind), RewardsCopy.bestValue(it.kind, it.value))
            StoryChip(text, best = true, met = false)
        },
    )
    return StoryCard.Week(
        Ranges.of(DayWords.dayMonth(first), DayWords.dayMonth(first.plusDays(6))), strings.get(R.string.glow_share_my_week), "$count",
        strings.format(R.string.glow_goals_story_goal, strings.plural(R.plurals.glow_goals_workouts_word, count), goal),
        weekDots(view, first), totals, chips,
    )
}

/** The weekly goal on its own, in a sheet: the pencil on the Goals and bests page and the recap. */
@Composable
fun WeeklyGoalSheet(goal: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by rememberSaveable { mutableIntStateOf(goal) }
    PageSheet(onDismiss, stringResource(R.string.glow_goals_weekly_goal), note = stringResource(R.string.glow_goals_sheet_note)) {
        org.openstrap.app.ui.device.WorkoutGoalField(value) { value = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PageButton(stringResource(R.string.glow_cancel), onDismiss, Modifier.weight(1f))
            PageButton(stringResource(R.string.glow_save), { onSave(value) }, Modifier.weight(1f), primary = true)
        }
    }
}

/**
 * Goals and bests (board AchievementsV1): this week against the goal with the weeks in a row and the best run, the
 * personal bests as rosettes, the next badge to earn and how far along, the badges earned as medals, and the ways on.
 * Each best and badge opens its sheet. Before any workout there's still a page: the week at nought, the bests and
 * badges there are to go for.
 */
@Composable
fun GoalsAndBestsPage(
    view: RewardsView?,
    contentPadding: PaddingValues,
    onGoal: () -> Unit,
    onAllBadges: () -> Unit,
    onRecap: () -> Unit,
    onCelebrations: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var openBadge by rememberSaveable { mutableStateOf<String?>(null) }
    var openBest by rememberSaveable { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().goalsLight()) { ScreenList(contentPadding, gutter = GlowLayout.Gutter) {
        item { PageHeader(stringResource(R.string.glow_goals_title), onBack) }
        if (view == null) {
            item { PageNote(stringResource(R.string.glow_goals_loading)) }
            return@ScreenList
        }
        item {
            val week = view.week(view.thisWeek)
            GoalWeekCard(
                stringResource(R.string.glow_goals_this_week), week?.count ?: 0, view.goal, weekDots(view, view.thisWeek),
                RewardsCopy.runLine(view.inARow, view.bestRun), bar = true,
            ) { PageButton(stringResource(R.string.glow_goals_goal_button, view.goal), onGoal, icon = AppIcons.PencilEdit) }
        }
        item { BestsPanel(view, onOpen = { openBest = it }) }
        view.next?.let { next -> item { NextBadgeCard(next, view, onOpen = { openBadge = it.id }) } }
        item { BadgesPanel(view, onOpen = { openBadge = it.id }, onAllBadges = onAllBadges) }
        item { WaysOn(onAllBadges, onRecap, onCelebrations) }
    } }
    if (view != null) {
        openBadge?.let(Rewards::badge)?.let { b -> BadgeSheet(b, view, onDismiss = { openBadge = null }) }
        openBest?.let { kind -> BestSheet(kind, view, onDismiss = { openBest = null }) }
    }
}

/** All badges, last week's recap and the celebration settings, as rows in one panel. */
@Composable
private fun WaysOn(onAllBadges: () -> Unit, onRecap: () -> Unit, onCelebrations: () -> Unit) {
    PageRows {
        PageRow(stringResource(R.string.glow_goals_all_badges), onClick = onAllBadges, icon = AppIcons.CheckCircle, divider = false)
        PageRow(stringResource(R.string.glow_goals_last_week), onClick = onRecap, icon = AppIcons.CalendarBlank)
        PageRow(stringResource(R.string.glow_goals_celebrations), onClick = onCelebrations, icon = AppIcons.SlidersHorizontal)
    }
}

/**
 * Every badge by group, three medals to a row with only their names: earned ones lit, locked ones quiet with a bar for
 * how far along. Each opens its sheet.
 */
@Composable
fun AllBadgesPage(view: RewardsView?, contentPadding: PaddingValues, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().goalsLight(top = 0.38f, bottom = 0.2f, topY = 100f, bottomY = 560f)) { ScreenList(contentPadding, gutter = GlowLayout.Gutter) {
        item {
            val subtitle = view?.let { stringResource(R.string.glow_goals_earned_count, it.earned.size, Rewards.BADGES.size) }
            PageHeader(stringResource(R.string.glow_goals_all_badges), onBack, subtitle = subtitle)
        }
        if (view == null) return@ScreenList
        Rewards.Group.entries.forEach { group ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(groupName(group), Modifier.padding(top = 6.dp))
                    QuietPanel {
                        Rewards.BADGES.filter { it.group == group }.chunked(ALL_PER_ROW).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ALL_GAP)) {
                                row.forEach { b -> BadgeCell(b, view, { open = it.id }, GlowType.secondary, Modifier.weight(1f)) }
                                repeat(ALL_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    } }
    if (view != null) open?.let(Rewards::badge)?.let { b -> BadgeSheet(b, view, onDismiss = { open = null }) }
}

/**
 * The weekly recap (board WeeklyRecap) for the week from [first]: the goal and the days, the weeks in a row, the bests
 * set that week, and its time, distance and active kcal. Next week's goal can be changed from here.
 */
@Composable
fun WeeklyRecapPage(view: RewardsView?, first: LocalDate, contentPadding: PaddingValues, onGoal: () -> Unit, onBack: () -> Unit) {
    // Share week opens the share screen in the recap's place; close comes back here.
    var sharing by rememberSaveable { mutableStateOf(false) }
    if (sharing && view != null) {
        val card = remember(view, first) { weekStory(view, first) }
        WeekShareScreen(card, "unstrap_week_$first.png") { sharing = false }
        return
    }
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().goalsLight(top = 0.55f, bottom = 0.3f, topY = 220f, bottomY = 670f)) { ScreenList(contentPadding, gutter = GlowLayout.Gutter) {
        item { PageHeader(stringResource(R.string.glow_goals_your_week), onBack, subtitle = RewardsCopy.weekRange(first)) }
        if (view == null) return@ScreenList
        val week = view.week(first)
        val count = week?.count ?: 0
        val goal = week?.goal ?: view.goal
        item {
            val run = Rewards.inARow(view.weeks, first)
            GoalWeekCard(
                stringResource(if (week?.met == true) R.string.glow_goals_goal_met else R.string.glow_goals_weekly_goal),
                count, goal, weekDots(view, first),
                if (week?.met == true && run > 0) {
                    pluralStringResource(R.plurals.glow_goals_weekly_streak, run, run)
                } else {
                    (goal - count).coerceAtLeast(0).let { pluralStringResource(R.plurals.glow_goals_short_of_goal, it, it) }
                },
            )
        }
        val zone = ZoneId.systemDefault()
        val from = first.atStartOfDay(zone).toEpochSecond()
        val to = first.plusDays(7).atStartOfDay(zone).toEpochSecond()
        val highlights = view.bests.filter { it.date in from until to }
        if (highlights.isNotEmpty()) item {
            ListPanel(title = stringResource(R.string.glow_goals_highlights)) {
                highlights.forEachIndexed { i, b ->
                    ListRow(
                        stringResource(R.string.glow_goals_best, RewardsCopy.bestTitle(b.kind), RewardsCopy.bestValue(b.kind, b.value)),
                        BestColor,
                        DayWords.weekdayShort(Instant.ofEpochSecond(b.date).atZone(zone).toLocalDate()), null,
                        divider = i > 0, icon = AppIcons.FlagCheckered,
                    )
                }
            }
        }
        item {
            val mine = view.all.filter { it.startedByYou && it.shownStart in from until to }
            val seconds = mine.sumOf { it.activeSeconds }
            val meters = mine.sumOf { (it.meters ?: 0f).toDouble() }.toFloat()
            val kcal = mine.mapNotNull { it.activeKcal }.sum()
            PictureCard(GoalColor, Modifier.fillMaxWidth()) {
                EdgeTitle(
                    stringResource(R.string.glow_goals_in_all),
                    pluralStringResource(R.plurals.glow_goals_workouts_count, mine.size, mine.size),
                )
                DetailFigures(
                    listOf(
                        Units.clockDuration(seconds) to stringResource(R.string.glow_goals_hours),
                        Units.km(meters, Units.Precision.Day) to stringResource(R.string.glow_unit_km),
                        Units.count(Math.round(kcal)) to stringResource(R.string.glow_goals_active_kcal),
                    ),
                    Modifier.padding(top = 12.dp), valueStyle = GlowType.rowValue, missing = null, gap = 0.dp,
                )
            }
        }
        item {
            // Board WeeklyRecap: next week's goal and Share week side by side, Share the bright one.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PageButton(
                    stringResource(R.string.glow_goals_next_week, view.goal), onGoal, Modifier.weight(1f), icon = AppIcons.PencilEdit,
                )
                PageButton(
                    stringResource(R.string.glow_goals_share_week), { sharing = true }, Modifier.weight(1f), primary = true,
                    icon = AppIcons.UploadSimple,
                )
            }
        }
    } }
}
