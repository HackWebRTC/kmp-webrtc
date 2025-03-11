package com.piasy.kmp.webrtc.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-26.
 */
@Serializable
data class RtcStatsReport(
  @SerialName("timestamp_us") val timestampUs: Long,
  val stats: Map<String, RtcStats>
)

@Serializable
data class RtcStats(
  val id: String,
  val type: String,
  @SerialName("timestamp_us") val timestampUs: Long,
  val members: Map<String, String>
)
