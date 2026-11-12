package org.openstrap.app.ui.glow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.dashboard.NewsTag

/** A news tag's fill: white at 14%, no border (Elements board, Tags). */
internal val NewsPillFill = Color.White.copy(alpha = 0.14f)

/**
 * A news tag (spec 2.5, Tags): a pill, white at 14% with no border, its words 12 sp in white.
 * It sits 8 dp after a number's unit, or under a list row's name. Apart from the outlined [OutlineTag] beside a title,
 * which is about the data, never about you.
 */
@Composable
fun NewsPill(tag: NewsTag, modifier: Modifier = Modifier) = NewsPill(tag.text, modifier)

/** A news tag with its [text]. */
@Composable
fun NewsPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.background(NewsPillFill, GlowShapes.Pill).padding(horizontal = 8.dp, vertical = 2.dp),
        color = Color.White,
        style = GlowType.caption.copy(fontSize = 12.sp, lineHeight = 16.sp),
        maxLines = 1,
    )
}

/** From a number's unit to its tag. */
internal val NewsPillGap = 8.dp

/** From a number's dotted digits down to its tag, when the tag drops under them. */
internal val NewsPillDropGap = 12.dp

/**
 * A card's number with its unit and news tag (spec 2.5, Tags): [number] (the dotted digits and their unit), then the
 * tag 8 dp after it, dropping 12 dp under the number when the line can't hold both, as on a Small. The drop is wider
 * than the side gap because dotted digits read as a block and crowd a pill tucked right under them.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NumberWithTag(tag: NewsTag?, modifier: Modifier = Modifier, number: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(NewsPillGap),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(NewsPillDropGap),
        itemVerticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        number()
        if (tag != null) NewsPill(tag)
    }
}
