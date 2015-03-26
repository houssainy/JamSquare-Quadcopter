package com.graduation_project.jam_square.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gson.Gson;
import com.graduation_project.jam_square.Peer;
import com.graduation_project.jam_square.PeerManager;
import com.graduation_project.jam_square.Util;

/**
 * 
 * @author houssainy
 *
 *         This class is responsible of receiving any update in the events of
 *         webRTC (onOffer, onAnswer, onIceCandidate, onAddStream). Once the
 *         server receives a new event from one of the peers, it immediately
 *         sends it to all the other peers. Also this class will provide the
 *         ability of get all the ICECandidate, stream Ids which have been
 *         posted from the other peer.
 * 
 *         URL: /eventupdate
 */
public class EventServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		StringBuilder sb = new StringBuilder();
		BufferedReader br = req.getReader();
		String temp;
		while ((temp = br.readLine()) != null)
			sb.append(temp);

		Gson gson = new Gson();
		@SuppressWarnings("unchecked")
		HashMap<String, String> dataMap = gson.fromJson(sb.toString(),
				HashMap.class);

		String peerId = dataMap.get("id");
		if (peerId == null) {
			sendResponse(resp, "<h1>Missing User Id!</h1>");
			return;
		}

		String type = dataMap.get(Util.TYPE);
		String data = dataMap.get(Util.DATA);

		PeerManager pm = PeerManager.get();
		switch (type) {
		case Util.ONOFFER:
			onOffer(data, pm, peerId);
			break;
		case Util.ONANSWER:
			onAnswer(data, pm, peerId);
			break;
		case Util.ONICECANDIDATE:
			onIceCandidate(data, pm, peerId);
			break;
		case Util.ONADDSTREAM:
			onAddStream(data, pm, peerId);
			break;
		}
	}

	/**
	 * Send the received offer from the sending peer and pass it the second peer
	 * if exist.
	 * 
	 * @param offer
	 * @param pm
	 * @param peerId
	 */
	private void onOffer(String offer, PeerManager pm, String peerId) {
		if (peerId.equals(Util.QUADCOPTER_ID)) {
			if (pm.getClientPeer() != null)
				sendDataThroughChannelTo(pm.getClientPeer(), offer);

		} else {
			if (pm.getQuadCopterPeer() != null)
				sendDataThroughChannelTo(pm.getQuadCopterPeer(), offer);
		}
	}

	/**
	 * Send the received answer from the sending peer and pass it the second
	 * peer if exist.
	 * 
	 * @param answer
	 * @param pm
	 * @param peerId
	 */
	private void onAnswer(String answer, PeerManager pm, String peerId) {
		if (peerId.equals(Util.QUADCOPTER_ID)) {
			if (pm.getClientPeer() != null)
				sendDataThroughChannelTo(pm.getClientPeer(), answer);
		} else {
			if (pm.getQuadCopterPeer() != null)
				sendDataThroughChannelTo(pm.getQuadCopterPeer(), answer);

		}
	}

	/**
	 * 
	 * @param iceCandidate
	 * @param pm
	 * @param peerId
	 */
	private void onIceCandidate(String iceCandidate, PeerManager pm,
			String peerId) {
		if (peerId.equals(Util.QUADCOPTER_ID)) {
			pm.getQuadCopterPeer().addIceCandidate(iceCandidate);
			if (pm.getClientPeer() != null)
				sendDataThroughChannelTo(pm.getClientPeer(), iceCandidate);
		} else {
			pm.getClientPeer().addIceCandidate(iceCandidate);
			if (pm.getQuadCopterPeer() != null)
				sendDataThroughChannelTo(pm.getQuadCopterPeer(), iceCandidate);
		}
	}

	/**
	 * 
	 * @param stream
	 * @param pm
	 * @param peerId
	 */
	private void onAddStream(String stream, PeerManager pm, String peerId) {
		if (peerId.equals(Util.QUADCOPTER_ID)) {
			if (pm.getClientPeer() != null)
				sendDataThroughChannelTo(pm.getClientPeer(), stream);
		} else {
			if (pm.getQuadCopterPeer() != null)
				sendDataThroughChannelTo(pm.getQuadCopterPeer(), stream);
		}
	}

	private void sendDataThroughChannelTo(Peer peer, String data) {
		ChannelService channelService = ChannelServiceFactory
				.getChannelService();
		String channelKey = peer.getId();
		ChannelMessage msg = new ChannelMessage(channelKey, data);
		channelService.sendMessage(msg);
	}

	private void sendResponse(HttpServletResponse resp, String msg)
			throws IOException {
		resp.setContentType("text/html");
		resp.getWriter().write(msg);
	}
}
