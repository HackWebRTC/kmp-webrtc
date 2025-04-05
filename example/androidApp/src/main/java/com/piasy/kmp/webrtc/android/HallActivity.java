package com.piasy.kmp.webrtc.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.Random;
//import org.webrtc.BuildConfig;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class HallActivity extends Activity {

  public static final String EXTRA_IS_PUBLISHER = "avconf.IS_PUBLISHER";
  public static final String EXTRA_AUDIO_ONLY = "avconf.AUDIO_ONLY";
  public static final String EXTRA_RECORD_CALL = "avconf.RECORD_CALL";
  public static final String EXTRA_VIDEO_WIDTH = "avconf.VIDEO_WIDTH";
  public static final String EXTRA_VIDEO_HEIGHT = "avconf.VIDEO_HEIGHT";
  public static final String EXTRA_VIDEO_FPS = "avconf.VIDEO_FPS";
  public static final String EXTRA_VIDEO_BITRATE = "avconf.VIDEO_BITRATE";
  public static final String EXTRA_VIDEO_CODEC = "avconf.VIDEO_CODEC";

  private SharedPreferences sharedPref;
  private String keyprefUid;
  private String keyprefResolution;
  private String keyprefFps;
  private String keyprefVideoBitrateType;
  private String keyprefVideoBitrateValue;

  private CheckBox mRecording;
  private CheckBox mAudioOnly;
  private CheckBox mPublisher;
  private CheckBox mScreenShare;

  private boolean mGotPermission;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hall);

    // Get setting keys.
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    keyprefUid = getString(R.string.pref_uid_key);
    keyprefResolution = getString(R.string.pref_resolution_key);
    keyprefFps = getString(R.string.pref_fps_key);
    keyprefVideoBitrateType = getString(R.string.pref_maxvideobitrate_key);
    keyprefVideoBitrateValue = getString(R.string.pref_maxvideobitratevalue_key);

    TextView tvVersion = findViewById(R.id.tvVersion);
//    tvVersion.setText("kmp-webrtc " + BuildConfig.VERSION_NAME);

    mRecording = findViewById(R.id.recording);
    mAudioOnly = findViewById(R.id.audio_only);
    mPublisher = findViewById(R.id.publisher);
    mScreenShare = findViewById(R.id.screen_share);
    findViewById(R.id.setting).setOnClickListener(
        v -> startActivity(new Intent(this, SettingsActivity.class)));
    //findViewById(R.id.loopback).setOnClickListener(v -> startLoopback(CallActivity.class));
    findViewById(R.id.loopback).setOnClickListener(v -> startLoopback(MediasoupActivity.class));

    HallActivityPermissionsDispatcher.checkPermissionWithPermissionCheck(this);
  }

  @Override
  public void onRequestPermissionsResult(final int requestCode,
      @NonNull final String[] permissions,
      @NonNull final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    HallActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode,
        grantResults);
  }

  @NeedsPermission({
      Manifest.permission.INTERNET,
      Manifest.permission.CAMERA,
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.MODIFY_AUDIO_SETTINGS,
      Manifest.permission.BLUETOOTH,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_NETWORK_STATE
  })
  void checkPermission() {
    mGotPermission = true;
  }

  private void startLoopback(Class activityClass) {
    String uid = sharedPref.getString(keyprefUid, "");
    if (TextUtils.isEmpty(uid)) {
      uid = String.valueOf(new Random(System.currentTimeMillis()).nextInt(Integer.MAX_VALUE));
      sharedPref.edit().putString(keyprefUid, uid).commit();
    }

    // Get video resolution from settings.
    int videoWidth = 0;
    int videoHeight = 0;
    String resolution =
        sharedPref.getString(keyprefResolution,
            getString(R.string.pref_resolution_default));
    String[] dimensions = resolution.split("[ x]+");
    if (dimensions.length == 2) {
      try {
        videoWidth = Integer.parseInt(dimensions[0]);
        videoHeight = Integer.parseInt(dimensions[1]);
      } catch (NumberFormatException e) {
        videoWidth = 0;
        videoHeight = 0;
      }
    }

    // Get camera fps from settings.
    int cameraFps = 30;
    String fps = sharedPref.getString(keyprefFps, getString(R.string.pref_fps_default));
    String[] fpsValues = fps.split("[ x]+");
    if (fpsValues.length == 2) {
      try {
        cameraFps = Integer.parseInt(fpsValues[0]);
      } catch (NumberFormatException e) {
        cameraFps = 30;
      }
    }

    // Get video and audio start bitrate.
    int videoMaxBitrate = 0;
    String bitrateTypeDefault = getString(R.string.pref_maxvideobitrate_default);
    String bitrateType = sharedPref.getString(keyprefVideoBitrateType,
        bitrateTypeDefault);
    if (!bitrateType.equals(bitrateTypeDefault)) {
      String bitrateValue = sharedPref.getString(
          keyprefVideoBitrateValue,
          getString(R.string.pref_maxvideobitratevalue_default));
      videoMaxBitrate = Integer.parseInt(bitrateValue);
    } else {
      videoMaxBitrate = 800;
    }

    String codecStr = sharedPref.getString(getString(R.string.pref_videocodec_key),
            getString(R.string.pref_videocodec_default));
    // Start AppRTCMobile activity.
    Intent intent = new Intent(this, activityClass);
    intent.putExtra(EXTRA_IS_PUBLISHER, mPublisher.isChecked());
    intent.putExtra(EXTRA_AUDIO_ONLY, mAudioOnly.isChecked());
    intent.putExtra(EXTRA_RECORD_CALL, mRecording.isChecked());
    intent.putExtra(EXTRA_VIDEO_WIDTH, videoWidth);
    intent.putExtra(EXTRA_VIDEO_HEIGHT, videoHeight);
    intent.putExtra(EXTRA_VIDEO_FPS, cameraFps);
    intent.putExtra(EXTRA_VIDEO_BITRATE, videoMaxBitrate);
    intent.putExtra(EXTRA_VIDEO_CODEC, codecStr);

    startActivity(intent);
  }
}
