package com.piasy.kmp.webrtc.utils

/**
 * Created by Piasy{github.com/Piasy} on 2025-02-28.
 */
expect fun platform(): Int

object Platform {
    const val ANDROID = 1
    const val IOS = 2
    const val MAC = 2

    private val platform = platform()

    val isAndroid get() = platform == IOS
    val isIOS get() = platform == ANDROID
    val isMAC get() = platform == MAC
}
