package sh.slusa.remote_rc

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.slusa.remote_rc.core.BleConnectionManager
import sh.slusa.remote_rc.service.ControlSystemService
import sh.slusa.remote_rc.ui.screen.MainScreen

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private lateinit var bleConnectionManager: BleConnectionManager
    private lateinit var controlSystemService: ControlSystemService

    private val requiredPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    private val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                Log.d(TAG, "Uprawnienia przyznane.")
                checkBluetoothEnabled()
            } else {
                Log.w(TAG, "Użytkownik odmówił uprawnień.")
                showToast("Uprawnienia są wymagane do działania aplikacji.")
            }
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "Bluetooth włączony przez użytkownika.")
                startBleAutoConnect()
            } else {
                Log.w(TAG, "Użytkownik nie włączył Bluetooth.")
                showToast("Bluetooth musi być włączony.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleConnectionManager = BleConnectionManager(this)
        controlSystemService = ControlSystemService(bleConnectionManager)

        checkAndRequestBlePermissions()
        observeConnectionState()

        enableEdgeToEdge()
        setContent {
            val connectionState by bleConnectionManager.connectionState.collectAsState()
            MainScreen(connectionState = connectionState, controlSystemService = controlSystemService)
        }
    }

    override fun onStart() {
        super.onStart()
        checkAndRequestBlePermissions()
    }

    override fun onStop() {
        super.onStop()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        bleConnectionManager.stopAutoScan()
        bleConnectionManager.close()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        bleConnectionManager.stopAutoScan()
        bleConnectionManager.close()
    }

    private fun checkAndRequestBlePermissions() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            Log.i(TAG, "Prośba o uprawnienia BLE...")
            requestPermissionsLauncher.launch(requiredPermissions)
            return
        }

        Log.d(TAG, "Uprawnienia już przyznane.")
        checkBluetoothEnabled()
    }

    private fun checkBluetoothEnabled() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            showToast("To urządzenie nie wspiera Bluetooth.")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.i(TAG, "Bluetooth jest wyłączony. Prośba o włączenie...")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return
        }

        Log.d(TAG, "Bluetooth jest już włączony.")
        lifecycleScope.launch {
            delay(3000)
            startBleAutoConnect()
        }
    }

    private fun startBleAutoConnect() {
        Log.i(TAG, "Wszystko gotowe. Rozpoczynam automatyczne skanowanie i łączenie.")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bleConnectionManager.startAutoConnectScan()
    }

    private fun observeConnectionState() {
        lifecycleScope.launch {
            bleConnectionManager.connectionState.collect { state ->
                when (state) {
                    BleConnectionManager.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Status: Rozłączono")
                    }
                    BleConnectionManager.STATE_SCANNING -> {
                        Log.i(TAG, "Status: Skanowanie...")
                    }
                    BleConnectionManager.STATE_CONNECTING -> {
                        Log.i(TAG, "Status: Łączenie...")
                    }
                    BleConnectionManager.STATE_CONNECTED -> {
                        Log.i(TAG, "Status: Połączono!")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}


