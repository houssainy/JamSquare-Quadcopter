/*
 * libjingle
 * Copyright 2014 Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.alexu.csed.jamsquare.connection;

import java.io.IOException;
import java.util.List;

import android.util.Log;

import com.alexu.csed.jamsquare.channel_api_client.ChannelAPI;
import com.alexu.csed.jamsquare.channel_api_client.ChannelAPI.ChannelException;
import com.alexu.csed.jamsquare.channel_api_client.ChannelService;
import com.alexu.csed.jamsquare.util.AsyncHttpURLConnection;
import com.alexu.csed.jamsquare.util.AsyncHttpURLConnection.AsyncHttpEvents;
import com.alexu.csed.jamsquare.util.LooperExecutor;

import org.apache.http.client.ClientProtocolException;
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

	private ChannelAPI channel;

	private LooperExecutor executor;

	private ConnectionState connectionState;

	private enum ConnectionState {
		NEW, CONNECTED, CLOSED, ERROR
	};

	private enum MessageType {
		MESSAGE, LEAVE
	};

	public JamSquareClient(SignalingEvents signalingEventsListner,
			LooperExecutor executor) {
		this.signalingEventsListner = signalingEventsListner;
		this.executor = executor;

		createSignalingParamters();
	}

	private void createSignalingParamters() {
		signalingParameters = new SignalingParameters(null, true,
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

	public void connectToSignalingServer() {
		AsyncHttpURLConnection httpConnection = new AsyncHttpURLConnection(
				"GET", getConnectURL() + "?id=" + QUADCOPTER_ID, null,
				new AsyncHttpEvents() {
					@Override
					public void onHttpError(String errorMessage) {
						reportError("GAE POST error: " + errorMessage);
					}

					@Override
					public void onHttpComplete(String response) {
						Log.d(TAG, "Connect response = " + response);
						// TODO(houssainy) Receive the message as Json after
						// updating the server's response
						String key = response;

						ChatListener chatListener = new ChatListener();
						try {
							channel = new ChannelAPI(SERVER_BASE_URL, key,
									chatListener);
							channel.open();
						} catch (ClientProtocolException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ChannelException e) {
							e.printStackTrace();
						}
					}
				});
		httpConnection.send();
	}

	public void disconnectFromSignalingServer() {
		Log.d(TAG, "Disconnect. Connection state: " + connectionState);
		if (connectionState == ConnectionState.CONNECTED) {
			Log.d(TAG, "Closing room.");
			sendPostMessage(MessageType.LEAVE, null);
		}
		connectionState = ConnectionState.CLOSED;

		try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Send Ice candidate to the other participant.
	public void sendLocalIceCandidate(final IceCandidate candidate) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				JSONObject json = new JSONObject();
				jsonPut(json, "type", "candidate");
				jsonPut(json, "id", signalingParameters.clientId);
				jsonPut(json, "data", candidate.sdp);

				// Call initiator sends ice candidates to GAE server.
				if (connectionState != ConnectionState.CONNECTED) {
					reportError("Sending ICE candidate in non connected state.");
					return;
				}
				sendPostMessage(MessageType.MESSAGE, json.toString());
			}
		});
	}

	// Send local offer SDP to the other participant.
	public void sendOfferSdp(final SessionDescription sdp) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				if (connectionState != ConnectionState.CONNECTED) {
					reportError("Sending offer SDP in non connected state.");
					return;
				}
				JSONObject json = new JSONObject();
				jsonPut(json, "type", "offer");
				jsonPut(json, "id", signalingParameters.clientId);
				jsonPut(json, "data", sdp.description);
				sendPostMessage(MessageType.MESSAGE, json.toString());
			}
		});
	}

	// --------------------------------------------------------------------
	// Helper functions.
	private void reportError(final String errorMessage) {
		Log.e(TAG, errorMessage);
		executor.execute(new Runnable() {
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

	// Send SDP or ICE candidate to a room server.
	private void sendPostMessage(final MessageType messageType,
			final String message) {
		// TODO(houssainy) Implement this method
		// String logInfo = url;
		// if (message != null) {
		// logInfo += ". Message: " + message;
		// }
		// Log.d(TAG, "C->GAE: " + logInfo);
		// AsyncHttpURLConnection httpConnection = new AsyncHttpURLConnection(
		// "POST", url, message, new AsyncHttpEvents() {
		// @Override
		// public void onHttpError(String errorMessage) {
		// reportError("GAE POST error: " + errorMessage);
		// }
		//
		// @Override
		// public void onHttpComplete(String response) {
		// if (messageType == MessageType.MESSAGE) {
		// try {
		// JSONObject roomJson = new JSONObject(response);
		// String result = roomJson.getString("result");
		// if (!result.equals("SUCCESS")) {
		// reportError("GAE POST error: " + result);
		// }
		// } catch (JSONException e) {
		// reportError("GAE POST JSON error: "
		// + e.toString());
		// }
		// }
		// }
		// });
		// httpConnection.send();
	}

	/**
	 * Callback interface for messages delivered on signaling channel.
	 * 
	 * <p>
	 * Methods are guaranteed to be invoked on the UI thread of |activity|.
	 */
	public static interface SignalingEvents {
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

	public class ChatListener implements ChannelService {

		/**
		 * Method gets called when we initially connect to the server
		 */
		@Override
		public void onOpen() {
			Log.d(TAG, "Channel API Opened.");
		}

		/**
		 * Method gets called when the server sends a message.
		 */
		@Override
		public void onMessage(String message) {
			// TODO(houssainy)
			System.out.println("Server push: " + message);
		}

		/**
		 * Method gets called when we close the connection to the server.
		 */
		@Override
		public void onClose() {
			Log.d(TAG, "Channel API Cloesed.");
		}

		/**
		 * Method gets called when an error occurs on the server.
		 */
		@Override
		public void onError(Integer errorCode, String description) {
			Log.d(TAG, "Error: " + errorCode + " Reason: " + description);
		}

	}
}
