/*
 *  Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
 */
package com.graduation_project.jam_square.server;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gson.Gson;
import com.graduation_project.jam_square.Util;

/**
 *
 * @author houssainy
 *
 *         This class is responsible in connecting receiving new client
 *         connection and creating new token for each new client.
 *
 *         URL: /connect?id=
 */
public class ConnectServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String peerId = req.getParameter("id");
		if (peerId == null) {
			sendResponse(resp, Util.ERROR, "URL is Missing Id");
			return;
		}
		ChannelService channelService = ChannelServiceFactory
				.getChannelService();
		String token = channelService.createChannel(peerId);

		sendResponse(resp, Util.TOKEN, token);
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
