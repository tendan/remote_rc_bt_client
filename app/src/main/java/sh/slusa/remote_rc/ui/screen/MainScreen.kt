package sh.slusa.remote_rc.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sh.slusa.remote_rc.core.BleConnectionManager
import sh.slusa.remote_rc.model.ControlViewModel
import sh.slusa.remote_rc.service.IControlSystemService
import sh.slusa.remote_rc.ui.component.ConnectionStatus
import sh.slusa.remote_rc.ui.component.Controls
import sh.slusa.remote_rc.ui.theme.RemoteRobotControlTheme


@Composable
@Preview(device = "spec:orientation=landscape,width=411dp,height=891dp")
fun PreviewMainScreen() {
    val controlSystemService = object : IControlSystemService {
        override fun accelerate(enable: Boolean, value: Int) {
        }

//        override fun backward(enable: Boolean) {
//        }

        override fun steer(value: Int) {
        }

//        override fun steerLeft(enable: Boolean) {
//        }
//
//        override fun steerRight(enable: Boolean) {
//        }

        override fun emergencyStop() {
        }

    }
    MainScreen(connectionState = BleConnectionManager.STATE_CONNECTED, controlSystemService = controlSystemService)
}

@Composable
fun MainScreen(
    connectionState: Int = BleConnectionManager.STATE_DISCONNECTED,
    controlSystemService: IControlSystemService
) {
    val controlViewModel = ControlViewModel()

    RemoteRobotControlTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { _ ->
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Controls(
                    controlViewModel = controlViewModel,
                    connectionState = connectionState,
                    controlSystemService = controlSystemService
                ) {
                    ConnectionStatus(
                        connectionState = connectionState)
                }
            }
        }
    }
}