package org.fossify.voicerecorder.helpers

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

class BluetoothScoManager(private val audioManager: AudioManager) {

    var isActive: Boolean = false
        private set

    fun isBluetoothDevice(device: AudioDeviceInfo): Boolean {
        return device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
    }

    fun start(device: AudioDeviceInfo? = null) {
        if (isActive) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            device?.let { audioManager.setCommunicationDevice(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
        }
        isActive = true
    }

    fun stop() {
        if (!isActive) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
        }
        isActive = false
    }
}
