package com.graduation_project.jam_square.server.handlers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
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

	/**
	 * 
	 */
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
			} else {
				sendResponse(resp, "Quadcopter already connected!");
			}
		} else {// Client
			if (pm.getClientPeer() == null) { // No connected client
				System.out.println("Client connected with id = "
						+ presence.clientId());
				pm.setClientPeer(new Peer(presence.clientId()));
			} else {
				sendResponse(resp, "There is an active client connected.");
			}
		}
		// TODO(houssainy) exchange offer between the two clients if exist -this
		// case will happen if one of the peers joined then the other client
		// joined after a while from the first peer
		// pm.update
	}

	private void sendResponse(HttpServletResponse resp, String msg)
			throws IOException {
		resp.setContentType("text/html");
		resp.getWriter().write(msg);
	}
}
