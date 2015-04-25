/*
 *  Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
 */
package com.graduation_project.jam_square.server.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelPresence;
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
 *
 *         Handler to handle on connect POST request from Channel Presence on
 *         _ah/channel/connected/
 */
public class OnConnectServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// In the handler for _ah/channel/connected/
		ChannelService channelService = ChannelServiceFactory
				.getChannelService();
		ChannelPresence presence = channelService.parsePresence(req);
		PeerManager pm = PeerManager.get();
		
		if (presence.clientId().equals(Util.QUADCOPTER_ID)) { // Quadcopter
			if (pm.getQuadCopterPeer() == null) { // No connected Quadcopter
				System.out.println("Quadcopter connected with id = "
						+ Util.QUADCOPTER_ID);
				pm.setQuadCopterPeer(new Peer(Util.QUADCOPTER_ID));

				// Get all cached data from ClientPeer
				if (pm.getClientPeer() != null)
					getDataFrom(pm.getClientPeer(), pm.getQuadCopterPeer());
			} else { // Quadcopter Connected
				sendResponse(resp, "Quadcopter already connected!");
			}
		} else {// Client
			if (pm.getClientPeer() == null) { // No connected client
				System.out.println("Client connected with id = "
						+ presence.clientId());
				pm.setClientPeer(new Peer(presence.clientId()));

				// Get all cached data from QuadcopterPeer
				if (pm.getQuadCopterPeer() != null)
					getDataFrom(pm.getQuadCopterPeer(), pm.getClientPeer());
			} else { // Client connected
				sendResponse(resp, "There is an active client connected.");
			}
		}
	}

	// Exchange all cached data from srcPeer to dstPeer.
	// This case will happen if one of the peers joined then the other client
	// joined after a while from the first peer
	private void getDataFrom(Peer srcPeer, Peer dstPeer) {
		if (dstPeer == null) // dstPeer still not connected
			return;

		if (srcPeer.getOffer() != null) {
			sendDataThroughChannelTo(dstPeer, srcPeer.getOffer(), Util.OFFER);
		}

		ArrayList<Object> iceCandidates = srcPeer.getIceCandidates();
		for (Iterator<Object> iterator = iceCandidates.iterator(); iterator
				.hasNext();) {
			Object candidate = iterator.next();
			sendDataThroughChannelTo(dstPeer, candidate, Util.ICECANDIDATE);
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

	private void sendResponse(HttpServletResponse resp, String msg)
			throws IOException {
		resp.setContentType("text/html");
		resp.getWriter().write(msg);
	}
}
