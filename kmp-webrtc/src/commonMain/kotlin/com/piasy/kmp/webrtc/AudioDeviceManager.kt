package com.piasy.kmp.webrtc

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-27.
 */
interface AudioDeviceManager {
  fun setSpeakerphoneOn(speakerOn: Boolean)

  fun start(callback: AudioDeviceManagerCallback)

  fun stop()
}

enum class AudioDevice {
  SPEAKER_PHONE,
  WIRED_HEADSET,
  EARPIECE,
  BLUETOOTH,
  NONE,
}

enum class SpeakerphoneMode {
  AUTO,
  OPEN,
  CLOSE,
}

interface AudioDeviceManagerCallback {
  fun onAudioDeviceChanged(device: AudioDevice)
}
