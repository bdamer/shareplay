package com.afqa123.shareplay.impl;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.Message;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;
import com.afqa123.shareplay.common.DBHelper;
import com.afqa123.shareplay.interfaces.IServerProvider;

public class ServerProvider implements IServerProvider, ServiceListener {

	private static final Logger logger = LoggerFactory.getLogger(ServerProvider.class);
	private static final String TYPE_DAAP = "_daap._tcp.local.";
	private static final String WIFI_LOCK = "shareplay-lock";
	
	private Context _context;
	private JmDNS _jmdns;
	private List<Server> _foundServers;
	private List<Server> _savedServers;
	private Handler _handler;
	private DBHelper _db;

	public ServerProvider(final Context context, final DBHelper db, final Handler updateHandler) {
		_context = context;
	    _handler = updateHandler;	
	    _db = db;
		_foundServers = new ArrayList<Server>();
		_savedServers = new ArrayList<Server>();		
	    getSavedServers();
	}
		
	@Override
	public Server getServer(final Long id) {	
		if (id == null) {
			return null;
		}
		
		Server result = null;
		final SQLiteDatabase db = _db.getReadableDatabase();
		final Cursor c = db.query(DBHelper.TBL_SERVERS, DBHelper.COLS_SERVER, DBHelper.COL_ID + "=?", new String[] { id.toString() }, null, null, null);
	
		if (c.getCount() == 1) {			
			c.moveToFirst();
			result = Server.createFromCursor(c);
		}
		c.close();
		db.close();
		
		return result;
	}	
	
	// issue #8 again - let's take a look at what is actually in the server table
	public String getServerListDebug() {
		StringBuilder sb = new StringBuilder();

		final SQLiteDatabase db = _db.getReadableDatabase();
		final Cursor c = db.query(DBHelper.TBL_SERVERS, DBHelper.COLS_SERVER, null, null, null, null, null);

		if (c.getCount() > 0) {
			c.moveToFirst();
			do {
				sb.append(c.getLong(c.getColumnIndex(DBHelper.COL_ID)));
				sb.append(",");
				sb.append(c.getString(c.getColumnIndex(DBHelper.COL_NAME)));
				sb.append(",");
				sb.append(c.getString(c.getColumnIndex(DBHelper.COL_ADDRESS)));
				sb.append(",");
				sb.append(c.getString(c.getColumnIndex(DBHelper.COL_HOST)));
				sb.append(",");
				sb.append(c.getInt(c.getColumnIndex(DBHelper.COL_PORT)));
				sb.append(",");
				sb.append(c.getInt(c.getColumnIndex(DBHelper.COL_REVISION)));
				sb.append(",");
				sb.append(c.getString(c.getColumnIndex(DBHelper.COL_DISCOVERED)));
				sb.append(",");
				sb.append(c.getString(c.getColumnIndex(DBHelper.COL_PASSWORD_HASH)));
				sb.append("\n");			
			} while (c.moveToNext());
		} else {
			sb.append("Empty server list.");
		}

		c.close();
		db.close();
		
		return sb.toString();
	}
	
	@Override
	public List<Server> getServers() {
		List<Server> result = new ArrayList<Server>();
		result.addAll(_foundServers);
		result.addAll(_savedServers);
		return result;
	}
	
	private void getSavedServers() {
		_savedServers.clear();

		final SQLiteDatabase db = _db.getReadableDatabase();
		final Cursor c = db.query(DBHelper.TBL_SERVERS, DBHelper.COLS_SERVER, null, null, null, null, DBHelper.COL_NAME);

		while (c.moveToNext()) {
			_savedServers.add(Server.createFromCursor(c));
		}
		
		c.close();
		db.close();
	}
	
	private Server matchServer(final Server server, final List<Server> list) {
		for (Server s : list) {
			if (s.equals(server)) {
				return s;
			}
		}
		return null;
	}
	
	@Override
	public void addServer(final Server server) {
		logger.debug("Adding server: " + server);
		final SQLiteDatabase db = _db.getWritableDatabase();
		final ContentValues values = new ContentValues();
		values.put(DBHelper.COL_NAME, server.getName());
		values.put(DBHelper.COL_HOST, server.getHost());
		values.put(DBHelper.COL_ADDRESS, server.getAddress());
		values.put(DBHelper.COL_PORT, server.getPort());
		values.put(DBHelper.COL_DISCOVERED, DBHelper.Date2DB(server.getLastDiscovered()));
		values.put(DBHelper.COL_PASSWORD_HASH, server.getPasswordHash());

		server.setId(db.insert(DBHelper.TBL_SERVERS, null, values));
		// see if we have to add server to lists
		if (addServerToLists(server)) {
			Message.obtain(_handler).sendToTarget();
		}
		db.close();
	}
	
	@Override
	public void updateServer(final Server server) {
		logger.debug("Updating server: " + server);
		final SQLiteDatabase db = _db.getWritableDatabase();
		final ContentValues values = new ContentValues();
		values.put(DBHelper.COL_NAME, server.getName());
		values.put(DBHelper.COL_HOST, server.getHost());
		values.put(DBHelper.COL_ADDRESS, server.getAddress());
		values.put(DBHelper.COL_PORT, server.getPort());
		values.put(DBHelper.COL_DISCOVERED, DBHelper.Date2DB(server.getLastDiscovered()));
		values.put(DBHelper.COL_PASSWORD_HASH, server.getPasswordHash());		
		db.update(DBHelper.TBL_SERVERS, values, DBHelper.COL_ID + "=?", new String[] { server.getId().toString() });
		db.close();
	}
	
	@Override
	public void deleteServer(final Server server) {
		logger.debug("Deleting server: " + server);
		if (deleteServerFromDb(server) && removeServerFromLists(server)) {
			Message.obtain(_handler).sendToTarget();
		}		
	}
	
	private boolean deleteServerFromDb(final Server server) {
		boolean result = false;
		SQLiteDatabase db = null;
		
		try {
			if (server.getId() != null) {
				final String[] arg = new String[] { server.getId().toString() };
				db = _db.getWritableDatabase();
				int res = db.delete(DBHelper.TBL_SERVERS, DBHelper.COL_ID + "=?", arg);
				if (res != 1) {
					throw new Exception("Could not delete server from database!");
				}
				
				// delete catalog entries for this server
				db.delete(DBHelper.TBL_SONGS, DBHelper.COL_SERVER_ID + "=?", arg);
				db.delete(DBHelper.TBL_ALBUMS, DBHelper.COL_SERVER_ID + "=?", arg);
				db.delete(DBHelper.TBL_ARTISTS, DBHelper.COL_SERVER_ID + "=?", arg);
			}

			result = true;
		
		} catch (Exception ex) {
			logger.error("Error deleting server.", ex);
		} finally {
			if (db != null) {
				db.close();
			}
		}

		return result;
	}
	
	@Override
	public void deleteAll() {
		logger.debug("Deleting all servers.");
		endScan();

		for (Server server : _savedServers) {
			deleteServerFromDb(server);
		}
		for (Server server : _foundServers) {
			deleteServerFromDb(server);
		}		
		_savedServers.clear();
		_foundServers.clear();
		
		beginScan();
	}
	
	private boolean addServerToLists(final Server server) {
		Server oldServer = matchServer(server, _foundServers);
		if (oldServer != null) {
			// server was already found
			return false;
		}
		oldServer = matchServer(server, _savedServers);
		if (oldServer != null) {
			_savedServers.remove(oldServer);
			server.setId(oldServer.getId());
			server.setPasswordHash(oldServer.getPasswordHash());
			updateServer(server);
			_foundServers.add(server);
		} else {
			// we have a new server
			_foundServers.add(server);
		}		
		return true;
	}
	
	private boolean removeServerFromLists(final Server server) {
		Server oldServer = matchServer(server, _foundServers);
		if (oldServer != null) {
			_foundServers.remove(oldServer);
			return true;
		}
		oldServer = matchServer(server, _savedServers);
		if (oldServer != null) {
			_savedServers.remove(oldServer);
			return true;
		}
		return false;
	}
	
	@Override
	public void serviceAdded(ServiceEvent event) {
		final ServiceInfo info = _jmdns.getServiceInfo(event.getType(), event.getName());
		final Server newServer = Server.createFromInfo(info);
		//Log.d(Constants.LOG_SOURCE, "ServerScanner.serviceAdded: " + newServer);
		if (addServerToLists(newServer)) {
			Message.obtain(_handler).sendToTarget();
		}
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		final Server newServer = Server.createFromInfo(event.getInfo());
		//Log.d(Constants.LOG_SOURCE, "ServerScanner.serviceRemoved: " + newServer);
		final Server oldServer = matchServer(newServer, _foundServers);
		if (oldServer != null) {
			_foundServers.remove(oldServer);
			if (oldServer.getId() != null) {
				oldServer.setOnline(false);
				_savedServers.add(oldServer);
			}
			Message m = Message.obtain(_handler);		
			m.sendToTarget();
		}
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
		
	}
	
    private byte[] convertIP(final int i) {
        return new byte[] { (byte)(i & 0xff), (byte)((i >> 8) & 0xff), (byte)((i >> 16) & 0xff), (byte) ((i >> 24) & 0xff) };
    }
	
    @Override
	public void beginScan() {
    	logger.debug("Beginning server scan.");
		MulticastLock lock = null;
		try {
			WifiManager wifiManager = (WifiManager)_context.getSystemService(Context.WIFI_SERVICE);
			byte[] wifiAddress = convertIP(wifiManager.getDhcpInfo().ipAddress);
			InetAddress wifi = InetAddress.getByAddress(wifiAddress);
			lock = wifiManager.createMulticastLock(WIFI_LOCK);
			lock.acquire();
			_jmdns = JmDNS.create(wifi);
			_jmdns.addServiceListener(TYPE_DAAP, this);
		} catch (Exception ex) {
			logger.error("Error during server scan.", ex);
        } finally {
        	if (lock != null) {
        		lock.release();
        	}
        }
	}
	
    @Override
	public void endScan() {
    	logger.debug("Ending server scan.");
		try {
			_jmdns.removeServiceListener(TYPE_DAAP, this);
			_jmdns.close();
		} catch (Exception ex) {
			logger.error("Error ending server scan.", ex);
		}
	}
}
