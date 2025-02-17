package com.piasy.kmp.webrtc.android;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.piasy.avconf.AudioMixer;
import com.piasy.avconf.view.FreezeAwareRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.piasy.kmp.webrtc.AndroidPrivateConfig;
import com.piasy.kmp.webrtc.IceCandidate;
import com.piasy.kmp.webrtc.PeerConnectionClient;
import com.piasy.kmp.webrtc.PeerConnectionClientCallback;
import com.piasy.kmp.webrtc.PeerConnectionClientFactory;
import com.piasy.kmp.webrtc.RtcStatsReport;
import com.piasy.kmp.webrtc.SessionDescription;
import com.piasy.kmp.xlog.Logging;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;

import static com.piasy.kmp.webrtc.AndroidPeerConnectionClientFactoryKt.createPeerConnectionClientFactory;
import static com.piasy.kmp.webrtc.AndroidPeerConnectionClientFactoryKt.initializeWebRTC;
import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_AUDIO_ONLY;
import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_IS_PUBLISHER;
import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_RECORD_CALL;
import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_BITRATE;
import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_CODEC;
import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_FPS;
import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_HEIGHT;
import static com.piasy.kmp.webrtc.android.HallActivity.EXTRA_VIDEO_WIDTH;

public class CallActivity extends AppCompatActivity implements PeerConnectionClientCallback, AudioMixer.MixerCallback {

  private static final String TAG = "CallActivity";

  private static final boolean SAVE_YUV_FRAME = false;
  private static final boolean PAUSE_STREAMING = true;

  private boolean isPublisher;
  private boolean audioOnly;
  private int videoWidth;
  private int videoHeight;
  private int videoFps;
  private int videoMaxBitrate;
  private String videoCodec;

  private Button recordButton;
  private boolean recording;

  private Button mixerButton;
  private boolean mixing;

  private Button pauseButton;
  private boolean streamingPaused = false;

  private FrameLayout rootLayout;
  private FreezeAwareRenderer localRenderer;
  private FreezeAwareRenderer remoteRenderer;

  private HandlerThread yuvHandlerThread;
  private Handler yuvHandler;

  private EglBase eglBase;
  private PeerConnectionClientFactory pcClientFactory;
  private PeerConnectionClient pcClient;
  private AudioMixer mixer;

  private static int getSystemUiVisibility() {
    return View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
  }

  private DisplayMetrics getDisplayMetrics() {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    WindowManager windowManager =
        (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
    windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
    return displayMetrics;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
    getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
    setContentView(R.layout.activity_call);

    final Intent intent = getIntent();

    isPublisher = intent.getBooleanExtra(EXTRA_IS_PUBLISHER, true);
    recording = intent.getBooleanExtra(EXTRA_RECORD_CALL, false);
    audioOnly = intent.getBooleanExtra(EXTRA_AUDIO_ONLY, false);

    videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 640);
    videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 480);
    videoFps = intent.getIntExtra(EXTRA_VIDEO_FPS, 15);
    videoMaxBitrate = intent.getIntExtra(EXTRA_VIDEO_BITRATE, 800);
    videoCodec = intent.getStringExtra(EXTRA_VIDEO_CODEC);

    // Create UI controls.
    rootLayout = findViewById(R.id.rootLayout);

    findViewById(R.id.btnStop).setOnClickListener(v -> disconnect());

    recordButton = findViewById(R.id.btnRecord);
    recordButton.setText(recording ? "stop record" : "start record");
    recordButton.setOnClickListener(v -> {
      recording = !recording;
      recordButton.setText(recording ? "stop record" : "start record");
      if (recording) {
        File path = new File(getExternalFilesDir(null), "send.mkv");
        pcClient.startRecorder(PeerConnectionClient.DIR_SEND_ONLY, path.getAbsolutePath());
      } else {
        pcClient.stopRecorder(PeerConnectionClient.DIR_SEND_ONLY);
      }
    });

    mixerButton = findViewById(R.id.btnMixer);
    mixerButton.setText(mixing ? "stop mixer" : "start mixer");
    mixerButton.setOnClickListener(v -> {
      mixing = !mixing;
      mixerButton.setText(mixing ? "stop mixer" : "start mixer");
      if (mixing) {
        File path = new File(getExternalFilesDir(null), "mozart.mp3");
        mixer = new AudioMixer(path.getAbsolutePath(), 48000, 1, 10000, false, 5, this);
        mixer.startMixer();
        mixer.toggleMusicStreaming(true);
      } else {
        mixer.stopMixer();
        mixer = null;
      }
    });

    Button switchCamera = findViewById(R.id.btnCamera);
    switchCamera.setOnClickListener(v -> {
      switchCamera.setEnabled(false);

      pcClientFactory.switchCamera(new Function1<Boolean, Unit>() {
        @Override
        public Unit invoke(Boolean isFrontCamera) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              switchCamera.setText(isFrontCamera ? "FRONT" : "BACK");
              switchCamera.setEnabled(true);
            }
          });
          return null;
        }
      });
    });

    findViewById(R.id.btnVideo).setOnClickListener(v -> {
      audioOnly = !audioOnly;
//      avConf.setVideoEnabled(!audioOnly);
    });

    pauseButton = findViewById(R.id.btnPauseStreaming);
    if (PAUSE_STREAMING) {
      pauseButton.setOnClickListener(v -> {
        streamingPaused = !streamingPaused;
        pauseButton.setText(streamingPaused ? "resume" : "pause");

//        avConf.togglePauseVideoStreaming(streamingPaused);

        //if (streamingPaused) {
        //  File file = new File(getExternalFilesDir(null), "test.jpeg");
        //  avConf.toggleSendLastFrame(file, jpeg -> {
        //    runOnUiThread(() -> Toast.makeText(this, "jpeg: " + jpeg, Toast.LENGTH_SHORT).show());
        //  });
        //} else {
        //  avConf.toggleSendLastFrame(null, null);
        //}
      });
    } else {
      pauseButton.setVisibility(View.GONE);
    }

    eglBase = EglBase.create();
    localRenderer = createRenderer(true);
    remoteRenderer = createRenderer(false);

    prepareMusic();

    startLoopback();
  }

  private void prepareMusic() {
    try {
      File path = new File(getExternalFilesDir(null), "mozart.mp3");
      if (path.exists()) {
        return;
      }
      InputStream inputStream = getAssets().open("mozart.mp3");
      FileOutputStream outputStream = new FileOutputStream(path);

      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, length);
      }
      inputStream.close();
      outputStream.close();
      Logging.INSTANCE.info(TAG, "copy music success");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private FreezeAwareRenderer createRenderer(boolean local) {
    WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    DisplayMetrics displayMetrics = new DisplayMetrics();
    windowManager.getDefaultDisplay().getMetrics(displayMetrics);

    FrameLayout wrapper = new FrameLayout(CallActivity.this);
    rootLayout.addView(wrapper);
    FrameLayout.LayoutParams params
            = (FrameLayout.LayoutParams) wrapper.getLayoutParams();
    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
    params.height = displayMetrics.heightPixels / 2;
    params.gravity = local ? Gravity.TOP : Gravity.BOTTOM;
    wrapper.setLayoutParams(params);

    FreezeAwareRenderer renderer = new FreezeAwareRenderer(this,
            local ? "loopback_local" : "loopback_remote", FreezeAwareRenderer.SCALE_TYPE_CENTER_CROP);
    renderer.init(eglBase.getEglBaseContext(), null);
    renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    renderer.setMirror(local);

    wrapper.addView(renderer, ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);
    return renderer;
  }

  @Override
  protected void onDestroy() {
    Thread.setDefaultUncaughtExceptionHandler(null);
    disconnect();
    super.onDestroy();
  }

  private void startLoopback() {
    // 1. initialize
    String fieldTrials = "WebRTC-Video-H26xPacketBuffer/Enabled/";
    initializeWebRTC(this, fieldTrials, true);

    // 2. create PcClientFactory
    PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
    options.disableEncryption = true;
    AndroidPrivateConfig privateConfig = new AndroidPrivateConfig(
            getLifecycle(), eglBase, options, true, null, null, null
    );
    PeerConnectionClientFactory.Config config = new PeerConnectionClientFactory.Config(
            PeerConnectionClientFactory.Config.VIDEO_CAPTURE_IMPL_SYSTEM_CAMERA,
            videoWidth, videoHeight, videoFps, PeerConnectionClientFactory.Config.CAMERA_FACE_FRONT,
            privateConfig
    );
    pcClientFactory = createPeerConnectionClientFactory(config, new Function2<Integer, String, Unit>() {
      @Override
      public Unit invoke(Integer code, String msg) {
        Logging.INSTANCE.error(TAG, "PcClient error " + code + ", " + msg);
        return null;
      }
    });

    // 3. create local tracks
    pcClientFactory.createLocalTracks();

    // 4. add local preview & start camera capture
    pcClientFactory.addLocalTrackRenderer(localRenderer);
    pcClientFactory.startVideoCapture();

    // 5. create PcClient
    pcClient = pcClientFactory.createPeerConnectionClient(
            "test", PeerConnectionClient.DIR_SEND_RECV, true, videoMaxBitrate, videoFps, this
    );

    // 6. create pc
    pcClient.createPeerConnection(Collections.emptyList());

    // 7. create offer
    pcClient.createOffer();
  }

  @Override
  public void onLocalDescription(@NotNull String peerUid, @NotNull SessionDescription sdp) {
    // 8. send offer to remote, get answer from remote, and set answer
    SessionDescription answer = new SessionDescription(SessionDescription.ANSWER, sdp.getSdpDescription());
    pcClient.setRemoteDescription(answer);
  }

  @Override
  public void onIceCandidate(@NotNull String peerUid, @NotNull IceCandidate candidate) {
    // 9. send ice candidate to remote, get ice candidate from remote, add ice candidate
    pcClient.addIceCandidate(candidate);
  }

  @Override
  public void onIceConnected(@NotNull String peerUid) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        pcClient.addRemoteTrackRenderer(remoteRenderer);
      }
    });
  }

  // Disconnect from remote resources, dispose of local resources, and exit.
  private void disconnect() {
    if (mixer != null) {
      mixer.stopMixer();
      mixer = null;
    }
    if (pcClientFactory != null) {
      pcClientFactory.stopVideoCapture();
      pcClient.close();
      pcClientFactory.destroyPeerConnectionFactory();
      pcClientFactory = null;
      pcClient = null;
    }

    localRenderer.release();
    remoteRenderer.release();

    finish();
  }

  @Override
  public @NotNull String onPreferCodecs(@NotNull String peerUid, @NotNull String sdp) {
    return sdp;
  }

  @Override
  public void onIceCandidatesRemoved(@NotNull String peerUid, @NotNull List<@NotNull IceCandidate> candidates) {
  }

  @Override
  public void onPeerConnectionStatsReady(@NotNull String peerUid, @NotNull RtcStatsReport report) {
  }

  @Override
  public void onIceDisconnected(@NotNull String peerUid) {
  }

  @Override
  public void onError(@NotNull String peerUid, int code) {
  }

  @Override
  public void onMixerSsrcFinished(int ssrc) {
    Logging.INSTANCE.info(TAG, "onMixerSsrcFinished " + ssrc);
    onMixerStopped();
  }

  @Override
  public void onMixerSsrcError(int ssrc, int error) {
    Logging.INSTANCE.error(TAG, "onMixerSsrcError " + ssrc + ", " + error);
    onMixerStopped();
  }

  private void onMixerStopped() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mixing = false;
        mixerButton.setText("start mixer");
        mixer.stopMixer();
        mixer = null;
      }
    });
  }
}
