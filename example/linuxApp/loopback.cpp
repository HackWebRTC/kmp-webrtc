#include "loopback.h"

#include <iostream>
#include <string>
#include <sstream>
#include <cwchar>
#include <cstring>
#include <chrono>
#include <inttypes.h>

#include "libKmpWebrtc.h"

void* gPcClientFactory = nullptr;
void* gPcClient = nullptr;
PCClientCallback* gPcClientCallback = nullptr;

static void pcClientFactoryErrorHandler(void*, int error, const char* message) {
    std::ostringstream oss;
    oss << "pcClientFactoryError code: " << error << ", message: " << message;
    LogInfo(oss.str().c_str());
}

static const char* pcClientOnPreferCodecs(void*, const char* peer_uid, const char* sdp) {
    return PreferSdp(sdp, kKmpWebRTCVideoCodecH264HighProfile);
}

static void pcClientOnPeerConnectionStatsReady(void*, const char* peer_uid, const char* stats) {
    std::ostringstream oss;
    oss << "pcClientOnPeerConnectionStatsReady " << peer_uid << " " << stats;
    LogInfo(oss.str().c_str());
}

static void pcClientOnIceDisconnected(void*, const char* peer_uid) {
}

static void pcClientOnError(void*, const char* peer_uid, int code) {
    std::ostringstream oss;
    oss << "pcClientError code: " << code;
    LogInfo(oss.str().c_str());
}

static void pcClientOnLocalDescription(void*, const char* peer_uid, int type, const char* sdp);
static void pcClientOnIceCandidate(void*, const char* peer_uid, const char* sdp_mid, int m_line_index, const char* sdp);
static void pcClientOnIceConnected(void*, const char* peer_uid);

void loopback(const char* path) {
    // 1. initialize
    InitializeWebRTC("", true);

    // 2. create gPcClientFactory
    PCClientFactoryConfig* config = DefaultPCClientFactoryConfig();
    config->video_capture_impl = kKmpWebRTCCaptureFile;
    config->private_config.disable_encryption = 1;
    config->private_config.capture_file_path = path;
    gPcClientFactory = CreatePCClientFactory(config, pcClientFactoryErrorHandler, nullptr);
    PCClientFactoryConfigDestroy(&config);

    // 3. create local tracks
    CreateLocalTracks(gPcClientFactory);

    // 4. add local preview & start camera capture
    StartVideoCapture(gPcClientFactory);

    // 5. create PcClient
    gPcClientCallback = new PCClientCallback();
    gPcClientCallback->on_prefer_codecs = pcClientOnPreferCodecs;
    gPcClientCallback->on_local_description = pcClientOnLocalDescription;
    gPcClientCallback->on_ice_candidate = pcClientOnIceCandidate;
    gPcClientCallback->on_stats_ready = pcClientOnPeerConnectionStatsReady;
    gPcClientCallback->on_ice_connected = pcClientOnIceConnected;
    gPcClientCallback->on_ice_disconnected = pcClientOnIceDisconnected;
    gPcClientCallback->on_error = pcClientOnError;
    gPcClient = CreatePeerConnectionClient(gPcClientFactory, "test", kKmpWebRTCDirSendRecv, 1, 800, 30, gPcClientCallback, nullptr);

    // 6. create pc
    CreatePeerConnection(gPcClient);

    // 7. create offer
    CreateOffer(gPcClient);
}

static void pcClientOnLocalDescription(void*, const char* peer_uid, int type, const char* sdp) {
    std::ostringstream oss;
    oss << "pcClientOnLocalDescription " << peer_uid << " " << sdp;
    LogInfo(oss.str().c_str());
    // 8. send offer to remote, get answer from remote, and set answer
    SetRemoteDescription(gPcClient, kKmpWebRTCSdpAnswer, sdp);
}

static void pcClientOnIceCandidate(void*, const char* peer_uid, const char* sdp_mid, int m_line_index, const char* sdp) {
    std::ostringstream oss;
    oss << "pcClientOnIceCandidate " << peer_uid << " " << sdp;
    LogInfo(oss.str().c_str());
    // 9. send ice candidate to remote, get ice candidate from remote, add ice candidate
    AddIceCandidate(gPcClient, sdp_mid, m_line_index, sdp);
}

static void pcClientOnIceConnected(void*, const char* peer_uid) {
    std::ostringstream oss;
    oss << "pcClientOnIceConnected " << peer_uid;
    LogInfo(oss.str().c_str());

    // 10. on ice connected, add renderer for remote stream
    StartRecorder(gPcClient, kKmpWebRTCDirRecvOnly, "recv.mkv");
}
