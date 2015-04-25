// Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
//
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
 *         webRTC events (onOffer, onAnswer, onIceCandidate, onAddStream). Once
 *         the server receives a new event from one of the peers, it immediately
 *         sends it to all the other peers. Also this class will provide the
 *         ability of get all the ICECandidate, stream Ids which have been
 *         posted from the other peer.
 * 
 *         URL: /eventupdate
 */
public class EventServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String tempStr;
		StringBuilder sb = new StringBuilder();
		BufferedReader br = req.getReader();

		while ((tempStr = br.readLine()) != null)
			sb.append(tempStr + "\r\n");

		Gson gson = new Gson();
		@SuppressWarnings("unchecked")
		HashMap<String, Object> dataMap = gson.fromJson(sb.toString(),
				HashMap.class);

		Object temp = dataMap.get(Util.ID);
		if (temp == null) {
			sendResponse(resp, Util.ERROR, "ERROR: Missing User Id!");
			return;
		}
		String peerId = temp.toString();

		temp = dataMap.get(Util.TYPE);
		if (temp == null) {
			sendResponse(resp, Util.ERROR, "ERROR: Message Type is Missed!");
			return;
		}

		String type = temp.toString();

		Object data = dataMap.get(Util.DATA);
		if (data == null) {
			sendResponse(resp, Util.ERROR, "ERROR: No Data is Received!");
			return;
		}

		System.out.println("Message Received:\n" + dataMap);

		PeerManager pm = PeerManager.get();
		switch (type) {
		case Util.OFFER:
			onOffer(data, pm, peerId);
			break;
		case Util.ANSWER:
			onAnswer(data, pm, peerId);
			break;
		case Util.ICECANDIDATE:
			onIceCandidate(data, pm, peerId);
			break;
		}
		sendResponse(resp, Util.OK, "OK");
	}

	/**
	 * Send the received offer from the sending peer and pass it the second peer
	 * if exist.
	 * 
	 * @param offer
	 * @param pm
	 * @param peerId
	 */
	private void onOffer(Object offer, PeerManager pm, String peerId) {
		if (peerId.equals(Util.QUADCOPTER_ID)) {
			pm.getQuadCopterPeer().setOffer(offer);

			if (pm.getClientPeer() != null)
				sendDataThroughChannelTo(pm.getClientPeer(), offer, Util.OFFER);

		} else if (pm.getClientPeer() != null
				&& pm.getClientPeer().getId().equals(peerId)) {

			pm.getClientPeer().setOffer(offer);
			if (pm.getQuadCopterPeer() != null)
				sendDataThroughChannelTo(pm.getQuadCopterPeer(), offer,
						Util.OFFER);
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
	private void onAnswer(Object answer, PeerManager pm, String peerId) {
		if (peerId.equals(Util.QUADCOPTER_ID)) {
			if (pm.getClientPeer() != null)
				sendDataThroughChannelTo(pm.getClientPeer(), answer,
						Util.ANSWER);
		} else if (pm.getClientPeer() != null
				&& pm.getClientPeer().getId().equals(peerId)) {

			if (pm.getQuadCopterPeer() != null)
				sendDataThroughChannelTo(pm.getQuadCopterPeer(), answer,
						Util.ANSWER);
		}
	}

	/**
	 * 
	 * @param iceCandidate
	 * @param pm
	 * @param peerId
	 */
	private void onIceCandidate(Object iceCandidate, PeerManager pm,
			String peerId) {
		if (peerId.equals(Util.QUADCOPTER_ID)) {
			pm.getQuadCopterPeer().addIceCandidate(iceCandidate);
			if (pm.getClientPeer() != null)
				sendDataThroughChannelTo(pm.getClientPeer(), iceCandidate,
						Util.ICECANDIDATE);
		} else if (pm.getClientPeer() != null
				&& pm.getClientPeer().getId().equals(peerId)) {
			pm.getClientPeer().addIceCandidate(iceCandidate);
			if (pm.getQuadCopterPeer() != null)
				sendDataThroughChannelTo(pm.getQuadCopterPeer(), iceCandidate,
						Util.ICECANDIDATE);
		}
	}

	private void sendDataThroughChannelTo(Peer peer, Object data, String type) {
		ChannelService channelService = ChannelServiceFactory
				.getChannelService();
		String channelKey = peer.getId();

		HashMap<String, Object> mapData = new HashMap<String, Object>();
		mapData.put("type", type);
		mapData.put("data", data);

		ChannelMessage msg = new ChannelMessage(channelKey,
				new Gson().toJson(mapData));
		channelService.sendMessage(msg);
	}

	private void sendResponse(HttpServletResponse resp, String type, String msg)
			throws IOException {
		HashMap<String, Object> mapData = new HashMap<String, Object>();
		mapData.put("type", type);
		mapData.put("data", msg);
		
		resp.setContentType("text/html");
		resp.getWriter().write(new Gson().toJson(mapData));
	}
}
