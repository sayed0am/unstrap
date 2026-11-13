package org.openstrap.app.ui.workout

import androidx.compose.ui.graphics.Color
import org.openstrap.app.data.Workouts
import org.openstrap.app.presentation.WorkoutColors
import org.openstrap.app.ui.theme.Blend
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.presentation.WorkoutWords

/** Zone 1 (easy) to 5 (max), cool to hot ([WorkoutColors.ZONES]); drawn small (a dot, a bar, a chip), never as a screen's light. */
internal val ZONE_COLORS: List<Color> = WorkoutColors.ZONES.map(::Color)
/**
 * A workout's top card colour: the zone it spent most time in, so the card agrees with its sentence ("mostly in zone
 * 1"), or [fallback] with no heart rate in any zone.
 */
internal fun mainZoneColor(zones: List<Int>, fallback: Color): Color =
    zones.indices.maxByOrNull { zones[it] }?.takeIf { zones[it] > 0 }?.let { ZONE_COLORS.getOrNull(it) } ?: fallback

/** Read each time, so the names follow the app's language. */
internal val ZONE_NAMES: List<String> get() = WorkoutWords.ZONE_NAMES

/**
 * Each kind of workout's blend (Workouts board, Palette): the light on Choose, Ready, the live screens and the summary.
 * Distance takes Steps' lime, Indoor cardio violet into indigo, Strength bronze into iron, Intervals flare pink into
 * deep teal, Games turf into pitch, and Mind and body the evening's violet. Zones never light a screen.
 */
internal fun familyBlend(family: Workouts.Family): Blend = when (family) {
    Workouts.Family.Distance -> Blends.Distance
    Workouts.Family.IndoorCardio -> Blends.IndoorCardio
    Workouts.Family.Strength -> Blends.Strength
    Workouts.Family.Intervals -> Blends.Intervals
    Workouts.Family.MindBody -> Blends.MindAndBody
    Workouts.Family.Games -> Blends.Games
}

/** The blend of a sport's family, where a sport is all there is to go on (a tile, a pill). */
internal fun sportBlend(sportType: Int): Blend = familyBlend(Workouts.family(sportType))

/** A family's first stop, for the small marks that take one colour (a dot, an icon). */
internal fun familyColor(family: Workouts.Family): Color = familyBlend(family).first
