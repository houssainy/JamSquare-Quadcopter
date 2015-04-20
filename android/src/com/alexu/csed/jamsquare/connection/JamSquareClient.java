package com.alexu.csed.jamsquare.connection;

import com.alexu.csed.jamsquare.SignalingParameters;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class JamSquareClient {

	public JamSquareClient(SignalingEvents signalingEventsListner,
			String stringExtra) {
		// TODO Auto-generated constructor stub
	}

	public void connectToSignalingServer() {
		// TODO Auto-generated method stub

	}

	public void disconnectFromServer() {
		// TODO Auto-generated method stub

	}

	public void sendLocalIceCandidate(IceCandidate candidate) {
		// TODO Auto-generated method stub

	}

	public void sendOfferSdp(SessionDescription sdp) {
		// TODO Auto-generated method stub

	}

	public void sendAnswerSdp(SessionDescription sdp) {
		// TODO Auto-generated method stub

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

}
