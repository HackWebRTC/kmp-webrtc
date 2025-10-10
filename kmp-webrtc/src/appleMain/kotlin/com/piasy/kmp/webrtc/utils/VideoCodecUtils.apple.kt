@file:Suppress("UNCHECKED_CAST")

package com.piasy.kmp.webrtc.utils

import cocoapods.WebRTC.CFUtils

/**
 * Created by Piasy{github.com/Piasy} on 2025-07-18.
 */
internal actual fun platformIsSameH264Profile(
    aParams: Map<String, String?>,
    bParams: Map<String, String?>
): Boolean = CFUtils.isSameH264Profile(aParams as Map<Any?, *>?, bParams as Map<Any?, *>?)

internal actual fun platformH264GenerateProfileLevelIdForAnswer(
    aParams: Map<String, String?>,
    bParams: Map<String, String?>
): String = CFUtils.H264GenerateProfileLevelIdForAnswer(aParams as Map<Any?, *>?, bParams as Map<Any?, *>?) ?: ""
