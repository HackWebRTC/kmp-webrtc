package com.piasy.kmp.webrtc

import com.piasy.kmp.webrtc.data.*
import com.piasy.kmp.xlog.Logging
import kotlinx.browser.window
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.mediacapture.LIVE
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamConstraints
import org.w3c.dom.mediacapture.MediaStreamTrackState
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

/**
 * Created by Piasy{github.com/Piasy} on 2021-11-14.
 */
class JsPeerConnectionClient(
  private val peerUid: String,
  private val dir: Int,
  private val dummyVideoTrack: dynamic,
  private val videoMaxBitrateBps: Int,
  private val videoCaptureFps: Int,
  private val callback: PeerConnectionClientCallback
) : PeerConnectionClient {
  private var pc: RTCPeerConnection? = null
  private var queuedCandidates: ArrayList<IceCandidate>? = ArrayList()

  private var isInitiator = false
  private var streamFulfilled = false
  private var pendingCreateOffer = false
  private var pendingCreateAnswer = false
  private var usingDummyVideo = false

  private var remoteStream: MediaStream? = null
  private var remoteRenderer: HTMLVideoElement? = null

  override fun createPeerConnection(iceServers: List<IceServer>) {
    logI("createPeerConnection $iceServers")

    val config = json(
      "iceServers" to iceServers.map { it.toJs() }.toTypedArray(),
    )
    pc = RTCPeerConnection(config).apply {
      // onsignalingstatechange = {}
      // oniceconnectionstatechange = {} // oniceconnectionstatechange is legacy
      onconnectionstatechange = {
        when (this@JsPeerConnectionClient.pc?.iceConnectionState) {
          "connected" -> callback.onIceConnected(peerUid)
          "disconnected" -> callback.onIceDisconnected(peerUid)
          "failed" -> callback.onError(peerUid, PeerConnectionClientCallback.ERR_ICE_FAIL)
        }
      }
      // onicegatheringstatechange = {}
      onicecandidate = { iceEvent ->
        if (iceEvent.candidate != null) {
          callback.onIceCandidate(peerUid, fromWebRTCIceCandidate(iceEvent.candidate))
        }
      }
      // ondatachannel = {}
      // onnegotiationneeded = {}
      ontrack = {
        if (it.streams.isNotEmpty()) {
          remoteStream = it.streams[0]
          trySetRemoteRenderer()
        }
      }
    }

    if (send()) {
      logI("createPeerConnection wait for stream")
      stream?.then {
        logI(
          "createPeerConnection stream fulfilled with ${it.getAudioTracks().size} " +
              "audio tracks and ${it.getVideoTracks().size} video tracks"
        )

        streamFulfilled = true
        for (track in it.getTracks()) {
          pc?.addTrack(track, it)
        }

        if (it.getVideoTracks().isEmpty()) {
          logI("add dummy video track for audio only publisher")
          it.addTrack(dummyVideoTrack)
          pc?.addTrack(dummyVideoTrack, it)
          usingDummyVideo = true
        }

        if (pendingCreateOffer) {
          logI("createPeerConnection create pending offer")
          doCreateOffer()
        }

        if (pendingCreateAnswer) {
          logI("createPeerConnection create pending answer")
          doCreateAnswer()
        }

        logI("createPeerConnection success")
      }
    } else {
      logI("createPeerConnection success")
    }
  }

  override fun getStats() {
    pc?.getStats()?.then<dynamic> {
      val allStats = HashMap<String, RtcStats>()
      it.forEach { stats ->
        val members = HashMap<String, String>()
        js("Object.keys(stats)").forEach { key ->
          members.put(key.toString(), stats[key].toString())
        }
        // don't cast with as, otherwise JS code will throw exception
        val rtcStats = RtcStats(
          stats.id.toString(), stats.type.toString(), stats.timestamp * 1000, members
        )
        allStats[stats.id.toString()] = rtcStats
        Unit
      }

      // don't cast with as, otherwise JS code will throw exception
      val rtcStatsReport = RtcStatsReport(it.timestamp * 1000, allStats)
      callback.onPeerConnectionStatsReady(peerUid, rtcStatsReport)
    }
  }

  override fun setAudioSendingEnabled(enable: Boolean) {
    logI("setAudioSendingEnabled $enable")
    toggleTrackMute(false, !enable)
  }

  override fun setVideoSendingEnabled(enable: Boolean) {
    logI("setVideoSendingEnabled $enable")
    if (enable && usingDummyVideo) {
      window.navigator.mediaDevices.getUserMedia(
        MediaStreamConstraints(
          video = true,
          audio = false
        )
      )
        .then {
          val tracks = it.getVideoTracks()
          if (tracks.size == 1) {
            stream?.then { it2 ->
              it2.removeTrack(dummyVideoTrack)
              it2.addTrack(tracks[0])
              localPreview?.srcObject = it2
              usingDummyVideo = false

              pc?.getSenders()?.forEach { it3 ->
                if (it3.track?.kind == "video") {
                  it3.replaceTrack(tracks[0])
                }
              }
            }
          }
        }
    } else {
      toggleTrackMute(true, !enable)
    }
  }

  override fun setAudioReceivingEnabled(enable: Boolean) {
  }

  override fun setVideoReceivingEnabled(enable: Boolean) {
  }

  override fun createOffer() {
    logI("createOffer")

    isInitiator = true
    if (streamFulfilled || !send()) {
      doCreateOffer()
    } else {
      logI("createOffer pending")
      pendingCreateOffer = true
      checkStreamTimeout()
    }
  }

  private fun checkStreamTimeout() {
    window.setTimeout({
      if (!streamStarted) {
        logE("open media stream timeout")
        callback.onError(peerUid, PeerConnectionClientCallback.ERR_OPEN_MEDIA_STREAM_FAIL)
      }
    }, 3000)
  }

  private fun doCreateOffer() {
    pendingCreateOffer = false

    if (send()) {
      pc?.createOffer(json())
    } else {
      pc?.createOffer(
        json(
          "offerToReceiveAudio" to true,
          "offerToReceiveVideo" to true
        )
      )
    }?.then { setLocalDescription(it) }
  }

  private fun setLocalDescription(sdp: RTCSessionDescription) {
    logI("${sdp.type} created")

    val refinedSdp = callback.onPreferCodecs(peerUid, sdp.sdp)
    pc?.setLocalDescription(
      json(
        "type" to sdp.type,
        "sdp" to refinedSdp
      )
    )?.then {
      logI("set ${sdp.type} success")

      if (isInitiator) {
        // For offering peer connection we first create offer and set
        // local SDP, then after receiving answer set remote SDP.
        if (pc?.remoteDescription == null) {
          logI("Local SDP set successfully")
          callback.onLocalDescription(
            peerUid, SessionDescription(fromWebRTCSdpType(sdp.type), refinedSdp)
          )
          doSetVideoMaxBitrate()
        } else {
          // We've just set remote description, so drain remote
          // and send local ICE candidates.
          logI("Remote SDP set successfully")
          drainIceCandidate()
        }
      } else {
        // For answering peer connection we set remote SDP and then
        // create answer and set local SDP.
        if (pc?.localDescription != null) {
          // We've just set our local SDP so time to send it, drain
          // remote and send local ICE candidates.
          logI("Local SDP set successfully")
          callback.onLocalDescription(
            peerUid, SessionDescription(fromWebRTCSdpType(sdp.type), refinedSdp)
          )
          doSetVideoMaxBitrate()
          drainIceCandidate()
        } else {
          // We've just set remote SDP - do nothing for now -
          // answer will be created soon.
          logI("Remote SDP set successfully")
        }
      }
    }
  }

  override fun createAnswer() {
    logI("createAnswer")

    isInitiator = false
    if (streamFulfilled || !send()) {
      doCreateAnswer()
    } else {
      logI("createAnswer pending")
      pendingCreateAnswer = true
      checkStreamTimeout()
    }
  }

  private fun doCreateAnswer() {
    pendingCreateAnswer = false

    if (send()) {
      pc?.createAnswer(json())
    } else {
      pc?.createAnswer(
        json(
          "offerToReceiveAudio" to true,
          "offerToReceiveVideo" to true
        )
      )
    }?.then { setLocalDescription(it) }
  }

  private fun doSetVideoMaxBitrate() {
    val senders = pc?.getSenders() ?: return
    for (sender in senders) {
      val track = sender.track ?: continue
      if (track.kind == "video") {
        logI("doSetVideoMaxBitrate: found video sender")
        val params = sender.getParameters()
        if (params.encodings.isEmpty()) {
          logE("doSetVideoMaxBitrate: RtpParameters are not ready")
          return
        }

        for (encoding in params.encodings) {
          encoding.maxBitrate = videoMaxBitrateBps * 1000
          encoding.maxFramerate = videoCaptureFps
        }
        sender.setParameters(params)
        logI("doSetVideoMaxBitrate to $videoMaxBitrateBps")
      }
    }
  }

  override fun addIceCandidate(candidate: IceCandidate) {
    logI("addIceCandidate $candidate")
    if (queuedCandidates != null) {
      queuedCandidates?.add(candidate)
    } else {
      doAddIceCandidate(candidate)
    }
  }

  private fun doAddIceCandidate(candidate: IceCandidate) {
    val jsCandidate = RTCIceCandidate(
      json(
        "sdpMid" to candidate.sdpMid,
        "sdpMLineIndex" to candidate.sdpMLineIndex,
        "candidate" to candidate.sdp
      )
    )
    pc?.addIceCandidate(jsCandidate)
  }

  private fun drainIceCandidate() {
    val candidates = queuedCandidates ?: return
    queuedCandidates = null
    for (candidate in candidates) {
      doAddIceCandidate(candidate)
    }
  }

  override fun removeIceCandidates(candidates: List<IceCandidate>) {
    // unsupported
  }

  override fun setRemoteDescription(sdp: SessionDescription) {
    logI("setRemoteDescription")
    pc?.setRemoteDescription(toWebRTCSessionDescription(sdp))?.then {
      if (isInitiator) {
        drainIceCandidate()
      }
    }
  }

  override fun addRemoteTrackRenderer(renderer: Any) {
    if (renderer is HTMLVideoElement) {
      remoteRenderer = renderer
      trySetRemoteRenderer()
    }
  }

  override fun send(): Boolean {
    return PeerConnectionClient.dirSend(dir)
  }

  override fun receive(): Boolean {
    return PeerConnectionClient.dirRecv(dir)
  }

  override fun startRecorder(dir: Int, path: String): Int {
    logE("recorder not supported on Web")
    return -1
  }

  override fun stopRecorder(dir: Int): Int {
    logE("recorder not supported on Web")
    return -1
  }

  override fun togglePauseStreaming(pause: Boolean) {
    logE("pause streaming not supported on Web")
  }

  override fun requestFir() {
    logE("requestFir not supported on Web")
  }

  override fun getRealClient(): Any {
    return pc ?: false
  }

  override fun close() {
    logI("close")
    pc?.close()
    logI("close success")
  }

  private fun trySetRemoteRenderer() {
    val stream = remoteStream ?: return
    val renderer = remoteRenderer ?: return
    logI("setRemoteRenderer $renderer")
    renderer.srcObject = stream
  }

  private fun IceServer.toJs(): Json = json(
    "urls" to urls,
    "username" to username,
    "credential" to password
  )

  private fun fromWebRTCSdpType(type: String) = when (type) {
    "offer" -> SessionDescription.OFFER
    "answer" -> SessionDescription.ANSWER
    "pranswer" -> SessionDescription.PRANSWER
    else -> SessionDescription.OFFER
  }

  private fun toWebRTCSessionDescription(sdp: SessionDescription): dynamic {
    val type = when (sdp.type) {
      SessionDescription.OFFER -> "offer"
      SessionDescription.ANSWER -> "answer"
      SessionDescription.PRANSWER -> "pranswer"
      else -> "offer"
    }
    return json(
      "type" to type,
      "sdp" to sdp.sdpDescription,
    )
  }

  private fun fromWebRTCIceCandidate(candidate: RTCIceCandidate) = IceCandidate(
    candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate
  )

  private fun logI(content: String) {
    Logging.info("PeerConnectionClient@${hashCode()}", content)
  }

  private fun logE(content: String) {
    Logging.error("PeerConnectionClient@${hashCode()}", content)
  }

  companion object {
    private var stream: Promise<MediaStream>? = null
    private var localPreview: HTMLVideoElement? = null
    private var streamStarted = false
//    private var resolveAudioTrack: dynamic = null
//    private var resolveVideoTrack: dynamic = null
//    private val audioTrack = Promise { resolve: (MediaStreamTrack) -> Unit, _: (Throwable) -> Unit ->
//      resolveAudioTrack = resolve
//    }
//    private var videoTrack = Promise { resolve: (MediaStreamTrack) -> Unit, _: (Throwable) -> Unit ->
//      resolveVideoTrack = resolve
//    }

    fun startLocalStream(needVideo: Boolean, needAudio: Boolean) {
      streamStarted = false

      stream =
        window.navigator.mediaDevices.getUserMedia(MediaStreamConstraints(needVideo, needAudio))
      stream?.then {
        streamStarted = true
      }
    }

    fun startLocalPreview(renderer: HTMLVideoElement) {
      localPreview = renderer
      stream?.then {
        Logging.info("PeerConnectionClient", "startLocalPreview $renderer")
        renderer.srcObject = it
      }
    }

    fun stopLocalStream() {
      stream?.then {
        for (track in it.getTracks()) {
          if (track.readyState == MediaStreamTrackState.LIVE) {
            track.stop()
          }
        }
      }
    }

    fun toggleTrackMute(video: Boolean, mute: Boolean) {
      stream?.then {
        for (track in it.getTracks()) {
          if ((track.kind == "video" && video) || (track.kind == "audio" && !video)) {
            track.enabled = !mute
          }
        }
      }
    }
  }
}
