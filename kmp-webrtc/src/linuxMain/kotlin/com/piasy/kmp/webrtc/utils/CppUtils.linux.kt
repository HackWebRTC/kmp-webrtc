package com.piasy.kmp.webrtc.utils

import com.piasy.kmp.webrtc.PeerConnectionClient
import com.piasy.kmp.webrtc.PeerConnectionClientFactory
import com.piasy.kmp.webrtc.LinuxPrivateConfig
import kotlinx.cinterop.COpaquePointer

/**
 * Created by Piasy{github.com/Piasy} on 2025-03-06.
 *
 * Utility functions used by cpp example code.
 */

fun createPcClientFactoryConfig(
    config: PeerConnectionClientFactory.Config, privateConfig: LinuxPrivateConfig
): PeerConnectionClientFactory.Config {
    return PeerConnectionClientFactory.Config(
        config.videoCaptureImpl, config.videoCaptureWidth, config.videoCaptureHeight, config.videoCaptureFps,
        config.initCameraFace, privateConfig
    )
}

fun addRemoteTrackRenderer(client: PeerConnectionClient, renderer: COpaquePointer) {
    client.addRemoteTrackRenderer(renderer)
}
