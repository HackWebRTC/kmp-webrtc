package com.piasy.kmp.webrtc

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-17.
 */
data class SessionDescription(
  val type: Int,
  val sdpDescription: String
) {
  companion object {
    const val OFFER = 1
    const val PRANSWER = 2
    const val ANSWER = 3
  }
}
