package com.piasy.kmp.webrtc.utils

/**
 * Created by Piasy{github.com/Piasy} on 2025-07-18.
 */
internal actual fun platformIsSameH264Profile(
    aParams: Map<String, String?>,
    bParams: Map<String, String?>
) = com.piasy.avconf.utils.VideoCodecUtils.isSameH264Profile(aParams, bParams)

internal actual fun platformH264GenerateProfileLevelIdForAnswer(
    aParams: Map<String, String?>,
    bParams: Map<String, String?>
) = com.piasy.avconf.utils.VideoCodecUtils.H264GenerateProfileLevelIdForAnswer(aParams, bParams)
