/*
 *  Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
 */
package com.alexu.csed.jamsquare.connection;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

public class JamSquareClient {
	private static final String TAG = "JamSquareClient";
	private static final String QUADCOPTER_ID = "__quadcopter1992";
	private final String SERVER_BASE_URL = "https://jam-square.appspot.com";

	private SignalingEvents signalingEventsListner;
	private SignalingParameters signalingParameters;

	private ExecutorService postMessagesExcutor;

	private Activity activity;
	private WebView webView;

	private ConnectionState connectionState;

	private enum ConnectionState {
		NEW, CONNECTED, CLOSED, ERROR
	};

	public JamSquareClient(SignalingEvents signalingEventsListner,
			Activity activity) {
		this.signalingEventsListner = signalingEventsListner;
		this.activity = activity;

		this.postMessagesExcutor = Executors.newFixedThreadPool(1);

		connectionState = ConnectionState.NEW;

		createSignalingParamters();
		initializaWebView();
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@SuppressLint({ "JavascriptInterface", "SetJavaScriptEnabled" })
	private void initializaWebView() {
		webView = (WebView) activity
				.findViewById(com.alexu.csed.jamsquare.R.id.webview);
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);

		webView.addJavascriptInterface(new SignalingJSInterface(), "Android");

		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("MyApplication",
						cm.message() + " -- From line " + cm.lineNumber()
								+ " of " + cm.sourceId());
				return true;
			}

			@Override
			public void onCloseWindow(WebView window) {
				super.onCloseWindow(window);
				Log.d(TAG, "Window close");
			};
		});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			WebView.setWebContentsDebuggingEnabled(true);
		}

		// webView.loadUrl("https://jam-square.appspot.com/quadcopter.html");
		webView.loadUrl("http://192.168.43.67:8080//quadcopter.html");
	}

	private void createSignalingParamters() {
		signalingParameters = new SignalingParameters(
				new LinkedList<PeerConnection.IceServer>(), true,
				getPcConstraints(), getVideoConstraints(),
				getAudioConstraints(), QUADCOPTER_ID, getConnectURL(),
				getPostURL(), null, null);
	}

	private String getPostURL() {
		return SERVER_BASE_URL + "/eventupdate";
	}

	private String getConnectURL() {
		return SERVER_BASE_URL + "/connect";
	}

	private MediaConstraints getPcConstraints() {
		// TODO(houssainy) Check the needed constraints
		return new MediaConstraints();
	}

	private MediaConstraints getVideoConstraints() {
		// TODO(houssainy) Check the needed constraints
		return new MediaConstraints();
	}

	private MediaConstraints getAudioConstraints() {
		// TODO(houssainy) Check the needed constraints
		return new MediaConstraints();
	}

	public SignalingParameters getSignalingParameters() {
		return signalingParameters;
	}

	// ************* JS Calls ****************
	public void connectToSignalingServer() {
		loadURLOnWebView("javascript:connectToSignallingServer(\""
				+ QUADCOPTER_ID + "\")");
	}

	public void disconnectFromSignalingServer() {
		Log.d(TAG, "Disconnect. Connection state: " + connectionState);
		Log.d(TAG, "Closing room.");

		connectionState = ConnectionState.CLOSED;

		loadURLOnWebView("javascript:close()");
	}

	// Send Ice candidate to the other participant.
	public void sendLocalIceCandidate(final IceCandidate candidate) {
		postMessagesExcutor.execute(new Runnable() {
			@Override
			public void run() {
				if (candidate.sdp == null)
					return;

				JSONObject json = new JSONObject();
				jsonPut(json, "type", "candidate");
				jsonPut(json, "id", signalingParameters.clientId);

				JSONObject jsonCandidate = new JSONObject();
				jsonPut(jsonCandidate, "sdpMLineIndex", candidate.sdpMLineIndex);
				jsonPut(jsonCandidate, "sdpMid", candidate.sdpMid);
				jsonPut(jsonCandidate, "candidate", candidate.sdp);

				jsonPut(json, "data", jsonCandidate);

				// Call initiator sends ice candidates to GAE server.
				if (connectionState != ConnectionState.CONNECTED) {
					reportError("Sending ICE candidate in non connected state.");
					return;
				}
				sendPostMessage(json.toString());
			}
		});
	}

	// Send local offer SDP to the other participant.
	public void sendOfferSdp(final SessionDescription sdp) {
		postMessagesExcutor.execute(new Runnable() {
			@Override
			public void run() {
				if (connectionState != ConnectionState.CONNECTED) {
					reportError("Sending offer SDP in non connected state.");
					return;
				}
				JSONObject json = new JSONObject();
				jsonPut(json, "type", "offer");
				jsonPut(json, "id", signalingParameters.clientId);

				JSONObject jsonSdp = new JSONObject();
				jsonPut(jsonSdp, "sdp", sdp.description);
				jsonPut(jsonSdp, "type", sdp.type.canonicalForm());
				System.out.println(sdp.description);
				jsonPut(json, "data", jsonSdp);

				sendPostMessage(json.toString());
			}
		});
	}

	// Send message to signaling server using JS method
	private void sendPostMessage(String msg) {
		loadURLOnWebView("javascript:sendToServer(\'" + msg + "\');");
	}

	// --------------------------------------------------------------------
	// Helper functions.
	private void loadURLOnWebView(final String url) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				webView.loadUrl(url);
			}
		});
	}

	private void reportError(final String errorMessage) {
		Log.e(TAG, errorMessage);
		postMessagesExcutor.execute(new Runnable() {
			@Override
			public void run() {
				if (connectionState != ConnectionState.ERROR) {
					connectionState = ConnectionState.ERROR;
					signalingEventsListner.onChannelError(errorMessage);
				}
			}
		});
	}

	// Put a |key|->|value| mapping in |json|.
	private static void jsonPut(JSONObject json, String key, Object value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Struct holding the signaling parameters of an JamSquare Server.
	 */
	public class SignalingParameters {
		public final List<PeerConnection.IceServer> iceServers;
		public final boolean initiator;
		public final MediaConstraints pcConstraints;
		public final MediaConstraints videoConstraints;
		public final MediaConstraints audioConstraints;
		public final String clientId;
		public final String wsGetUrl;
		public final String wsPostUrl;
		public final SessionDescription offerSdp;
		public final List<IceCandidate> iceCandidates;

		public SignalingParameters(List<PeerConnection.IceServer> iceServers,
				boolean initiator, MediaConstraints pcConstraints,
				MediaConstraints videoConstraints,
				MediaConstraints audioConstraints, String clientId,
				String wsGetUrl, String wsPostUrl, SessionDescription offerSdp,
				List<IceCandidate> iceCandidates) {
			this.iceServers = iceServers;
			this.initiator = initiator;
			this.pcConstraints = pcConstraints;
			this.videoConstraints = videoConstraints;
			this.audioConstraints = audioConstraints;
			this.clientId = clientId;
			this.wsGetUrl = wsGetUrl;
			this.wsPostUrl = wsPostUrl;
			this.offerSdp = offerSdp;
			this.iceCandidates = iceCandidates;
		}
	}

	/**
	 * Callback interface to be exposed to JavaScript through webView.
	 */
	public class SignalingJSInterface {
		@JavascriptInterface
		public void onPageReady() {
			Log.d(TAG, "Page Ready...");
			logAndToast("Page Ready...");
			signalingEventsListner.onServerPageRead();
		}

		@JavascriptInterface
		public void onConnectToSignalingServer() {
			Log.d(TAG, "Connection opened...");
			logAndToast("Connection opened...");

			connectionState = ConnectionState.CONNECTED;
			signalingEventsListner.onConnectedToServer();
		}

		@JavascriptInterface
		public void onSignalingChannelClosed() {
			Log.d(TAG, "Connection To Server Closed...");
		}

		@JavascriptInterface
		public void onResponse(String response) {
			Log.d(TAG, "Response: " + response);
		}

		@JavascriptInterface
		public void onSignalingError(String error) {
			reportError(error);
		}

		@JavascriptInterface
		public void onMessage(String msg) {
			Log.d(TAG, "On message " + msg);
			try {
				JSONObject tempJson = new JSONObject(msg);
				JSONObject json = new JSONObject(tempJson.getString("data"));
				String type = json.getString("type");
				if (type.equals("answer")) {
					JSONObject jsonAnswer = json.getJSONObject("data");

					SessionDescription sdp = new SessionDescription(
							SessionDescription.Type.fromCanonicalForm(jsonAnswer
									.getString("type")),
							jsonAnswer.getString("sdp"));
					signalingEventsListner.onRemoteDescription(sdp);
				} else if (type.equals("candidate")) { // Candidate
					JSONObject jsonCandidate = json.getJSONObject("data");

					IceCandidate candidate = new IceCandidate(
							jsonCandidate.getString("sdpMid"),
							jsonCandidate.getInt("sdpMLineIndex"),
							jsonCandidate.getString("candidate"));

					signalingEventsListner.onRemoteIceCandidate(candidate);
				} else if (type.equals("bye")) {
					
				} else {
					reportError("Unexpected message: " + msg);
				}

			} catch (JSONException e) {
				reportError("Message JSON parsing error: " + e.toString());
			}
		}
	}

	public void logAndToast(String toast) {
		Log.d(TAG, toast);
		Toast.makeText(activity, toast, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Callback interface for messages delivered on signaling channel.
	 * 
	 * <p>
	 * Methods are guaranteed to be invoked on the UI thread of |activity|.
	 */
	public static interface SignalingEvents {
		/**
		 * Callback fired once the html page of the server is ready.
		 */
		public void onServerPageRead();

		/**
		 * Callback fired once the channel api is opened and ready to be used.
		 */
		public void onConnectedToServer();

		/**
		 * Callback fired once the room's signaling parameters
		 * SignalingParameters are extracted.
		 */
		public void onConnectedToRoom(final SignalingParameters params);

		/**
		 * Callback fired once remote SDP is received.
		 */
		public void onRemoteDescription(final SessionDescription sdp);

		/**
		 * Callback fired once remote Ice candidate is received.
		 */
		public void onRemoteIceCandidate(final IceCandidate candidate);

		/**
		 * Callback fired once channel is closed.
		 */
		public void onChannelClose();

		/**
		 * Callback fired once channel error happened.
		 */
		public void onChannelError(final String description);
		
		/**
		 * Callback fired once the other peer closed.
		 */
		public void onPeerConnectionClosed();
	}

	public void close() {
		loadURLOnWebView("javascript:close()");
		webView.removeAllViews();
		webView.destroy();
	}
}
