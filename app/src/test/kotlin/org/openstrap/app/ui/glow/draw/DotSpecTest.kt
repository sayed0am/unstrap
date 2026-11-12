package org.openstrap.app.ui.glow.draw

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
/** The dot numerals' shared geometry (widgets spec 6.2): what DotMatrixText and a widget's bitmap both draw. */
class DotSpecTest {
    @Test fun `8,432 in the second glow's dots`() {
        val l = dotLayout("8,432", DotSize.Second, density = 1f)
        // Four digits read without a gap (Numbers board): four 5-wide digits and three empty columns between them.
        assertEquals(23, DotFont.columns("8,432"))
        // 8 has 17 dots, 4 has 14, 3 has 15, 2 has 14.
        assertEquals(60, l.centers.size)
        val pitch = 3f + 3f * DotSize.DOT_GAP
        assertEquals(pitch * 23 - 3f * DotSize.DOT_GAP, l.width, 1e-4f)
        assertEquals(pitch * 7 - 3f * DotSize.DOT_GAP, l.height, 1e-4f)
        assertEquals(1.5f, l.radius, 1e-6f)
    }

    @Test fun `five digits keep a gap of half a digit before the last three`() {
        assertEquals("12,480", DotFont.grouped("12,480"))
        assertEquals("8412", DotFont.grouped("8,412"))
        // Five 5-wide digits, four gaps between glyphs, and the 2-wide group gap with its own column.
        assertEquals(5 * 5 + 2 + 5, DotFont.columns("12,480"))
    }

    @Test fun `every dot sits inside the box at any density`() {
        listOf(1f, 2.625f, 3.5f).forEach { density ->
            DotSize.entries.forEach { size ->
                val l = dotLayout("1,568 −94%", size, density)
                val w = l.width * density
                val h = l.height * density
                l.centers.forEach { c ->
                    assertTrue("$size $density $c", c.x - l.radius >= -1e-3f && c.x + l.radius <= w + 1e-3f)
                    assertTrue("$size $density $c", c.y - l.radius >= -1e-3f && c.y + l.radius <= h + 1e-3f)
                }
            }
        }
    }

    @Test fun `the layout is the one DotMatrixText always drew`() {
        // The inline drawing DotMatrixText had before the geometry was shared, kept here as the reference.
        val text = "62:07"
        val density = 2.75f
        val dot = 4.5f
        val gap = dot * DotSize.DOT_GAP
        val d = dot * density
        val step = (dot + gap) * density
        val expected = mutableListOf<Offset>()
        var x0 = 0f
        text.map(DotFont::glyph).forEach { rows ->
            rows.forEachIndexed { row, line ->
                line.forEachIndexed { col, c -> if (c == '#') expected += Offset(x0 + col * step + d / 2, row * step + d / 2) }
            }
            x0 += (rows[0].length + 1) * step
        }
        assertEquals(expected, dotLayout(text, DotSize.Hero, density).centers)
    }

    @Test fun `a character the font lacks is a space`() {
        assertTrue(!DotFont.has('x'))
        assertEquals(dotLayout("1 2", DotSize.Edge, 1f).width, dotLayout("1x2", DotSize.Edge, 1f).width, 0f)
        assertEquals(dotLayout("12", DotSize.Edge, 1f).centers.size, dotLayout("1x2", DotSize.Edge, 1f).centers.size)
    }

    @Test fun `a number's width matches its widest digits`() {
        val four = dotLayout("8888", DotSize.Detail, 1f)
        assertEquals(four.width, org.openstrap.app.ui.glow.dotNumberWidth(4, DotSize.Detail).value, 1e-4f)
    }
}
