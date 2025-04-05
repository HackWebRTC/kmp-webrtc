package com.piasy.kmp.webrtc.android.mediasoup

import com.piasy.avconf.utils.VideoCodecUtils
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Created by Piasy{github.com/Piasy} on 2025-04-02.
 */
@Serializable
data class RtcpFeedback(
    val type: String,
    val parameter: String = "",
)

@Serializable
data class RtpCodec(
    val kind: String,
    val mimeType: String,
    val clockRate: Int,
    val channels: Int? = null,
    val rtcpFeedback: List<RtcpFeedback>,
    @Serializable(with = RtpParameterSerializer::class)
    val parameters: Map<String, Any>,
    val preferredPayloadType: Int,
) {
    fun isRtxCodec() = RTX_MIME.matches(mimeType)

    fun matchCodec(codec: RtpCodec, strict: Boolean = false, modify: Boolean = false): Boolean {
        val aMime = mimeType.lowercase()
        if (aMime != codec.mimeType.lowercase()
            || clockRate != codec.clockRate
            || channels != codec.channels
        ) {
            return false
        }

        if (strict) {
            when (aMime) {
                MIME_H264 -> {
                    if (param<Int>(PARAM_PACKETIZATION_MODE)
                        != codec.param<Int>(PARAM_PACKETIZATION_MODE)
                    ) {
                        return false
                    }

                    val aParams = mapOf(
                        PARAM_LEVEL_ASYMMETRY_ALLOWED to param<Int>(PARAM_LEVEL_ASYMMETRY_ALLOWED).toString(),
                        PARAM_PACKETIZATION_MODE to param<Int>(PARAM_PACKETIZATION_MODE).toString(),
                        PARAM_PROFILE_LEVEL_ID to param<String>(PARAM_PROFILE_LEVEL_ID),
                    )
                    val bParams = mapOf(
                        PARAM_LEVEL_ASYMMETRY_ALLOWED to codec.param<Int>(PARAM_LEVEL_ASYMMETRY_ALLOWED).toString(),
                        PARAM_PACKETIZATION_MODE to codec.param<Int>(PARAM_PACKETIZATION_MODE).toString(),
                        PARAM_PROFILE_LEVEL_ID to codec.param<String>(PARAM_PROFILE_LEVEL_ID),
                    )
                    if (!VideoCodecUtils.isSameH264Profile(aParams, bParams)) {
                        return false
                    }

                    if (modify) {
                        // xxx
                    }
                }
            }
        }

        return true
    }

    fun reduceRtcpFeedback(codec: RtpCodec) = rtcpFeedback.filter { codec.rtcpFeedback.contains(it) }

    inline fun <reified T : Any> param(key: String): T? {
        val value = parameters[key] ?: return null
        if (value is T) {
            return value
        }
        return null
    }

    companion object {
        private val RTX_MIME = Regex("^(audio|video)/rtx$", RegexOption.IGNORE_CASE)

        const val KIND_AUDIO = "audio"
        const val KIND_VIDEO = "video"

        const val MIME_H264 = "video/h264"

        const val PARAM_APT = "apt"
        const val PARAM_PACKETIZATION_MODE = "packetization-mode"
        const val PARAM_LEVEL_ASYMMETRY_ALLOWED = "level-asymmetry-allowed"
        const val PARAM_PROFILE_LEVEL_ID = "profile-level-id"
    }
}

@Serializable
data class RtpHeaderExtension(
    val kind: String,
    val uri: String,
    val preferredId: Int,
    val preferredEncrypt: Boolean,
    val direction: String,
) {
    fun matchExt(ext: RtpHeaderExtension): Boolean {
        return false
    }

    companion object {
        const val SEND_RECV = "sendrecv"
        const val SEND_ONLY = "sendonly"
        const val RECV_ONLY = "recvonly"
        const val INACTIVE = "inactive"
    }
}

@Serializable
data class RtpCapabilities(
    val codecs: List<RtpCodec>,
    val headerExtensions: List<RtpHeaderExtension>,
)

@Serializable
data class ExtendedRtpCodec(
    val kind: String,
    val mimeType: String,
    val clockRate: Int,
    val channels: Int? = null,
    val localPayloadType: Int,
    var localRtxPayloadType: Int?,
    val remotePayloadType: Int,
    var remoteRtxPayloadType: Int?,
    @Serializable(with = RtpParameterSerializer::class)
    val localParameters: Map<String, Any>,
    @Serializable(with = RtpParameterSerializer::class)
    val remoteParameters: Map<String, Any>,
    val rtcpFeedback: List<RtcpFeedback>,
)

@Serializable
data class ExtendedRtpHeaderExtension(
    val kind: String,
    val uri: String,
    val sendId: Int,
    val recvId: Int,
    val encrypt: Boolean,
    val direction: String,
)

@Serializable
data class ExtendedRtpCapabilities(
    val codecs: List<ExtendedRtpCodec>,
    val headerExtensions: List<ExtendedRtpHeaderExtension>,
)

object RtpParameterSerializer : KSerializer<Map<String, Any>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Map<String, Any>")

    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("Only JSON supported")
        jsonEncoder.encodeJsonElement(
            JsonObject(
                value.mapValues { (_, v) -> anyToJsonElement(v) }
            )
        )
    }

    private fun anyToJsonElement(value: Any): JsonElement {
        return when (value) {
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> throw SerializationException("Unsupported type: ${value::class}")
        }
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Only JSON supported")
        val jsonElement = jsonDecoder.decodeJsonElement()
        require(jsonElement is JsonObject) { "Expected JSON object" }

        return jsonElement.mapValues { (_, value) ->
            when (value) {
                is JsonPrimitive -> if (value.isString) {
                    value.content
                } else {
                    value.booleanOrNull ?: value.intOrNull ?: value.floatOrNull ?: value.content
                }

                else -> throw SerializationException("Unsupported value: $value")
            }
        }
    }
}
