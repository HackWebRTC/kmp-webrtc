package com.piasy.kmp.webrtc

import WebRTC.*

/**
 * Created by Piasy{github.com/Piasy} on 2019-12-02.
 */
class IOSPeerConnectionClientFactory(
    config: Config,
    errorHandler: (Int, String) -> Unit,
) : ObjCPeerConnectionClientFactory(config, errorHandler, IOSAudioDeviceManager()) {
    private var screenCapturer: CFRPCapturer? = null
    private var isFrontCamera = false

    override fun createLocalTracks() {
        super.createLocalTracks()

        if (config.screenShare) {
            val screenCaptureErrorHandler: ((String?) -> Unit) = { error ->
                logE("screenCaptureError $error")
                errorHandler(ERR_SCREEN_CAPTURER_FAIL, error ?: "")
            }
            screenCapturer = CFRPCapturer(
                CFPeerConnectionClient.getHijackCapturerDelegate(), screenCaptureErrorHandler,
                config.videoCaptureHeight, false, config.videoCaptureFps
            )
        } else {
            isFrontCamera = config.initCameraFace == Config.CAMERA_FACE_FRONT
        }
    }

    override fun startVideoCapture() {
        super.startVideoCapture()
        screenCapturer?.startCapture()
    }

    override fun stopVideoCapture() {
        super.stopVideoCapture()
        screenCapturer?.stopCapture()
    }

    override fun switchCamera(onFinished: (Boolean) -> Unit) {
        isFrontCamera = !isFrontCamera
        cameraCaptureController?.switchCamera {
            if (it != null) {
                errorHandler(ERR_CAMERA_CAPTURER_FAIL, it.localizedDescription)
            } else {
                onFinished(isFrontCamera)
            }
        }
    }
}

actual fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    errorHandler: (Int, String) -> Unit,
): PeerConnectionClientFactory {
    CFPeerConnectionClient.createPeerConnectionFactory((config.privateConfig as ObjCPrivateConfig).pcFactoryOption)
    return IOSPeerConnectionClientFactory(config, errorHandler)
}
