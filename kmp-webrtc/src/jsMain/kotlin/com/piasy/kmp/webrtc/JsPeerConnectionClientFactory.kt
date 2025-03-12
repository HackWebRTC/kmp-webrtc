package com.piasy.kmp.webrtc

import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.xlog.initializeConsoleLog
import org.w3c.dom.HTMLVideoElement

/**
 * Created by Piasy{github.com/Piasy} on 2025-03-11.
 */
data class JsPrivateConfig(
    internal val dummyVideoTrack: dynamic,
) : PeerConnectionClientFactory.PrivateConfig()

class JsPeerConnectionClientFactory(
    config: Config,
    errorHandler: (Int, String) -> Unit,
    private val privateConfig: JsPrivateConfig,
) : PeerConnectionClientFactory(config, errorHandler, DummyAudioDeviceManager()) {
    override fun createPeerConnectionClient(
        peerUid: String, dir: Int, hasVideo: Boolean,
        videoMaxBitrateBps: Int, videoMaxFrameRate: Int,
        callback: PeerConnectionClientCallback
    ) = JsPeerConnectionClient(
        peerUid, dir, privateConfig.dummyVideoTrack,
        videoMaxBitrateBps, videoMaxFrameRate, callback
    )

    override fun createLocalTracks() {
        JsPeerConnectionClient.startLocalStream(needVideo = true, needAudio = true)
    }

    override fun addLocalTrackRenderer(renderer: Any) {
        if (renderer is HTMLVideoElement) {
            JsPeerConnectionClient.startLocalPreview(renderer)
        }
    }

    override fun startVideoCapture() {
    }

    override fun stopVideoCapture() {
        JsPeerConnectionClient.stopLocalStream()
    }

    override fun adaptVideoOutputFormat(width: Int, height: Int, fps: Int) {
        // unsupported
    }
}

actual fun initializeWebRTC(context: Any?, fieldTrials: String, debugLog: Boolean): Boolean {
    if (PeerConnectionClientFactory.sInitialized) {
        Logging.info(PeerConnectionClientFactory.TAG, "initialize, already initialized")
        return true
    }
    initializeConsoleLog()
    PeerConnectionClientFactory.sInitialized = true
    Logging.info(PeerConnectionClientFactory.TAG, "initialize success")
    return true
}

actual fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config, errorHandler: (Int, String) -> Unit
): PeerConnectionClientFactory =
    JsPeerConnectionClientFactory(config, errorHandler, config.privateConfig as JsPrivateConfig)
