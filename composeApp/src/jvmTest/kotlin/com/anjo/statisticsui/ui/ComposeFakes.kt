package com.anjo.statisticsui.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.anjo.statisticsui.FakeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.show(
    backend: FakeBackend,
    content: @Composable () -> Unit,
) {
    val koin =
        koinApplication {
            modules(
                module {
                    single { backend.api }
                    single { backend.session }
                },
            )
        }
    setContent { KoinIsolatedContext(koin) { MaterialTheme { content() } } }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.awaitText(
    text: String,
    substring: Boolean = false,
) = waitUntilAtLeastOneExists(hasText(text, substring), 5_000)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.click(text: String) = onNode(hasText(text) and hasClickAction()).reveal().performClick()

internal fun SemanticsNodeInteraction.reveal() =
    apply { if (hasAnyAncestor(hasScrollAction()).matches(fetchSemanticsNode())) performScrollTo() }

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.type(
    label: String,
    text: String,
    index: Int = 0,
) = onAllNodes(hasText(label) and hasSetTextAction())[index].performTextReplacement(text)

@OptIn(ExperimentalTestApi::class)
internal suspend fun uiTest(
    size: Size = Size(1024f, 768f),
    block: suspend ComposeUiTest.() -> Unit,
) = withContext(Dispatchers.Swing) { runSkikoComposeUiTest(size, testTimeout = 60.seconds, block = block) }

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.pick(
    label: String,
    option: String,
    index: Int = 0,
) {
    onAllNodes(hasText(label) and hasClickAction())[index].reveal().performClick()
    onNode(hasText(option) and hasClickAction() and hasAnyAncestor(isPopup())).performClick()
}
