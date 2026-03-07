package org.fossify.voicerecorder.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.util.Log
import org.fossify.voicerecorder.extensions.config

class MediaRecorderWrapper(val context: Context) : Recorder {

    companion object {
        private const val TAG = "MediaRecorderWrapper"
    }

    private var outputParcelFileDescriptor: ParcelFileDescriptor? = null

    private var recorder = MediaRecorder(context).apply {
        setAudioSource(context.config.microphoneMode)
        setOutputFormat(context.config.getOutputFormat())
        setAudioEncoder(context.config.getAudioEncoder())
        setAudioEncodingBitRate(context.config.bitrate)
        setAudioSamplingRate(context.config.samplingRate)
        setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MediaRecorder error: what=$what extra=$extra")
        }
        setOnInfoListener { _, what, extra ->
            Log.i(TAG, "MediaRecorder info: what=$what extra=$extra")
        }
    }

    override fun setOutputFile(path: String) {
        recorder.setOutputFile(path)
    }

    override fun setOutputFile(parcelFileDescriptor: ParcelFileDescriptor) {
        outputParcelFileDescriptor?.close()
        val pFD = ParcelFileDescriptor.dup(parcelFileDescriptor.fileDescriptor)
        outputParcelFileDescriptor = pFD
        recorder.setOutputFile(pFD.fileDescriptor)
    }

    override fun setPreferredDevice(device: AudioDeviceInfo?) {
        recorder.setPreferredDevice(device)
    }

    override fun prepare() {
        recorder.prepare()
    }

    override fun start() {
        recorder.start()
    }

    override fun stop() {
        recorder.stop()
    }

    @SuppressLint("NewApi")
    override fun pause() {
        recorder.pause()
    }

    @SuppressLint("NewApi")
    override fun resume() {
        recorder.resume()
    }

    override fun release() {
        recorder.release()
        outputParcelFileDescriptor?.close()
        outputParcelFileDescriptor = null
    }

    override fun getMaxAmplitude(): Int {
        return recorder.maxAmplitude
    }
}
