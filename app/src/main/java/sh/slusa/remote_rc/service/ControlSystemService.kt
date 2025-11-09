package sh.slusa.remote_rc.service

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import sh.slusa.remote_rc.core.BleConnectionManager

class ControlSystemService (private val bleConnectionManager: BleConnectionManager) : IControlSystemService {
    private val TAG: String = "ControlSystemService"

    private fun toByte(bool: Boolean): Byte {
        return if (bool) { 0x1 } else { 0x0 }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun accelerate(enable: Boolean) {
        Log.d(TAG, "Accelerating...")
        bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x1, toByte(enable)))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun backward(enable: Boolean) {
        Log.d(TAG, "Backwards...")
        bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x2, toByte(enable)))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun steerLeft(enable: Boolean) {
        Log.d(TAG, "Steering left...")
        bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x3, toByte(enable)))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun steerRight(enable: Boolean) {
        Log.d(TAG, "Steering right...")
        bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x3, if (enable) { 0x2 } else { 0x0 }))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun emergencyStop() {
        Log.d(TAG, "Emergency stop...")
        bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x0, 0x0))
    }
}
