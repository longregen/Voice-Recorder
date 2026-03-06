package org.fossify.voicerecorder.helpers

import android.media.AudioDeviceInfo
import android.media.AudioManager

class BluetoothScoManager(private val audioManager: AudioManager) {

    var isActive: Boolean = false
        private set

    fun isBluetoothDevice(device: AudioDeviceInfo): Boolean {
        return device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
    }

    @Suppress("DEPRECATION")
    fun start() {
        if (isActive) return
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        isActive = true
    }

    @Suppress("DEPRECATION")
    fun stop() {
        if (!isActive) return
        audioManager.isBluetoothScoOn = false
        audioManager.stopBluetoothSco()
        isActive = false
    }
}
