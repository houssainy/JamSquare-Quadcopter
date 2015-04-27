/*
 *  Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
 */
package com.graduation_project.jam_square.server.handlers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelPresence;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.graduation_project.jam_square.PeerManager;

public class OnDisconnectServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// In the handler for _ah/channel/connected/
		ChannelService channelService = ChannelServiceFactory
				.getChannelService();
		ChannelPresence presence = channelService.parsePresence(req);

		PeerManager.get().disconnectClientWithId(presence.clientId());
	}
}