package com.piasy.kmp.webrtc

import com.piasy.kmp.webrtc.data.IceCandidate
import com.piasy.kmp.webrtc.data.IceServer
import com.piasy.kmp.webrtc.data.RtcStatsReport
import com.piasy.kmp.webrtc.data.SessionDescription
import com.piasy.kmp.xlog.Logging
import kotlinx.cinterop.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-30.
 */
private fun freeKString(ptr: CPointer<ByteVar>?) {
    if (ptr != null) {
        nativeHeap.free(ptr)
    }
}

private fun pcClientCallbackOnPreferCodecs(
    opaque: COpaquePointer?,
    peerUidPtr: CPointer<ByteVar>?,
    sdpPtr: CPointer<ByteVar>?
): CPointer<ByteVar>? {
    if (opaque == null) {
        return null
    }
    val peerUid = peerUidPtr?.toKString()
    val sdp = sdpPtr?.toKString()
    if (peerUid == null || sdp == null) {
        return null
    }

    // this function is designed to be called from cpp code,
    // and cpp code needs to access the returned string,
    // so we need to make sure the cstr still lives after
    // the function returns, so we use nativeHeap.allocArray
    // and nativeHeap.free (in freeKString above).
    // but when kotlin code calls cpp code, when we want to pass
    // cstr to cpp code, we can just use memScoped, because the
    // whole cpp function call lifetime is inside the memScoped
    // block (in CppUtils.kt).
    val refinedSdp = opaque.asStableRef<PeerConnectionClientCallback>()
        .get()
        .onPreferCodecs(peerUid, sdp)
    return refinedSdp.cstr.place(nativeHeap.allocArray(refinedSdp.length + 1))
}

private fun pcClientCallbackOnLocalDescription(
    opaque: COpaquePointer?,
    peerUidPtr: CPointer<ByteVar>?,
    type: Int,
    descriptionPtr: CPointer<ByteVar>?
) {
    if (opaque == null) {
        return
    }
    val peerUid = peerUidPtr?.toKString()
    val description = descriptionPtr?.toKString()
    if (peerUid == null || description == null) {
        return
    }

    opaque.asStableRef<PeerConnectionClientCallback>()
        .get()
        .onLocalDescription(peerUid, SessionDescription(type, description))
}

private fun pcClientCallbackOnIceCandidate(
    opaque: COpaquePointer?,
    peerUidPtr: CPointer<ByteVar>?,
    sdpMidPtr: CPointer<ByteVar>?,
    sdpMlineIndex: Int,
    sdpPtr: CPointer<ByteVar>?
) {
    if (opaque == null) {
        return
    }
    val peerUid = peerUidPtr?.toKString()
    val sdpMid = sdpMidPtr?.toKString()
    val sdp = sdpPtr?.toKString()
    if (peerUid == null || sdpMid == null || sdp == null) {
        return
    }

    opaque.asStableRef<PeerConnectionClientCallback>()
        .get()
        .onIceCandidate(peerUid, IceCandidate(sdpMid, sdpMlineIndex, sdp))
}

private fun pcClientCallbackOnPeerConnectionStatsReady(
    opaque: COpaquePointer?,
    peerUidPtr: CPointer<ByteVar>?,
    statsPtr: CPointer<ByteVar>?
) {
    if (opaque == null) {
        return
    }
    val peerUid = peerUidPtr?.toKString()
    val stats = statsPtr?.toKString()
    if (peerUid == null || stats == null) {
        return
    }

    try {
        val report = CppPeerConnectionClient.json.decodeFromString(RtcStatsReport.serializer(), stats)
        opaque.asStableRef<PeerConnectionClientCallback>()
            .get()
            .onPeerConnectionStatsReady(peerUid, report)
    } catch (e: SerializationException) {
        Logging.error("WebRTC", "parse StatsReport fail: ${e.message}")
    }
}

private fun pcClientCallbackOnIceConnected(
    opaque: COpaquePointer?,
    peerUidPtr: CPointer<ByteVar>?
) {
    if (opaque == null) {
        return
    }
    val peerUid = peerUidPtr?.toKString() ?: return

    opaque.asStableRef<PeerConnectionClientCallback>()
        .get()
        .onIceConnected(peerUid)
}

private fun pcClientCallbackOnIceDisconnected(
    opaque: COpaquePointer?,
    peerUidPtr: CPointer<ByteVar>?
) {
    if (opaque == null) {
        return
    }
    val peerUid = peerUidPtr?.toKString() ?: return

    opaque.asStableRef<PeerConnectionClientCallback>()
        .get()
        .onIceDisconnected(peerUid)
}

private fun pcClientCallbackOnError(
    opaque: COpaquePointer?,
    peerUidPtr: CPointer<ByteVar>?,
    code: Int
) {
    if (opaque == null) {
        return
    }
    val peerUid = peerUidPtr?.toKString() ?: return

    opaque.asStableRef<PeerConnectionClientCallback>()
        .get()
        .onError(peerUid, code)
}

enum class LogSeverity {
    Verbose,
    Info,
    Warning,
    Error;
}

private fun pcClientLogCallback(severity: Int, log: CPointer<ByteVarOf<Byte>>?) {
    val logStr = log?.toKString() ?: "<bad log buf>"
    when (severity) {
        LogSeverity.Verbose.ordinal -> Logging.debug("webrtc", logStr)
        LogSeverity.Info.ordinal, LogSeverity.Warning.ordinal -> Logging.info("webrtc", logStr)
        LogSeverity.Error.ordinal -> Logging.error("webrtc", logStr)
    }
}

internal fun setPcClientLogCallback(severity: LogSeverity) {
    WebRTC.PCClientSetLogCallback(staticCFunction(::pcClientLogCallback), severity.ordinal)
}

abstract class CppPeerConnectionClient(
    peerUid: String,
    private val dir: Int,
    needCaptureVideo: Boolean,
    videoMaxBitrate: Int,
    videoCaptureFps: Int,
    callback: PeerConnectionClientCallback
) : PeerConnectionClient {
    protected val realClient = WebRTC.PCClientCreate(
        peerUid, dir, if (needCaptureVideo) 1 else 0,
        cValue {
            free_kstring = staticCFunction(::freeKString)
            on_prefer_codecs = staticCFunction(::pcClientCallbackOnPreferCodecs)
            on_local_description = staticCFunction(::pcClientCallbackOnLocalDescription)
            on_ice_candidate = staticCFunction(::pcClientCallbackOnIceCandidate)
            on_stats_ready = staticCFunction(::pcClientCallbackOnPeerConnectionStatsReady)
            on_ice_connected = staticCFunction(::pcClientCallbackOnIceConnected)
            on_ice_disconnected = staticCFunction(::pcClientCallbackOnIceDisconnected)
            on_error = staticCFunction(::pcClientCallbackOnError)
            opaque = StableRef.create(callback).asCPointer()
        },
        videoMaxBitrate, videoCaptureFps
    )

    override fun createPeerConnection(iceServers: List<IceServer>) {
        WebRTC.PCClientCreatePeerConnection(realClient)
    }

    override fun getStats() {
        WebRTC.PCClientGetStats(realClient)
    }

    override fun setAudioSendingEnabled(enable: Boolean) {
        WebRTC.PCClientSetAudioSendingEnabled(realClient, if (enable) 1 else 0)
    }

    override fun setVideoSendingEnabled(enable: Boolean) {
        WebRTC.PCClientSetVideoSendingEnabled(realClient, if (enable) 1 else 0)
    }

    override fun setAudioReceivingEnabled(enable: Boolean) {
        WebRTC.PCClientSetAudioReceivingEnabled(realClient, if (enable) 1 else 0)
    }

    override fun setVideoReceivingEnabled(enable: Boolean) {
        WebRTC.PCClientSetVideoReceivingEnabled(realClient, if (enable) 1 else 0)
    }

    override fun createOffer() {
        WebRTC.PCClientCreateOffer(realClient)
    }

    override fun createAnswer() {
        WebRTC.PCClientCreateAnswer(realClient)
    }

    override fun addIceCandidate(candidate: IceCandidate) {
        WebRTC.PCClientAddIceCandidate(
            realClient, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp
        )
    }

    override fun removeIceCandidates(candidates: List<IceCandidate>) {
        // unsupported
    }

    override fun setRemoteDescription(sdp: SessionDescription) {
        WebRTC.PCClientSetRemoteDescription(realClient, sdp.type, sdp.sdpDescription)
    }

    override fun send(): Boolean {
        return PeerConnectionClient.dirSend(dir)
    }

    override fun receive(): Boolean {
        return PeerConnectionClient.dirRecv(dir)
    }

    override fun startRecorder(dir: Int, path: String): Int {
        Logging.error("WebRTC", "recorder not supported on CPP")
        return -1
    }

    override fun stopRecorder(dir: Int): Int {
        Logging.error("WebRTC", "recorder not supported on CPP")
        return -1
    }

    override fun togglePauseStreaming(pause: Boolean) {
        Logging.error("WebRTC", "pause streaming not supported on CPP")
    }

    override fun requestFir() {
        Logging.error("WebRTC", "requestFir not supported on CPP")
    }

    override fun getRealClient(): Any {
        return realClient ?: 0
    }

    override fun close() {
        WebRTC.PCClientClose(realClient)
    }

    companion object {
        internal val json = Json {
            encodeDefaults = false
            ignoreUnknownKeys = true
        }
    }
}
