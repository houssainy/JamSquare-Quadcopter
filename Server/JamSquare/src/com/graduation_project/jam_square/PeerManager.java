package com.graduation_project.jam_square;

public class PeerManager {
	private Peer quadCopterPeer;
	private Peer clientPeer;

	private static PeerManager pm;

	private PeerManager() {
	}

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
