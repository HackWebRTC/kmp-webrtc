package com.piasy.kmp.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection.Callback
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.piasy.avconf.capturer.YuvCapturer
import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.xlog.initializeMarsXLog
import org.webrtc.*
import org.webrtc.CameraVideoCapturer.CameraEventsHandler
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Created by Piasy{github.com/Piasy} on 2019-12-02.
 */
data class AndroidPrivateConfig(
    internal val lifecycle: Lifecycle,
    internal val rootEglBase: EglBase,
    internal val options: PeerConnectionFactory.Options,
    internal val enableH264HighProfile: Boolean = true,
    internal val mediaProjectionPermissionResultData: Intent? = null,
    internal val recordSamplesReadyCallback: JavaAudioDeviceModule.SamplesReadyCallback? = null,
    internal val trackSamplesReadyCallback: JavaAudioDeviceModule.SamplesReadyCallback? = null,
) : PeerConnectionClientFactory.PrivateConfig()

class AndroidPeerConnectionClientFactory(
    config: Config,
    errorHandler: (Int, String) -> Unit,
    appContext: Context,
    private val privateConfig: AndroidPrivateConfig,
) : PeerConnectionClientFactory(config, errorHandler, AndroidAudioDeviceManager(appContext, SpeakerphoneMode.AUTO)),
    DefaultLifecycleObserver {
    init {
        privateConfig.lifecycle.addObserver(this)
    }

    private val rootEglBase = EglBase.create()
    private var videoCapturer: VideoCapturer? = null
    private var videoCapturePaused = false

    private val cameraEventsHandler: CameraEventsHandler = object : CameraEventsHandler {
        override fun onCameraError(errorDescription: String?) {
            logE("onCameraError $errorDescription")
            errorHandler(ERR_CAMERA_CAPTURER_FAIL, errorDescription ?: "")
        }

        override fun onCameraOpening(cameraName: String?) {
        }

        override fun onCameraDisconnected() {
        }

        override fun onCameraFreezed(errorDescription: String?) {
            logE("onCameraFreezed $errorDescription")
            errorHandler(ERR_CAMERA_CAPTURER_FAIL, errorDescription ?: "")
        }

        override fun onFirstFrameAvailable() {
        }

        override fun onCameraClosed() {
        }
    }

    override fun createPeerConnectionClient(
        peerUid: String, dir: Int, hasVideo: Boolean,
        videoMaxBitrate: Int, videoMaxFrameRate: Int,
        callback: PeerConnectionClientCallback
    ): PeerConnectionClient {
        return AndroidPeerConnectionClient(
            peerUid, dir, hasVideo, videoMaxBitrate, videoMaxFrameRate, callback
        )
    }

    override fun createLocalTracks() {
        val appContext = sAppContext ?: return
        videoCapturer = createVideoCapturer()
        if (videoCapturer == null) {
            logE("createLocalTracks fail: createCapturer fail")
            errorHandler(ERR_VIDEO_CAPTURER_CREATE_FAIL, "")
            return
        }
        com.piasy.avconf.PeerConnectionClient.createLocalTracks(appContext, rootEglBase, videoCapturer)
    }

    override fun addLocalTrackRenderer(renderer: Any) {
        if (renderer is VideoSink) {
            com.piasy.avconf.PeerConnectionClient.addLocalTrackRenderer(renderer)
        }
    }

    override fun startVideoCapture() {
        videoCapturer?.startCapture(config.videoCaptureWidth, config.videoCaptureHeight, config.videoCaptureFps)
    }

    override fun stopVideoCapture() {
        videoCapturer?.stopCapture()
    }

    override fun switchCamera(onFinished: (Boolean) -> Unit) {
        if (videoCapturer is CameraVideoCapturer) {
            (videoCapturer as CameraVideoCapturer?)?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    onFinished(isFrontCamera)
                }

                override fun onCameraSwitchError(error: String) {
                    errorHandler(ERR_CAMERA_CAPTURER_FAIL, error)
                }
            })
        }
    }

    override fun adaptVideoOutputFormat(
        width: Int,
        height: Int,
        fps: Int
    ) {
        com.piasy.avconf.PeerConnectionClient.adaptVideoOutputFormat(width, height, fps)
    }

    override fun destroyPeerConnectionFactory() {
        super.destroyPeerConnectionFactory()
        com.piasy.avconf.PeerConnectionClient.destroyPeerConnectionFactory()
    }

    fun setMediaProjectionPermissionResultData(data: Intent) {
        val capturer = videoCapturer
        if (capturer is ScreenCapturerAndroid) {
            capturer.setMediaProjectionPermissionResultData(data)
        }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        when {
            config.screenShare -> {
                return if (privateConfig.mediaProjectionPermissionResultData is Intent) {
                    ScreenCapturerAndroid(
                        privateConfig.mediaProjectionPermissionResultData,
                        object : Callback() {})
                } else {
                    ScreenCapturerAndroid(null, object : Callback() {})
                }
            }

            config.videoCaptureImpl == Config.VIDEO_CAPTURE_IMPL_APP -> {
                return YuvCapturer()
            }

            else -> {
                return createCameraCapturer()
            }
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera1Enumerator(false)
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        logI("createCapturer: Looking for front facing cameras")
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)
                && config.initCameraFace == Config.CAMERA_FACE_FRONT
            ) {
                logI("Creating front facing camera capturer")
                val videoCapturer = enumerator.createCapturer(deviceName, cameraEventsHandler)

                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        logI("createCapturer: Looking for other cameras")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                logI("Creating other camera capturer")
                val videoCapturer = enumerator.createCapturer(deviceName, cameraEventsHandler)

                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        logE("createCapturer: No capturer found")

        return null
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (videoCapturePaused) {
            videoCapturer?.startCapture(config.videoCaptureWidth, config.videoCaptureHeight, config.videoCaptureFps)
            videoCapturePaused = false
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        videoCapturer?.stopCapture()
        videoCapturePaused = true
    }

    companion object {
        internal var sAppContext: Context? = null

        // only WebRTC Android have tag for log callback
        internal val loggable: Loggable = Loggable { message, severity, tag ->
            when (severity) {
                org.webrtc.Logging.Severity.LS_VERBOSE -> Logging.debug(tag, message)
                org.webrtc.Logging.Severity.LS_INFO, org.webrtc.Logging.Severity.LS_WARNING
                    -> Logging.info(tag, message)

                org.webrtc.Logging.Severity.LS_ERROR -> Logging.error(tag, message)
                else -> {}
            }
        }
    }
}

actual fun initializeWebRTC(context: Any?, fieldTrials: String, debugLog: Boolean): Boolean {
    if (PeerConnectionClientFactory.sInitialized) {
        Logging.info(
            PeerConnectionClientFactory.TAG,
            "initialize ${BuildConfig.VERSION_NAME}, already initialized"
        )
        return true
    }
    if (context !is Context) {
        return false
    }
    val appContext = context.applicationContext
    AndroidPeerConnectionClientFactory.sAppContext = appContext

    val storagePath = appContext.getExternalFilesDir(null)?.absolutePath ?: return false
    initializeMarsXLog(
        appContext,
        "$storagePath/webrtc/log",
        if (debugLog) Logging.LEVEL_DEBUG else Logging.LEVEL_INFO,
        "webrtc",
        debugLog
    )

    com.piasy.avconf.PeerConnectionClient.initialize(
        appContext, fieldTrials, AndroidPeerConnectionClientFactory.loggable,
        if (debugLog) org.webrtc.Logging.Severity.LS_VERBOSE else org.webrtc.Logging.Severity.LS_INFO
    )
    Logging.info(PeerConnectionClientFactory.TAG, "initialize ${BuildConfig.VERSION_NAME}")
    PeerConnectionClientFactory.sInitialized = true
    return true
}

actual fun createPeerConnectionClientFactory(
    config: PeerConnectionClientFactory.Config,
    errorHandler: (Int, String) -> Unit,
): PeerConnectionClientFactory {
    val appContext = AndroidPeerConnectionClientFactory.sAppContext!!
    val privateConfig = config.privateConfig as AndroidPrivateConfig
    com.piasy.avconf.PeerConnectionClient.createPeerConnectionFactory(
        appContext,
        privateConfig.rootEglBase,
        privateConfig.options,
        privateConfig.recordSamplesReadyCallback,
        privateConfig.trackSamplesReadyCallback,
        privateConfig.enableH264HighProfile,
    )
    return AndroidPeerConnectionClientFactory(config, errorHandler, appContext, privateConfig)
}
