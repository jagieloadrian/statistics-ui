package com.anjo.statisticsui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.anjo.statisticsui.ui.theme.ACCENTS
import com.anjo.statisticsui.ui.theme.accentScheme
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe

private fun contrast(
    a: Color,
    b: Color,
): Float {
    val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (light + 0.05f) / (dark + 0.05f)
}

class AccentSchemeTest :
    FunSpec({

        test("everyAccentKeepsTextContrast") {
            ACCENTS.size shouldBeGreaterThanOrEqualTo 16
            for (accent in ACCENTS.keys) {
                for (dark in listOf(false, true)) {
                    val s = accentScheme(accent, dark)
                    listOf(
                        s.primary to s.onPrimary,
                        s.primaryContainer to s.onPrimaryContainer,
                        s.secondaryContainer to s.onSecondaryContainer,
                        s.tertiaryContainer to s.onTertiaryContainer,
                        s.error to s.onError,
                        s.surface to s.onSurface,
                        s.surfaceContainerHighest to s.onSurfaceVariant,
                    ).forEach { (background, text) ->
                        withClue("$accent dark=$dark") { contrast(background, text) shouldBeGreaterThanOrEqualTo 4.5f }
                    }
                }
            }
        }

        test("unknownAccentFallsBackToTeal") {
            accentScheme("plaid", dark = false).primary shouldBe accentScheme("teal", dark = false).primary
        }
    })
