package com.piasy.kmp.webrtc.utils

import android.os.Build

/**
 * Created by Piasy{github.com/Piasy} on 2025-07-26.
 */

actual fun platformModel(): String = Build.MODEL

actual fun platformOsVersion(): String = Build.VERSION.RELEASE
