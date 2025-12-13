package sh.slusa.remote_rc.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ControlViewModel : ViewModel() {

    private val _accelerates = MutableStateFlow(false)
    val isAccelerates: StateFlow<Boolean> = _accelerates.asStateFlow()

    private val _backwards = MutableStateFlow(false)
    val isBackwards: StateFlow<Boolean> = _backwards.asStateFlow()

    private val _steer = MutableStateFlow(false)
    val isSteeringChanged: StateFlow<Boolean> = _steer.asStateFlow()

    private val _steerLeft = MutableStateFlow(false)
    val isSteeringLeft: StateFlow<Boolean> = _steerLeft.asStateFlow()

    private val _steerRight = MutableStateFlow(false)
    val isSteeringRight: StateFlow<Boolean> = _steerRight.asStateFlow()

    private val _stop = MutableStateFlow(false)
    val isStopping: StateFlow<Boolean> = _stop.asStateFlow()

    fun accelerate(enable: Boolean) {
        _stop.value = false
        _accelerates.value = enable
        _backwards.value = false
    }
    fun backwards(enable: Boolean) {
        _stop.value = false
        _backwards.value = enable
        _accelerates.value = false
    }
    fun steerLeft(enable: Boolean) {
        _stop.value = false
        _steerLeft.value = enable
        _steerRight.value = false
    }
    fun steerRight(enable: Boolean) {
        _stop.value = false
        _steerRight.value = enable
        _steerLeft.value = false
    }

    fun stop() {
        _stop.value = true
    }
}