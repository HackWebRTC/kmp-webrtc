package com.piasy.kmp.webrtc

import com.piasy.kmp.xlog.Logging

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-26.
 */
interface PeerConnectionClient {

    fun createPeerConnection(iceServers: List<IceServer>)

    fun getStats()

    fun setAudioSendingEnabled(enable: Boolean)

    fun setVideoSendingEnabled(enable: Boolean)

    fun setAudioReceivingEnabled(enable: Boolean)

    fun setVideoReceivingEnabled(enable: Boolean)

    fun createOffer()

    fun createAnswer()

    fun addIceCandidate(candidate: IceCandidate)

    fun removeIceCandidates(candidates: List<IceCandidate>)

    fun setRemoteDescription(sdp: SessionDescription)

    fun send(): Boolean

    fun receive(): Boolean

    fun startRecorder(
        dir: Int,
        path: String
    ): Int

    fun stopRecorder(dir: Int): Int

    fun togglePauseStreaming(pause: Boolean)

    fun requestFir()

    fun getRealClient(): Any

    fun close()

    companion object {
        const val DIR_SEND_RECV = 0
        const val DIR_SEND_ONLY = 1
        const val DIR_RECV_ONLY = 2
        const val DIR_INACTIVE = 3

        const val K_AUDIO_TRACK_ID = "CFAMSa0"
        const val K_VIDEO_TRACK_ID = "CFAMSv0"
        const val K_STREAM_ID = "CFAMS"

        const val K_BPS_IN_KBPS = 1000

        fun dirSend(dir: Int): Boolean {
            return dir == DIR_SEND_ONLY || dir == DIR_SEND_RECV
        }

        fun dirRecv(dir: Int): Boolean {
            return dir == DIR_RECV_ONLY || dir == DIR_SEND_RECV
        }

        fun dir(
            send: Boolean,
            recv: Boolean
        ): Int {
            return if (send && recv) {
                DIR_SEND_RECV
            } else if (send) {
                DIR_SEND_ONLY
            } else if (recv) {
                DIR_RECV_ONLY
            } else {
                DIR_INACTIVE
            }
        }

        fun dirName(dir: Int): String {
            return when (dir) {
                DIR_INACTIVE -> "INACTIVE"
                DIR_RECV_ONLY -> "RECV_ONLY"
                DIR_SEND_ONLY -> "SEND_ONLY"
                DIR_SEND_RECV -> "SEND_RECV"
                else -> "UNKNOWN"
            }
        }
    }
}

abstract class PeerConnectionClientFactory {
    data class Config(
        val videoCaptureImpl: Int,
        val videoCaptureWidth: Int,
        val videoCaptureHeight: Int,
        val videoCaptureFps: Int,
        val initCameraFace: Int,
    ) {
        internal val screenShare: Boolean
            get() = videoCaptureImpl == VIDEO_CAPTURE_IMPL_SCREEN

        companion object {
            const val VIDEO_CAPTURE_IMPL_SYSTEM_CAMERA = 1
            const val VIDEO_CAPTURE_IMPL_SCREEN = 2
            const val VIDEO_CAPTURE_IMPL_APP = 4

            const val CAMERA_FACE_FRONT = 0
            const val CAMERA_FACE_BACK = 1
        }
    }

    abstract fun createPeerConnectionClient(
        peerUid: String,
        dir: Int,
        hasVideo: Boolean,
        videoMaxBitrate: Int,
        videoMaxFrameRate: Int,
        callback: PeerConnectionClientCallback
    ): PeerConnectionClient

    abstract fun createLocalTracks()

    abstract fun adaptVideoOutputFormat(
        width: Int,
        height: Int,
        fps: Int
    )

    protected fun logI(content: String) {
        Logging.info("PCClientFactory@${hashCode()}", content)
    }

    protected fun logE(content: String) {
        Logging.error("PCClientFactory@${hashCode()}", content)
    }
}

interface PeerConnectionClientCallback {
    fun onPreferCodecs(
        peerUid: String,
        sdp: String
    ): String

    fun onLocalDescription(
        peerUid: String,
        sdp: SessionDescription
    )

    fun onIceCandidate(
        peerUid: String,
        candidate: IceCandidate
    )

    fun onIceCandidatesRemoved(
        peerUid: String,
        candidates: List<IceCandidate>
    )

    fun onPeerConnectionStatsReady(
        peerUid: String,
        report: RtcStatsReport
    )

    fun onIceConnected(peerUid: String)

    fun onIceDisconnected(peerUid: String)

    fun onError(
        peerUid: String,
        code: Int
    )

    companion object {
        const val ERR_NO_FACTORY = 1000
        const val ERR_NO_SENDING_TRACK = 1001
        const val ERR_CREATE_PC_FAIL = 1002
        const val ERR_ICE_FAIL = 1003
        const val ERR_CREATE_MULTIPLE_SDP = 1004
        const val ERR_CREATE_SDP_FAIL = 1005
        const val ERR_SET_SDP_FAIL = 1006
        const val ERR_OPEN_MEDIA_STREAM_FAIL = 1007

        fun errName(code: Int) = when (code) {
            ERR_NO_FACTORY -> "ERR_NO_FACTORY"
            ERR_NO_SENDING_TRACK -> "ERR_NO_SENDING_TRACK"
            ERR_CREATE_PC_FAIL -> "ERR_CREATE_PC_FAIL"
            ERR_ICE_FAIL -> "ERR_ICE_FAIL"
            ERR_CREATE_MULTIPLE_SDP -> "ERR_CREATE_MULTIPLE_SDP"
            ERR_CREATE_SDP_FAIL -> "ERR_CREATE_SDP_FAIL"
            ERR_SET_SDP_FAIL -> "ERR_SET_SDP_FAIL"
            ERR_OPEN_MEDIA_STREAM_FAIL -> "ERR_OPEN_MEDIA_STREAM_FAIL"
            else -> "UNKNOWN"
        }
    }
}

expect fun initializeWebRTC(appContext: Any?, fieldTrials: String, debugLog: Boolean)

expect fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    screenCaptureErrorHandler: (String?) -> Unit
): PeerConnectionClientFactory
