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
        peerUid: String, dir: Int, hasVideo: Boolean,
        videoMaxBitrateBps: Int, videoMaxFrameRate: Int,
        callback: PeerConnectionClientCallback
    ): PeerConnectionClient

    abstract fun createLocalTracks()

    abstract fun addLocalTrackRenderer(renderer: Any)

    abstract fun startVideoCapture()
    abstract fun stopVideoCapture()
    open fun switchCamera(onFinished: (Boolean) -> Unit) {
        onFinished(true)
    }

    fun toggleSpeaker(speakerOn: Boolean) {
        audioDeviceManager.setSpeakerphoneOn(speakerOn)
    }

    abstract fun adaptVideoOutputFormat(width: Int, height: Int, fps: Int)

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

        const val DIR_SEND_RECV = 0
        const val DIR_SEND_ONLY = 1
        const val DIR_RECV_ONLY = 2
        const val DIR_INACTIVE = 3

        const val VIDEO_CODEC_VP8 = 1
        const val VIDEO_CODEC_VP9 = 2
        const val VIDEO_CODEC_H264_BASELINE = 3
        const val VIDEO_CODEC_H264_HIGH_PROFILE = 4
        const val VIDEO_CODEC_H265 = 5
        const val VIDEO_CODEC_AV1 = 6

        const val VIDEO_CAPTURE_IMPL_SYSTEM_CAMERA = 1
        const val VIDEO_CAPTURE_IMPL_SCREEN = 2
        const val VIDEO_CAPTURE_IMPL_FILE = 3
        const val VIDEO_CAPTURE_IMPL_APP = 4

        const val CAMERA_FACE_FRONT = 0
        const val CAMERA_FACE_BACK = 1

        // peer connection fail
        const val ERR_PC_FAIL = 1001
        const val ERR_CREATE_LOCAL_TRACKS_FAIL = 1002

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

fun Boolean.toInt() = if (this) 1 else 0

expect fun initializeWebRTC(context: Any?, fieldTrials: String, debugLog: Boolean): Boolean

expect fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    errorHandler: (Int, String) -> Unit,
): PeerConnectionClientFactory
