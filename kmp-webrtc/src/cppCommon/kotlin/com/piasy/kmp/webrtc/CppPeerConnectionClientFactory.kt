package com.piasy.kmp.webrtc

import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.xlog.initializeMarsXLog
import kotlinx.cinterop.*

/**
 * Created by Piasy{github.com/Piasy} on 2019-12-02.
 */
abstract class CppPeerConnectionClientFactory(
    config: Config,
    errorHandler: (Int, String) -> Unit,
) : PeerConnectionClientFactory(config, errorHandler, DummyAudioDeviceManager()) {
    private var videoCapturer: COpaquePointer? = null

    override fun createLocalTracks() {
        logI("createLocalTracks, impl ${config.videoCaptureImpl}, " +
                "${config.videoCaptureWidth}x${config.videoCaptureHeight}@${config.videoCaptureFps}")
        videoCapturer = createVideoCapturer()
        if (videoCapturer == null) {
            logE("createLocalTracks WebRTC.PCClientVideoCapturerCreate return null")
            errorHandler(ERR_VIDEO_CAPTURER_CREATE_FAIL, "")
            return
        }

        // OWT requires video in SDP, so must have video track
        val res = WebRTC.PCClientCreateLocalTracks(videoCapturer)
        if (res != 0) {
            logE("createLocalTracks fail: WebRTC.PCClientCreateLocalTracks fail, res $res")
            errorHandler(ERR_CREATE_LOCAL_TRACKS_FAIL, "res: $res")
        }
    }

    protected abstract fun createVideoCapturer(): COpaquePointer?

    override fun addLocalTrackRenderer(renderer: Any) {
    }

    override fun startVideoCapture() {
        logI("startVideoCapture $videoCapturer")
        if (videoCapturer != null) {
            WebRTC.PCClientVideoCapturerStart(videoCapturer, config.videoCaptureImpl)
        }
    }

    override fun stopVideoCapture() {
        logI("stopVideoCapture $videoCapturer")
        if (videoCapturer != null) {
            WebRTC.PCClientVideoCapturerStop(videoCapturer, config.videoCaptureImpl)
        }
    }

    override fun adaptVideoOutputFormat(width: Int, height: Int, fps: Int) {
        logI("adaptVideoOutputFormat $videoCapturer, ${width}x${height}@$fps")
        if (videoCapturer != null) {
            WebRTC.PCClientAdaptVideoOutputFormat(videoCapturer, config.videoCaptureImpl, width, height, fps)
        }
    }

    override fun destroyPeerConnectionFactory() {
        super.destroyPeerConnectionFactory()
        WebRTC.PCClientDestroyLocalTracks()
    }
}

actual fun initializeWebRTC(context: Any?, fieldTrials: String, debugLog: Boolean): Boolean {
    if (PeerConnectionClientFactory.sInitialized) {
        Logging.info(
            PeerConnectionClientFactory.TAG,
            "initialize ${WebRTC.PCClientVersion()?.toKString()}, already initialized"
        )
        return true
    }
    initializeMarsXLog(if (debugLog) Logging.LEVEL_DEBUG else Logging.LEVEL_INFO, "webrtc", debugLog)
    setPcClientLogCallback(if (debugLog) LogSeverity.Verbose else LogSeverity.Info)
    if (WebRTC.PCClientInitialize(fieldTrials) != 0) {
        return false
    }
    Logging.info(PeerConnectionClientFactory.TAG, "initialize ${WebRTC.PCClientVersion()?.toKString()}")
    PeerConnectionClientFactory.sInitialized = true
    return true
}
