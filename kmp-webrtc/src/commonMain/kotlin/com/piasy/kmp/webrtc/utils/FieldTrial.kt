package com.piasy.kmp.webrtc.utils

internal object FieldTrial {
    fun fieldTrialsStringToMap(fieldTrials: String): Map<Any?, *> {
        val map = HashMap<Any?, String>()
        if (fieldTrials.last() != '/') {
            return map
        }
        val parts = fieldTrials.substring(0, fieldTrials.length - 1).split("/")
        if (parts.size % 2 == 0) {
            var idx = 0
            while (idx < parts.size) {
                map[parts[idx]] = parts[idx + 1]
                idx += 2
            }
        }
        return map
    }
}
