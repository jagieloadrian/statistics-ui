package com.anjo.statisticsui.presentation.configurationchart.temperature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anjo.statisticsui.model.dto.TemperatureDevice
import com.anjo.statisticsui.service.rest.TemperatureApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.collections.emptyList
import com.anjo.statisticsui.model.Result
import com.anjo.statisticsui.model.Status

@KoinViewModel
class TemperatureRunsViewModel(
    private val temperatureApi: TemperatureApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel()  {

    private val _devices = MutableStateFlow(Result<List<TemperatureDevice>>(data = emptyList()))
    val devices: StateFlow<Result<List<TemperatureDevice>>> = _devices.asStateFlow()

    fun getDevices() {
        viewModelScope.launch(ioDispatcher) {
            _devices.update {
                val data = temperatureApi.getTemperatureDevices()
                it.copy(status = Status.SUCCESS, data = data)
            }
        }
    }
}