package com.piasy.kmp.webrtc

import WebRTC.*
import com.piasy.kmp.webrtc.utils.FieldTrial
import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.xlog.initializeMarsXLog

/**
 * Created by Piasy{github.com/Piasy} on 2019-12-02.
 */
data class ObjCPrivateConfig(
    internal val pcFactoryOption: CFPeerConnectionFactoryOption,
) : PeerConnectionClientFactory.PrivateConfig()

class ObjCPeerConnectionClientFactory(
    config: Config,
    errorHandler: (Int, String) -> Unit,
) : PeerConnectionClientFactory(config, errorHandler, IOSAudioDeviceManager()) {
    private var cameraCapturer: RTCCameraVideoCapturer? = null
    private var cameraCaptureController: CFCaptureController? = null
    private var screenCapturer: CFRPCapturer? = null
    private var isFrontCamera = false

    override fun createPeerConnectionClient(
        peerUid: String,
        dir: Int,
        hasVideo: Boolean,
        videoMaxBitrate: Int,
        videoMaxFrameRate: Int,
        callback: PeerConnectionClientCallback
    ): PeerConnectionClient {
        return ObjCPeerConnectionClient(
            peerUid, dir, hasVideo, videoMaxBitrate, videoMaxFrameRate, callback
        )
    }

    override fun createLocalTracks() {
        // OWT requires video in SDP, so must have video track
        if (CFPeerConnectionClient.createLocalTracks(true, config.screenShare) != 0) {
            logE("createLocalTracks fail: CFPeerConnectionClient.createLocalTracks fail")
            return
        }

        val hijackCapturerDelegate = CFPeerConnectionClient.getHijackCapturerDelegate()
        if (config.screenShare) {
            val screenCaptureErrorHandler: ((String?) -> Unit) = { error ->
                logE("screenCaptureError $error")
                errorHandler(ERR_SCREEN_CAPTURER_FAIL, error ?: "")
            }
            screenCapturer = CFRPCapturer(
                hijackCapturerDelegate, screenCaptureErrorHandler, config.videoCaptureHeight, false,
                config.videoCaptureFps
            )
        } else {
            cameraCapturer = RTCCameraVideoCapturer(hijackCapturerDelegate)
            cameraCaptureController = CFCaptureController(
                cameraCapturer!!, config.initCameraFace, config.videoCaptureWidth, config.videoCaptureHeight
            )
            isFrontCamera = config.initCameraFace == Config.CAMERA_FACE_FRONT
        }
    }

    override fun addLocalTrackRenderer(renderer: Any) {
        if (renderer is RTCVideoRendererProtocol) {
            CFPeerConnectionClient.addLocalTrackRenderer(renderer)
        }
    }

    override fun startVideoCapture() {
        cameraCaptureController?.startCapture {
            if (it != null) {
                errorHandler(ERR_CAMERA_CAPTURER_FAIL, it.localizedDescription)
            }
        }
        screenCapturer?.startCapture()
    }

    override fun stopVideoCapture() {
        cameraCaptureController?.stopCapture()
        screenCapturer?.stopCapture()
    }

    override fun switchCamera(onFinished: (Boolean) -> Unit) {
        isFrontCamera = !isFrontCamera
        cameraCaptureController?.switchCamera {
            if (it != null) {
                errorHandler(ERR_CAMERA_CAPTURER_FAIL, it.localizedDescription)
            } else {
                onFinished(isFrontCamera)
            }
        }
    }

    override fun adaptVideoOutputFormat(
        width: Int,
        height: Int,
        fps: Int
    ) {
        CFPeerConnectionClient.adaptVideoOutputFormat(width, height, fps)
    }

    override fun destroyPeerConnectionFactory() {
        super.destroyPeerConnectionFactory()
        CFPeerConnectionClient.destroyPeerConnectionFactory()
    }
}

actual fun initializeWebRTC(context: Any?, fieldTrials: String, debugLog: Boolean): Boolean {
    if (PeerConnectionClientFactory.sInitialized) {
        Logging.info(
            PeerConnectionClientFactory.TAG,
            "initialize ${CFPeerConnectionClient.versionName()}, already initialized"
        )
        return true
    }
    initializeMarsXLog(if (debugLog) Logging.LEVEL_DEBUG else Logging.LEVEL_INFO, "webrtc", debugLog)
    RTCCallbackLogger().startWithMessageAndSeverityHandler { log, severity ->
        if (log == null) {
            return@startWithMessageAndSeverityHandler
        }
        when (severity) {
            RTCLoggingSeverity.RTCLoggingSeverityVerbose -> Logging.debug("webrtc", log)
            RTCLoggingSeverity.RTCLoggingSeverityInfo, RTCLoggingSeverity.RTCLoggingSeverityWarning ->
                Logging.info("webrtc", log)

            RTCLoggingSeverity.RTCLoggingSeverityError -> Logging.error("webrtc", log)
            else -> {}
        }
    }
    CFPeerConnectionClient.initialize(FieldTrial.fieldTrialsStringToMap(fieldTrials))
    Logging.info(PeerConnectionClientFactory.TAG, "initialize ${CFPeerConnectionClient.versionName()}")
    PeerConnectionClientFactory.sInitialized = true
    return true
}

actual fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    errorHandler: (Int, String) -> Unit,
): PeerConnectionClientFactory {
    CFPeerConnectionClient.createPeerConnectionFactory((config.privateConfig as ObjCPrivateConfig).pcFactoryOption)
    return ObjCPeerConnectionClientFactory(config, errorHandler)
}
