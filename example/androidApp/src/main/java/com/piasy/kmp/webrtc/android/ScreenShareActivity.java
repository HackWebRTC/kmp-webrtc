//package com.piasy.kmp.webrtc.android;
//
//import android.annotation.TargetApi;
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.Intent;
//import android.media.projection.MediaProjectionManager;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.util.DisplayMetrics;
//import android.view.Gravity;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.Window;
//import android.view.WindowManager;
//import android.webkit.WebChromeClient;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//import android.widget.Button;
//import android.widget.FrameLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import com.piasy.avconf.AvConf;
//import com.piasy.avconf.ConfConfig;
//import com.piasy.avconf.ConfEvents;
//import com.piasy.avconf.example.R;
//import com.piasy.avconf.utils.AndroidConfExtras;
//import com.piasy.avconf.view.FreezeAwareRenderer;
//import com.piasy.kmpp.logging.LoggingImpl;
//import org.webrtc.Camera1Enumerator;
//import org.webrtc.CameraVideoCapturer;
//import org.webrtc.CapturerObserver;
//import org.webrtc.PeerConnectionFactory;
//import org.webrtc.RendererCommon;
//import org.webrtc.SurfaceTextureHelper;
//import org.webrtc.VideoFrame;
//
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_IS_PUBLISHER;
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_ROOM_ID;
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_ROOM_NAME;
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_UID;
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_BITRATE;
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_CODEC;
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_FPS;
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_HEIGHT;
//import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_WIDTH;
//
//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
//public class ScreenShareActivity extends AppCompatActivity {
//
//  private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
//
//  private static final String TAG = "OwtCallActivity";
//
//  private boolean isError;
//  private Toast logToast;
//
//  private String rsUrl;
//  private String roomId;
//  private boolean isPublisher;
//  private int videoWidth;
//  private int videoHeight;
//  private int videoFps;
//  private int videoMaxBitrate;
//  private int videoCodec;
//
//  private FrameLayout rootLayout;
//
//  private ConfEvents confEvents;
//  private AvConf avConf;
//
//  private SurfaceTextureHelper cameraCaptureSurfaceTextureHelper;
//  private CameraVideoCapturer cameraCapturer;
//  private FreezeAwareRenderer cameraPreview;
//
//  private FreezeAwareRenderer receiveRenderer;
//
//  private long callStartedTimeMs = 0;
//  private long callSuccessTimeMs = 0;
//  private boolean establishTimeDisplayed;
//
//  private String selfUid;
//
//  @TargetApi(19)
//  private static int getSystemUiVisibility() {
//    int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//      flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//    }
//    return flags;
//  }
//
//  @TargetApi(17)
//  private DisplayMetrics getDisplayMetrics() {
//    DisplayMetrics displayMetrics = new DisplayMetrics();
//    WindowManager windowManager =
//        (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
//    windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
//    return displayMetrics;
//  }
//
//  @Override
//  protected void onCreate(Bundle savedInstanceState) {
//    super.onCreate(savedInstanceState);
//    requestWindowFeature(Window.FEATURE_NO_TITLE);
//    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
//        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
//    getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
//    setContentView(R.layout.activity_owt_screen_share);
//
//    final Intent intent = getIntent();
//
//    Uri roomUri = intent.getData();
//    if (roomUri == null) {
//      logAndToast(getString(R.string.missing_url));
//      LoggingImpl.error(TAG, "Didn't get any URL in intent!");
//      setResult(RESULT_CANCELED);
//      finish();
//      return;
//    }
//    rsUrl = roomUri.toString();
//
//    // Get Intent parameters.
//    selfUid = intent.getStringExtra(EXTRA_UID);
//    roomId = intent.getStringExtra(EXTRA_ROOM_ID);
//    String roomName = intent.getStringExtra(EXTRA_ROOM_NAME);
//    LoggingImpl.info(TAG, "Room ID: " + roomId);
//    if (roomId == null || roomId.length() == 0) {
//      logAndToast(getString(R.string.missing_url));
//      LoggingImpl.error(TAG, "Incorrect room ID in intent!");
//      setResult(RESULT_CANCELED);
//      finish();
//      return;
//    }
//
//    TextView tvRoomName = findViewById(R.id.tvRoomName);
//    tvRoomName.setText(roomName);
//    WebView webView = findViewById(R.id.webView);
//
//    isPublisher = intent.getBooleanExtra(EXTRA_IS_PUBLISHER, true);
//
//    videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 640);
//    videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 480);
//    videoFps = intent.getIntExtra(EXTRA_VIDEO_FPS, 15);
//    videoMaxBitrate = intent.getIntExtra(EXTRA_VIDEO_BITRATE, 800);
//    videoCodec = intent.getIntExtra(EXTRA_VIDEO_CODEC, ConfConfig.VIDEO_CODEC_H264_BASELINE);
//
//    rootLayout = findViewById(R.id.rootLayout);
//
//    Button leaveButton = findViewById(R.id.btnStop);
//    leaveButton.setOnClickListener(v -> disconnect());
//
//    if (isPublisher) {
//      webView.setVisibility(View.VISIBLE);
//      webView.loadUrl("https://i.y.qq.com/v8/playsong.html?songmid=003gRfTV272GzH");
//      webView.setWebChromeClient(new WebChromeClient());
//      webView.setWebViewClient(new WebViewClient());
//      WebSettings webSettings = webView.getSettings();
//      webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
//      webSettings.setJavaScriptEnabled(true);
//
//      MediaProjectionManager mediaProjectionManager =
//          (MediaProjectionManager) getApplication().getSystemService(
//              Context.MEDIA_PROJECTION_SERVICE);
//      startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
//          CAPTURE_PERMISSION_REQUEST_CODE);
//    } else {
//      callStartedTimeMs = System.currentTimeMillis();
//      startCall(null);
//    }
//  }
//
//  @Override
//  protected void onDestroy() {
//    Thread.setDefaultUncaughtExceptionHandler(null);
//    disconnect();
//    if (logToast != null) {
//      logToast.cancel();
//    }
//    super.onDestroy();
//  }
//
//  @Override
//  public void onActivityResult(int requestCode, int resultCode, Intent data) {
//    super.onActivityResult(requestCode, resultCode, data);
//    if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE || resultCode != Activity.RESULT_OK) {
//      return;
//    }
//
//    callStartedTimeMs = System.currentTimeMillis();
//    startCall(data);
//    startCameraPreview();
//  }
//
//  @Override
//  protected void onPause() {
//    super.onPause();
//
//    if (avConf != null) {
//      avConf.onPause();
//    }
//  }
//
//  @Override
//  protected void onResume() {
//    super.onResume();
//
//    if (avConf != null) {
//      avConf.onResume();
//    }
//  }
//
//  private void startCall(Intent mediaProjectionPermissionResultData) {
//    PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
//
//    ConfConfig.Builder confConfigBuilder = new ConfConfig.Builder()
//        .rsUrl(rsUrl)
//        .videoCaptureImpl(ConfConfig.VIDEO_CAPTURE_IMPL_SCREEN)
//        .videoCaptureWidth(videoWidth)
//        .videoCaptureHeight(videoHeight)
//        .videoCaptureFps(videoFps)
//        .videoMaxBitrate(videoMaxBitrate)
//        .startBitrate(600)
//        .videoCodec(videoCodec);
//    if (mediaProjectionPermissionResultData != null) {
//      confConfigBuilder.extras(new AndroidConfExtras(mediaProjectionPermissionResultData));
//    }
//
//    confEvents = new ConfEvents() {
//      @Override public void onPeerJoined(@NonNull String uid) {
//        super.onPeerJoined(uid);
//
//        if (TextUtils.equals(uid, selfUid)) {
//          return;
//        }
//
//        if (receiveRenderer != null) {
//          avConf.addRenderer(uid, receiveRenderer);
//          return;
//        }
//
//        FrameLayout wrapper = new FrameLayout(ScreenShareActivity.this);
//        rootLayout.addView(wrapper, 0,
//            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
//
//        receiveRenderer =
//            new FreezeAwareRenderer(ScreenShareActivity.this, uid,
//                FreezeAwareRenderer.SCALE_TYPE_CENTER_CROP);
//        wrapper.addView(receiveRenderer, ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.MATCH_PARENT);
//
//        receiveRenderer.init(avConf.getRootEglBase().getEglBaseContext(), null);
//        receiveRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//
//        avConf.addRenderer(uid, receiveRenderer);
//      }
//
//      @Override
//      public void onStreamStarted(@NonNull final String uid) {
//        super.onStreamStarted(uid);
//
//        callSuccessTimeMs = System.currentTimeMillis();
//        displayEstablishTime();
//      }
//
//      @Override
//      public void onStreamUpdated(@NonNull final String uid, final boolean audioActive,
//          final boolean videoActive) {
//        super.onStreamUpdated(uid, audioActive, videoActive);
//
//        LoggingImpl.info(TAG, "onStreamUpdated " + uid
//            + ", audioActive " + audioActive
//            + ", videoActive " + videoActive);
//        LoggingImpl.info(TAG, "all stream status: " + avConf.getStreamStatus());
//      }
//
//      @Override
//      public void onStreamEvent(@NonNull final String uid, final int event,
//          @NonNull final String data) {
//        super.onStreamEvent(uid, event, data);
//
//        Toast.makeText(ScreenShareActivity.this, "onStreamEvent " + uid + " " + event,
//            Toast.LENGTH_SHORT).show();
//      }
//
//      @Override
//      public void onPeerLeft(@NonNull final String uid) {
//        super.onPeerLeft(uid);
//
//        disconnect();
//      }
//
//      @Override
//      public void onError(final int code, @NonNull final String data) {
//        if (code == ConfEvents.ERR_RETRYING) {
//          Toast.makeText(ScreenShareActivity.this, "retrying...",
//              Toast.LENGTH_SHORT).show();
//        } else {
//          if (!isError) {
//            isError = true;
//            disconnectWithErrorMessage("onError: " + ConfEvents.errCodeName(code) + " " + data);
//          }
//        }
//      }
//    };
//    avConf = new AvConf(selfUid, isPublisher, confConfigBuilder.build(), confEvents, options);
//
//    avConf.join(roomId);
//  }
//
//  private void startCameraPreview() {
//    Camera1Enumerator enumerator = new Camera1Enumerator(true);
//    for (String device : enumerator.getDeviceNames()) {
//      if (enumerator.isFrontFacing(device)) {
//        cameraCapturer = enumerator.createCapturer(device, null);
//        break;
//      }
//    }
//
//    if (cameraCapturer != null) {
//      FrameLayout wrapper = new FrameLayout(ScreenShareActivity.this);
//
//      rootLayout.addView(wrapper);
//
//      DisplayMetrics displayMetrics = getDisplayMetrics();
//      int horizontalMargin = 40;
//      int bottomMargin = 400;
//      int smallWindowWidth = (displayMetrics.widthPixels - horizontalMargin * 4) / 3;
//      int smallWindowHeight = smallWindowWidth * videoWidth / videoHeight;
//      FrameLayout.LayoutParams params
//          = (FrameLayout.LayoutParams) wrapper.getLayoutParams();
//      params.width = smallWindowWidth;
//      params.height = smallWindowHeight;
//      params.gravity = Gravity.BOTTOM;
//      params.bottomMargin = bottomMargin;
//      params.leftMargin = horizontalMargin;
//      wrapper.setLayoutParams(params);
//
//      cameraPreview = new FreezeAwareRenderer(ScreenShareActivity.this,
//          selfUid, FreezeAwareRenderer.SCALE_TYPE_CENTER_CROP);
//      wrapper.addView(cameraPreview, ViewGroup.LayoutParams.MATCH_PARENT,
//          ViewGroup.LayoutParams.MATCH_PARENT);
//
//      cameraPreview.init(avConf.getRootEglBase().getEglBaseContext(), null);
//      cameraPreview.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//      cameraPreview.setMirror(true);
//
//      cameraCaptureSurfaceTextureHelper = SurfaceTextureHelper.create("CameraCapture",
//          avConf.getRootEglBase().getEglBaseContext());
//      cameraCapturer.initialize(cameraCaptureSurfaceTextureHelper, getApplicationContext(),
//          new CapturerObserver() {
//            @Override public void onCapturerStarted(boolean b) {
//            }
//
//            @Override public void onCapturerStopped() {
//            }
//
//            @Override public void onFrameCaptured(VideoFrame videoFrame) {
//              cameraPreview.onFrame(videoFrame);
//            }
//          });
//      cameraCapturer.startCapture(1280, 720, 30);
//    }
//  }
//
//  private void displayEstablishTime() {
//    if (callStartedTimeMs != 0 && callSuccessTimeMs != 0 && !establishTimeDisplayed) {
//      establishTimeDisplayed = true;
//      long recvSuccessDelay = callSuccessTimeMs > callStartedTimeMs
//          ? callSuccessTimeMs - callStartedTimeMs : 100;
//      logAndToast("Call success, take " + recvSuccessDelay + "ms");
//    }
//  }
//
//  // Disconnect from remote resources, dispose of local resources, and exit.
//  private void disconnect() {
//    if (cameraCapturer != null) {
//      try {
//        cameraCapturer.stopCapture();
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
//      cameraCapturer.dispose();
//      cameraCapturer = null;
//    }
//    if (cameraCaptureSurfaceTextureHelper != null) {
//      cameraCaptureSurfaceTextureHelper.dispose();
//      cameraCaptureSurfaceTextureHelper = null;
//    }
//    if (cameraPreview != null) {
//      cameraPreview.release();
//      cameraPreview = null;
//    }
//
//    if (receiveRenderer != null) {
//      receiveRenderer.release();
//      receiveRenderer = null;
//    }
//
//    if (avConf != null) {
//      avConf.leave();
//      avConf = null;
//    }
//
//    finish();
//  }
//
//  private void disconnectWithErrorMessage(final String errorMessage) {
//    new AlertDialog.Builder(this)
//        .setTitle(getText(R.string.channel_error_title))
//        .setMessage(errorMessage)
//        .setCancelable(false)
//        .setNeutralButton(R.string.ok,
//            (dialog, id) -> {
//              dialog.cancel();
//              disconnect();
//            })
//        .create()
//        .show();
//  }
//
//  // Log |msg| and Toast about it.
//  private void logAndToast(String msg) {
//    LoggingImpl.info(TAG, msg);
//    if (logToast != null) {
//      logToast.cancel();
//    }
//    logToast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
//    logToast.show();
//  }
//}
