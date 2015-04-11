package com.alexu.csed.jamsquare;

import java.util.List;

import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

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
	public final String wssUrl;
	public final String wssPostUrl;
	public final SessionDescription offerSdp;
	public final List<IceCandidate> iceCandidates;

	public SignalingParameters(List<PeerConnection.IceServer> iceServers,
			boolean initiator, MediaConstraints pcConstraints,
			MediaConstraints videoConstraints,
			MediaConstraints audioConstraints, String clientId, String wssUrl,
			String wssPostUrl, SessionDescription offerSdp,
			List<IceCandidate> iceCandidates) {
		this.iceServers = iceServers;
		this.initiator = initiator;
		this.pcConstraints = pcConstraints;
		this.videoConstraints = videoConstraints;
		this.audioConstraints = audioConstraints;
		this.clientId = clientId;
		this.wssUrl = wssUrl;
		this.wssPostUrl = wssPostUrl;
		this.offerSdp = offerSdp;
		this.iceCandidates = iceCandidates;
	}
}