package sh.slusa.remote_rc.service

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import sh.slusa.remote_rc.core.BleConnectionManager

class LinearControlSystemService (private val bleConnectionManager: BleConnectionManager) : IControlSystemService {
    private val TAG: String = "ControlSystemService"

    private fun toByte(bool: Boolean): Byte {
        return if (bool) { 0x1 } else { 0x0 }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun accelerate(enable: Boolean, value: Int) {
        Log.d(TAG, "Accelerating...")
        val byteToSend = if (enable) value.toByte() else 127;
        bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x1, byteToSend))
    }

//    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
//    override fun backward(enable: Boolean) {
//        Log.d(TAG, "Backwards...")
//        bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x2, toByte(enable)))
//    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun steer(value: Int) {
        // Something to do...
        //bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x1, value.toByte()))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun emergencyStop() {
        Log.d(TAG, "Emergency stop...")
        bleConnectionManager.writeDataToCharacteristic(byteArrayOf(0x0, 0x1, 0x0, 0x0))
    }
}
