/*
 *  Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
 */
package com.alexu.csed.jamsquare;

import java.nio.ByteBuffer;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.DataChannel.Buffer;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import com.alexu.csed.jamsquare.PeerConnectionClient.PeerConnectionEvents;
import com.alexu.csed.jamsquare.PeerConnectionClient.PeerConnectionParameters;
import com.alexu.csed.jamsquare.connection.JamSquareClient;
import com.alexu.csed.jamsquare.connection.JamSquareClient.SignalingEvents;
import com.alexu.csed.jamsquare.connection.JamSquareClient.SignalingParameters;
import com.alexu.csed.jamsquare.pid.IMU;
import com.alexu.csed.jamsquare.pid.PID;
import com.alexu.csed.jamsquare.pid.RemotControl;
import com.alexu.csed.jamsquare.pid.SerialComunication;
import com.alexu.csed.jamsquare.pid.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class CallActivity extends Activity {
	private final String TAG = "CallActivity";

	// Constants
	public static final String EXTRA_LOOPBACK = "com.alexu.csed.LOOPBACK";
	public static final String EXTRA_VIDEO_CALL = "com.alexu.csed.VIDEO_CALL";
	public static final String EXTRA_VIDEO_WIDTH = "com.alexu.csed.VIDEO_WIDTH";
	public static final String EXTRA_VIDEO_HEIGHT = "com.alexu.csed.VIDEO_HEIGHT";
	public static final String EXTRA_VIDEO_FPS = "com.alexu.csed.VIDEO_FPS";
	public static final String EXTRA_VIDEO_BITRATE = "com.alexu.csed.VIDEO_BITRATE";
	public static final String EXTRA_VIDEOCODEC = "com.alexu.csed.VIDEOCODEC";
	public static final String EXTRA_HWCODEC_ENABLED = "com.alexu.csed.HWCODEC";
	public static final String EXTRA_AUDIO_BITRATE = "com.alexu.csed.AUDIO_BITRATE";
	public static final String EXTRA_AUDIOCODEC = "com.alexu.csed.AUDIOCODEC";
	public static final String EXTRA_CPUOVERUSE_DETECTION = "com.alexu.csed.CPUOVERUSE_DETECTION";
	public static final String EXTRA_DISPLAY_HUD = "com.alexu.csed.DISPLAY_HUD";

	private long callStartedTimeMs = 0;

	private boolean isError;

	private PeerConnectionClient peerConnectionClient = null;
	private JamSquareClient jamSquareClient;

	private SignalingParameters signalingParameters;
	private PeerConnectionParameters peerConnectionParameters;

	// PID and sensors Objects
	private SerialComunication serial;
	private IMU imu;
	// Helper class to hold the latest received values from the Quadcopter.
	private RemotControl remote;

	private PID rollController, pitchController, yawController;

	private boolean isStopped;

	// Motors UI Value
	// TODO(houssainy) Remove UI elements
	private TextView motor0, motor1, motor2, motor3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_activity);

		// TODO(houssiany)
		motor0 = (TextView) findViewById(R.id.motor0);
		motor1 = (TextView) findViewById(R.id.motor1);
		motor2 = (TextView) findViewById(R.id.motor2);
		motor3 = (TextView) findViewById(R.id.motor3);
		//

		initializaPeerConnectionParameters();

		// Create connection client.
		jamSquareClient = new JamSquareClient(new SignalingEventsListner(),
				this);
		signalingParameters = jamSquareClient.getSignalingParameters();

		// Initialize PID and sensors Objects
		serial = new SerialComunication(this);
		imu = new IMU(this);

		remote = new RemotControl(0, 0, 0, 0);

		pidInitialize();
	}

	private void initializaPeerConnectionParameters() {
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
				Log.e(TAG, "Wrong video resolution setting: " + resolution);
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

		peerConnectionParameters = new PeerConnectionParameters(
				videoCallEnabled, false, videoWidth, videoHeight, cameraFps,
				videoStartBitrate, videoCodec, hwCodec, audioStartBitrate,
				audioCodec, cpuOveruseDetection);
	}

	@Override
	protected void onPause() {
		super.onPause();
		serial.unRegister();
		imu.unRegisterSensor();

		isStopped = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		serial.register();
		imu.registerSensors();

		isStopped = false;
		new Thread(pidCalculation).start();
	}

	// Activity interfaces
	@Override
	protected void onDestroy() {
		if (peerConnectionClient != null) {
			peerConnectionClient.stopVideoSource();
		}
		jamSquareClient.close();
		disconnect();
		super.onDestroy();
	}

	// Log |msg| and Toast about it.
	private void logAndToast(final String msg) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Log.d(TAG, msg);
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT)
						.show();
			}
		});
	}

	private void startCall() {
		callStartedTimeMs = System.currentTimeMillis();

		// Start connection to signaling server.
		logAndToast("Connecting to Signaling Server...");
		jamSquareClient.connectToSignalingServer();
	}

	// Should be called from UI thread
	private void callConnected() {
		final long delta = System.currentTimeMillis() - callStartedTimeMs;
		Log.i(TAG, "Call connected: delay=" + delta + "ms");

		// UnEnable statistics callback.
		peerConnectionClient.enableStatsEvents(false, 0);
	}

	// Create peer connection factory when EGL context is ready.
	private void createPeerConnectionFactory() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (peerConnectionClient == null) {
					final long delta = System.currentTimeMillis()
							- callStartedTimeMs;
					Log.d(TAG, "Creating peer connection factory, delay="
							+ delta + "ms");
					peerConnectionClient = new PeerConnectionClient();
					peerConnectionClient.createPeerConnectionFactory(
							CallActivity.this, null /*
													 * TODO(housainy) Make sure
													 * that null will not cause
													 * a problem
													 */,
							peerConnectionParameters,
							new PeerConnectionEventsListner());
				}
				if (signalingParameters != null) {
					Log.w(TAG, "EGL context is ready after room connection.");
					onConnectedToRoomInternal(signalingParameters);
				}
			}
		});
	}

	// Disconnect from remote resources, dispose of local resources, and exit.
	private void disconnect() {
		if (jamSquareClient != null) {
			jamSquareClient.disconnectFromSignalingServer();
			jamSquareClient = null;
		}
		if (peerConnectionClient != null) {
			peerConnectionClient.close();
			peerConnectionClient = null;
		}

		finish();
	}

	private void disconnectWithErrorMessage(final String errorMessage) {
		new AlertDialog.Builder(this).setTitle("Connection error")
				.setMessage(errorMessage).setCancelable(false)
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						disconnect();
					}
				}).create().show();

	}

	private void reportError(final String description) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!isError) {
					isError = true;
					disconnectWithErrorMessage(description);
				}
			}
		});
	}

	// -----Implementation of AppRTCClient.AppRTCSignalingEvents
	// ---------------
	// All callbacks are invoked from websocket signaling looper thread and
	// are routed to UI thread.
	private void onConnectedToRoomInternal(final SignalingParameters params) {
		final long delta = System.currentTimeMillis() - callStartedTimeMs;

		signalingParameters = params;
		if (peerConnectionClient == null) {
			Log.w(TAG, "Room is connected, but EGL context is not ready yet.");
			return;
		}
		logAndToast("Creating peer connection, delay=" + delta + "ms");
		peerConnectionClient.createPeerConnection(null, null /*
															 * TODO(housainy)
															 * Make sure that
															 * null will not
															 * cause a problem
															 * for both local
															 * and remote
															 * renders
															 */,
				signalingParameters, new DataChannelObserver());

		if (signalingParameters.initiator) {
			logAndToast("Creating OFFER...");
			// Create offer. Offer SDP will be sent to answering client in
			// PeerConnectionEvents.onLocalDescription event.
			peerConnectionClient.createOffer();
		}
	}

	// Implementation of JamSquareClient.SignalingEvents Interface
	private class SignalingEventsListner implements SignalingEvents {
		@Override
		public void onServerPageRead() {
			startCall();
		}

		@Override
		public void onConnectedToServer() {
			createPeerConnectionFactory();
		}

		@Override
		public void onConnectedToRoom(final SignalingParameters params) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onConnectedToRoomInternal(params);
				}
			});
		}

		@Override
		public void onRemoteIceCandidate(final IceCandidate candidate) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (peerConnectionClient == null) {
						Log.e(TAG,
								"Received ICE candidate for non-initilized peer connection.");
						return;
					}
					peerConnectionClient.addRemoteIceCandidate(candidate);
				}
			});
		}

		@Override
		public void onRemoteDescription(final SessionDescription sdp) {
			final long delta = System.currentTimeMillis() - callStartedTimeMs;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (peerConnectionClient == null) {
						Log.e(TAG,
								"Received remote SDP for non-initilized peer connection.");
						return;
					}
					logAndToast("Received remote " + sdp.type + ", delay="
							+ delta + "ms");
					peerConnectionClient.setRemoteDescription(sdp);
					if (!signalingParameters.initiator) {
						logAndToast("Creating ANSWER...");
						// Create answer. Answer SDP will be sent to offering
						// client
						// in
						// PeerConnectionEvents.onLocalDescription event.
						peerConnectionClient.createAnswer();
					}
				}
			});
		}

		@Override
		public void onChannelClose() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					logAndToast("Remote end hung up; dropping PeerConnection");
					disconnect();
				}
			});
		}

		@Override
		public void onChannelError(final String description) {
			reportError(description);
		}

		@Override
		public void onPeerConnectionClosed() {
			peerConnectionClient.close();
		}

	}

	// Implementation of PeerConnectionClient.PeerConnectionEvents Interface
	private class PeerConnectionEventsListner implements PeerConnectionEvents {
		@Override
		public void onPeerConnectionStatsReady(final StatsReport[] reports) {
		}

		@Override
		public void onPeerConnectionError(final String description) {
			reportError(description);
		}

		@Override
		public void onPeerConnectionClosed() {
		}

		// -----Implementation of
		// PeerConnectionClient.PeerConnectionEvents.---------
		// Send local peer connection SDP and ICE candidates to remote party.
		// All callbacks are invoked from peer connection client looper thread
		// and are routed to UI thread.
		@Override
		public void onLocalDescription(final SessionDescription sdp) {
			final long delta = System.currentTimeMillis() - callStartedTimeMs;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (jamSquareClient != null) {
						logAndToast("Sending " + sdp.type + ", delay=" + delta
								+ "ms");
						jamSquareClient.sendOfferSdp(sdp);
					}
				}
			});
		}

		@Override
		public void onIceDisconnected() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					logAndToast("ICE disconnected");
					disconnect();
				}
			});
		}

		@Override
		public void onIceConnected() {
			final long delta = System.currentTimeMillis() - callStartedTimeMs;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					logAndToast("ICE connected, delay=" + delta + "ms");
					callConnected();
				}
			});
		}

		@Override
		public void onIceCandidate(final IceCandidate candidate) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (jamSquareClient != null) {
						jamSquareClient.sendLocalIceCandidate(candidate);
					}
				}
			});
		}

	}

	// Implementation of DataChannel.Observer Interface
	private class DataChannelObserver implements DataChannel.Observer {

		@Override
		public void onStateChange() {
			Log.d(TAG, "On DataChannel State Change");
		}

		@Override
		public void onMessage(Buffer buffer) {
			Log.d(TAG, "On DataChannel Message");

			ByteBuffer data = buffer.data;
			byte[] msgBytes = new byte[data.capacity()];
			data.get(msgBytes);

			try {
				JSONObject json = new JSONObject(new String(msgBytes));
				int throttle = json.getInt("throttle");
				int yaw = json.getInt("yaw");
				int pitch = json.getInt("pitch");
				int roll = json.getInt("roll");

				remote.updateRemot(throttle, yaw, pitch, roll);

				logAndToast("Message Received:\n Throttle = " + throttle
						+ ", Yaw = " + yaw + ", Pitch = " + pitch + ", Roll = "
						+ roll);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	// ******************* PID and Sensors Methods *****************
	//
	private void pidInitialize() {
		rollController = new PID();
		pitchController = new PID();
		yawController = new PID();

		rollController.setOutputLimits(Util.ROLL_PID_MIN, Util.ROLL_PID_MAX);
		pitchController.setOutputLimits(Util.PITCH_PID_MIN, Util.PITCH_PID_MAX);
		yawController.setOutputLimits(Util.YAW_PID_MIN, Util.YAW_PID_MAX);
		rollController.SetMode(Util.AUTOMATIC);
		pitchController.SetMode(Util.AUTOMATIC);
		yawController.SetMode(Util.AUTOMATIC);
		rollController.setSampleTime(Util.SAMPLETIME);
		pitchController.setSampleTime(Util.SAMPLETIME);
		yawController.setSampleTime(Util.SAMPLETIME);

	}

	private void pidCompute() {
		rollController.Compute();
		pitchController.Compute();
		yawController.Compute();
	}

	/**
	 * Runnable that calculate and read values from sensor and update output to
	 * the motors.
	 */
	private int m0;
	private int m1;
	private int m2;
	private int m3;
	private Runnable pidCalculation = new Runnable() {

		@Override
		public void run() {
			while (!isStopped) {
				double[] imuAngles = imu.getAngles();
				pitchController.updatePID(imuAngles[0],
						pitchController.getOutput(), remote.getPitch(), 5.0,
						0.0, 0.0, Util.DIRECT); // X
				rollController.updatePID(imuAngles[1],
						rollController.getOutput(), remote.getRoll(), 5.0, 0.0,
						0.0, Util.DIRECT); // Y
				yawController.updatePID(imuAngles[2],
						yawController.getOutput(), remote.getYaw(), 1.0, 0.0,
						0.0, Util.DIRECT); // Z
				pidCompute();

				double ratio = (double) remote.getThrottle() / 100;
				int throttle = (int) (ratio * 1000) + 1000;
				System.out.println(throttle + "  "
						+ pitchController.getOutput() + "  "
						+ rollController.getOutput() + "  "
						+ yawController.getOutput());
				// those values should be written to serial
				m0 = (int) (throttle + rollController.getOutput()
						- pitchController.getOutput() + yawController
						.getOutput());
				if (m0 > Util.MAX)
					m0 = (int) Util.MAX;
				else if (m0 < Util.MIN)
					m0 = (int) Util.MIN;
				m1 = (int) (throttle - rollController.getOutput()
						- pitchController.getOutput() - yawController
						.getOutput());
				if (m1 > Util.MAX)
					m1 = (int) Util.MAX;
				else if (m1 < Util.MIN)
					m1 = (int) Util.MIN;
				m2 = (int) (throttle + rollController.getOutput()
						+ pitchController.getOutput() - yawController
						.getOutput());
				if (m2 > Util.MAX)
					m2 = (int) Util.MAX;
				else if (m2 < Util.MIN)
					m2 = (int) Util.MIN;
				m3 = (int) (throttle - rollController.getOutput()
						+ pitchController.getOutput() + yawController
						.getOutput());

				if (m3 > Util.MAX)
					m3 = (int) Util.MAX;
				else if (m3 < Util.MIN)
					m3 = (int) Util.MIN;

				Log.d("", " " + m0 + ", " + m1 + ", " + m2 + ", " + m3);
				serial.sendToArduino(m0 + " " + m1 + " " + m2 + " " + m3 + " ");

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						motor0.setText(m0 + "");
						motor1.setText(m1 + "");
						motor2.setText(m2 + "");
						motor3.setText(m3 + "");
					}
				});
			}
		}

	};

}
