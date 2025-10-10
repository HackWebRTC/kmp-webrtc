package com.piasy.kmp.webrtc.utils

import cocoapods.WebRTC.CFUtils

/**
 * Created by Piasy{github.com/Piasy} on 2025-07-26.
 */

actual fun platformModel(): String = CFUtils.modelName() ?: "Apple Unknown"

actual fun platformOsVersion(): String = CFUtils.osVersion() ?: "Apple Unknown"
