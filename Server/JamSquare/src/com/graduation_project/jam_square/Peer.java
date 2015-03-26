package com.graduation_project.jam_square;

import java.util.ArrayList;

public class Peer {
	private String id;
	private ArrayList<String> iceCandidates;
	private String stream;
	private String offer;

	public Peer(String id) {
		this.id = id;
		this.iceCandidates = new ArrayList<String>();
	}

	public void addIceCandidate(String iceCandidate) {
		iceCandidates.add(iceCandidate);
	}

	public ArrayList<String> getIceCandidates() {
		return iceCandidates;
	}

	public void addStream(String stream) {
		this.stream = stream;
	}

	public String getStream() {
		return stream;
	}

	public String getId() {
		return id;
	}

	public String getOffer() {
		return offer;
	}

	public void setOffer(String offer) {
		this.offer = offer;
	}
}
