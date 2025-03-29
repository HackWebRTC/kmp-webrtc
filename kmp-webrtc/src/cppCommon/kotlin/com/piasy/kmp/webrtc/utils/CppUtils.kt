package com.piasy.kmp.webrtc.utils

import com.piasy.kmp.webrtc.CppPeerConnectionClient
import com.piasy.kmp.webrtc.PeerConnectionClientCallback
import com.piasy.kmp.webrtc.PeerConnectionClientFactory
import com.piasy.kmp.webrtc.data.IceCandidate
import com.piasy.kmp.webrtc.data.IceServer
import com.piasy.kmp.webrtc.data.RtcStatsReport
import com.piasy.kmp.webrtc.data.SessionDescription
import com.piasy.kmp.xlog.Logging
import kotlinx.cinterop.*

/**
 * Created by Piasy{github.com/Piasy} on 2025-03-06.
 *
 * Utility functions used by cpp example code.
 */
fun createErrorHandler(errorHandler: WebRTC.KmpWebRTCErrorHandler, opaque: COpaquePointer?): (Int, String) -> Unit {
    return { err, msg ->
        memScoped {
            errorHandler(opaque, err, msg.cstr.ptr)
        }
    }
}

fun logInfo(log: String) {
    Logging.info("CppUtils", log)
}

fun preferCodec(sdp: String, codec: Int): String {
    return SdpManipulator.preferCodecs(sdp, listOf(codec), true)
}

fun emptyIceServers() = emptyList<IceServer>()

private class CppPcClientCallback(private val callback: WebRTC.PCClientCallback, private val opaque: COpaquePointer?) :
    PeerConnectionClientCallback {
    override fun onPreferCodecs(peerUid: String, sdp: String): String {
        return memScoped {
            callback.on_prefer_codecs?.invoke(opaque, peerUid.cstr.ptr, sdp.cstr.ptr)?.toKString() ?: sdp
        }
    }

    override fun onLocalDescription(peerUid: String, sdp: SessionDescription) {
        memScoped {
            callback.on_local_description?.invoke(opaque, peerUid.cstr.ptr, sdp.type, sdp.sdpDescription.cstr.ptr)
        }
    }

    override fun onIceCandidate(peerUid: String, candidate: IceCandidate) {
        memScoped {
            callback.on_ice_candidate?.invoke(
                opaque, peerUid.cstr.ptr, candidate.sdpMid.cstr.ptr, candidate.sdpMLineIndex,
                candidate.sdp.cstr.ptr
            )
        }
    }

    override fun onIceCandidatesRemoved(peerUid: String, candidates: List<IceCandidate>) {
        // unsupported yet
    }

    override fun onPeerConnectionStatsReady(peerUid: String, report: RtcStatsReport) {
        val reportStr = CppPeerConnectionClient.json.encodeToString(RtcStatsReport.serializer(), report)
        memScoped {
            callback.on_stats_ready?.invoke(opaque, peerUid.cstr.ptr, reportStr.cstr.ptr)
        }
    }

    override fun onIceConnected(peerUid: String) {
        memScoped {
            callback.on_ice_connected?.invoke(opaque, peerUid.cstr.ptr)
        }
    }

    override fun onIceDisconnected(peerUid: String) {
        memScoped {
            callback.on_ice_disconnected?.invoke(opaque, peerUid.cstr.ptr)
        }
    }

    override fun onError(peerUid: String, code: Int) {
        memScoped {
            callback.on_error?.invoke(opaque, peerUid.cstr.ptr, code)
        }
    }
}

fun createPcClientCallback(callback: WebRTC.PCClientCallback, opaque: COpaquePointer?): PeerConnectionClientCallback {
    return CppPcClientCallback(callback, opaque)
}
