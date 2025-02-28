/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#import "APPRTCViewController.h"

#import <AVFoundation/AVFoundation.h>

@import WebRTC;
@import kmp_webrtc;

static NSUInteger const kContentWidth = 900;
static NSUInteger const kRoomFieldWidth = 200;
static NSUInteger const kActionItemHeight = 30;
static NSUInteger const kBottomViewHeight = 200;

@class APPRTCMainView;
@protocol APPRTCMainViewDelegate

- (void)startLoopback;
- (void)stopLoopback;

@end

@interface APPRTCMainView : NSView

@property(nonatomic, weak) id<APPRTCMainViewDelegate> delegate;
@property(nonatomic, readonly) NSView<RTC_OBJC_TYPE(RTCVideoRenderer)>* localVideoView;
@property(nonatomic, readonly) NSView<RTC_OBJC_TYPE(RTCVideoRenderer)>* remoteVideoView;
@property(nonatomic, readonly) NSTextView* logView;

- (void)displayLogMessage:(NSString*)message;

@end

@interface APPRTCMainView () <NSTextFieldDelegate, RTC_OBJC_TYPE (RTCVideoViewDelegate)>
@end
@implementation APPRTCMainView  {
  NSScrollView* _scrollView;
  NSView* _actionItemsView;
  NSButton* _loopbackButton;
  CGSize _localVideoSize;
  CGSize _remoteVideoSize;

  BOOL _testing;
}

@synthesize delegate = _delegate;
@synthesize localVideoView = _localVideoView;
@synthesize remoteVideoView = _remoteVideoView;
@synthesize logView = _logView;

- (void)displayLogMessage:(NSString *)message {
  dispatch_async(dispatch_get_main_queue(), ^{
    self.logView.string = [NSString stringWithFormat:@"%@%@\n", self.logView.string, message];
    NSRange range = NSMakeRange(self.logView.string.length, 0);
    [self.logView scrollRangeToVisible:range];
  });
}

#pragma mark - Private

- (instancetype)initWithFrame:(NSRect)frame {
  self = [super initWithFrame:frame];
  if (self) {
    [self setupViews];
  }
  return self;
}

+ (BOOL)requiresConstraintBasedLayout {
  return YES;
}

- (void)updateConstraints {
  NSParameterAssert(
      _scrollView != nil &&
      _remoteVideoView != nil &&
      _localVideoView != nil &&
      _actionItemsView!= nil &&
      _loopbackButton != nil);

  [self removeConstraints:[self constraints]];
  NSDictionary* viewsDictionary =
      NSDictionaryOfVariableBindings(_scrollView,
                                     _remoteVideoView,
                                     _localVideoView,
                                     _actionItemsView,
                                     _loopbackButton);

  NSSize remoteViewSize = [self remoteVideoViewSize];
  NSDictionary* metrics = @{
    @"remoteViewWidth" : @(remoteViewSize.width),
    @"remoteViewHeight" : @(remoteViewSize.height),
    @"kBottomViewHeight" : @(kBottomViewHeight),
    @"localViewHeight" : @(remoteViewSize.height / 3),
    @"localViewWidth" : @(remoteViewSize.width / 3),
    @"kActionItemHeight" : @(kActionItemHeight)
  };
  // Declare this separately to avoid compiler warning about splitting string
  // within an NSArray expression.
  NSString* verticalConstraintLeft =
      @"V:|-[_remoteVideoView(remoteViewHeight)]-[_scrollView(kBottomViewHeight)]-|";
  NSString* verticalConstraintRight =
      @"V:|-[_remoteVideoView(remoteViewHeight)]-[_actionItemsView(kBottomViewHeight)]-|";
  NSArray* constraintFormats = @[
      verticalConstraintLeft,
      verticalConstraintRight,
      @"H:|-[_remoteVideoView(remoteViewWidth)]-|",
      @"V:|-[_localVideoView(localViewHeight)]",
      @"H:|-[_localVideoView(localViewWidth)]",
      @"H:|-[_scrollView(==_actionItemsView)]-[_actionItemsView]-|"
  ];

  [APPRTCMainView addConstraints:constraintFormats
                          toView:self
                 viewsDictionary:viewsDictionary
                         metrics:metrics];
  [super updateConstraints];
}

#pragma mark - Constraints helper

+ (void)addConstraints:(NSArray*)constraints toView:(NSView*)view
       viewsDictionary:(NSDictionary*)viewsDictionary
               metrics:(NSDictionary*)metrics {
  for (NSString* constraintFormat in constraints) {
    NSArray* constraints =
    [NSLayoutConstraint constraintsWithVisualFormat:constraintFormat
                                            options:0
                                            metrics:metrics
                                              views:viewsDictionary];
    for (NSLayoutConstraint* constraint in constraints) {
      [view addConstraint:constraint];
    }
  }
}

#pragma mark - Control actions

- (void)startLoopback:(id)sender {
    _testing = !_testing;
    _loopbackButton.title = _testing ? @"Stop loopback" : @"Start loopback";
    [self setNeedsUpdateConstraints:YES];
    if (_testing) {
        [self.delegate startLoopback];
    } else {
        [self.delegate stopLoopback];
    }
}

#pragma mark - RTCVideoViewDelegate

- (void)videoView:(id<RTC_OBJC_TYPE(RTCVideoRenderer)>)videoView didChangeVideoSize:(CGSize)size {
  if (videoView == _remoteVideoView) {
    _remoteVideoSize = size;
  } else if (videoView == _localVideoView) {
    _localVideoSize = size;
  } else {
    return;
  }

  [self setNeedsUpdateConstraints:YES];
}

#pragma mark - Private

- (void)setupViews {
  NSParameterAssert([[self subviews] count] == 0);

  _logView = [[NSTextView alloc] initWithFrame:NSZeroRect];
  [_logView setMinSize:NSMakeSize(0, kBottomViewHeight)];
  [_logView setMaxSize:NSMakeSize(FLT_MAX, FLT_MAX)];
  [_logView setVerticallyResizable:YES];
  [_logView setAutoresizingMask:NSViewWidthSizable];
  NSTextContainer* textContainer = [_logView textContainer];
  NSSize containerSize = NSMakeSize(kContentWidth, FLT_MAX);
  [textContainer setContainerSize:containerSize];
  [textContainer setWidthTracksTextView:YES];
  [_logView setEditable:NO];

  [self setupActionItemsView];

  _scrollView = [[NSScrollView alloc] initWithFrame:NSZeroRect];
  [_scrollView setTranslatesAutoresizingMaskIntoConstraints:NO];
  [_scrollView setHasVerticalScroller:YES];
  [_scrollView setDocumentView:_logView];
  [self addSubview:_scrollView];

  _remoteVideoView = [[RTC_OBJC_TYPE(RTCMTLNSVideoView) alloc] initWithFrame:NSZeroRect];
  _localVideoView = [[RTC_OBJC_TYPE(RTCMTLNSVideoView) alloc] initWithFrame:NSZeroRect];

  [_remoteVideoView setTranslatesAutoresizingMaskIntoConstraints:NO];
  [self addSubview:_remoteVideoView];
  [_localVideoView setTranslatesAutoresizingMaskIntoConstraints:NO];
  [self addSubview:_localVideoView];
}

- (void)setupActionItemsView {
  _actionItemsView = [[NSView alloc] initWithFrame:NSZeroRect];
  [_actionItemsView setTranslatesAutoresizingMaskIntoConstraints:NO];
  [self addSubview:_actionItemsView];

  _loopbackButton = [[NSButton alloc] initWithFrame:NSZeroRect];
  [_loopbackButton setTranslatesAutoresizingMaskIntoConstraints:NO];
  _loopbackButton.title = @"Start loopback";
  _loopbackButton.bezelStyle = NSBezelStyleSmallSquare;
  _loopbackButton.target = self;
  _loopbackButton.action = @selector(startLoopback:);
  [_actionItemsView addSubview:_loopbackButton];
}

- (NSSize)remoteVideoViewSize {
  if (!_remoteVideoView.bounds.size.width) {
    return NSMakeSize(kContentWidth, 0);
  }
  NSInteger width = MAX(_remoteVideoView.bounds.size.width, kContentWidth);
  NSInteger height = (width/16) * 9;
  return NSMakeSize(width, height);
}

@end

@interface APPRTCViewController ()
    <APPRTCMainViewDelegate, Kmp_webrtcPeerConnectionClientCallback>
@property(nonatomic, readonly) APPRTCMainView* mainView;
@end

@implementation APPRTCViewController {
  Kmp_webrtcMacPeerConnectionClientFactory* _pcClientFactory;
  id<Kmp_webrtcPeerConnectionClient> _pcClient;
}

- (void)dealloc {
  [self disconnect];
}

- (void)viewDidAppear {
  [super viewDidAppear];
  [self displayUsageInstructions];
}

- (void)loadView {
  APPRTCMainView* view = [[APPRTCMainView alloc] initWithFrame:NSZeroRect];
  [view setTranslatesAutoresizingMaskIntoConstraints:NO];
  view.delegate = self;
  self.view = view;
}

- (void)windowWillClose:(NSNotification*)notification {
  [self disconnect];
}

#pragma mark - Usage

- (void)displayUsageInstructions {
  [self.mainView displayLogMessage:
   @"Click Start loopback to start loopback test"];
}

#pragma mark - APPRTCMainViewDelegate

- (void)startLoopback {
    // 1. initialize
    NSString *fieldTrials = @"WebRTC-Video-H26xPacketBuffer/Enabled/";
    [Kmp_webrtcObjCPeerConnectionClientFactoryKt initializeWebRTCContext:nil fieldTrials:fieldTrials debugLog:YES];

    // 2. create PcClientFactory
    CFPeerConnectionFactoryOption* option = [[CFPeerConnectionFactoryOption alloc] init];
    option.preferredVideoCodec = [[RTCVideoCodecInfo alloc] initWithName:@"H264"];
    option.disableEncryption = YES;
    Kmp_webrtcObjCPrivateConfig* privateConfig = [[Kmp_webrtcObjCPrivateConfig alloc] initWithPcFactoryOption:option];

    Kmp_webrtcPeerConnectionClientFactoryConfig* pcClientFactoryConfig
    = [[Kmp_webrtcPeerConnectionClientFactoryConfig alloc]
        initWithVideoCaptureImpl:1
               videoCaptureWidth:1280
              videoCaptureHeight:720
                 videoCaptureFps:30
                  initCameraFace:0
                   privateConfig:privateConfig];
    _pcClientFactory = (Kmp_webrtcMacPeerConnectionClientFactory*)
    [Kmp_webrtcMacPeerConnectionClientFactoryKt createPeerConnectionClientFactoryConfig:pcClientFactoryConfig
                                                                           errorHandler:^(Kmp_webrtcInt * _Nonnull code, NSString * _Nonnull msg) {
        NSLog(@"XXPXX PCFactory errorHandler %@ %@", code, msg);
    }];

    // 3. create local tracks
    [_pcClientFactory createLocalTracks];

    // 4. add local preview & start camera capture
    [_pcClientFactory addLocalTrackRendererRenderer:self.mainView.localVideoView];
    [_pcClientFactory startVideoCapture];

    // 5. create PcClient
    _pcClient = [_pcClientFactory createPeerConnectionClientPeerUid:@"test"
                                                               dir:0
                                                          hasVideo:YES
                                                   videoMaxBitrate:2000
                                                 videoMaxFrameRate:30
                                                          callback:self];

    // 6. create pc
    NSArray* iceServers = [NSArray array];
    [_pcClient createPeerConnectionIceServers:iceServers];

    // 7. create offer
    [_pcClient createOffer];
}

- (void)stopLoopback {
    [self disconnect];
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
        [sself->_pcClient addRemoteTrackRendererRenderer:sself.mainView.remoteVideoView];

        //[sself startGetStats];
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

#pragma mark - Private

- (APPRTCMainView*)mainView {
  return (APPRTCMainView*)self.view;
}

- (void)showAlertWithMessage:(NSString*)message {
  dispatch_async(dispatch_get_main_queue(), ^{
    NSAlert* alert = [[NSAlert alloc] init];
    [alert setMessageText:message];
    [alert runModal];
  });
}

- (void)resetUI {
  [self.mainView.remoteVideoView renderFrame:nil];
  [self.mainView.localVideoView renderFrame:nil];
}

- (void)disconnect {
  [_pcClientFactory stopVideoCapture];
  [_pcClient close];
  [_pcClientFactory destroyPeerConnectionFactory];
  _pcClient = nil;
  _pcClientFactory = nil;
  [self resetUI];
}

@end
