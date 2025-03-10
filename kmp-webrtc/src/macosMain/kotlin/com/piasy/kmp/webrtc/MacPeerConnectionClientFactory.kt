package com.piasy.kmp.webrtc

import WebRTC.*

/**
 * Created by Piasy{github.com/Piasy} on 2025-02-27.
 */
class MacPeerConnectionClientFactory(
    config: Config,
    errorHandler: (Int, String) -> Unit,
) : ObjCPeerConnectionClientFactory(config, errorHandler, DummyAudioDeviceManager())

actual fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    errorHandler: (Int, String) -> Unit,
): PeerConnectionClientFactory {
    CFPeerConnectionClient.createPeerConnectionFactory((config.privateConfig as ObjCPrivateConfig).pcFactoryOption)
    return MacPeerConnectionClientFactory(config, errorHandler)
}
