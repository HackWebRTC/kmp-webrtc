package com.piasy.kmp.webrtc.data

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-17.
 */
data class IceCandidate(
  val sdpMid: String,
  val sdpMLineIndex: Int,
  val sdp: String
)
