/*
 * Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
 */
package com.graduation_project.jam_square;

import java.util.ArrayList;

/**
 * 
 * @author houssainy
 *
 *         Struct to hold all the information needed about the peer. So this
 *         struct hold Peer Id, iceCandidates, and sdpOffer.
 *
 */
public class Peer {
	private String id;
	private ArrayList<Object> iceCandidates;
	private Object offer;

	public Peer(String id) {
		this.id = id;
		this.iceCandidates = new ArrayList<Object>();
	}

	public void addIceCandidate(Object iceCandidate) {
		iceCandidates.add(iceCandidate);
	}

	public ArrayList<Object> getIceCandidates() {
		return iceCandidates;
	}

	public String getId() {
		return id;
	}

	public Object getOffer() {
		return offer;
	}

	public void setOffer(Object offer) {
		this.offer = offer;
	}
}
