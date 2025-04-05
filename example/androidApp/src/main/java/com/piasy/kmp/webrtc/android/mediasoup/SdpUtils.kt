package com.piasy.kmp.webrtc.android.mediasoup

import com.piasy.kmp.webrtc.android.mediasoup.RtpHeaderExtension.Companion.INACTIVE
import com.piasy.kmp.webrtc.android.mediasoup.RtpHeaderExtension.Companion.RECV_ONLY
import com.piasy.kmp.webrtc.android.mediasoup.RtpHeaderExtension.Companion.SEND_ONLY
import com.piasy.kmp.webrtc.android.mediasoup.RtpHeaderExtension.Companion.SEND_RECV
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Created by Piasy{github.com/Piasy} on 2025-04-02.
 */
object SdpUtils {
    internal val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    fun getExtendedRtpCapabilities(
        localCapsStr: String, remoteCapsStr: String,
    ): ExtendedRtpCapabilities? =
        try {
            val localCaps = json.decodeFromString<RtpCapabilities>(localCapsStr)
            val remoteCaps = json.decodeFromString<RtpCapabilities>(remoteCapsStr)

            val codecs = ArrayList<ExtendedRtpCodec>()
            remoteCaps.codecs.filter { !it.isRtxCodec() }.forEach { remoteCodec ->
                localCaps.codecs.firstOrNull { it.matchCodec(remoteCodec, strict = true, modify = true) }?.let {
                    codecs.add(
                        ExtendedRtpCodec(
                            it.kind, it.mimeType, it.clockRate, it.channels,
                            it.preferredPayloadType, null,
                            remoteCodec.preferredPayloadType, null,
                            it.parameters, remoteCodec.parameters, it.reduceRtcpFeedback(remoteCodec),
                        )
                    )
                }
            }
            codecs.forEach { extendedCodec ->
                localCaps.codecs.firstOrNull {
                    it.isRtxCodec() && it.param<Int>(RtpCodec.PARAM_APT) == extendedCodec.localPayloadType
                }?.let { localRtxCodec ->
                    remoteCaps.codecs.firstOrNull {
                        it.isRtxCodec() && it.param<Int>(RtpCodec.PARAM_APT) == extendedCodec.remotePayloadType
                    }?.let { remoteRtxCodec ->
                        extendedCodec.localRtxPayloadType = localRtxCodec.preferredPayloadType
                        extendedCodec.remoteRtxPayloadType = remoteRtxCodec.preferredPayloadType
                    }
                }
            }

            val headerExtensions = ArrayList<ExtendedRtpHeaderExtension>()
            remoteCaps.headerExtensions.forEach { remoteExt ->
                localCaps.headerExtensions.firstOrNull { it.matchExt(remoteExt) }?.let { localExt ->
                    val remoteExtDirection = when (remoteExt.direction) {
                        SEND_RECV -> SEND_RECV
                        SEND_ONLY -> RECV_ONLY
                        RECV_ONLY -> SEND_ONLY
                        INACTIVE -> INACTIVE
                        else -> INACTIVE
                    }
                    headerExtensions.add(
                        ExtendedRtpHeaderExtension(
                            remoteExt.kind, remoteExt.uri,
                            localExt.preferredId, remoteExt.preferredId,
                            localExt.preferredEncrypt, remoteExtDirection,
                        )
                    )
                }
            }

            ExtendedRtpCapabilities(codecs, headerExtensions)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SerializationException) {
            null
        }
}