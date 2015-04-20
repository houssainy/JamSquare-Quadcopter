package com.alexu.csed.jamsquare;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import com.alexu.csed.jamsquare.PeerConnectionClient.PeerConnectionEvents;
import com.alexu.csed.jamsquare.PeerConnectionClient.PeerConnectionParameters;
import com.alexu.csed.jamsquare.connection.JamSquareClient;
import com.alexu.csed.jamsquare.connection.JamSquareClient.SignalingEvents;

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
	public static final String SIGNALING_ID = "com.alexu.csed.SIGNALING_ID";

	private SignalingParameters signalingParameters;
	private PeerConnectionParameters peerConnectionParameters;

	private boolean isError;

	private PeerConnectionClient peerConnectionClient = null;
	private JamSquareClient jamSquareClient;

	private long callStartedTimeMs = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		signalingParameters = null;

		createPeerConnectionFactory();

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
		jamSquareClient = new JamSquareClient(signalingEventsListner,
				intent.getStringExtra(SIGNALING_ID));

		startCall();
	}

	// Activity interfaces
	@Override
	public void onPause() {
		super.onPause();
		// TODO(houssainy) Check if i need to stop the video source or not
		if (peerConnectionClient != null) {
			peerConnectionClient.stopVideoSource();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// TODO(houssainy) Remove this if source video will not be stopped
		if (peerConnectionClient != null) {
			peerConnectionClient.startVideoSource();
		}
	}

	@Override
	protected void onDestroy() {
		disconnect();
		super.onDestroy();
	}

	// Log |msg| and Toast about it.
	private void logAndToast(String msg) {
		Log.d(TAG, msg);
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	private void startCall() {
		callStartedTimeMs = System.currentTimeMillis();

		// Start connection to signaling server.
		logAndToast("Connecting to Signaling Server...");
		jamSquareClient.connectToSignalingServer();
		logAndToast("Connected.");
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
							peerConnectionEventsListner);
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
			jamSquareClient.disconnectFromServer();
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
				signalingParameters);

		if (signalingParameters.initiator) {
			logAndToast("Creating OFFER...");
			// Create offer. Offer SDP will be sent to answering client in
			// PeerConnectionEvents.onLocalDescription event.
			peerConnectionClient.createOffer();
		} else {
			if (params.offerSdp != null) {
				peerConnectionClient.setRemoteDescription(params.offerSdp);
				logAndToast("Creating ANSWER...");
				// Create answer. Answer SDP will be sent to offering client
				// in
				// PeerConnectionEvents.onLocalDescription event.
				peerConnectionClient.createAnswer();
			}
			if (params.iceCandidates != null) {
				// Add remote ICE candidates from room.
				for (IceCandidate iceCandidate : params.iceCandidates) {
					peerConnectionClient.addRemoteIceCandidate(iceCandidate);
				}
			}
		}
	}

	// Implementation of JamSquareClient.SignalingEvents Interface
	private SignalingEvents signalingEventsListner = new SignalingEvents() {

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

	};

	// Implementation of PeerConnectionClient.PeerConnectionEvents Interface
	private PeerConnectionEvents peerConnectionEventsListner = new PeerConnectionEvents() {
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
						if (signalingParameters.initiator) {
							jamSquareClient.sendOfferSdp(sdp);
						} else {
							jamSquareClient.sendAnswerSdp(sdp);
						}
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
	};
}
