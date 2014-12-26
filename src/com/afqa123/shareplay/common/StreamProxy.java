package com.afqa123.shareplay.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.StringTokenizer;

import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpRequest;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;

public class StreamProxy implements Runnable {

	private static final int PORT = 4242;
	private static final Logger logger = LoggerFactory.getLogger(StreamProxy.class);
	
	private ServerSocket _socket;
	private int _port;	
	private Thread _thread;
	private boolean _isRunning;
	
	public StreamProxy() {

	}
	
	public int getPort() {
		return _port;
	}
	
	public void init() {
		try {
			_socket = new ServerSocket(PORT, 0, 
					InetAddress.getByAddress(new byte[] {127,0,0,1}));
			_socket.setSoTimeout(5000);
			_port = _socket.getLocalPort();
			
		} catch (Exception ex) {
			logger.error("Error creating stream proxy.", ex);
		}
	}
	
	public void start() {
		_thread = new Thread(this);
		_thread.start();
	}
	
	public void stop() {
		_isRunning = false;
		
 		if (_thread == null) {
 			throw new IllegalStateException("Proxy wasn't started.");
 		}
 		
 		_thread.interrupt();
 		
 		try {
 			_thread.join(5000);
 		} catch (InterruptedException ex) {
 			
 		}

 		try {
	 		_socket.close();
 		} catch (Exception ex) {
 			logger.error("Error closing socket.", ex);
 		}
	}
	
	@Override
	public void run() {
		_isRunning = true;
		
		while (_isRunning) {

			try {
				Socket client = _socket.accept();
				if (client == null) {
					continue;
				}
				
				//Log.d(Constants.LOG_SOURCE, "Client connected.");
				
				HttpRequest request = readRequest(client);
				processRequest(request, client);

			} catch (SocketTimeoutException e) {
				// Do nothing
			} catch (IOException e) {
				logger.error("Error connecting to client.", e);
			}
		}
		
		//Log.d(Constants.LOG_SOURCE, "Proxy interrupted. Shutting down.");
	}
	
	private HttpRequest readRequest(Socket client) {
		HttpRequest request = null;
		InputStream is;
		String firstLine;
		try {
			is = client.getInputStream();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is), 8192);
			firstLine = reader.readLine();
		} catch (IOException e) {
			logger.error("Error parsing request", e);
			return request;
		}

		if (firstLine == null) {
			logger.debug("Proxy client closed connection without a request.");
			return request;
		}
		
		StringTokenizer st = new StringTokenizer(firstLine);
		String method = st.nextToken();
		String uri = st.nextToken();
		String realUri = uri.substring(1);
		
		//Log.d(Constants.LOG_SOURCE, method + " " + realUri);
		
		ProtocolVersion v = new ProtocolVersion("HTTP", 1, 0);
		return new BasicHttpRequest(method, realUri, v);
	}

	private void processRequest(HttpRequest request, Socket client)
			throws IllegalStateException, IOException {
		if (request == null) {
			return;
		}

		final URL url = new URL(request.getRequestLine().getUri());
		HttpURLConnection c = (HttpURLConnection)url.openConnection();
		StringBuilder httpString = new StringBuilder();
		byte[] buffer;

		//Log.d(Constants.LOG_SOURCE, "Reading header");
		
		boolean chunked = false;
		
		// read data from server
		try {
			c.connect();
			
			for (int i = 0; ; i++) {
				String key = c.getHeaderFieldKey(i);
				String val = c.getHeaderField(i);
				
				if (key == null && val == null) 
					break;
								
				if (key != null) {
					// mediaplayer can't handle chunked encoding, so pretend that
					// it isn't
					if (key.equals("transfer-encoding") && val.equals("chunked")) {
						chunked = true;
						continue;
					}
					
					httpString.append(key);
					httpString.append(": ");
				}
				
				httpString.append(val);
				httpString.append("\n");
			}
		
			// chunked content and no total length is bad!
			final int contentLength = c.getContentLength();
			if (chunked && contentLength == -1) {
				logger.warn("Chunked content without content length returned!");
			}
			
			httpString.append("\n");
						
			buffer = httpString.toString().getBytes();
			client.getOutputStream().write(buffer, 0, buffer.length);
			
		} catch (IOException ex) {
			logger.warn("Error processing header.", ex);
		}

		//Log.d(Constants.LOG_SOURCE, "Reading content");
		
		// write data back to client
		InputStream data = null;
		int readBytes = 0;
		byte[] buff = new byte[1024 * 50];
		
		try {
			data = c.getInputStream();
			
			while (_isRunning
					&& (readBytes = data.read(buff, 0, buff.length)) != -1) {
				client.getOutputStream().write(buff, 0, readBytes);
			}

		} catch (Exception ex) {
			// client has gone away...
			//Log.v(Constants.LOG_SOURCE, "Error processing content.", ex);
		} finally {
			//Log.d(Constants.LOG_SOURCE, "All done");

			// stop reading from server
			try {
				if (c != null)
					c.disconnect();

				// data already closed from disconnect
				// if (data != null)
				// data.close();

				if (client != null)
					client.close();

			} catch (Exception ex2) {
				logger.warn("Error cleaning up.", ex2);
			}
		}
	}
}
