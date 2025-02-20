package com.piasy.kmp.webrtc

import WebRTC.*
import com.piasy.kmp.webrtc.AudioDevice.BLUETOOTH
import com.piasy.kmp.webrtc.AudioDevice.EARPIECE
import com.piasy.kmp.webrtc.AudioDevice.NONE
import com.piasy.kmp.webrtc.AudioDevice.SPEAKER_PHONE
import com.piasy.kmp.webrtc.AudioDevice.WIRED_HEADSET
import platform.darwin.NSObject

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-30.
 */
private class ObjCAudioDeviceManagerCallback(private val realCallback: AudioDeviceManagerCallback) :
    CFAudioDeviceManagerDelegateProtocol, NSObject() {
    override fun onAudioDeviceChanged(audioDevice: CFAudioDevice) {
        realCallback.onAudioDeviceChanged(
            when (audioDevice) {
                CFAudioDevice.CF_SPEAKER_PHONE -> SPEAKER_PHONE
                CFAudioDevice.CF_WIRED_HEADSET -> WIRED_HEADSET
                CFAudioDevice.CF_EARPIECE -> EARPIECE
                CFAudioDevice.CF_BLUETOOTH -> BLUETOOTH
                else -> NONE
            }
        )
    }
}

class IOSAudioDeviceManager : AudioDeviceManager {
    private val realManager = CFAudioDeviceManager()

    override fun setSpeakerphoneOn(speakerOn: Boolean) {
        realManager.setSpeakerphoneOn(speakerOn)
    }

    override fun start(callback: AudioDeviceManagerCallback) {
        realManager.start(ObjCAudioDeviceManagerCallback(callback))
    }

    override fun stop() {
        realManager.stop()
    }
}
