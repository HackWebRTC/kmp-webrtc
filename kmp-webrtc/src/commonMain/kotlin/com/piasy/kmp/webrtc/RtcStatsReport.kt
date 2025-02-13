package com.piasy.kmp.webrtc

import kotlinx.serialization.Serializable

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-26.
 */
@Serializable
data class RtcStatsReport(
  val timestamp_us: Double,
  val stats: Map<String, RtcStats>
)

@Serializable
data class RtcStats(
  val id: String,
  val type: String,
  val timestamp_us: Double,
  val members: Map<String, String>
)
