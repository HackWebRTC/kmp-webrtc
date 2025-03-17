#include "libKmpWebrtc.h"

#include <cstring>

#if defined(WEBRTC_WIN)
#include "kmp_webrtc_api.h"
#elif defined(WEBRTC_LINUX)
#include "libkmp_webrtc_api.h"
#else
#error "Unknown target"
#endif

#include "lib_pc_client.h"

#if defined(WEBRTC_WIN)
#define KT_SYMBOL(sym) sym
#elif defined(WEBRTC_LINUX)
#define KT_SYMBOL(sym) lib##sym
#else
#error "Unknown target"
#endif

#define KFunc(NAME) g_lib->kotlin.root.com.piasy.kmp.webrtc.NAME
#define KType(NAME) KT_SYMBOL(kmp_webrtc_kref_com_piasy_kmp_webrtc_##NAME)

static KT_SYMBOL(kmp_webrtc_ExportedSymbols)* g_lib = nullptr;

int InitializeWebRTC(const char* field_trials, int debug_log) {
    if (!g_lib) {
        g_lib = KT_SYMBOL(kmp_webrtc_symbols)();
    }
    return KFunc(initializeWebRTC)(KT_SYMBOL(kmp_webrtc_kref_kotlin_Any)(), field_trials, debug_log);
}

struct PCClientFactoryConfig* DefaultPCClientFactoryConfig() {
    PCClientFactoryConfig* config = new PCClientFactoryConfig();
    config->video_capture_impl = kKmpWebRTCCaptureSystemCamera;
    config->video_capture_width = 1280;
    config->video_capture_height = 720;
    config->video_capture_fps = 30;
    config->private_config.hwnd = nullptr;
    return config;
}

void PCClientFactoryConfigDestroy(struct PCClientFactoryConfig** config) {
    delete (*config);
    *config = nullptr;
}

struct PcClientFactoryHolder {
    KType(PeerConnectionClientFactory) factory;
};

void* CreatePCClientFactory(struct PCClientFactoryConfig* config, PCClientFactoryErrorHandler handler, void* opaque) {
    KType(PeerConnectionClientFactory_PrivateConfig) pconfig = KFunc(PeerConnectionClientFactory.PrivateConfig.PrivateConfig)();
    KType(PeerConnectionClientFactory_Config) k_config = KFunc(PeerConnectionClientFactory.Config.Config)(
        (int) config->video_capture_impl, config->video_capture_width, config->video_capture_height,
        config->video_capture_fps, 0, pconfig);
#if defined(WEBRTC_WIN)
    KType(WinPrivateConfig) private_config = KFunc(WinPrivateConfig.WinPrivateConfig)(config->private_config.hwnd, config->private_config.disable_encryption);
#else
    KType(LinuxPrivateConfig) private_config = KFunc(LinuxPrivateConfig.LinuxPrivateConfig)(config->private_config.hwnd, config->private_config.disable_encryption, config->private_config.capture_file_path);
#endif
    KType(PeerConnectionClientFactory_Config) k_config_with_pri = KFunc(utils.createPcClientFactoryConfig)(k_config, private_config);

    KT_SYMBOL(kmp_webrtc_kref_kotlin_Function2) error_handler = KFunc(utils.createErrorHandler)((void*) handler, opaque);

    PcClientFactoryHolder* holder = new PcClientFactoryHolder();
    holder->factory = KFunc(createPeerConnectionClientFactory)(k_config_with_pri, error_handler);
    return holder;
}

void DestroyPCClientFactory(void** pc_client_factory) {
    PcClientFactoryHolder* holder = reinterpret_cast<PcClientFactoryHolder*>(*pc_client_factory);
    KFunc(PeerConnectionClientFactory.destroyPeerConnectionFactory)(holder->factory);
    delete (holder);
    *pc_client_factory = nullptr;
}

void CreateLocalTracks(void* pc_client_factory) {
    PcClientFactoryHolder* holder = reinterpret_cast<PcClientFactoryHolder*>(pc_client_factory);
    KFunc(PeerConnectionClientFactory.createLocalTracks)(holder->factory);
}

void StartVideoCapture(void* pc_client_factory) {
    PcClientFactoryHolder* holder = reinterpret_cast<PcClientFactoryHolder*>(pc_client_factory);
    KFunc(PeerConnectionClientFactory.startVideoCapture)(holder->factory);
}

void StopVideoCapture(void* pc_client_factory) {
    PcClientFactoryHolder* holder = reinterpret_cast<PcClientFactoryHolder*>(pc_client_factory);
    KFunc(PeerConnectionClientFactory.stopVideoCapture)(holder->factory);
}

struct PcClientHolder {
    KType(PeerConnectionClient) client;
};

void* CreatePeerConnectionClient(void* pc_client_factory, const char* peer_uid, KmpWebRTCDir dir, int has_video,
                                 int video_max_bitrate_kbps, int video_max_frame_rate, PCClientCallback* callback, void* opaque) {
    PcClientFactoryHolder* factory_holder = reinterpret_cast<PcClientFactoryHolder*>(pc_client_factory);
    KType(PeerConnectionClientCallback) pc_client_callback = KFunc(utils.createPcClientCallback)(callback, opaque);

    PcClientHolder* holder = new PcClientHolder();
    holder->client = KFunc(PeerConnectionClientFactory.createPeerConnectionClient)(factory_holder->factory, peer_uid, (int)dir,
        has_video, video_max_bitrate_kbps, video_max_frame_rate, pc_client_callback);
    return holder;
}

void ClosePeerConnectionClient(void** pc_client) {
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(*pc_client);
    KFunc(PeerConnectionClient.close)(holder->client);
    delete (holder);
    *pc_client = nullptr;
}

void CreatePeerConnection(void* pc_client) {
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(pc_client);
    KT_SYMBOL(kmp_webrtc_kref_kotlin_collections_List) ice_servers = KFunc(utils.emptyIceServers)();
    KFunc(PeerConnectionClient.createPeerConnection)(holder->client, ice_servers);
}

void CreateOffer(void* pc_client) {
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(pc_client);
    KFunc(PeerConnectionClient.createOffer)(holder->client);
}

void SetRemoteDescription(void* pc_client, KmpWebRTCSdpType type, const char* sdp) {
    KType(data_SessionDescription) answer = KFunc(data.SessionDescription.SessionDescription)((int) type, sdp);
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(pc_client);
    KFunc(PeerConnectionClient.setRemoteDescription)(holder->client, answer);
}

void AddIceCandidate(void* pc_client, const char* sdp_mid, int m_line_index, const char* sdp) {
    KType(data_IceCandidate) candidate = KFunc(data.IceCandidate.IceCandidate)(sdp_mid, m_line_index, sdp);
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(pc_client);
    KFunc(PeerConnectionClient.addIceCandidate)(holder->client, candidate);
}

void GetStats(void* pc_client) {
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(pc_client);
    KFunc(PeerConnectionClient.getStats)(holder->client);
}

int StartRecorder(void* pc_client, int dir, const char* path) {
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(pc_client);
    return KFunc(PeerConnectionClient.startRecorder)(holder->client, dir, path);
}

int StopRecorder(void* pc_client, int dir) {
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(pc_client);
    return KFunc(PeerConnectionClient.stopRecorder)(holder->client, dir);
}

#if defined(WEBRTC_WIN)
void AddRemoteTrackRenderer(void* pc_client, void* renderer) {
    PcClientHolder* holder = reinterpret_cast<PcClientHolder*>(pc_client);
    KFunc(utils.addRemoteTrackRenderer)(holder->client, renderer);
}
#endif

void LogInfo(const char* log) {
    KFunc(utils.logInfo)(log);
}
