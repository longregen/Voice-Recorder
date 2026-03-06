package org.fossify.voicerecorder.helpers

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BluetoothScoManagerTest {

    private lateinit var audioManager: AudioManager
    private lateinit var scoManager: BluetoothScoManager

    @Before
    fun setUp() {
        audioManager = RuntimeEnvironment.getApplication()
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager
        scoManager = BluetoothScoManager(audioManager)
    }

    @Test
    fun `isBluetoothDevice returns true for SCO device`() {
        val device = createMockDevice(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        assertTrue(scoManager.isBluetoothDevice(device))
    }

    @Test
    fun `isBluetoothDevice returns true for A2DP device`() {
        val device = createMockDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        assertTrue(scoManager.isBluetoothDevice(device))
    }

    @Test
    fun `isBluetoothDevice returns false for builtin mic`() {
        val device = createMockDevice(AudioDeviceInfo.TYPE_BUILTIN_MIC)
        assertFalse(scoManager.isBluetoothDevice(device))
    }

    @Test
    fun `isBluetoothDevice returns false for USB device`() {
        val device = createMockDevice(AudioDeviceInfo.TYPE_USB_DEVICE)
        assertFalse(scoManager.isBluetoothDevice(device))
    }

    @Test
    fun `isBluetoothDevice returns false for wired headset`() {
        val device = createMockDevice(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        assertFalse(scoManager.isBluetoothDevice(device))
    }

    @Test
    fun `start activates SCO and sets isActive`() {
        assertFalse(scoManager.isActive)
        scoManager.start()
        assertTrue(scoManager.isActive)
    }

    @Test
    fun `stop deactivates SCO and clears isActive`() {
        scoManager.start()
        assertTrue(scoManager.isActive)

        scoManager.stop()
        assertFalse(scoManager.isActive)
    }

    @Test
    fun `stop is no-op when not active`() {
        assertFalse(scoManager.isActive)
        scoManager.stop() // should not throw
        assertFalse(scoManager.isActive)
    }

    @Test
    fun `start is idempotent when already active`() {
        scoManager.start()
        assertTrue(scoManager.isActive)

        scoManager.start() // second call should not throw
        assertTrue(scoManager.isActive)
    }

    @Test
    fun `start then stop then start works correctly`() {
        scoManager.start()
        assertTrue(scoManager.isActive)

        scoManager.stop()
        assertFalse(scoManager.isActive)

        scoManager.start()
        assertTrue(scoManager.isActive)
    }

    @Test
    fun `start sets bluetoothScoOn on AudioManager`() {
        scoManager.start()
        assertTrue(audioManager.isBluetoothScoOn)
    }

    @Test
    fun `stop clears bluetoothScoOn on AudioManager`() {
        scoManager.start()
        assertTrue(audioManager.isBluetoothScoOn)

        scoManager.stop()
        assertFalse(audioManager.isBluetoothScoOn)
    }

    private fun createMockDevice(type: Int): AudioDeviceInfo {
        val device = mock<AudioDeviceInfo>()
        whenever(device.type).thenReturn(type)
        return device
    }
}
