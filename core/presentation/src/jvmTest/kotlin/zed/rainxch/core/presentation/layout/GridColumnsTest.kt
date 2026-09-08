package zed.rainxch.core.presentation.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class GridColumnsTest {

    @Test
    fun columnCountMatchesFormulaForAnchorCases() {
        val cases = listOf(
            Triple(10, 270, 1),
            Triple(11, 270, 1),
            Triple(190, 270, 1),
            Triple(195, 270, 1),
            Triple(270, 270, 1),
            Triple(289, 550, 1),
            Triple(345, 550, 1),
            Triple(400, 270, 2),
            Triple(400, 550, 1),
            Triple(570, 550, 1),
            Triple(571, 550, 2),
            Triple(588, 550, 2),
            Triple(588, 270, 3),
            Triple(640, 550, 2),
            Triple(700, 550, 2),
            Triple(700, 270, 3),
            Triple(820, 550, 2),
            Triple(820, 270, 3),
            Triple(1280, 550, 3),
            Triple(1280, 270, 5),
            Triple(3440, 270, 13),
            Triple(3440, 550, 7),
        )
        for ((width, maxCard, expected) in cases) {
            assertEquals(
                expected,
                gridColumnCount(width.toFloat(), maxCard.toFloat()),
                "width=$width maxCard=$maxCard",
            )
        }
    }

    @Test
    fun widthAtOrBelowLowerBoundClampsToOne() {
        assertEquals(1, gridColumnCount(0f, 270f))
        assertEquals(1, gridColumnCount(-5f, 270f))
        assertEquals(1, gridColumnCount(10f, 270f))
        assertEquals(1, gridColumnCount(0f, 550f))
    }
}
