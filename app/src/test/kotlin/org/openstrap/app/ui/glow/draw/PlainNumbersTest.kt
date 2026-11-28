package org.openstrap.app.ui.glow.draw

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Plain numbers must stay inside the room the dots would take, since every layout was sized for the dots. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlainNumbersTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val density = 3f

    private fun render(text: String): Pair<Bitmap, DotLayout> {
        val layout = dotLayout(text, DotSize.Hero.dot.value, 1f)
        return renderPlainNumber(context, text, layout, density) to layout
    }

    /** The first and last rows with ink in the columns [across]. */
    private fun inkRows(b: Bitmap, across: IntRange = 0 until b.width): IntRange {
        val rows = (0 until b.height).filter { y -> across.any { x -> b.getPixel(x, y) ushr 24 > 0 } }
        return rows.first()..rows.last()
    }

    @Test fun `no number is wider or higher than its dots`() {
        listOf("8", "57", "111", "8888", "25 559", "7:42", "1:07:42", "−12", "+29", "100%", "26.1k", "8,432").forEach { text ->
            val (bitmap, layout) = render(text)
            assertTrue("$text wide ${bitmap.width} in ${layout.width * density}", bitmap.width <= layout.width * density + 1)
            assertTrue("$text high", bitmap.height <= layout.height * density + 1)
        }
    }

    @Test fun `digits fill the dots' height`() {
        val (bitmap, _) = render("57")
        val ink = inkRows(bitmap)
        assertTrue("$ink of ${bitmap.height}", ink.last - ink.first >= bitmap.height * 0.9f)
    }

    @Test fun `grouped thousands keep a gap`() {
        // Funnel has no thin space and the stand-in all but closed the gap on a widget.
        val paint = plainNumberPaint(context, "8", 100f, Float.MAX_VALUE)
        val gap = paint.measureText(plainText("8\u2009432")) - paint.measureText("8432")
        assertTrue("$gap at ${paint.textSize}", gap >= paint.textSize * 0.24f)
    }

    @Test fun `a comma's tail is drawn, not cut off`() {
        val bitmap = render("8,432").first
        // The first digit sits up off the foot to leave the tail its room, and the tail reaches the foot.
        val eight = inkRows(bitmap, 0 until bitmap.width / 8)
        assertTrue("$eight in ${bitmap.height}", eight.last < bitmap.height * 0.92f)
        assertTrue("${inkRows(bitmap)} in ${bitmap.height}", inkRows(bitmap).last >= bitmap.height - 2)
    }
}
