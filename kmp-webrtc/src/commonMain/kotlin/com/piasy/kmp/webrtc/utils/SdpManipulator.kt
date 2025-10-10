package com.piasy.kmp.webrtc.utils

import com.piasy.kmp.webrtc.PeerConnectionClientFactory

/**
 * Created by Piasy{github.com/Piasy} on 2019-11-29.
 */
object SdpManipulator {
    private val DISABLE_CVO_FILTER: (String) -> Boolean = {
        !it.contains("urn:3gpp:video-orientation")
    }

    fun preferCodecsByName(sdp: String, codecTypes: List<String>, video: Boolean): String {
        val preferredCodecs = ArrayList<String>()
        preferredCodecs.addAll(codecTypes)
        return preferCodec(sdp, preferredCodecs, video)
    }

    fun preferCodecsById(sdp: String, codecTypes: List<Int>, video: Boolean): String {
        val av1CodecName = if (sdp.contains("AV1X/")) "AV1X" else "AV1"

        val preferredCodecs = ArrayList<String>()
        for (codec in codecTypes) {
            preferredCodecs.add(codecName(codec, av1CodecName))
        }

        return preferCodec(sdp, preferredCodecs, video)
    }

    fun disableCVO(sdp: String): String {
        val lines = sdp.split("(\r\n|\n)".toRegex())
            .dropLastWhile { it.isEmpty() }
            .filter(DISABLE_CVO_FILTER)
        return lines.filter(DISABLE_CVO_FILTER).joinToString("\r\n", postfix = "\r\n")
    }

    fun codecName(codec: Int, av1CodecName: String = "AV1") = when (codec) {
        PeerConnectionClientFactory.VIDEO_CODEC_VP8 -> "VP8"
        PeerConnectionClientFactory.VIDEO_CODEC_VP9 -> "VP9"
        PeerConnectionClientFactory.VIDEO_CODEC_H264_BASELINE, PeerConnectionClientFactory.VIDEO_CODEC_H264_HIGH_PROFILE -> "H264"
        PeerConnectionClientFactory.VIDEO_CODEC_H265 -> "H265"
        PeerConnectionClientFactory.VIDEO_CODEC_AV1 -> av1CodecName
        else -> "UNKNOWN"
    }

    private fun preferCodec(sdp: String, preferredCodecs: ArrayList<String>, video: Boolean): String {
        preferredCodecs.add("red")
        preferredCodecs.add("ulpfec")
        preferredCodecs.add("flexfec-03")

        val lines = sdp.split("(\r\n|\n)".toRegex())
            .dropLastWhile { it.isEmpty() }
        val newLines = ArrayList<String>()

        var audioMLineIndex = -1
        var videoMLineIndex = -1
        var processingAudioSection = false
        var processingVideoSection = false
        // <codecName, payloadType>
        val preferredPayloadTypes = HashMap<String, ArrayList<String>>()
        for (i in lines.indices) {
            val line = lines[i]
            // we only check it for rtpmap/rtcp-fb/fmtp, and they only exist in audio/video section.
            val processingRightSection =
                (video && processingVideoSection) || (!video && processingAudioSection)
            if (line.startsWith("a=rtpmap:")) {
                val payloadType = line.split(" ")[0].split(":")[1]
                val codecName = line.split(" ")[1].split("/")[0]
                // match our preferred codec?
                val codecPreferred = preferredCodecs.contains(codecName)
                // is rtx for our preferred codec?
                val rtxPreferred = codecName == "rtx"
                        && containsValue(preferredPayloadTypes, lines[i + 1].split("apt=")[1])
                if (codecPreferred || rtxPreferred) {
                    putEntry(preferredPayloadTypes, codecName, payloadType)
                } else if (processingRightSection) {
                    continue
                }
            } else if (line.startsWith("a=rtcp-fb:") || line.startsWith("a=fmtp:")) {
                val payloadType = line.split(" ")[0].split(":")[1]
                if (processingRightSection && !containsValue(preferredPayloadTypes, payloadType)) {
                    continue
                }
            } else if (line.startsWith("m=audio")) {
                audioMLineIndex = newLines.size
                processingAudioSection = true
                processingVideoSection = false
            } else if (line.startsWith("m=video")) {
                videoMLineIndex = newLines.size
                processingAudioSection = false
                processingVideoSection = true
            }
            newLines.add(line)
        }

        if (!video && audioMLineIndex != -1) {
            newLines[audioMLineIndex] = changeMLine(
                newLines[audioMLineIndex],
                preferredCodecs,
                preferredPayloadTypes
            )
        }
        if (video && videoMLineIndex != -1) {
            newLines[videoMLineIndex] = changeMLine(
                newLines[videoMLineIndex],
                preferredCodecs,
                preferredPayloadTypes
            )
        }
        return newLines.filter(DISABLE_CVO_FILTER).joinToString("\r\n", postfix = "\r\n")
    }

    private fun containsValue(payloadTypes: HashMap<String, ArrayList<String>>, value: String): Boolean {
        for (v in payloadTypes.values) {
            for (s in v) {
                if (s == value) {
                    return true
                }
            }
        }
        return false
    }

    private fun putEntry(payloadTypes: HashMap<String, ArrayList<String>>, key: String, value: String) {
        var payload = payloadTypes[key]
        if (payload == null) {
            payload = ArrayList()
            payloadTypes[key] = payload
        }
        payload.add(value)
    }

    private fun changeMLine(
        mLine: String,
        preferredCodecs: List<String>,
        preferredPayloadTypes: HashMap<String, ArrayList<String>>
    ): String {
        val oldMLineParts = mLine.split(" ")
        val mLineHeader = oldMLineParts.subList(0, 3)

        val newMLineParts = ArrayList(mLineHeader)
        for (preferredCodec in preferredCodecs) {
            val payload = preferredPayloadTypes[preferredCodec]
            if (payload != null) {
                newMLineParts.addAll(payload)
            }
        }
        val rtxPayload = preferredPayloadTypes["rtx"]
        if (rtxPayload != null) {
            newMLineParts.addAll(rtxPayload)
        }
        return newMLineParts.joinToString(" ")
    }
}
