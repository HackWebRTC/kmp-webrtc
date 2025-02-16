package com.piasy.kmp.webrtc

import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.TlsCertPolicy
import org.webrtc.PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK
import org.webrtc.PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE
import org.webrtc.RTCStatsReport
import org.webrtc.SessionDescription.Type.ANSWER
import org.webrtc.SessionDescription.Type.OFFER
import org.webrtc.SessionDescription.Type.PRANSWER
import org.webrtc.VideoSink

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-27.
 */
private class AndroidPeerConnectionClientCallback(private val callback: PeerConnectionClientCallback) :
    com.piasy.avconf.PeerConnectionClientCallback {
    override fun onPreferCodecs(
        peerUid: String,
        sdp: String
    ): String {
        return callback.onPreferCodecs(peerUid, sdp)
    }

    override fun onLocalDescription(
        peerUid: String,
        sdp: org.webrtc.SessionDescription
    ) {
        return callback.onLocalDescription(peerUid, fromWebRTCSessionDescription(sdp))
    }

    override fun onIceCandidate(
        peerUid: String,
        candidate: org.webrtc.IceCandidate
    ) {
        callback.onIceCandidate(peerUid, fromWebRTCIceCandidate(candidate))
    }

    override fun onIceCandidatesRemoved(
        peerUid: String,
        candidates: List<org.webrtc.IceCandidate>
    ) {
        callback.onIceCandidatesRemoved(peerUid, fromWebRTCIceCandidates(candidates))
    }

    override fun onPeerConnectionStatsReady(
        peerUid: String,
        report: RTCStatsReport
    ) {
        callback.onPeerConnectionStatsReady(peerUid, fromWebRTCStatsReport(report))
    }

    override fun onIceConnected(peerUid: String) {
        callback.onIceConnected(peerUid)
    }

    override fun onIceDisconnected(peerUid: String) {
        callback.onIceDisconnected(peerUid)
    }

    override fun onError(
        peerUid: String,
        code: Int
    ) {
        callback.onError(peerUid, code)
    }
}

class AndroidPeerConnectionClient(
    peerUid: String,
    private val dir: Int,
    needCaptureVideo: Boolean,
    videoMaxBitrate: Int,
    videoCaptureFps: Int,
    callback: PeerConnectionClientCallback
) : PeerConnectionClient {
    private val realClient = com.piasy.avconf.PeerConnectionClient(
        peerUid, dir, needCaptureVideo, AndroidPeerConnectionClientCallback(callback),
        videoMaxBitrate, videoCaptureFps
    )

    override fun createPeerConnection(iceServers: List<IceServer>) {
        realClient.createPeerConnection(toWebRTCIceServers(iceServers))
    }

    override fun getStats() {
        realClient.getStats()
    }

    override fun setAudioSendingEnabled(enable: Boolean) {
        realClient.setAudioSendingEnabled(enable)
    }

    override fun setVideoSendingEnabled(enable: Boolean) {
        realClient.setVideoSendingEnabled(enable)
    }

    override fun setAudioReceivingEnabled(enable: Boolean) {
        realClient.setAudioReceivingEnabled(enable)
    }

    override fun setVideoReceivingEnabled(enable: Boolean) {
        realClient.setVideoReceivingEnabled(enable)
    }

    override fun createOffer() {
        realClient.createOffer()
    }

    override fun createAnswer() {
        realClient.createAnswer()
    }

    override fun addIceCandidate(candidate: IceCandidate) {
        realClient.addIceCandidate(toWebRTCIceCandidate(candidate))
    }

    override fun removeIceCandidates(candidates: List<IceCandidate>) {
        realClient.removeIceCandidates(toWebRTCIceCandidates(candidates))
    }

    override fun setRemoteDescription(sdp: SessionDescription) {
        realClient.setRemoteDescription(toWebRTCSessionDescription(sdp))
    }

    override fun addRemoteTrackRenderer(renderer: Any) {
        if (renderer is VideoSink) {
            realClient.addRemoteTrackRenderer(renderer)
        }
    }

    override fun send(): Boolean {
        return PeerConnectionClient.dirSend(dir)
    }

    override fun receive(): Boolean {
        return PeerConnectionClient.dirRecv(dir)
    }

    override fun startRecorder(dir: Int, path: String): Int {
        return realClient.startRecorder(dir, path)
    }

    override fun stopRecorder(dir: Int): Int {
        return realClient.stopRecorder(dir)
    }

    override fun togglePauseStreaming(pause: Boolean) {
        com.piasy.avconf.PeerConnectionClient.getsHijackCaptureObserver()?.togglePause(pause)
    }

    override fun requestFir() {
        realClient.requestFir()
    }

    override fun getRealClient(): Any {
        return realClient
    }

    override fun close() {
        realClient.close()
    }
}

private fun toWebRTCIceServers(iceServers: List<IceServer>): List<PeerConnection.IceServer> {
    val servers = ArrayList<PeerConnection.IceServer>()
    for (iceServer in iceServers) {
        val server = PeerConnection.IceServer.builder(iceServer.urls)
            .setUsername(iceServer.username)
            .setPassword(iceServer.password)
            .setTlsCertPolicy(toWebRTCTlsCertPolicy(iceServer.tlsCertPolicy))
            .setHostname(iceServer.hostname)
            .setTlsAlpnProtocols(iceServer.tlsAlpnProtocols)
            .setTlsEllipticCurves(iceServer.tlsEllipticCurves)
            .createIceServer()
        servers.add(server)
    }
    return servers
}

private fun toWebRTCTlsCertPolicy(policy: Int): TlsCertPolicy = when (policy) {
    IceServer.TLS_CERT_POLICY_SECURE -> TLS_CERT_POLICY_SECURE
    IceServer.TLS_CERT_POLICY_INSECURE_NO_CHECK -> TLS_CERT_POLICY_INSECURE_NO_CHECK
    else -> TLS_CERT_POLICY_SECURE
}

private fun toWebRTCIceCandidate(candidate: IceCandidate): org.webrtc.IceCandidate {
    return org.webrtc.IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
}

private fun toWebRTCIceCandidates(candidates: List<IceCandidate>): List<org.webrtc.IceCandidate> {
    val iceCandidates = ArrayList<org.webrtc.IceCandidate>()
    for (candidate in candidates) {
        iceCandidates.add(toWebRTCIceCandidate(candidate))
    }
    return iceCandidates
}

private fun toWebRTCSessionDescription(sdp: SessionDescription): org.webrtc.SessionDescription {
    return org.webrtc.SessionDescription(toWebRTCSdpType(sdp.type), sdp.sdpDescription)
}

private fun toWebRTCSdpType(type: Int): org.webrtc.SessionDescription.Type = when (type) {
    SessionDescription.OFFER -> OFFER
    SessionDescription.ANSWER -> ANSWER
    SessionDescription.PRANSWER -> PRANSWER
    else -> OFFER
}

private fun fromWebRTCSessionDescription(sdp: org.webrtc.SessionDescription): SessionDescription {
    return SessionDescription(fromWebRTCSdpType(sdp.type), sdp.description)
}

private fun fromWebRTCSdpType(type: org.webrtc.SessionDescription.Type): Int = when (type) {
    OFFER -> SessionDescription.OFFER
    ANSWER -> SessionDescription.ANSWER
    PRANSWER -> SessionDescription.PRANSWER
    else -> SessionDescription.OFFER
}

private fun fromWebRTCIceCandidate(candidate: org.webrtc.IceCandidate): IceCandidate {
    return IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
}

private fun fromWebRTCIceCandidates(candidates: List<org.webrtc.IceCandidate>): List<IceCandidate> {
    val iceCandidates = ArrayList<IceCandidate>()
    for (candidate in candidates) {
        iceCandidates.add(fromWebRTCIceCandidate(candidate))
    }
    return iceCandidates
}

private fun fromWebRTCStatsReport(report: RTCStatsReport): RtcStatsReport {
    val stats = HashMap<String, RtcStats>()

    for ((key, value) in report.statsMap) {
        stats[key] = fromStats(value)
    }

    return RtcStatsReport(report.timestampUs, stats)
}

private fun fromStats(stats: org.webrtc.RTCStats): RtcStats {
    val members = HashMap<String, String>()

    for ((key, value) in stats.members) {
        members[key] = value.toString()
    }

    return RtcStats(stats.id, stats.type, stats.timestampUs, members)
}
