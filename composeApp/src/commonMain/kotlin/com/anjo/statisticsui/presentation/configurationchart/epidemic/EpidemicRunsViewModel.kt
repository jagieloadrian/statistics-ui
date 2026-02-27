package com.anjo.statisticsui.presentation.configurationchart.epidemic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anjo.statisticsui.model.Result
import com.anjo.statisticsui.model.Status
import com.anjo.statisticsui.model.dto.EpidemicShortRun
import com.anjo.statisticsui.service.rest.EpidemicApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EpidemicRunsViewModel(private val epidemicApi: EpidemicApi,
                            private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val logger = KotlinLogging.logger {}

    private val _runs = MutableStateFlow(Result<List<EpidemicShortRun>>(data = emptyList()))
    val runs = _runs.asStateFlow()

    fun fetchRuns() {
        viewModelScope.launch(ioDispatcher) {
            logger.info { "Start fetching runs..." }
            _runs.update {
                logger.info { "Fetching runs..." }
                val data = epidemicApi.getEpidemicRuns()
                it.copy(status = Status.SUCCESS, data = data)
            }
        }
    }
}