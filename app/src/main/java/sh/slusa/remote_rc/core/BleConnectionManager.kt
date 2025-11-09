package sh.slusa.remote_rc.core

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sh.slusa.remote_rc.config.Connection
import java.util.LinkedList
import java.util.Queue

class BleConnectionManager(private val context: Context) {

    private val TAG = "BleConnectionManager"

    companion object {
        const val STATE_DISCONNECTED = 0
        const val STATE_SCANNING = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
    }

    private val _connectionState = MutableStateFlow(STATE_DISCONNECTED)

    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false

    private val writeQueue: Queue<ByteArray> = LinkedList()
    private var isWriting = false

    private val scanFilter = ScanFilter.Builder()
        .setDeviceAddress("FF:E4:05:1A:8F:FF")
//        .setServiceUuid(ParcelUuid(Connection.STEERING_SERVICE_UUID))
        .build()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val autoConnectScanCallback = object : ScanCallback() {

        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "Coś znaleziono!");

            Log.i(TAG, "Znaleziono urządzenie pasujące do UUID: ${result.device.address}")
            stopAutoScan() // Znaleźliśmy, przestań skanować
            connect(result.device.address) // Rozpocznij łączenie
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Skanowanie BLE nie powiodło się, błąd: $errorCode")
            _connectionState.value = STATE_DISCONNECTED
            isScanning = false
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startAutoConnectScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e(TAG, "Brak uprawnienia BLUETOOTH_SCAN")
            return
        }
        if (isScanning || _connectionState.value != STATE_DISCONNECTED) {
            Log.w(TAG, "Skanowanie już trwa lub jesteśmy połączeni.")
            return
        }

        bluetoothLeScanner!!.startScan(
            listOf(scanFilter),
            scanSettings,
            autoConnectScanCallback)
        Log.i(TAG, "Rozpoczęto skanowanie")
        _connectionState.value = STATE_SCANNING
        isScanning = true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopAutoScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e(TAG, "Brak uprawnienia BLUETOOTH_SCAN")
            return
        }
        if (!isScanning) return

        Log.i(TAG, "Zatrzymuję skanowanie.")
        bluetoothLeScanner?.stopScan(autoConnectScanCallback)
        isScanning = false
        if (_connectionState.value == STATE_SCANNING) {
            _connectionState.value = STATE_DISCONNECTED
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.i(TAG, "Połączono z $deviceAddress")
                    bluetoothGatt = gatt
                    // Po połączeniu, odkryj usługi (to jest "konfiguracja klienta")
                    gatt.discoverServices()
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.i(TAG, "Rozłączono z $deviceAddress")
                    _connectionState.value = STATE_DISCONNECTED
                    gatt.close()
                }
            } else {
                Log.w(TAG, "Błąd GATT ($status) podczas łączenia z $deviceAddress. Rozłączam.")
                _connectionState.value = STATE_DISCONNECTED
                gatt.close()
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Odkryto usługi! Połączenie kompletne.")
                _connectionState.value = STATE_CONNECTED // Gotowe!
            } else {
                Log.w(TAG, "Odkrywanie usług nie powiodło się, status: $status. Rozłączam.")
                disconnect()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid == Connection.STEERING_CHARACTERISTIC_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Zapisano dane pomyślnie: ${characteristic.value?.joinToString()}")
                } else {
                    Log.w(TAG, "Zapis danych nie powiódł się, status: $status")
                }

                isWriting = false
                processWriteQueue(characteristic)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connect(deviceAddress: String) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter nie jest dostępny.")
            return
        }
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG, "Brak uprawnienia BLUETOOTH_CONNECT")
            return
        }

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device == null) {
            Log.w(TAG, "Nie znaleziono urządzenia o adresie $deviceAddress")
            return
        }

        Log.i(TAG, "Próba połączenia GATT z $deviceAddress...")
        _connectionState.value = STATE_CONNECTING

        device.connectGatt(context, false, gattCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG, "Brak uprawnienia BLUETOOTH_CONNECT")
            return
        }
        Log.i(TAG, "Rozłączanie...")
        bluetoothGatt?.disconnect()
        _connectionState.value = STATE_DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    fun writeDataToCharacteristic(data: ByteArray) {
        if (_connectionState.value != STATE_CONNECTED || bluetoothGatt == null) {
            Log.w(TAG, "Nie połączono. Nie można dodać danych do kolejki.")
            return
        }

        synchronized(writeQueue) {
            writeQueue.add(data)
            Log.d(TAG, "Dodano do kolejki, rozmiar: ${writeQueue.size}")
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG, "Brak uprawnienia BLUETOOTH_CONNECT")
            return
        }

        if (_connectionState.value != STATE_CONNECTED || bluetoothGatt == null) {
            Log.w(TAG, "Nie połączono. Nie można wysłać danych.")
            return
        }

        val service = bluetoothGatt!!.getService(Connection.STEERING_SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "Nie znaleziono usługi ${Connection.STEERING_SERVICE_UUID}")
            return
        }

        val characteristic = service.getCharacteristic(Connection.STEERING_CHARACTERISTIC_UUID)
        if (characteristic == null) {
            Log.e(TAG, "Nie znaleziono charakterystyki ${Connection.STEERING_CHARACTERISTIC_UUID}")
            return
        }

        processWriteQueue(characteristic)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun processWriteQueue(characteristic: BluetoothGattCharacteristic) {
        synchronized(writeQueue) {
            if (isWriting) {
                Log.d(TAG, "Już trwa zapis")
                return
            }

            if (writeQueue.isEmpty()) {
                Log.d(TAG, "Kolejka jest pusta")
                return
            }

            val dataToSend = writeQueue.poll()
            if (dataToSend != null) {
                isWriting = true
                Log.i(TAG, "Wysyłanie bajtów")
                internalWriteDataToCharacteristic(characteristic, dataToSend)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun internalWriteDataToCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ) {
        val writeType = when {
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0 ->
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0 ->
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else -> {
                Log.e(TAG, "Charakterystyka nie wspiera zapisu.")
                return
            }
        }

        Log.d(TAG, "Zapisuję dane do charakterystyki: ${data.joinToString()}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = bluetoothGatt!!.writeCharacteristic(characteristic, data, writeType)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Nie udało się zainicjować zapisu (API 33+). Status: $status")
            }
        } else {
            characteristic.value = data
            characteristic.writeType = writeType
            if (!bluetoothGatt!!.writeCharacteristic(characteristic)) {
                Log.e(TAG, "Nie udało się zainicjować zapisu (starsze API).")
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun close() {
        disconnect()
        stopAutoScan()

        synchronized(writeQueue) {
            writeQueue.clear()
        }
        isWriting = false
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}