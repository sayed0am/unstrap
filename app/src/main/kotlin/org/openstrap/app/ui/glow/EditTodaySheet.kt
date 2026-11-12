package org.openstrap.app.ui.glow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openstrap.app.R
import org.openstrap.app.ui.components.GlassSheet
import org.openstrap.app.ui.dashboard.TodayCard
import org.openstrap.app.ui.dashboard.TodayEdits
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.clearGlass

/**
 * Edit Today (audit 3, Sheets): which cards Today shows, and up to two pinned under the hero, as a sheet of rows on
 * clear glass. Each row has the card's name, a Pin chip when it can be pinned, and a switch for whether it shows. Cancel
 * and Done stay at the foot while the rows scroll. Saved as a whole on Done. Pulling the sheet down or Cancel leaves
 * Today as it was. The pins and hides not yet saved outlive a rotation, a fold and process death ([TodayEditsSaver]).
 */
@Composable
internal fun EditTodaySheet(edits: TodayEdits, onSave: (TodayEdits) -> Unit, onDismiss: () -> Unit) {
    var e by rememberSaveable(stateSaver = TodayEditsSaver) { mutableStateOf(edits) }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
    GlassSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Box(Modifier.weight(1f, fill = false)) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.glow_today_edit), color = Color.White, style = GlowType.cardTitle, maxLines = 1)
                        Text(
                            stringResource(R.string.glow_today_edit_note), color = Color.White.copy(alpha = 0.62f),
                            style = GlowType.secondary,
                        )
                    }
                    Column(Modifier.fillMaxWidth().clearGlass(GlowShapes.Card).padding(start = 18.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)) {
                        TodayCard.entries.filter { it.editable }.forEachIndexed { i, card ->
                            val shown = card !in e.hidden
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 52.dp).topLine(i > 0),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    card.label, Modifier.weight(1f), style = GlowType.body,
                                    color = if (shown) Color.White else Color.White.copy(alpha = 0.55f),
                                )
                                if (card.pinnable) {
                                    val pinned = card in e.pinned
                                    PinChip(pinned, enabled = e.canPin(card), label = card.label) { e = e.togglePin(card) }
                                }
                                EditSwitch(shown, card.label) { e = e.toggleHidden(card) }
                            }
                        }
                    }
                }
                // The rows fade out into the foot, as the board's last row does.
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(70.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, SheetFade))),
                )
            }
            Row(
                Modifier.fillMaxWidth().topLine(true, 0.08f).padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    .navigationBarsPadding().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FootButton(stringResource(R.string.glow_cancel), onDismiss, Modifier.weight(1f))
                FootButton(stringResource(R.string.glow_done), { onSave(e) }, Modifier.weight(1f), primary = true)
            }
        }
    }
}

/** The sheet's fill, for the fade at the foot of the rows (the boards' rgba(22,22,26,.95)). */
private val SheetFade = Color(0xFF16161A).copy(alpha = 0.95f)

private val PinIcon: ImageVector by lazy {
    ImageVector.Builder("pin", 14.dp, 14.dp, 24f, 24f).addPath(
        addPathNodes("M9 4h6l-1 6 4 3v1H6v-1l4-3zM12 14v6"), stroke = SolidColor(Color.White), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
    ).build()
}

private val CheckIcon: ImageVector by lazy {
    ImageVector.Builder("check", 18.dp, 18.dp, 24f, 24f).addPath(
        addPathNodes("M5 12.5l4.5 4.5L19 7.5"), stroke = SolidColor(Color.White), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
    ).build()
}

/** A hairline along the top of a row: white at [alpha], the boards' `inset 0 1px 0`. */
private fun Modifier.topLine(show: Boolean, alpha: Float = 0.07f): Modifier =
    if (show) drawBehind { drawLine(Color.White.copy(alpha = alpha), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) } else this

/**
 * The Pin chip: white with dark text and a dark pin when pinned, else a 10% white pill with a 14% ring. 30 dp tall
 * inside a 44 dp tap target. When it can't take another pin it sits at 55% and does nothing.
 */
@Composable
private fun PinChip(pinned: Boolean, enabled: Boolean, label: String, onClick: () -> Unit) {
    val text = stringResource(if (pinned) R.string.glow_today_edit_pinned else R.string.glow_today_edit_pin)
    val ink = if (pinned) Color(0xFF0B0B0D) else Color.White
    Box(
        Modifier.height(44.dp).alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .semantics { stateDescription = text; contentDescription = "$label, $text" },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.height(30.dp)
                .let {
                    if (pinned) it.background(Color.White, GlowShapes.Pill)
                    else it.clearGlass(GlowShapes.Pill, fill = 0.10f)
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(PinIcon, null, Modifier.size(14.dp), tint = ink)
            Text(text, color = ink, style = GlowType.secondary.copy(fontWeight = if (pinned) FontWeight.SemiBold else FontWeight.Normal), maxLines = 1)
        }
    }
}

/** The switch as the board draws it: 48 by 29, white with a dark knob when on, a 16% white track with a white knob when off. */
@Composable
private fun EditSwitch(on: Boolean, label: String, onToggle: () -> Unit) {
    val knobX by animateDpAsState(if (on) 22.dp else 3.dp, label = "edit switch knob")
    val track by animateColorAsState(if (on) Color.White else Color.White.copy(alpha = 0.16f), label = "edit switch track")
    val knob by animateColorAsState(if (on) Color(0xFF0B0B0D) else Color.White, label = "edit switch knob colour")
    Box(
        Modifier.height(44.dp)
            // Toggleable, so TalkBack and tests read its on or off state.
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.size(48.dp, 29.dp).clip(GlowShapes.Pill).background(track)) {
            Box(Modifier.offset(x = knobX, y = 3.dp).size(23.dp).background(knob, GlowShapes.Pill))
        }
    }
}

/** Cancel (clear glass) or Done (white with a check), 52 dp tall pills. */
@Composable
private fun FootButton(text: String, onClick: () -> Unit, modifier: Modifier, primary: Boolean = false) {
    val ink = if (primary) Color(0xFF0B0B0D) else Color.White
    Row(
        modifier.height(52.dp)
            .let { if (primary) it.background(Color.White, GlowShapes.Pill) else it.clearGlass(GlowShapes.Pill, fill = 0.10f) }
            .clip(GlowShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (primary) Icon(CheckIcon, null, Modifier.size(18.dp), tint = ink)
        Text(text, color = ink, style = GlowType.body.copy(fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium), maxLines = 1)
    }
}

/** Keeps [TodayEdits] in a saved state bundle as the hidden and the pinned cards' names, pins in their order. */
internal val TodayEditsSaver: Saver<TodayEdits, Any> = listSaver(
    save = { e -> listOf(ArrayList(e.hidden.map { it.name }), ArrayList(e.pinned.map { it.name })) },
    restore = { v ->
        @Suppress("UNCHECKED_CAST")
        fun cards(i: Int) = (v[i] as List<String>).map(TodayCard::valueOf)
        TodayEdits(cards(0).toSet(), cards(1))
    },
)
