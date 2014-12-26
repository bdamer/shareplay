package com.afqa123.shareplay.impl;

import java.net.Inet4Address;
import java.util.Date;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import android.database.Cursor;

import com.afqa123.shareplay.common.Base64;
import com.afqa123.shareplay.common.DBHelper;

public class Server {

	public static final String FLAG_DMAP_VERSION = "DMAP Version";
	public static final String FLAG_DAAP_VERSION = "DAAP Version";
	public static final String FLAG_AUTHENTICATION = "Authentication";
	public static final String FLAG_LOGIN_REQUIRED = "Login Required";
	public static final String FLAG_TIMEOUT_INTERVAL = "Timeout Interval";
	public static final String FLAG_AUTO_LOGOUT = "Auto Logout";
	public static final String FLAG_UPDATE = "Update";
	public static final String FLAG_PERSISTENT_IDS = "Persistent IDs";
	public static final String FLAG_EXTENSIONS = "Extensions";
	public static final String FLAG_BROWSING = "Browsing";
	public static final String FLAG_QUERIES = "Queries";
	public static final String FLAG_INDEXING = "Indexing";
	public static final String FLAG_RESOLVE = "Resolve";
	
	private Long _id;
	private String _name;
	private String _address;
	private String _host;
	private int _port;
	private int _revision;
	private boolean _online;
	private Date _lastDiscovered;
	private int _databaseCount;
	private boolean _stale;
	private String _passwordHash;
	private Map<String,Object> _flags;
	
	public Server() {

	}
	
	public static Server createFromCursor(final Cursor c) {
		Server result = new Server();
		
		result._id = c.getLong(c.getColumnIndex(DBHelper.COL_ID));
		result._name = c.getString(c.getColumnIndex(DBHelper.COL_NAME));
		result._address = c.getString(c.getColumnIndex(DBHelper.COL_ADDRESS));
		result._host = c.getString(c.getColumnIndex(DBHelper.COL_HOST));
		result._port = c.getInt(c.getColumnIndex(DBHelper.COL_PORT));
		result._revision = c.getInt(c.getColumnIndex(DBHelper.COL_REVISION));
		result._lastDiscovered = DBHelper.DB2Date(c.getString(c.getColumnIndex(DBHelper.COL_DISCOVERED)));
		result._passwordHash = c.getString(c.getColumnIndex(DBHelper.COL_PASSWORD_HASH));
		
		return result;
	}
	
	public static Server createFromInfo(final ServiceInfo info) {
		Server result = new Server();
		
		result._name = info.getName();
		result._host = info.getServer();
		
		Inet4Address a[] = info.getInet4Addresses();
		result._address = a[0].toString();
		// clean up address
		if (result._address.startsWith("/")) {
			result._address = result._address.substring(1);
		}
		result._port = info.getPort();
		result._lastDiscovered = new Date();
		result._online = true;
			
		return result;
	}
	
	public Long getId() {
		return _id;
	}
	
	public void setId(final Long id) {
		_id = id;
	}
	
	public String getName() {
		return _name;
	}

	public void setName(final String name) {
		_name = name;
	}

	public String getAddress() {
		return _address;
	}
	
	public void setAddress(final String address) {
		_address = address;
	}
	
	public String getHost() {
		return _host;
	}

	public void setHost(final String host) {
		_host = host;
	}

	public int getPort() {
		return _port;
	}

	public void setPort(final int port) {
		_port = port;
	}

	public int getRevision() {
		return _revision;
	}
	
	public void setRevision(final int revision) {
		_stale = (revision > _revision);
		this._revision = revision;
	}

	public boolean isOnline() {
		return _online;
	}
	
	public void setOnline(final boolean online) {
		_online = online;		
	}
	
	public Date getLastDiscovered() {
		return _lastDiscovered;
	}
	
	public void setLastDiscovered(final Date lastDiscovered) {
		_lastDiscovered = lastDiscovered;
	}
	
	public Map<String, Object> getFlags() {
		return _flags;
	}

	public void setFlags(Map<String, Object> flags) {
		_flags = flags;
	}

	public int getDatabaseCount() {
		return _databaseCount;
	}

	public void setDatabaseCount(final int databaseCount) {
		_databaseCount = databaseCount;
	}

	public boolean isStale() {
		return _stale;
	}
	
	public void setStale(boolean stale) {
		_stale = stale;
	}
	
	@Override
	public String toString() {
		return "http://" + _host + ":" + _port;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Server))
			return false;

		if (o == this)
			return true;

		Server s = (Server)o;
		if (!_name.equals(s.getName()) ||
			!_host.equals(s.getHost())) 
			return false;
		
		return true;
	}
	
	public boolean requiresAuthorization() {
		if (_flags != null && _flags.containsKey(FLAG_AUTHENTICATION)) {
			return (Boolean)_flags.get(FLAG_AUTHENTICATION);
		}
		
		return false;
	}
	
	public String getPasswordHash() {
		return _passwordHash;
	}
	
	public void setPasswordHash(final String passwordHash) {
		_passwordHash = passwordHash;
	}

	public void setPassword(final String password) {
		if (password == null || password.trim().length() == 0) {
			_passwordHash = null;
		} else {
			// This is actually username:password, but DAAP ignores the username
			final String auth = ":" + password;
			_passwordHash = Base64.encodeBytes(auth.getBytes());
		}
	}
}