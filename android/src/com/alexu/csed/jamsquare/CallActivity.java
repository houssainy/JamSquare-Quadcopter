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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

	private SignalingParameters signalingParameters;
	private PeerConnectionParameters peerConnectionParameters;

	private boolean isError;

	private PeerConnectionClient peerConnectionClient = null;
	private JamSquareClient jamSquareClient;

	private long callStartedTimeMs = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_activity);

		final Intent intent = getIntent();
		peerConnectionParameters = new PeerConnectionParameters(
				intent.getBooleanExtra(EXTRA_VIDEO_CALL, true),
				intent.getBooleanExtra(EXTRA_LOOPBACK, false),
				intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0), intent.getIntExtra(
						EXTRA_VIDEO_HEIGHT, 0), intent.getIntExtra(
						EXTRA_VIDEO_FPS, 0), intent.getIntExtra(
						EXTRA_VIDEO_BITRATE, 0),
				intent.getStringExtra(EXTRA_VIDEOCODEC),
				intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
				intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0),
				intent.getStringExtra(EXTRA_AUDIOCODEC),
				intent.getBooleanExtra(EXTRA_CPUOVERUSE_DETECTION, true));

		// Create connection client.
		jamSquareClient = new JamSquareClient(new SignalingEventsListner(),
				this);
		signalingParameters = jamSquareClient.getSignalingParameters();
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
			// TODO(houssainy)
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

				logAndToast("Message Received:\n Throttle = " + throttle
						+ ", Yaw = " + yaw + ", Pitch = " + pitch + ", Roll = "
						+ roll);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

	}
}
