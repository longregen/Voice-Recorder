package org.fossify.voicerecorder.helpers

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class BluetoothScoManager(private val audioManager: AudioManager) {

    companion object {
        private const val TAG = "BluetoothScoManager"
        private const val COMMUNICATION_DEVICE_TIMEOUT_MS = 3000L
    }

    var isActive: Boolean = false
        private set

    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var deviceChangedListener: AudioManager.OnCommunicationDeviceChangedListener? = null

    fun isBluetoothDevice(device: AudioDeviceInfo): Boolean {
        val isBt = device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        if (isBt) {
            Log.d(TAG, "isBluetoothDevice: id=${device.id} type=${device.type} " +
                "productName=${device.productName} isSource=${device.isSource}")
        }
        return isBt
    }

    fun logAvailableDevices() {
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        Log.d(TAG, "Available INPUT devices (${inputs.size}):")
        inputs.forEach { d ->
            Log.d(TAG, "  id=${d.id} type=${d.type} name=${d.productName} " +
                "isSource=${d.isSource} isSink=${d.isSink}")
        }
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btOutputs = outputs.filter {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    (it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
        }
        if (btOutputs.isNotEmpty()) {
            Log.d(TAG, "Bluetooth OUTPUT devices (${btOutputs.size}):")
            btOutputs.forEach { d ->
                Log.d(TAG, "  id=${d.id} type=${d.type} name=${d.productName}")
            }
        }
    }

    /**
     * Start routing audio to the bluetooth device.
     * On Android 12+, uses setCommunicationDevice which is async.
     * The [onReady] callback is invoked when routing is confirmed (or on timeout).
     */
    fun start(device: AudioDeviceInfo? = null, onReady: (() -> Unit)? = null) {
        if (isActive) {
            Log.d(TAG, "start: already active, skipping")
            onReady?.invoke()
            return
        }

        previousAudioMode = audioManager.mode
        Log.d(TAG, "start: previousAudioMode=$previousAudioMode, setting MODE_IN_COMMUNICATION")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (device != null) {
                Log.d(TAG, "start: setCommunicationDevice id=${device.id} type=${device.type} name=${device.productName}")

                // Register listener to know when routing is complete
                if (onReady != null) {
                    val handler = Handler(Looper.getMainLooper())
                    val timeoutRunnable = Runnable {
                        Log.w(TAG, "start: communication device change timed out after ${COMMUNICATION_DEVICE_TIMEOUT_MS}ms, proceeding anyway")
                        removeDeviceChangedListener()
                        onReady()
                    }

                    val listener = AudioManager.OnCommunicationDeviceChangedListener { newDevice ->
                        Log.d(TAG, "start: communication device changed to: " +
                            "id=${newDevice?.id} type=${newDevice?.type} name=${newDevice?.productName}")
                        handler.removeCallbacks(timeoutRunnable)
                        removeDeviceChangedListener()
                        onReady()
                    }
                    deviceChangedListener = listener
                    audioManager.addOnCommunicationDeviceChangedListener(
                        { it.run() }, listener
                    )
                    handler.postDelayed(timeoutRunnable, COMMUNICATION_DEVICE_TIMEOUT_MS)
                }

                val result = audioManager.setCommunicationDevice(device)
                Log.d(TAG, "start: setCommunicationDevice returned $result")
                if (!result) {
                    Log.e(TAG, "start: setCommunicationDevice FAILED")
                    removeDeviceChangedListener()
                    onReady?.invoke()
                }
            } else {
                Log.w(TAG, "start: device is null on Android 12+")
                onReady?.invoke()
            }
        } else {
            Log.d(TAG, "start: using legacy SCO APIs")
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
            onReady?.invoke()
        }
        isActive = true
    }

    fun stop() {
        if (!isActive) return
        Log.d(TAG, "stop: cleaning up bluetooth audio routing")
        removeDeviceChangedListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
        }
        audioManager.mode = previousAudioMode
        Log.d(TAG, "stop: restored audioMode=$previousAudioMode")
        isActive = false
    }

    private fun removeDeviceChangedListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            deviceChangedListener?.let {
                audioManager.removeOnCommunicationDeviceChangedListener(it)
                deviceChangedListener = null
            }
        }
    }
}
