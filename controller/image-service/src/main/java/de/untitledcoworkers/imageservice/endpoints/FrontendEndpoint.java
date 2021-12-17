package de.untitledcoworkers.imageservice.endpoints;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.Session;

import java.io.IOException;
import java.lang.Throwable;

@ServerEndpoint(value = "")
public class FrontendEndpoint {

	private Session session;
	// private static Set<FrontendEndpoint> frontendEndpoints;

	@OnOpen
	public void onOpen(Session session) throws IOException {
		//
	}

	@OnMessage
	public void onMessage(Session session, String message) throws IOException {
		//
	}

	@OnClose
	public void onClose(Session session) throws IOException {
		//
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
	}
}
