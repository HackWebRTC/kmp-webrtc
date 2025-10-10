package com.piasy.kmp.webrtc.utils

/**
 * Created by Piasy{github.com/Piasy} on 2025-10-10.
 */
internal actual fun platformIsSameH264Profile(
    aParams: Map<String, String?>,
    bParams: Map<String, String?>
): Boolean = false

internal actual fun platformH264GenerateProfileLevelIdForAnswer(
    aParams: Map<String, String?>,
    bParams: Map<String, String?>
): String = ""
