package com.anjo.statisticsui

import androidx.compose.material3.SnackbarHostState
import com.anjo.statisticsui.ui.common.deleteWithUndo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DeleteWithUndoTest :
    FunSpec({

        test("undoSkipsTheDelete") {
            // given
            val host = SnackbarHostState()
            var deletes = 0

            // when
            coroutineScope {
                val result = async { host.deleteWithUndo("Deleted") { deletes++ } }
                host.awaitSnackbar().performAction()
                result.await() shouldBe false
            }

            // then
            deletes shouldBe 0
        }

        test("dismissDeletes") {
            // given
            val host = SnackbarHostState()
            var deletes = 0

            // when
            coroutineScope {
                val result = async { host.deleteWithUndo("Deleted") { deletes++ } }
                host.awaitSnackbar().dismiss()
                result.await() shouldBe true
            }

            // then
            deletes shouldBe 1
        }

        test("leavingTheScreenDeletes") {
            // given
            val host = SnackbarHostState()
            var deletes = 0

            // when
            coroutineScope {
                val job = launch { host.deleteWithUndo("Deleted") { deletes++ } }
                host.awaitSnackbar()
                job.cancelAndJoin()
            }

            // then
            deletes shouldBe 1
        }
    })
