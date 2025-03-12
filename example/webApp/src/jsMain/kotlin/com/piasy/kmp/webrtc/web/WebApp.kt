package com.piasy.kmp.webrtc.web

import com.piasy.kmp.webrtc.*
import com.piasy.kmp.webrtc.data.IceCandidate
import com.piasy.kmp.webrtc.data.RtcStatsReport
import com.piasy.kmp.webrtc.data.SessionDescription
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.Event

var isLoopback = false
var loopback: HTMLButtonElement? = null

var pcClientFactory: PeerConnectionClientFactory? = null
// on web, we can't disable encryption, so we need two pc clients.
var localPcClient: PeerConnectionClient? = null
var remotePcClient: PeerConnectionClient? = null

fun main() {
    window.onload = {
        loopback = document.getElementById("loopbackButton") as HTMLButtonElement
        loopback?.addEventListener("click", ::handleLoopbackClick)
    }
}

fun handleLoopbackClick(event: Event) {
    isLoopback = !isLoopback
    loopback?.innerHTML = if (isLoopback) "Stop" else "Loopback"
    if (isLoopback) {
        startLoopback()
    } else {
        stopLoopback()
    }
}

fun startLoopback() {
    // 1. initialize
    initializeWebRTC(null, "", true)

    // 2. create PcClientFactory
    val privateConfig = JsPrivateConfig(js("createBlackStream")())
    val config = PeerConnectionClientFactory.Config(
        PeerConnectionClientFactory.VIDEO_CAPTURE_IMPL_SYSTEM_CAMERA, 1280, 720, 30,
        PeerConnectionClientFactory.CAMERA_FACE_FRONT, privateConfig,
    )
    pcClientFactory = createPeerConnectionClientFactory(config) { code, msg ->
        println("PeerConnectionClientFactory error: $code, $msg")
    }

    // 3. create local tracks
    pcClientFactory?.createLocalTracks()

    // 4. add local preview & start camera capture
    pcClientFactory?.addLocalTrackRenderer(document.getElementById("localVideo")!!)
    pcClientFactory?.startVideoCapture()

    // 5. create PcClient
    localPcClient = pcClientFactory?.createPeerConnectionClient(
        "test_local", PeerConnectionClientFactory.DIR_SEND_ONLY, true,
        800_000, 30, localPcClientCallback
    )
    remotePcClient = pcClientFactory?.createPeerConnectionClient(
        "test_remote", PeerConnectionClientFactory.DIR_RECV_ONLY, true,
        800_000, 30, remotePcClientCallback
    )

    // 6. create pc
    localPcClient?.createPeerConnection(emptyList())
    remotePcClient?.createPeerConnection(emptyList())

    // 7. create offer
    localPcClient?.createOffer()
}

val localPcClientCallback = object : PeerConnectionClientCallback {
    override fun onLocalDescription(peerUid: String, sdp: SessionDescription) {
        // 8.1. send offer to remote
        remotePcClient?.setRemoteDescription(sdp)
        remotePcClient?.createAnswer()
    }

    override fun onIceCandidate(peerUid: String, candidate: IceCandidate) {
        // 9. send ice candidate to remote, get ice candidate from remote, add ice candidate
        remotePcClient?.addIceCandidate(candidate)
    }

    override fun onIceConnected(peerUid: String) {
    }

    override fun onPreferCodecs(peerUid: String, sdp: String): String {
        return sdp
    }

    override fun onIceCandidatesRemoved(peerUid: String, candidates: List<IceCandidate>) {
    }

    override fun onPeerConnectionStatsReady(peerUid: String, report: RtcStatsReport) {
    }

    override fun onIceDisconnected(peerUid: String) {
    }

    override fun onError(peerUid: String, code: Int) {
    }
}

val remotePcClientCallback = object : PeerConnectionClientCallback {
    override fun onLocalDescription(peerUid: String, sdp: SessionDescription) {
        // 8.2. get answer from remote, and set answer
        localPcClient?.setRemoteDescription(sdp);
    }

    override fun onIceCandidate(peerUid: String, candidate: IceCandidate) {
        // 9. send ice candidate to remote, get ice candidate from remote, add ice candidate
        localPcClient?.addIceCandidate(candidate);
    }

    override fun onIceConnected(peerUid: String) {
        // 10. on ice connected, add renderer for remote stream
        remotePcClient?.addRemoteTrackRenderer(document.getElementById("remoteVideo")!!)
    }

    override fun onPreferCodecs(peerUid: String, sdp: String): String {
        return sdp
    }

    override fun onIceCandidatesRemoved(peerUid: String, candidates: List<IceCandidate>) {
    }

    override fun onPeerConnectionStatsReady(peerUid: String, report: RtcStatsReport) {
    }

    override fun onIceDisconnected(peerUid: String) {
    }

    override fun onError(peerUid: String, code: Int) {
    }
}

fun stopLoopback() {
    pcClientFactory?.stopVideoCapture()
    localPcClient?.close()
    remotePcClient?.close()
    pcClientFactory?.destroyPeerConnectionFactory()
    pcClientFactory = null
    localPcClient = null
    remotePcClient = null
}
