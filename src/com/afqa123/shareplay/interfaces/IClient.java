package com.afqa123.shareplay.interfaces;

import com.afqa123.shareplay.common.DBHelper;
import com.afqa123.shareplay.impl.Server;

public interface IClient {
	
	/**
	 * Connects this client to server.
	 */
	void connect(final DBHelper db, final Server server);
	
	/**
	 * Provides connection state.
	 * 
	 * @return True if connected, otherwise false
	 */
	boolean isConnected();
	
	/**
	 * Returns catalog for this provider.
	 * 
	 * @return Catalog
	 */
	Catalog getCatalog();
	
	/**
	 * Returns playback URL.
	 * 
	 * @return String
	 */
	String getPlaybackURL();
	
	/**
	 * Returns server
	 * 
	 * @return Server
	 */
	Server getServer();
	
	/**
	 * Refreshes the catalog if stale.
	 * 
	 * @param callback
	 */
	void updateCatalog(final boolean force);
	
	/**
	 * Cancels update if in progress.
	 */
	void cancelUpdate();
	
	/**
	 * Downloads a song to local directory.
	 * 
	 * @param id Song database id
	 */
	void downloadSong(final long id);
	
	/**
	 * Cancels download in progress.
	 */
	void cancelDownload();
}
