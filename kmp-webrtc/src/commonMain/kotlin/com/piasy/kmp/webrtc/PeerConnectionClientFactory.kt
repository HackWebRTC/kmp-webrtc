package com.piasy.kmp.webrtc

import com.piasy.kmp.xlog.Logging

/**
 * Created by Piasy{github.com/Piasy} on 2025-02-16.
 */
abstract class PeerConnectionClientFactory(
    protected val config: Config,
    protected val errorHandler: (Int, String) -> Unit,
    private val audioDeviceManager: AudioDeviceManager,
) {
    data class Config(
        val videoCaptureImpl: Int,
        val videoCaptureWidth: Int,
        val videoCaptureHeight: Int,
        val videoCaptureFps: Int,
        val initCameraFace: Int,
        val privateConfig: PrivateConfig,
    ) {
        internal val screenShare: Boolean
            get() = videoCaptureImpl == VIDEO_CAPTURE_IMPL_SCREEN

        companion object {
            const val VIDEO_CAPTURE_IMPL_SYSTEM_CAMERA = 1
            const val VIDEO_CAPTURE_IMPL_SCREEN = 2
            const val VIDEO_CAPTURE_IMPL_APP = 4

            const val CAMERA_FACE_FRONT = 0
            const val CAMERA_FACE_BACK = 1
        }
    }

    open class PrivateConfig

    init {
        audioDeviceManager.start(object : AudioDeviceManagerCallback {
            override fun onAudioDeviceChanged(device: AudioDevice) {
                // do nothing now
            }
        })
    }

    abstract fun createPeerConnectionClient(
        peerUid: String,
        dir: Int,
        hasVideo: Boolean,
        videoMaxBitrate: Int,
        videoMaxFrameRate: Int,
        callback: PeerConnectionClientCallback
    ): PeerConnectionClient

    abstract fun createLocalTracks()

    abstract fun addLocalTrackRenderer(renderer: Any)

    abstract fun startVideoCapture()
    abstract fun stopVideoCapture()
    abstract fun switchCamera(onFinished: (Boolean) -> Unit)

    fun toggleSpeaker(speakerOn: Boolean) {
        audioDeviceManager.setSpeakerphoneOn(speakerOn)
    }

    abstract fun adaptVideoOutputFormat(
        width: Int,
        height: Int,
        fps: Int
    )

    open fun destroyPeerConnectionFactory() {
        audioDeviceManager.stop()
    }

    protected fun logI(content: String) {
        Logging.info("$TAG@${hashCode()}", content)
    }

    protected fun logE(content: String) {
        Logging.error("$TAG@${hashCode()}", content)
    }

    companion object {
        internal const val TAG = "PCClientFactory"
        internal var sInitialized = false

        // peer connection fail
        const val ERR_PC_FAIL = 1001

        // create video capturer fail
        const val ERR_VIDEO_CAPTURER_CREATE_FAIL = 1101

        // camera capturer fail
        const val ERR_CAMERA_CAPTURER_FAIL = 1102

        // screen capturer fail
        const val ERR_SCREEN_CAPTURER_FAIL = 1103

        // video encoder fail
        const val ERR_VIDEO_ENCODER_ERROR = 1131

        // audio capture fail
        const val ERR_AUDIO_CAPTURE_ERROR = 1201

        // audio playback fail
        const val ERR_AUDIO_PLAYBACK_ERROR = 1221
    }
}

expect fun initializeWebRTC(context: Any?, fieldTrials: String, debugLog: Boolean): Boolean

expect fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    errorHandler: (Int, String) -> Unit,
): PeerConnectionClientFactory
