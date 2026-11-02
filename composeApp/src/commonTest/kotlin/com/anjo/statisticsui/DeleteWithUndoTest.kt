package com.anjo.statisticsui

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import com.anjo.statisticsui.ui.common.deleteWithUndo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private suspend fun SnackbarHostState.awaitSnackbar(): SnackbarData {
    while (currentSnackbarData == null) yield()
    return currentSnackbarData!!
}

class DeleteWithUndoTest :
    FunSpec({

        test("undoSkipsTheDelete") {
            val host = SnackbarHostState()
            var deletes = 0
            coroutineScope {
                val result = async { host.deleteWithUndo("Deleted") { deletes++ } }
                host.awaitSnackbar().performAction()
                result.await() shouldBe false
            }
            deletes shouldBe 0
        }

        test("dismissDeletes") {
            val host = SnackbarHostState()
            var deletes = 0
            coroutineScope {
                val result = async { host.deleteWithUndo("Deleted") { deletes++ } }
                host.awaitSnackbar().dismiss()
                result.await() shouldBe true
            }
            deletes shouldBe 1
        }

        test("leavingTheScreenDeletes") {
            val host = SnackbarHostState()
            var deletes = 0
            coroutineScope {
                val job = launch { host.deleteWithUndo("Deleted") { deletes++ } }
                host.awaitSnackbar()
                job.cancelAndJoin()
            }
            deletes shouldBe 1
        }
    })
