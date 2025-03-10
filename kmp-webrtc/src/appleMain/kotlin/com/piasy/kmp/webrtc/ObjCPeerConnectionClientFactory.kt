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

abstract class ObjCPeerConnectionClientFactory(
    config: Config,
    errorHandler: (Int, String) -> Unit,
    audioDeviceManager: AudioDeviceManager,
) : PeerConnectionClientFactory(config, errorHandler, audioDeviceManager) {
    private var cameraCapturer: RTCCameraVideoCapturer? = null
    protected var cameraCaptureController: CFCaptureController? = null

    override fun createPeerConnectionClient(
        peerUid: String, dir: Int, hasVideo: Boolean,
        videoMaxBitrate: Int, videoMaxFrameRate: Int,
        callback: PeerConnectionClientCallback
    ) = ObjCPeerConnectionClient(
        peerUid, dir, hasVideo, videoMaxBitrate, videoMaxFrameRate, callback
    )

    override fun createLocalTracks() {
        // OWT requires video in SDP, so must have video track
        val res = CFPeerConnectionClient.createLocalTracks(true, config.screenShare)
        if (res != 0) {
            logE("createLocalTracks fail: CFPeerConnectionClient.createLocalTracks fail, res $res")
            errorHandler(ERR_CREATE_LOCAL_TRACKS_FAIL, "res: $res")
            return
        }

        if (!config.screenShare) {
            cameraCapturer = RTCCameraVideoCapturer(CFPeerConnectionClient.getHijackCapturerDelegate())
            cameraCaptureController = CFCaptureController(
                cameraCapturer!!, config.initCameraFace, config.videoCaptureWidth, config.videoCaptureHeight
            )
        }
    }

    override fun startVideoCapture() {
        cameraCaptureController?.startCapture {
            if (it != null) {
                errorHandler(ERR_CAMERA_CAPTURER_FAIL, it.localizedDescription)
            }
        }
    }

    override fun stopVideoCapture() {
        cameraCaptureController?.stopCapture()
    }

    override fun addLocalTrackRenderer(renderer: Any) {
        if (renderer is RTCVideoRendererProtocol) {
            CFPeerConnectionClient.addLocalTrackRenderer(renderer)
        }
    }

    override fun adaptVideoOutputFormat(width: Int, height: Int, fps: Int) {
        CFPeerConnectionClient.adaptVideoOutputFormat(width, height, fps)
    }

    override fun destroyPeerConnectionFactory() {
        super.destroyPeerConnectionFactory()
        CFPeerConnectionClient.destroyPeerConnectionFactory()
    }

    companion object {
        internal val logger = RTCCallbackLogger()
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
    ObjCPeerConnectionClientFactory.logger.severity = if (debugLog) {
        RTCLoggingSeverity.RTCLoggingSeverityVerbose
    } else {
        RTCLoggingSeverity.RTCLoggingSeverityInfo
    }
    // only WebRTC Android have tag for log callback
    ObjCPeerConnectionClientFactory.logger.startWithMessageAndSeverityHandler { log, severity ->
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
