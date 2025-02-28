package com.piasy.kmp.webrtc

/**
 * Created by Piasy{github.com/Piasy} on 2025-02-27.
 */
class MacAudioDeviceManager: AudioDeviceManager {
    override fun setSpeakerphoneOn(speakerOn: Boolean) {
        // do nothing for macOS
    }

    override fun start(callback: AudioDeviceManagerCallback) {
        // do nothing for macOS
    }

    override fun stop() {
        // do nothing for macOS
    }
}
