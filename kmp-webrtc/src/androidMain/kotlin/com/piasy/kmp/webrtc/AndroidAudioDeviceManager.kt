package com.piasy.kmp.webrtc

import android.content.Context
import android.media.AudioManager
import com.piasy.avconf.audio.AppRTCAudioManager
import com.piasy.avconf.audio.AppRTCAudioManager.AudioDevice.BLUETOOTH
import com.piasy.avconf.audio.AppRTCAudioManager.AudioDevice.EARPIECE
import com.piasy.avconf.audio.AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
import com.piasy.avconf.audio.AppRTCAudioManager.AudioDevice.WIRED_HEADSET

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-27.
 */
class AndroidAudioDeviceManager(private val context: Context, speakerPhoneMode: SpeakerphoneMode) :
    AudioDeviceManager {
    private val audioManager = AppRTCAudioManager.create(context, speakerphoneMode(speakerPhoneMode))
    private var speakerOn = false

    private fun speakerphoneMode(mode: SpeakerphoneMode) = when (mode) {
        SpeakerphoneMode.AUTO -> AppRTCAudioManager.SPEAKERPHONE_AUTO
        SpeakerphoneMode.OPEN -> AppRTCAudioManager.SPEAKERPHONE_TRUE
        SpeakerphoneMode.CLOSE -> AppRTCAudioManager.SPEAKERPHONE_FALSE
    }

    override fun setSpeakerphoneOn(speakerOn: Boolean) {
        this.speakerOn = speakerOn
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!audioManager.isWiredHeadsetOn) {
            audioManager.isSpeakerphoneOn = speakerOn
        }
    }

    override fun start(callback: AudioDeviceManagerCallback) {
        audioManager.start { selectedAudioDevice, _ ->
            when (selectedAudioDevice) {
                SPEAKER_PHONE -> {
                    setSpeakerphoneOn(speakerOn)
                    callback.onAudioDeviceChanged(AudioDevice.SPEAKER_PHONE)
                }

                WIRED_HEADSET -> callback.onAudioDeviceChanged(AudioDevice.WIRED_HEADSET)
                EARPIECE -> callback.onAudioDeviceChanged(AudioDevice.EARPIECE)
                BLUETOOTH -> callback.onAudioDeviceChanged(AudioDevice.BLUETOOTH)
                else -> callback.onAudioDeviceChanged(AudioDevice.NONE)
            }
        }
        audioManager.changeAudioRoute(true)
        if (audioManager.selectedAudioDevice == SPEAKER_PHONE) {
            setSpeakerphoneOn(true)
        }
    }

    override fun stop() {
        audioManager.stop()
    }
}
