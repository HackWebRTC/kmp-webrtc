package com.piasy.kmp.webrtc

import WebRTC.*
import WebRTC.RTCSdpType.RTCSdpTypeAnswer
import WebRTC.RTCSdpType.RTCSdpTypeOffer
import WebRTC.RTCSdpType.RTCSdpTypePrAnswer
import com.piasy.kmp.webrtc.data.IceCandidate
import com.piasy.kmp.webrtc.data.IceServer
import com.piasy.kmp.webrtc.data.RtcStats
import com.piasy.kmp.webrtc.data.RtcStatsReport
import com.piasy.kmp.webrtc.data.SessionDescription
import platform.darwin.NSObject
import kotlin.native.ref.WeakReference

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-30.
 */
private class ObjCPeerConnectionClientCallback(callback: PeerConnectionClientCallback) :
    CFPeerConnectionClientDelegateProtocol, NSObject() {
    private val realCallback = WeakReference(callback)

    override fun onPreferCodecs(
        peerUid: String,
        sdp: String
    ): String {
        return realCallback.get()?.onPreferCodecs(peerUid, sdp) ?: sdp
    }

    override fun onLocalDescription(
        peerUid: String,
        localSdp: RTCSessionDescription
    ) {
        realCallback.get()?.onLocalDescription(peerUid, fromWebRTCSessionDescription(localSdp))
    }

    override fun onIceCandidate(
        peerUid: String,
        candidate: RTCIceCandidate
    ) {
        realCallback.get()?.onIceCandidate(peerUid, fromWebRTCIceCandidate(candidate))
    }

    override fun onIceCandidatesRemoved(
        peerUid: String,
        candidates: List<*>
    ) {
        realCallback.get()?.onIceCandidatesRemoved(peerUid, fromWebRTCIceCandidates(candidates))
    }

    override fun onPeerConnectionStatsReady(
        peerUid: String,
        report: RTCStatisticsReport
    ) {
        realCallback.get()?.onPeerConnectionStatsReady(peerUid, fromWebRTCStatsReport(report))
    }

    override fun onIceConnected(peerUid: String) {
        realCallback.get()?.onIceConnected(peerUid)
    }

    override fun onIceDisconnected(peerUid: String) {
        realCallback.get()?.onIceDisconnected(peerUid)
    }

    override fun onError(
        peerUid: String,
        code: CFPeerConnectionError
    ) {
        realCallback.get()?.onError(peerUid, code.toInt())
    }
}

class ObjCPeerConnectionClient(
    peerUid: String,
    private val dir: Int,
    needCaptureVideo: Boolean,
    videoMaxBitrate: Int,
    videoCaptureFps: Int,
    callback: PeerConnectionClientCallback
) : PeerConnectionClient {
    private val realClient = CFPeerConnectionClient(
        peerUid, dir.toLong(), needCaptureVideo, ObjCPeerConnectionClientCallback(callback),
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
        if (renderer is RTCVideoRendererProtocol) {
            realClient.addRemoteTrackRenderer(renderer)
        }
    }

    override fun send(): Boolean {
        return PeerConnectionClient.dirSend(dir)
    }

    override fun receive(): Boolean {
        return PeerConnectionClient.dirRecv(dir)
    }

    override fun startRecorder(
        dir: Int,
        path: String
    ): Int {
        return realClient.startRecorder(dir, path)
    }

    override fun stopRecorder(dir: Int): Int {
        return realClient.stopRecorder(dir)
    }

    override fun togglePauseStreaming(pause: Boolean) {
        CFPeerConnectionClient.getHijackCapturerDelegate().togglePause(pause)
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

private fun toWebRTCIceServers(iceServers: List<IceServer>) =
    iceServers.map { RTCIceServer(it.urls, it.username, it.password) }

private fun toWebRTCIceCandidate(candidate: IceCandidate) =
    RTCIceCandidate(candidate.sdp, candidate.sdpMLineIndex, candidate.sdpMid)

private fun toWebRTCIceCandidates(candidates: List<IceCandidate>) = candidates.map { toWebRTCIceCandidate(it) }

private fun toWebRTCSessionDescription(sdp: SessionDescription) =
    RTCSessionDescription(toWebRTCSdpType(sdp.type), sdp.sdpDescription)

private fun toWebRTCSdpType(type: Int) = when (type) {
    SessionDescription.OFFER -> RTCSdpTypeOffer
    SessionDescription.ANSWER -> RTCSdpTypeAnswer
    SessionDescription.PRANSWER -> RTCSdpTypePrAnswer
    else -> RTCSdpTypeOffer
}

private fun fromWebRTCSessionDescription(sdp: RTCSessionDescription) =
    SessionDescription(fromWebRTCSdpType(sdp.type), sdp.sdp)

private fun fromWebRTCSdpType(type: RTCSdpType) = when (type) {
    RTCSdpTypeOffer -> SessionDescription.OFFER
    RTCSdpTypeAnswer -> SessionDescription.ANSWER
    RTCSdpTypePrAnswer -> SessionDescription.PRANSWER
    else -> SessionDescription.OFFER
}

private fun fromWebRTCIceCandidate(candidate: RTCIceCandidate) =
    IceCandidate(candidate.sdpMid ?: "", candidate.sdpMLineIndex, candidate.sdp)

private fun fromWebRTCIceCandidates(candidates: List<*>) =
    candidates.filter { it is RTCIceCandidate && it.sdpMid != null }
        .map { fromWebRTCIceCandidate(it as RTCIceCandidate) }

private fun fromWebRTCStatsReport(report: RTCStatisticsReport): RtcStatsReport {
    val stats = HashMap<String, RtcStats>()

    for ((key, value) in report.statistics) {
        if (key is String && value is RTCStatistics) {
            stats[key] = fromStats(value)
        }
    }

    return RtcStatsReport(report.timestamp_us, stats)
}

private fun fromStats(stats: RTCStatistics): RtcStats {
    val members = HashMap<String, String>()

    for ((key, value) in stats.values) {
        if (key is String) {
            members[key] = value.toString()
        }
    }

    return RtcStats(stats.id, stats.type, stats.timestamp_us, members)
}
