package com.piasy.kmp.webrtc

import kotlinx.cinterop.COpaquePointer

/**
 * Created by Piasy{github.com/Piasy} on 2025-03-05.
 */
data class LinuxPrivateConfig(
    internal val hwnd: COpaquePointer,
    internal val disableEncryption: Boolean,
) : PeerConnectionClientFactory.PrivateConfig()

class LinuxPeerConnectionClientFactory(
    config: Config,
    errorHandler: (Int, String) -> Unit,
) : CppPeerConnectionClientFactory(config, errorHandler) {

    override fun createPeerConnectionClient(
        peerUid: String, dir: Int, hasVideo: Boolean,
        videoMaxBitrateBps: Int, videoMaxFrameRate: Int,
        callback: PeerConnectionClientCallback
    ) = LinuxPeerConnectionClient(peerUid, dir, hasVideo, videoMaxBitrateBps, videoMaxFrameRate, callback)
}

actual fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    errorHandler: (Int, String) -> Unit,
): PeerConnectionClientFactory {
    val privateConfig = config.privateConfig as LinuxPrivateConfig
    val disableEncryption = if (privateConfig.disableEncryption) 1 else 0
    WebRTC.PCClientCreatePeerConnectionFactory(privateConfig.hwnd, disableEncryption, 0, 0)
    return LinuxPeerConnectionClientFactory(config, errorHandler)
}
