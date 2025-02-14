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

@import SDAutoLayout;
@import WebRTC;
@import kmp_webrtc;

@interface CallViewController () <Kmp_webrtcPeerConnectionClientCallback, CFAudioMixerDelegate>
@end

@implementation CallViewController {
    ARDSettingsModel* _settingsModel;
    bool _isLandscape;

    Kmp_webrtcObjCPeerConnectionClientFactory* _pcClientFactory;
    id<Kmp_webrtcPeerConnectionClient> _pcClient;
    NSTimer* _statsTimer;
    CFAudioMixer* _mixer;

    UIView* _rootLayout;
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

- (void)loadView {
    CGSize fc = [UIScreen mainScreen].bounds.size;
    CGFloat fcWidth = _isLandscape ? fc.height : fc.width;
    CGFloat fcHeight = _isLandscape ? fc.width : fc.height;

    self.view =
    [[UIView alloc] initWithFrame:CGRectMake(0, 0, fcWidth, fcHeight)];

    _rootLayout = [[UIView alloc] init];
    [self.view addSubview:_rootLayout];
    _rootLayout.sd_layout.widthRatioToView(self.view, 1)
        .heightRatioToView(self.view, 1);

    _recordButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [_recordButton setTitle:_recording ? @"stop record" : @"start record"
                   forState:UIControlStateNormal];
    _recordButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [_recordButton addTarget:self
                      action:@selector(onRecord:)
            forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:_recordButton];
    _recordButton.sd_layout.widthIs(150)
        .heightIs(20)
        .bottomSpaceToView(self.view, 30)
        .leftSpaceToView(self.view, 10);

    _mixerButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [_mixerButton setTitle:_mixingMusic ? @"stop mixer" : @"start mixer"
                   forState:UIControlStateNormal];
    _mixerButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [_mixerButton addTarget:self
                      action:@selector(onMixer:)
            forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:_mixerButton];
    _mixerButton.sd_layout.widthIs(150)
        .heightIs(20)
        .bottomEqualToView(_recordButton)
        .leftSpaceToView(_recordButton, 10);

    _leaveButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [_leaveButton setTitle:@"Leave" forState:UIControlStateNormal];
    _leaveButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [_leaveButton addTarget:self
                     action:@selector(onLeaveCall:)
           forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:_leaveButton];
    _leaveButton.sd_layout.widthIs(80)
        .heightIs(20)
        .bottomEqualToView(_recordButton)
        .leftSpaceToView(_mixerButton, 10);

    _switchCameraButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [_switchCameraButton setTitle:@"FRONT" forState:UIControlStateNormal];
    _switchCameraButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [_switchCameraButton addTarget:self
                            action:@selector(onSwitchCamera:)
                  forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:_switchCameraButton];
    _switchCameraButton.sd_layout.widthIs(80)
        .heightIs(20)
        .leftEqualToView(_recordButton)
        .bottomSpaceToView(_recordButton, 20);

    UIButton* videoButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [videoButton setTitle:@"Video" forState:UIControlStateNormal];
    videoButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [videoButton addTarget:self
                    action:@selector(onToggleVideo:)
          forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:videoButton];
    videoButton.sd_layout.widthIs(80)
        .heightIs(20)
        .bottomEqualToView(_switchCameraButton)
        .leftSpaceToView(_switchCameraButton, 10);

    UIButton* sendLastFrameButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [sendLastFrameButton setTitle:@"SLF" forState:UIControlStateNormal];
    sendLastFrameButton.titleLabel.font = [UIFont systemFontOfSize:20.0];
    [sendLastFrameButton addTarget:self
                            action:@selector(onToggleSendLastFrame:)
                  forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:sendLastFrameButton];
    sendLastFrameButton.sd_layout.widthIs(80)
        .heightIs(20)
        .bottomEqualToView(_switchCameraButton)
        .leftSpaceToView(videoButton, 10);

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

    UIView* wrapper = [[UIView alloc] init];
    [_rootLayout insertSubview:wrapper atIndex:0];
    wrapper.sd_layout.widthRatioToView(_rootLayout, 1)
        .heightRatioToView(_rootLayout, 0.5)
        .topEqualToView(_rootLayout);
    [wrapper addSubview:_localRenderer];

    wrapper = [[UIView alloc] init];
    [_rootLayout insertSubview:wrapper atIndex:0];
    wrapper.sd_layout.widthRatioToView(_rootLayout, 1)
        .heightRatioToView(_rootLayout, 0.5)
        .bottomEqualToView(_rootLayout);
    [wrapper addSubview:_remoteRenderer];

    int videoWidth = [_settingsModel currentVideoResolutionWidthFromStore];
    int videoHeight = [_settingsModel currentVideoResolutionHeightFromStore];

    // 1. initialize
    NSString *fieldTrials = @"WebRTC-Video-H26xPacketBuffer/Enabled/";
    [Kmp_webrtcObjCPeerConnectionClientFactoryKt initializeWebRTCAppContext:nil fieldTrials:fieldTrials debugLog:YES];

    // 2. create PcClientFactory
    Kmp_webrtcPeerConnectionClientFactoryConfig* pcClientFactoryConfig
    = [[Kmp_webrtcPeerConnectionClientFactoryConfig alloc]
       initWithVideoCaptureImpl:1
       videoCaptureWidth:videoWidth
       videoCaptureHeight:videoHeight
       videoCaptureFps:30
       initCameraFace:0];
    _pcClientFactory = (Kmp_webrtcObjCPeerConnectionClientFactory*)
      [Kmp_webrtcObjCPeerConnectionClientFactoryKt createPeerConnectionClientFactoryConfig:pcClientFactoryConfig
                                                                 screenCaptureErrorHandler:^(NSString * _Nullable error) {
        NSLog(@"XXPXX screenCaptureErrorHandler %@", error);
    }];

    // 3. create PcFactory
    CFPeerConnectionFactoryOption* option = [[CFPeerConnectionFactoryOption alloc] init];
    option.preferredVideoCodec = [_settingsModel currentVideoCodecSettingFromStore];
    option.disableEncryption = YES;
    [CFPeerConnectionClient createPeerConnectionFactory:option];

    // 4. create local tracks
    [_pcClientFactory createLocalTracks];

    // 5. add local preview & start camera capture
    [CFPeerConnectionClient addLocalTrackRenderer:_localRenderer];
    [_pcClientFactory.cameraCaptureController startCapture:^(NSError * _Nonnull error) {
        NSLog(@"XXPXX cameraCaptureController startCapture start error %@", error);
    }];

    // 6. create PcClient
    _pcClient = [_pcClientFactory createPeerConnectionClientPeerUid:@"test"
                                                               dir:0
                                                          hasVideo:YES
                                                   videoMaxBitrate:[[_settingsModel currentMaxBitrateSettingFromStore] intValue]
                                                 videoMaxFrameRate:30
                                                          callback:self];

    // 7. create pc
    NSArray* iceServers = [NSArray array];
    [_pcClient createPeerConnectionIceServers:iceServers];

    // 8. create offer
    [_pcClient createOffer];
}

- (void)dealloc {
    if (!_left) {
        [self hangup];
    }
}

- (void)hangup {
    _left = true;

    [_statsTimer invalidate];
    _statsTimer = nil;
    [_pcClientFactory.cameraCaptureController stopCapture];
    [_pcClient close];
    [CFPeerConnectionClient destroyPeerConnectionFactory];

    [self dismissViewControllerAnimated:YES completion:nil];
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
    CFPeerConnectionClient* realClient = (CFPeerConnectionClient*) [_pcClient getRealClient];
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
    _switchCameraButton.enabled = false;
    [_pcClientFactory.cameraCaptureController switchCamera:^(NSError * _Nonnull error) {
        NSLog(@"switchCamera result: %@", error);
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
    [self hangup];
}

#pragma mark - Kmp_webrtcPeerConnectionClientCallback

- (nonnull NSString *)onPreferCodecsPeerUid:(nonnull NSString *)peerUid sdp:(nonnull NSString *)sdp {
    return sdp;
}

- (void)onErrorPeerUid:(nonnull NSString *)peerUid code:(int32_t)code {
    NSLog(@"XXPXX onError %@ %d", peerUid, code);
}

- (void)onLocalDescriptionPeerUid:(nonnull NSString *)peerUid sdp:(nonnull Kmp_webrtcSessionDescription *)sdp { 
    // 9. send offer to remote, get answer, set answer
    Kmp_webrtcSessionDescription* answer = [[Kmp_webrtcSessionDescription alloc] initWithType:3 sdpDescription:sdp.sdpDescription];
    [_pcClient setRemoteDescriptionSdp:answer];
}

- (void)onIceCandidatePeerUid:(nonnull NSString *)peerUid candidate:(nonnull Kmp_webrtcIceCandidate *)candidate {
    // 10. send ice candidate to remote, get ice candidate, add ice candidate
    [_pcClient addIceCandidateCandidate:candidate];
}

- (void)onIceCandidatesRemovedPeerUid:(nonnull NSString *)peerUid candidates:(nonnull NSArray<Kmp_webrtcIceCandidate *> *)candidates {
}

- (void)onPeerConnectionStatsReadyPeerUid:(nonnull NSString *)peerUid report:(nonnull Kmp_webrtcRtcStatsReport *)report { 
    NSLog(@"XXPXX onPeerConnectionStatsReady %@", report);
}

- (void)onIceConnectedPeerUid:(nonnull NSString *)peerUid {
    NSLog(@"XXPXX onIceConnected %@", peerUid);
    // 11. on ice connected, add renderer for remote stream
    __weak typeof(self) wself = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        typeof(self) sself = wself;
        if (sself == nil) {
            return;
        }
        CFPeerConnectionClient* realClient = (CFPeerConnectionClient*) [sself->_pcClient getRealClient];
        [realClient addRemoteTrackRenderer:sself->_remoteRenderer];

        [sself startGetStats];
    });
}

- (void)onIceDisconnectedPeerUid:(nonnull NSString *)peerUid {
}

#pragma mark - CFAudioMixerDelegate

- (void)onSsrcError:(int32_t)ssrc code:(int32_t)code { 
    NSLog(@"XXPXX onSsrcError %d %d", ssrc, code);
    __weak typeof(self) wself = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        typeof(self) sself = wself;
        [sself onMixer:nil];
    });
}

- (void)onSsrcFinished:(int32_t)ssrc { 
    NSLog(@"XXPXX onSsrcFinished %d", ssrc);
    __weak typeof(self) wself = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        typeof(self) sself = wself;
        [sself onMixer:nil];
    });
}

@end
