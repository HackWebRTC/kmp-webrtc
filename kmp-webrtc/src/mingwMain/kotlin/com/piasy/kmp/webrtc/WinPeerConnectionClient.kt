package com.piasy.kmp.webrtc

import kotlinx.cinterop.COpaquePointer

/**
 * Created by Piasy{github.com/Piasy} on 2025-03-02.
 */
class WinPeerConnectionClient(
    peerUid: String,
    dir: Int,
    needCaptureVideo: Boolean,
    videoMaxBitrate: Int,
    videoCaptureFps: Int,
    callback: PeerConnectionClientCallback
) : CppPeerConnectionClient(peerUid, dir, needCaptureVideo, videoMaxBitrate, videoCaptureFps, callback) {

    override fun addRemoteTrackRenderer(renderer: Any) {
        if (renderer is COpaquePointer) {
            WebRTC.PCClientAddRemoteRenderer(realClient, renderer)
        }
    }
}
