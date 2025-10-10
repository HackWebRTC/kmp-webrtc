package com.piasy.kmp.webrtc.utils

import com.piasy.kmp.xlog.Platform

/**
 * Created by Piasy{github.com/Piasy} on 2025-07-26.
 */
object PlatformInfo {
    val name = when {
        Platform.isJvm -> "android"
        Platform.isIOS -> "ios"
        Platform.isMAC -> "mac"
        Platform.isWindows -> "windows"
        Platform.isLinux -> "linux"
        Platform.isJs -> "js"
        else -> "unknown"
    }

    val model = platformModel()

    val osVersion = platformOsVersion()
}

expect fun platformModel(): String

expect fun platformOsVersion(): String
