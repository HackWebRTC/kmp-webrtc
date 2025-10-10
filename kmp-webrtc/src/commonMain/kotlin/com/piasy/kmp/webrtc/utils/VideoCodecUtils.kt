package com.piasy.kmp.webrtc.utils

/**
 * Created by Piasy{github.com/Piasy} on 2025-07-18.
 */
object VideoCodecUtils {
    fun isSameH264Profile(aParams: Map<String, String?>, bParams: Map<String, String?>) =
        platformIsSameH264Profile(aParams, bParams)

    fun h264GenerateProfileLevelIdForAnswer(
        aParams: Map<String, String?>,
        bParams: Map<String, String?>
    ) = platformH264GenerateProfileLevelIdForAnswer(aParams, bParams)
}

internal expect fun platformIsSameH264Profile(
    aParams: Map<String, String?>,
    bParams: Map<String, String?>
): Boolean

internal expect fun platformH264GenerateProfileLevelIdForAnswer(
    aParams: Map<String, String?>,
    bParams: Map<String, String?>
): String
