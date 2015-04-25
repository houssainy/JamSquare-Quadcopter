/*
 * Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
 */
package com.graduation_project.jam_square;

/**
 * 
 * @author houssainy
 *
 *         Helper Class to hold information about the connected peers to the
 *         server. It assumes that only two peers will connect to server at one
 *         moment, so this class don't handle multiple connection and handling
 *         multiple peerconnections is left for future work.
 * 
 *         TODO(houssainy) Handle multiple peerconnections.
 */
public class PeerManager {
	private Peer quadCopterPeer;
	private Peer clientPeer;

	private static PeerManager pm;

	/**
	 * Singleton constructor.
	 */
	private PeerManager() {
	}

	/**
	 * 
	 * @return new Object of PerManager if not already created.
	 */
	public static PeerManager get() {
		if (pm != null)
			return pm;
		return (pm = new PeerManager());
	}

	public Peer getQuadCopterPeer() {
		return quadCopterPeer;
	}

	public void setQuadCopterPeer(Peer quadCopterPeer) {
		this.quadCopterPeer = quadCopterPeer;
	}

	public Peer getClientPeer() {
		return clientPeer;
	}

	public void setClientPeer(Peer clientPeer) {
		this.clientPeer = clientPeer;
	}
}
