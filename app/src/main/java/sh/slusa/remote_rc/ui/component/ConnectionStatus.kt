package sh.slusa.remote_rc.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.slusa.remote_rc.core.BleConnectionManager

@Composable
@Preview
fun ConnectionStatus(
    connectionState: Int = BleConnectionManager.STATE_DISCONNECTED
) {
    val statusText = when (connectionState) {
        BleConnectionManager.STATE_DISCONNECTED -> "Status: Rozłączono"
        BleConnectionManager.STATE_SCANNING -> "Status: Skanowanie..."
        BleConnectionManager.STATE_CONNECTING -> "Status: Łączenie..."
        BleConnectionManager.STATE_CONNECTED -> "Status: Połączono!"
        else -> "Status: Nieznany"
    }

    Column(
        modifier = Modifier
            .padding(1.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusText,
            fontSize = 20.sp
        )
    }
}