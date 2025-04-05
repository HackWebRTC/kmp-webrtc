package com.piasy.kmp.webrtc.android.mediasoup

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * Created by Piasy{github.com/Piasy} on 2025-04-02.
 */

@RunWith(AndroidJUnit4::class)
class RtpCapabilitiesTest {

    @Test
    fun testDeserialize() {
        val str = """
            {
              "kind": "video",
              "mimeType": "video/h264",
              "clockRate": 90000,
              "parameters": {
                "level-asymmetry-allowed": 1,
                "packetization-mode": 1,
                "profile-level-id": "42e032"
              },
              "rtcpFeedback": [
                { "type": "nack", "parameter": "" },
                { "type": "nack", "parameter": "pli" },
                { "type": "ccm", "parameter": "fir" },
                { "type": "goog-remb", "parameter": "" },
                { "type": "transport-cc", "parameter": "" }
              ],
              "preferredPayloadType": 103
            }
        """.trimIndent()

        val rtcpFeedbacks = listOf(
            RtcpFeedback("nack"),
            RtcpFeedback("nack", "pli"),
            RtcpFeedback("ccm", "fir"),
            RtcpFeedback("goog-remb"),
            RtcpFeedback("transport-cc", ""),
        )
        val params = mapOf(
            RtpCodec.PARAM_PACKETIZATION_MODE to 1,
            RtpCodec.PARAM_LEVEL_ASYMMETRY_ALLOWED to 1,
            RtpCodec.PARAM_PROFILE_LEVEL_ID to "42e032",
        )
        val codecA = RtpCodec(RtpCodec.KIND_VIDEO, RtpCodec.MIME_H264, 90000, null, rtcpFeedbacks, params, 103)
        val codecB = SdpUtils.json.decodeFromString(RtpCodec.serializer(), str)

        assertEquals(codecA, codecB)
    }
}
