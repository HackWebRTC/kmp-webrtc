#pragma once

#include "lib_pc_client.h"

#if defined(WEBRTC_WIN)
#if defined(SELF_KMP_WEBRTC_DLL)
#define KMP_WEBRTC_API __declspec(dllexport)
#else
#define KMP_WEBRTC_API __declspec(dllimport)
#endif
#else
#define KMP_WEBRTC_API
#endif

#if __cplusplus
extern "C" {
#endif

enum KmpWebRTCDir {
    kKmpWebRTCDirSendRecv = 0,
    kKmpWebRTCDirSendOnly = 1,
    kKmpWebRTCDirRecvOnly = 2,
    kKmpWebRTCDirInactive = 3,
};

enum KmpWebRTCVideoCodec {
    kKmpWebRTCVideoCodecVP8 = 1,
    kKmpWebRTCVideoCodecVP9 = 2,
    kKmpWebRTCVideoCodecH264Baseline = 3,
    kKmpWebRTCVideoCodecH264HighProfile = 4,
    kKmpWebRTCVideoCodecH265 = 5,
    kKmpWebRTCVideoCodecAV1 = 6,
};

enum KmpWebRTCCaptureImpl {
    kKmpWebRTCCaptureSystemCamera = 1,
    kKmpWebRTCCaptureScreen = 2,
    kKmpWebRTCCaptureFile = 3,
    kKmpWebRTCCaptureApp = 4,
};

enum KmpWebRTCSdpType {
    kKmpWebRTCSdpOffer = 1,
    kKmpWebRTCSdpPrAnswer = 2,
    kKmpWebRTCSdpAnswer = 3,
};

enum KmpWebRTCError {
    kKmpWebRTCErrPcFail = 1001,
    kKmpWebRTCErrCreateLocalTracksFail = 1002,
    kKmpWebRTCErrVideoCapturerCreateFail = 1101,
    kKmpWebRTCErrCameraCapturerFail = 1102,
    kKmpWebRTCErrScreenCapturerFail = 1103,
    kKmpWebRTCErrVideoEncoderError = 1131,
    kKmpWebRTCErrAudioCaptureError = 1201,
    kKmpWebRTCErrAudioPlaybackError = 1221,
};

struct PCClientFactoryPrivateConfig {
    void* hwnd;
    int disable_encryption;
    int dummy_audio_device;
    int transit_video;
    const char* capture_file_path;
    const char* capture_dump_path;
};

struct PCClientFactoryConfig {
    KmpWebRTCCaptureImpl video_capture_impl;
    int video_capture_width;
    int video_capture_height;
    int video_capture_fps;
    PCClientFactoryPrivateConfig private_config;
};

KMP_WEBRTC_API struct PCClientFactoryConfig* DefaultPCClientFactoryConfig();
KMP_WEBRTC_API void PCClientFactoryConfigDestroy(struct PCClientFactoryConfig** config);

typedef void (*PCClientFactoryErrorHandler)(void*, int, const char*);

KMP_WEBRTC_API int InitializeWebRTC(const char* field_trials, int debug_log);

KMP_WEBRTC_API void* CreatePCClientFactory(struct PCClientFactoryConfig* config, PCClientFactoryErrorHandler handler, void* opaque);
KMP_WEBRTC_API void DestroyPCClientFactory(void** pc_client_factory);

KMP_WEBRTC_API void CreateLocalTracks(void* pc_client_factory);
KMP_WEBRTC_API void StartVideoCapture(void* pc_client_factory);
KMP_WEBRTC_API void StopVideoCapture(void* pc_client_factory);

KMP_WEBRTC_API void* CreatePeerConnectionClient(void* pc_client_factory, const char* peer_uid, KmpWebRTCDir dir, int has_video,
                                                int video_max_bitrate_kbps, int video_max_frame_rate, PCClientCallback* callback, void* opaque);
KMP_WEBRTC_API void ClosePeerConnectionClient(void** pc_client);

KMP_WEBRTC_API void CreatePeerConnection(void* pc_client);
KMP_WEBRTC_API void CreateOffer(void* pc_client);
KMP_WEBRTC_API void SetRemoteDescription(void* pc_client, KmpWebRTCSdpType type, const char* sdp);
KMP_WEBRTC_API void AddIceCandidate(void* pc_client, const char* sdp_mid, int m_line_index, const char* sdp);
KMP_WEBRTC_API void GetStats(void* pc_client);

KMP_WEBRTC_API int StartRecorder(void* pc_client, int dir, const char* path);
KMP_WEBRTC_API int StopRecorder(void* pc_client, int dir);

#if defined(WEBRTC_WIN)
KMP_WEBRTC_API void AddRemoteTrackRenderer(void* pc_client, void* renderer);
#endif

KMP_WEBRTC_API void LogInfo(const char* log);

KMP_WEBRTC_API const char* PreferSdp(const char* sdp, int codec);

#if __cplusplus
}
#endif
