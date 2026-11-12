package org.openstrap.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * A two-stop blend (Glow × Glass, Palette board). [first] sits where the light comes from and is the row icon's colour,
 * [second] lands on the answer (now, the top, the value). [chart] is [first] lightened until it reads at 4.5:1 on dark
 * glass, for chart marks and small text. [light] and [lightSecond] are both stops muted for the screen's light (Matrix
 * board): same hue, 60% of the saturation, 40% lightness, which is what keeps white text on glass at 4.5:1.
 *
 * A blend says which metric or place, never how it is going. Values stay white.
 */
@Immutable
class Blend(val first: Color, val second: Color) {
    val chart: Color = chartTone(first)
    val chartSecond: Color = chartTone(second)
    val light: Color = muted(first)
    val lightSecond: Color = muted(second)

    /** The blend along a line or bar, first stop at the start, second on the answer. */
    fun brush(): Brush = Brush.horizontalGradient(listOf(first, second))

    /** The chart version of [brush], for marks on dark glass. */
    fun chartBrush(): Brush = Brush.horizontalGradient(listOf(chart, chartSecond))

    override fun equals(other: Any?) = other is Blend && other.first == first && other.second == second
    override fun hashCode() = first.hashCode() * 31 + second.hashCode()
    override fun toString() = "Blend($first, $second)"
}

/** Every blend in the app (Palette board). A screen's light is its hero's blend. */
object Blends {
    val Recovery = Blend(Color(0xFF7CF2C9), Color(0xFF2FC79A))
    val Hrv = Blend(Color(0xFF2DD4BF), Color(0xFF1B8FA8))
    val Spo2 = Blend(Color(0xFF38BDF8), Color(0xFF5AD1FF))
    val Breathing = Blend(Color(0xFFE6ECF5), Color(0xFFC9B8FF))
    val Sleep = Blend(Color(0xFF4C6FFF), Color(0xFF2E3FC9))
    val Load = Blend(Color(0xFFA78BFA), Color(0xFF796AFF))
    /** A moment, not a metric: the evening hero. */
    val Evening = Blend(Color(0xFFA274F2), Color(0xFF5B2A86))
    val HabitAge = Blend(Color(0xFFD86BEA), Color(0xFF6FE3F0))
    val Stress = Blend(Color(0xFFF062C0), Color(0xFFFF9AD5))
    val Heart = Blend(Color(0xFFFF5A5F), Color(0xFFD9233F))
    /** Ember into violet. */
    val Energy = Blend(Color(0xFFFF7A2E), Color(0xFFB05CFF))
    /** Cool into warm. */
    val Temperature = Blend(Color(0xFF6FC3FF), Color(0xFFFF8A3D))
    val Calories = Blend(Color(0xFFFFD23F), Color(0xFFF2A93B))
    val Steps = Blend(Color(0xFFC3F53C), Color(0xFFEEF04A))

    /** Places, not metrics: they light their own pages and never a chart. Strap also lights setup. */
    val Strap = Blend(Color(0xFFAEB6C2), Color(0xFF4A5262))
    val You = Blend(Color(0xFFF0B48C), Color(0xFFA8613F))
    val Goals = Blend(Color(0xFF9BFFB0), Color(0xFF7C6BFF))

    /**
     * Workout families (Workouts board): the family's blend lights Ready, live, summary, share and the notification.
     * Zones never light a screen. Distance is Steps' blend and Mind and body is the Evening's.
     */
    val Distance = Steps
    val IndoorCardio = Blend(Color(0xFFC04FE0), Color(0xFF6B3FD8))
    val Strength = Blend(Color(0xFFB98A4E), Color(0xFF5A4636))
    /** Flare pink for work, deep teal for rest: the light that means the phase. */
    val Intervals = Blend(Color(0xFFFF44B0), Color(0xFF0E8A87))
    val Games = Blend(Color(0xFF4CB85C), Color(0xFF2F6B3F))
    val MindAndBody = Evening

    /** Flat Deep for small marks (legend dots), lightened to 3:1. The stage bars keep [MetricColors.sleepDeep]. */
    val SleepDeepMark = Color(0xFF5A7BFF)

    /**
     * The blend for a v2 ink ([MetricColors]), so parts that still take one colour light with the right blend while
     * they move over. Null for colours that aren't a metric's (status green and amber, neutrals).
     */
    fun forInk(ink: Color): Blend? = when (ink) {
        MetricColors.readiness -> Recovery
        MetricColors.hrv -> Hrv
        MetricColors.spo2 -> Spo2
        MetricColors.sleepLight -> Breathing
        MetricColors.sleepDeep -> Sleep
        MetricColors.load -> Load
        MetricColors.evening -> Evening
        MetricColors.habitAge -> HabitAge
        MetricColors.stress -> Stress
        MetricColors.heart -> Heart
        MetricColors.charge -> Energy
        MetricColors.temperature -> Temperature
        MetricColors.calories -> Calories
        MetricColors.steps -> Steps
        else -> all.firstOrNull { it.second.first == ink }?.second
    }

    /** Every blend, for tests and the debug gallery. */
    val all: List<Pair<String, Blend>> = listOf(
        "Recovery" to Recovery, "HRV" to Hrv, "Blood oxygen" to Spo2, "Breathing rate" to Breathing, "Sleep" to Sleep,
        "Load" to Load, "Evening" to Evening, "Habit Age" to HabitAge, "Stress" to Stress, "Heart rate" to Heart,
        "Energy" to Energy, "Skin temperature" to Temperature, "Calories" to Calories, "Steps" to Steps,
        "Strap" to Strap, "You" to You, "Goals" to Goals,
        "Indoor cardio" to IndoorCardio, "Strength" to Strength, "Intervals" to Intervals, "Games" to Games,
    )
}

/** The darkest glass a chart sits on: white 7% over a lit ground. Chart tones are checked against it. */
val ChartGlass = Color(0xFF26282C)

/** WCAG contrast ratio of [a] on [b]. */
fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return max(la, lb) / min(la, lb)
}

/** [c] with its HSL lightness raised until it reads at 4.5:1 on [ChartGlass]. Already light colours come back as they are. */
internal fun chartTone(c: Color, on: Color = ChartGlass, target: Float = 4.5f): Color {
    if (contrast(c, on) >= target) return c
    val (h, s, l) = hsl(c)
    var light = l
    while (light < 1f) {
        light = min(1f, light + 0.01f)
        val next = Color.hsl(h, s, light)
        if (contrast(next, on) >= target) return next
    }
    return Color.White
}

/** [c] for the screen's light: same hue, 60% of its saturation, 40% lightness (Matrix board). */
internal fun muted(c: Color): Color {
    val (h, s, _) = hsl(c)
    return Color.hsl(h, s * 0.6f, 0.4f)
}

internal fun hsl(c: Color): Triple<Float, Float, Float> {
    val r = c.red; val g = c.green; val b = c.blue
    val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
    val l = (mx + mn) / 2f
    if (mx == mn) return Triple(0f, 0f, l)
    val d = mx - mn
    val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f))
        g -> ((b - r) / d + 2f)
        else -> ((r - g) / d + 4f)
    } * 60f
    return Triple(h, s, l)
}
