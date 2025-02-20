package com.piasy.kmp.webrtc.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class FieldTrialTest {
    @Test
    fun fieldTrialsStringToMap() {
        val str1 = "WebRTC-Video-H26xPacketBuffer/Enabled/xxx/Enabled/yyyy/Disabled/"
        assertEquals<Map<Any?, *>>(
            mapOf(
                "WebRTC-Video-H26xPacketBuffer" to "Enabled",
                "xxx" to "Enabled",
                "yyyy" to "Disabled"
            ),
            FieldTrial.fieldTrialsStringToMap(str1)
        )

        val str2 = "WebRTC-Video-H26xPacketBuffer/Enabled/xxx/Enabled/yyyy/Disabled"
        assertEquals(
            emptyMap<Any?, Any>(),
            FieldTrial.fieldTrialsStringToMap(str2)
        )
    }
}
