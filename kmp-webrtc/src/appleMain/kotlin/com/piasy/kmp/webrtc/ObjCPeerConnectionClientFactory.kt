package com.piasy.kmp.webrtc

import WebRTC.*
import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.xlog.initializeMarsXLog

/**
 * Created by Piasy{github.com/Piasy} on 2019-12-02.
 */
class ObjCPeerConnectionClientFactory(
    private val config: Config,
    private val screenCaptureErrorHandler: (String?) -> Unit
) : PeerConnectionClientFactory() {
    private var cameraCapturer: RTCCameraVideoCapturer? = null
    var cameraCaptureController: CFCaptureController? = null
    var screenCapturer: CFRPCapturer? = null
    var isFrontCamera = true

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
            screenCapturer = CFRPCapturer(
                hijackCapturerDelegate, screenCaptureErrorHandler, config.videoCaptureHeight, false,
                config.videoCaptureFps
            )
        } else {
            cameraCapturer = RTCCameraVideoCapturer(hijackCapturerDelegate)
            cameraCaptureController = CFCaptureController(
                cameraCapturer!!, config.initCameraFace, config.videoCaptureWidth, config.videoCaptureHeight
            )
        }
    }

    override fun adaptVideoOutputFormat(
        width: Int,
        height: Int,
        fps: Int
    ) {
        CFPeerConnectionClient.adaptVideoOutputFormat(width, height, fps)
    }
}

actual fun initializeWebRTC(appContext: Any?, fieldTrials: String, debugLog: Boolean) {
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
}

actual fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    screenCaptureErrorHandler: (String?) -> Unit
): PeerConnectionClientFactory {
    return ObjCPeerConnectionClientFactory(config, screenCaptureErrorHandler)
}
