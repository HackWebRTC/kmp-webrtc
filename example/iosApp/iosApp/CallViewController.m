//
/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
//


#import "CallViewController.h"

#import "ARDSettingsModel.h"
#import "ARDToast.h"

@import WebRTC;
@import kmp_webrtc;

@interface CallViewController () <Kmp_webrtcPeerConnectionClientCallback, CFAudioMixerDelegate>
@end

@implementation CallViewController {
    ARDSettingsModel* _settingsModel;
    bool _isLandscape;

    Kmp_webrtcIOSPeerConnectionClientFactory* _pcClientFactory;
    id<Kmp_webrtcPeerConnectionClient> _pcClient;
    NSTimer* _statsTimer;
    CFAudioMixer* _mixer;

    CFEAGLVideoView* _remoteRenderer;
    CFEAGLVideoView* _localRenderer;

    UIButton* _leaveButton;
    UIButton* _recordButton;
    UIButton* _mixerButton;
    UIButton* _switchCameraButton;

    bool _recording;
    bool _mixingMusic;
    bool _videoEnabled;

    bool _sendLastFrame;
    bool _left;
}

- (instancetype)initWithAudioOnly:(bool)audioOnly
                      isLandscape:(bool)isLandscape {
    if (self = [super init]) {
        _settingsModel = [[ARDSettingsModel alloc] init];
        _isLandscape = isLandscape;
    }
    return self;
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    return _isLandscape ? UIInterfaceOrientationMaskLandscape
    : UIInterfaceOrientationMaskPortrait;
}

- (void)viewDidLoad {
    [super viewDidLoad];

    _localRenderer =
        [[CFEAGLVideoView alloc] initWithFrame:CGRectZero
                                        andUid:@"test_local"
                                  andScaleType:CF_SCALE_TYPE_CENTER_CROP];
    _localRenderer.mirror = YES;
    _localRenderer.autoresizingMask =
        UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

    _remoteRenderer =
        [[CFEAGLVideoView alloc] initWithFrame:CGRectZero
                                        andUid:@"test_remote"
                                  andScaleType:CF_SCALE_TYPE_CENTER_CROP];
    _remoteRenderer.mirror = NO;
    _remoteRenderer.autoresizingMask =
        UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

    UIView* localWrapper = [[UIView alloc] init];
    [localWrapper addSubview:_localRenderer];
    UIView* remoteWrapper = [[UIView alloc] init];
    [remoteWrapper addSubview:_remoteRenderer];

    UIStackView *mainStackView = [[UIStackView alloc] init];
    mainStackView.axis = UILayoutConstraintAxisVertical;
    mainStackView.distribution = UIStackViewDistributionFillEqually;
    mainStackView.alignment = UIStackViewAlignmentFill;
    mainStackView.spacing = 0;
    mainStackView.translatesAutoresizingMaskIntoConstraints = NO;

    [mainStackView addArrangedSubview:localWrapper];
    [mainStackView addArrangedSubview:remoteWrapper];
    [self.view addSubview:mainStackView];

    [NSLayoutConstraint activateConstraints:@[
        [mainStackView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [mainStackView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [mainStackView.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [mainStackView.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
    ]];

    _recordButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [_recordButton setTitle:_recording ? @"stop record" : @"start record"
                   forState:UIControlStateNormal];
    _recordButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [_recordButton addTarget:self
                      action:@selector(onRecord:)
            forControlEvents:UIControlEventTouchUpInside];

    _mixerButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [_mixerButton setTitle:_mixingMusic ? @"stop mixer" : @"start mixer"
                   forState:UIControlStateNormal];
    _mixerButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [_mixerButton addTarget:self
                      action:@selector(onMixer:)
            forControlEvents:UIControlEventTouchUpInside];

    _leaveButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [_leaveButton setTitle:@"Leave" forState:UIControlStateNormal];
    _leaveButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [_leaveButton addTarget:self
                     action:@selector(onLeaveCall:)
           forControlEvents:UIControlEventTouchUpInside];

    UIStackView *ops1 = [[UIStackView alloc] init];
    ops1.axis = UILayoutConstraintAxisHorizontal;
    ops1.distribution = UIStackViewDistributionFillEqually;
    ops1.alignment = UIStackViewAlignmentFill;
    ops1.spacing = 0;
    ops1.translatesAutoresizingMaskIntoConstraints = NO;

    [ops1 addArrangedSubview:_recordButton];
    [ops1 addArrangedSubview:_mixerButton];
    [ops1 addArrangedSubview:_leaveButton];
    [self.view addSubview:ops1];

    [NSLayoutConstraint activateConstraints:@[
        [ops1.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [ops1.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [ops1.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
        [ops1.heightAnchor constraintGreaterThanOrEqualToConstant:30]
    ]];

    _switchCameraButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [_switchCameraButton setTitle:@"FRONT" forState:UIControlStateNormal];
    _switchCameraButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [_switchCameraButton addTarget:self
                            action:@selector(onSwitchCamera:)
                  forControlEvents:UIControlEventTouchUpInside];

    UIButton* videoButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [videoButton setTitle:@"Video" forState:UIControlStateNormal];
    videoButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [videoButton addTarget:self
                    action:@selector(onToggleVideo:)
          forControlEvents:UIControlEventTouchUpInside];

    UIButton* sendLastFrameButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [sendLastFrameButton setTitle:@"SLF" forState:UIControlStateNormal];
    sendLastFrameButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [sendLastFrameButton addTarget:self
                            action:@selector(onToggleSendLastFrame:)
                  forControlEvents:UIControlEventTouchUpInside];

    UIStackView *ops2 = [[UIStackView alloc] init];
    ops2.axis = UILayoutConstraintAxisHorizontal;
    ops2.distribution = UIStackViewDistributionFillEqually;
    ops2.alignment = UIStackViewAlignmentFill;
    ops2.spacing = 0;
    ops2.translatesAutoresizingMaskIntoConstraints = NO;

    [ops2 addArrangedSubview:_switchCameraButton];
    [ops2 addArrangedSubview:videoButton];
    [ops2 addArrangedSubview:sendLastFrameButton];
    [self.view addSubview:ops2];

    [NSLayoutConstraint activateConstraints:@[
        [ops2.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [ops2.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [ops2.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor constant:-60],
        [ops2.heightAnchor constraintGreaterThanOrEqualToConstant:30]
    ]];

    [self startLoopback];
}

- (void)viewWillDisappear:(BOOL)animated {
    if (!_left) {
        [self hangup:NO];
    }
}

- (void)dealloc {
    if (!_left) {
        [self hangup:NO];
    }
}

- (void)hangup:(BOOL)dismissView {
    _left = true;

    [_mixer stopMixer];
    _mixer = nil;

    [_statsTimer invalidate];
    _statsTimer = nil;
    [_pcClientFactory stopVideoCapture];
    [_pcClient close];
    [_pcClientFactory destroyPeerConnectionFactory];

    if (dismissView) {
        [self dismissViewControllerAnimated:YES completion:nil];
    }
}

- (void)startGetStats {
    _statsTimer = [NSTimer scheduledTimerWithTimeInterval:5.0
                                                   target:self
                                                 selector:@selector(getStats:)
                                                 userInfo:nil
                                                  repeats:YES];
}

- (void)getStats:(NSTimer*)timer {
    [_pcClient getStats];
}

- (void)onRecord:(id)sender {
    _recording = !_recording;
    [_recordButton setTitle:_recording ? @"stop record" : @"start record"
                   forState:UIControlStateNormal];
    CFPeerConnectionClient* realClient = (CFPeerConnectionClient*) [_pcClient getRealClient];
    if (_recording) {
        [realClient startRecorder:RTCRtpTransceiverDirectionSendOnly path:[self pathUnderDocuments:@"send.mkv"]];
    } else {
        [realClient stopRecorder:RTCRtpTransceiverDirectionSendOnly];
    }
}

- (void)onMixer:(id)sender {
    _mixingMusic = !_mixingMusic;
    [_mixerButton setTitle:_mixingMusic ? @"stop mixer" : @"start mixer"
                  forState:UIControlStateNormal];
    if (_mixingMusic) {
        _mixer = [[CFAudioMixer alloc]
            initWithBackingTrack:[self pathForFileName:@"mozart.mp3"]
               captureSampleRate:48000
               captureChannelNum:1
                 frameDurationUs:10000
              enableMusicSyncFix:false
            waitingMixDelayFrame:5
                        delegate:self];
        [_mixer startMixer];
        [_mixer toggleMusicStreaming:true];
    } else {
        [_mixer stopMixer];
        _mixer = nil;
    }
}

- (nullable NSString*)pathUnderDocuments:(NSString*)fileName {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirPath = paths.firstObject;
    NSString *filePath =
        [documentsDirPath stringByAppendingPathComponent:fileName];
    return filePath;
}

- (nullable NSString*)pathForFileName:(NSString*)fileName {
    NSArray* nameComponents = [fileName componentsSeparatedByString:@"."];
    if (nameComponents.count != 2) {
        return nil;
    }

    NSString* path = [[NSBundle mainBundle] pathForResource:nameComponents[0]
                                                     ofType:nameComponents[1]];
    return path;
}

- (void)onSwitchCamera:(id)sender {
    _switchCameraButton.enabled = NO;
    __weak typeof(self) wself = self;
    [_pcClientFactory switchCameraOnFinished:^(Kmp_webrtcBoolean * _Nonnull isFront) {
        dispatch_async(dispatch_get_main_queue(), ^{
            typeof(self) sself = wself;
            if (sself == nil) {
                return;
            }
            sself->_switchCameraButton.enabled = YES;
            [sself->_switchCameraButton setTitle:[isFront boolValue] ? @"FRONT" : @"BACK" forState:UIControlStateNormal];
        });
    }];
}

- (void)onToggleVideo:(id)sender {
    _videoEnabled = !_videoEnabled;
//    [_avConf setVideoEnabled:_videoEnabled];
}

- (void)onToggleSendLastFrame:(id)sender {
    _sendLastFrame = !_sendLastFrame;
    
//    NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
//                                                         NSUserDomainMask, YES);
//    NSString* docDir = [paths objectAtIndex:0];
//    long long nowMs = (long long)([[NSDate date] timeIntervalSince1970] * 1000.0);
//    NSString* path = [NSString stringWithFormat:@"%@/%lld.jpg", docDir, nowMs];
//    [_avConf toggleSendLastFrame:_sendLastFrame ? path : nil
//                        callback:_sendLastFrame ? [[OWTJpegFrameCallback alloc] initWithVC:self] : nil];
}

- (void)onLeaveCall:(id)sender {
    [self hangup:YES];
}

- (void)startLoopback {
    int videoWidth = [_settingsModel currentVideoResolutionWidthFromStore];
    int videoHeight = [_settingsModel currentVideoResolutionHeightFromStore];

    // 1. initialize
    NSString *fieldTrials = @"WebRTC-Video-H26xPacketBuffer/Enabled/";
    [Kmp_webrtcObjCPeerConnectionClientFactoryKt initializeWebRTCContext:nil fieldTrials:fieldTrials debugLog:YES];

    // 2. create PcClientFactory
    CFPeerConnectionFactoryOption* option = [[CFPeerConnectionFactoryOption alloc] init];
    option.preferredVideoCodec = [_settingsModel currentVideoCodecSettingFromStore];
    option.disableEncryption = YES;
    Kmp_webrtcObjCPrivateConfig* privateConfig = [[Kmp_webrtcObjCPrivateConfig alloc] initWithPcFactoryOption:option];

    Kmp_webrtcPeerConnectionClientFactoryConfig* pcClientFactoryConfig
    = [[Kmp_webrtcPeerConnectionClientFactoryConfig alloc]
        initWithVideoCaptureImpl:1
               videoCaptureWidth:videoWidth
              videoCaptureHeight:videoHeight
                 videoCaptureFps:30
                  initCameraFace:0
                   privateConfig:privateConfig];
    _pcClientFactory = (Kmp_webrtcIOSPeerConnectionClientFactory*)
    [Kmp_webrtcIOSPeerConnectionClientFactoryKt createPeerConnectionClientFactoryConfig:pcClientFactoryConfig
                                                                           errorHandler:^(Kmp_webrtcInt * _Nonnull code, NSString * _Nonnull msg) {
        NSLog(@"XXPXX PCFactory errorHandler %@ %@", code, msg);
    }];

    // 3. create local tracks
    [_pcClientFactory createLocalTracks];

    // 4. add local preview & start camera capture
    [_pcClientFactory addLocalTrackRendererRenderer:_localRenderer];
    [_pcClientFactory startVideoCapture];

    // 5. create PcClient
    _pcClient = [_pcClientFactory createPeerConnectionClientPeerUid:@"test"
                                                               dir:0
                                                          hasVideo:YES
                                                   videoMaxBitrate:[[_settingsModel currentMaxBitrateSettingFromStore] intValue]
                                                 videoMaxFrameRate:30
                                                          callback:self];

    // 6. create pc
    NSArray* iceServers = [NSArray array];
    [_pcClient createPeerConnectionIceServers:iceServers];

    // 7. create offer
    [_pcClient createOffer];
}

#pragma mark - Kmp_webrtcPeerConnectionClientCallback

- (void)onLocalDescriptionPeerUid:(nonnull NSString *)peerUid sdp:(nonnull Kmp_webrtcSessionDescription *)sdp { 
    // 8. send offer to remote, get answer from remote, and set answer
    Kmp_webrtcSessionDescription* answer = [[Kmp_webrtcSessionDescription alloc] initWithType:3 sdpDescription:sdp.sdpDescription];
    [_pcClient setRemoteDescriptionSdp:answer];
}

- (void)onIceCandidatePeerUid:(nonnull NSString *)peerUid candidate:(nonnull Kmp_webrtcIceCandidate *)candidate {
    // 9. send ice candidate to remote, get ice candidate from remote, add ice candidate
    [_pcClient addIceCandidateCandidate:candidate];
}

- (void)onIceConnectedPeerUid:(nonnull NSString *)peerUid {
    NSLog(@"XXPXX onIceConnected %@", peerUid);
    __weak typeof(self) wself = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        typeof(self) sself = wself;
        if (sself == nil) {
            return;
        }
        // 10. on ice connected, add renderer for remote stream
        [sself->_pcClient addRemoteTrackRendererRenderer:sself->_remoteRenderer];

        [sself startGetStats];
    });
}

- (nonnull NSString *)onPreferCodecsPeerUid:(nonnull NSString *)peerUid sdp:(nonnull NSString *)sdp {
    return sdp;
}

- (void)onErrorPeerUid:(nonnull NSString *)peerUid code:(int32_t)code {
    NSLog(@"XXPXX onError %@ %d", peerUid, code);
}

- (void)onIceCandidatesRemovedPeerUid:(nonnull NSString *)peerUid candidates:(nonnull NSArray<Kmp_webrtcIceCandidate *> *)candidates {
}

- (void)onPeerConnectionStatsReadyPeerUid:(nonnull NSString *)peerUid report:(nonnull Kmp_webrtcRtcStatsReport *)report {
    NSLog(@"XXPXX onPeerConnectionStatsReady %@", report);
}

- (void)onIceDisconnectedPeerUid:(nonnull NSString *)peerUid {
}

#pragma mark - CFAudioMixerDelegate

- (void)onSsrcError:(int32_t)ssrc code:(int32_t)code { 
    NSLog(@"XXPXX onSsrcError %d %d", ssrc, code);
    [self onMixerStopped];
}

- (void)onSsrcFinished:(int32_t)ssrc { 
    NSLog(@"XXPXX onSsrcFinished %d", ssrc);
    [self onMixerStopped];
}

- (void)onMixerStopped {
    __weak typeof(self) wself = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        typeof(self) sself = wself;
        sself->_mixingMusic = NO;
        [sself->_mixerButton setTitle:@"start mixer"
                             forState:UIControlStateNormal];
        [sself->_mixer stopMixer];
        sself->_mixer = nil;
    });
}

@end
