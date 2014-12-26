package com.afqa123.shareplay.common;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.content.Context;

import com.afqa123.shareplay.impl.Client;
import com.afqa123.shareplay.impl.Server;
import com.afqa123.shareplay.interfaces.IClient;

public class CustomFeedback extends Feedback {

	private IClient client;
	private Server server;

	public CustomFeedback(final Context context, final String source, final Exception ex) {
		super(context, source, ex);
	}
	
	public void setServer(final Server server) {
		this.server = server;
	}
	
	public void setClient(final IClient client) {
		this.client = client;
	}

	@Override
	protected JSONObject prepareFeedback() throws Exception {
		JSONObject root = super.prepareFeedback();
		// assemble client data
		JSONObject jsonClient = new JSONObject();
		if (client != null) {
			jsonClient.put("catalog", client.getCatalog() == null ? "null" : "object");
			jsonClient.put("connected", client.isConnected());
			jsonClient.put("sessionId", ((Client)client).getSessionId());

			// assemble connection information
			URLConnection conn = ((Client)client).getConnection();
			JSONObject jsonConn = new JSONObject();
			if (conn != null) {
				jsonConn.put("url", conn.getURL());
				
				// assemble header information
				Map<String,List<String>> headers = conn.getHeaderFields();
				JSONObject jsonHeaders = new JSONObject();
				if (headers != null) {
					for (String key : headers.keySet()) {
						jsonHeaders.put(key, headers.get(key));
					}
				}					
				jsonConn.put("headers", jsonHeaders);
				
				if (conn instanceof HttpURLConnection) {
					HttpURLConnection httpConn = (HttpURLConnection)conn;
					jsonConn.put("request-method", httpConn.getRequestMethod());
					jsonConn.put("response-code", httpConn.getResponseCode());
					jsonConn.put("response-message", httpConn.getResponseMessage());
				}
			}
			jsonClient.put("connection", jsonConn);
		}
		root.put("client", jsonClient);
		
		// assemble server data
		JSONObject jsonServer = new JSONObject();
		if (server != null) {
			jsonServer.put("address", server.getAddress());
			jsonServer.put("databaseCount", server.getDatabaseCount());
			jsonServer.put("host", server.getHost());
			jsonServer.put("lastDiscovered", server.getLastDiscovered());
			jsonServer.put("name", server.getName());
			jsonServer.put("isOnline", server.isOnline());
			jsonServer.put("port", server.getPort());
			jsonServer.put("revision", server.getRevision());
			
			// assemble server flags
			Map<String,Object> flags = server.getFlags();
			JSONObject jsonFlags = new JSONObject();
			if (flags != null) {
				for (String key : flags.keySet()) {
					jsonFlags.put(key, flags.get(key));
				}
			}
			jsonServer.put("flags", jsonFlags);
		}
		root.put("server", jsonServer);

		return root;
	}
}