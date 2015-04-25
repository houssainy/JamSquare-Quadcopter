/*
 * Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
 */
package com.alexu.csed.jamsquare;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	private final String TAG = "MAINACTIVITY";
	private Button connectButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		connectButton = (Button) findViewById(R.id.connectButton);
		connectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Video call enabled flag.
				boolean videoCallEnabled = Boolean
						.valueOf(getString(R.string.pref_videocall_default));

				// Get default codecs.
				String videoCodec = getString(R.string.pref_videocodec_default);
				String audioCodec = getString(R.string.pref_audiocodec_default);

				// Check HW codec flag.
				boolean hwCodec = Boolean
						.valueOf(getString(R.string.pref_hwcodec_default));

				// Get video resolution from settings.
				int videoWidth = 0;
				int videoHeight = 0;
				String resolution = getString(R.string.pref_resolution_default);
				String[] dimensions = resolution.split("[ x]+");
				if (dimensions.length == 2) {
					try {
						videoWidth = Integer.parseInt(dimensions[0]);
						videoHeight = Integer.parseInt(dimensions[1]);
					} catch (NumberFormatException e) {
						videoWidth = 0;
						videoHeight = 0;
						Log.e(TAG, "Wrong video resolution setting: "
								+ resolution);
					}
				}

				// Get camera fps from settings.
				int cameraFps = 0;
				String fps = getString(R.string.pref_fps_default);
				try {
					cameraFps = Integer.parseInt(fps);
				} catch (NumberFormatException e) {
					Log.e(TAG, "Wrong camera fps setting: " + fps);
				}

				// Get video and audio start bitrate.
				int videoStartBitrate = Integer
						.parseInt(getString(R.string.pref_startvideobitratevalue_default));

				int audioStartBitrate = Integer
						.parseInt(getString(R.string.pref_startaudiobitratevalue_default));

				// Test if CpuOveruseDetection should be disabled. By default is
				// on.
				boolean cpuOveruseDetection = Boolean
						.valueOf(getString(R.string.pref_cpu_usage_detection_default));

				// Check statistics display option.
				boolean displayHud = Boolean
						.valueOf(getString(R.string.pref_displayhud_default));

				Intent intent = new Intent(
						"com.alexu.csed.jamsquare.CALLACTIVITY");
				intent.putExtra(CallActivity.EXTRA_LOOPBACK, false);
				intent.putExtra(CallActivity.EXTRA_VIDEO_CALL, videoCallEnabled);
				intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, videoWidth);
				intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, videoHeight);
				intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, cameraFps);
				intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE,
						videoStartBitrate);
				intent.putExtra(CallActivity.EXTRA_VIDEOCODEC, videoCodec);
				intent.putExtra(CallActivity.EXTRA_HWCODEC_ENABLED, hwCodec);
				intent.putExtra(CallActivity.EXTRA_AUDIO_BITRATE,
						audioStartBitrate);
				intent.putExtra(CallActivity.EXTRA_AUDIOCODEC, audioCodec);
				intent.putExtra(CallActivity.EXTRA_CPUOVERUSE_DETECTION,
						cpuOveruseDetection);
				intent.putExtra(CallActivity.EXTRA_DISPLAY_HUD, displayHud);
				
				startActivity(intent);
			}
		});
	}
}
