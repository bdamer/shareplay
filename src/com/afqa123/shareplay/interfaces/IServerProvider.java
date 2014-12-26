package com.afqa123.shareplay.interfaces;

import java.util.List;

import com.afqa123.shareplay.impl.Server;


public interface IServerProvider {

	Server getServer(final Long id);
	
	List<Server> getServers();
	
	void addServer(final Server server);
	
	void updateServer(final Server server);
	
	void deleteServer(final Server server);
	
	void deleteAll();
	
	void beginScan();
	
	void endScan();
}
