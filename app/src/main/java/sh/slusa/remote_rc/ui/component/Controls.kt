package sh.slusa.remote_rc.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import sh.slusa.remote_rc.config.Connection
import sh.slusa.remote_rc.core.BleConnectionManager
import sh.slusa.remote_rc.model.ControlViewModel
import sh.slusa.remote_rc.service.ControlSystemService
import sh.slusa.remote_rc.service.IControlSystemService

@Composable
fun ControlButton(
    onPush: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    if (isPressed) {
        LaunchedEffect(Unit) {
            onPush()
        }
        DisposableEffect(Unit) {
            onDispose {
                onRelease()
            }
        }
    }

    Button(
        onClick = {},
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
    ) {
        content()
    }
}

@SuppressLint("MissingPermission")
@Composable
fun Controls(
    controlViewModel: ControlViewModel,
    connectionState: Int,
    controlSystemService: IControlSystemService,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isAccelerate by controlViewModel.isAccelerates.collectAsState()
    val isBackward by controlViewModel.isBackwards.collectAsState()
    val isSteeringLeft by controlViewModel.isSteeringLeft.collectAsState()
    val isSteeringRight by controlViewModel.isSteeringRight.collectAsState()
    val isStopping by controlViewModel.isStopping.collectAsState()

    LaunchedEffect(isAccelerate) {
        controlSystemService.accelerate(isAccelerate)
    }

    LaunchedEffect(isBackward) {
        controlSystemService.backward(isBackward)
    }

    LaunchedEffect(isSteeringLeft) {
        controlSystemService.steerLeft(isSteeringLeft)
    }

    LaunchedEffect(isSteeringRight) {
        controlSystemService.steerRight(isSteeringRight)
    }

    LaunchedEffect(isStopping) {
        controlSystemService.backward(isStopping)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
//        SteerSlider(Modifier
//            .width(300.dp)
//            .height(30.dp)
//            .padding(20.dp, 0.dp)
//        )
//        Button(onClick = {}, Modifier.padding(5.dp, 0.dp)) {
//            Text("Gaz")
//        }
//        FilledTonalButton(onClick = {},  Modifier.padding(5.dp, 0.dp)) {
//            Text("Hamulec")
//        }
        ControlButton(
            onPush = { controlViewModel.steerLeft(true) },
            onRelease = { controlViewModel.steerLeft(false) },
            enabled = connectionState == BleConnectionManager.STATE_CONNECTED && !isSteeringRight,
            modifier = modifier) {
            Text("Lewo")
        }
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            ControlButton(
                onPush = { controlViewModel.accelerate(true) },
                onRelease = { controlViewModel.accelerate(false) },
                enabled = connectionState == BleConnectionManager.STATE_CONNECTED && !isBackward,
                modifier = modifier) {
                Text("Przód")
            }
            content()
            ControlButton(
                onPush = { controlViewModel.backwards(true) },
                onRelease = { controlViewModel.backwards(false) },
                enabled = connectionState == BleConnectionManager.STATE_CONNECTED && !isAccelerate,
                modifier = modifier) {
                Text("Tył")
            }
        }
        ControlButton(
            onPush = { controlViewModel.steerRight(true) },
            onRelease = { controlViewModel.steerRight(false) },
            enabled = connectionState == BleConnectionManager.STATE_CONNECTED && !isSteeringLeft
        ) {
            Text("Prawo")
        }
    }

    LaunchedEffect(isAccelerate) {

    }
}

//@Composable
//fun SteerSlider(modifier: Modifier = Modifier): Unit {
//    var sliderPosition by remember { mutableFloatStateOf(90f) }
//    Slider(
//        valueRange = 0f..180f,
//        value = sliderPosition,
//        onValueChange = { sliderPosition = it },
//        modifier = modifier
//    )
//}