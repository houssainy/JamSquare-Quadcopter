package com.graduation_project.jam_square.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;

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

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String peerId = req.getParameter("id");
		if (peerId == null) {
			send(resp, "<h1>Missing User Id!</h1>");
			return;
		}
		ChannelService channelService = ChannelServiceFactory
				.getChannelService();
		String token = channelService.createChannel(peerId);

		send(resp, token);
	}
	
	private void send(HttpServletResponse resp, String msg) throws IOException {
		resp.setContentType("text/html");
		resp.getWriter().write(msg);
	}
}
