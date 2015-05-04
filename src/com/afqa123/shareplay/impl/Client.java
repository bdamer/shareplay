package com.afqa123.shareplay.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpException;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;
import com.afqa123.shareplay.common.AuthorizationException;
import com.afqa123.shareplay.common.DAAPException;
import com.afqa123.shareplay.common.DBHelper;
import com.afqa123.shareplay.common.Filename;
import com.afqa123.shareplay.common.StoppableThread;
import com.afqa123.shareplay.data.ContentCode;
import com.afqa123.shareplay.data.Item;
import com.afqa123.shareplay.data.Playlist;
import com.afqa123.shareplay.interfaces.Catalog;
import com.afqa123.shareplay.interfaces.IClient;

public class Client implements IClient {

	private static final Logger logger = LoggerFactory.getLogger(Client.class);
	public static final int DEFAULT_PORT = 3689;
	private static final int CLIENT_AUTHORIZATION = 1;
	private static final int CLIENT_ERROR = 2;
	private static final int CLIENT_CONNECTED = 3;
	private static final int CLIENT_CATALOG_EMPTY = 4;
	private static final int CLIENT_CATALOG_UPDATED = 5;
	private static final int CLIENT_PLAYLISTS_UPDATED = 6;
	private static final int CLIENT_CATALOG_COMPLETE = 7;
	private static final int CLIENT_CATALOG_ERROR = 8;
	private static final int CLIENT_DOWNLOAD_START = 9;
	private static final int CLIENT_DOWNLOAD_COMPLETE = 10;
	private static final int CLIENT_DOWNLOAD_ERROR = 11;
	private static final int DATABASE_ID = 1;
	private static final String DEFAULT_ALBUM = "Unknown album";
	private static final String DEFAULT_ARTIST = "Unknown artist";
	private static final boolean LOAD_SONGS = true;
	private static final boolean LOAD_PLAYLISTS = true;
	private static final int INITIAL_REVISION = 1;
	private static final Set<String> SUPPORTED_FORMATS = 
		new HashSet<String>(Arrays.asList(new String[] { "mp3", "m4a", "wav", "ogg" }));		
	private static final String SONG_PATH = "http://%s:%d/databases/%d/items/%%d.mp3?session-id=%d";
	private static final String PROTOCOL = "http";
	private static final String PARAM_AUTHORIZATION = "Authorization";
	private static final String VALUE_AUTHORIZATION = "Basic %s";
		
	/*	
	 * Available meta fields:
	 * daap.songbitrate,daap.songbeatsperminute,daap.songcomment,daap.songcompilation, 
	 * dmap.persistentid,daap.songcomposer,daap.songdateadded,daap.songdatemodified,
	 * daap.songdisccount,daap.songdiscnumber,daap.songdisabled, daap.songeqpreset,
	 * daap.songformat,daap.songgenre,daap.songdescription,daap.songrelativevolume, 
	 * daap.songsamplerate,daap.songsize,daap.songstarttime,daap.songstoptime,
	 * daap.songtime,daap.songtrackcount,daap.songuserrating,daap.songyear,daap.songdatakind,
	 * daap.songdataurl,com.apple.itunes.norm-volume
	 **/
	private static final String SONG_META_FIELDS = "dmap.itemkind,dmap.itemid,dmap.itemname,daap.songalbum,daap.songartist,daap.songtracknumber,daap.songformat";
	private static final String PLAYLIST_LIST_META_FIELDS = "dmap.itemid,dmap.itemname,dmap.itemcount,daap.baseplaylist";
	private static final String PLAYLIST_META_FELDS = "dmap.itemid";
	private static final int HTTP_OK = 200;
	private static final int BUFFER_SIZE = 1024;
	
	private interface IRequest {
		final static String SERVER_INFO = "server-info";
		final static String CONTENT_CODES = "content-codes";
		final static String LOGIN = "login";
		final static String LOGOUT = "logout?session-id=%d";
		final static String UPDATE = "update?session-id=%d&revision-number=%d";
		final static String DATABASE_LIST = "databases?session-id=%d&revision-id=%d";
		final static String SONG_LIST = "databases/%d/items?type=music&meta=%s&session-id=%d&revision-id=%d";	
		final static String PLAYLIST_LIST = "databases/%d/containers?meta=%s&session-id=%d&revision-id=%d";
		final static String PLAYLIST = "/databases/%d/containers/%d/items?type=music&meta=%s&session-id=%d&revision-id=%d";
	}
	
	@SuppressWarnings("unused")
	private interface IResponse {
		
		/*
		 * Unused: 
		 * 
		asbt	short	daap.songsbeatsperminute
		asbr	short	daap.songbitrate
		ascm	string	daap.songcomment
		asco	byte	daap.songcompilation
		asda	date	daap.songdateadded
		asdm	date	daap.songdatemodified
		asdc	short	daap.songdisccount
		asdn	short	daap.songdiscnumber
		asdb	byte	daap.songdisabled
		aseq	string	daap.songeqpreset
		asgn	string	daap.songgenre
		asdt	string	daap.songdescription
		asrv	byte	daap.songrelativevolume
		assr	int		daap.songsamplerate
		assz	int		daap.songsize
		asst	int		daap.songstarttime 	(in milliseconds)	
		assp	int		daap.songstoptime 	(in milliseconds)
		astm	int		daap.songtime		(in milliseconds)
		astc	short	daap.songtrackcount
		asur	byte	daap.songuserrating
		asyr	short	daap.songyear
		asdk	byte	daap.songdatakind
		asul	string	daap.songdataurl

		prsv	list	daap.resolve
		arif	list	daap.resolveinfo
		
		mcon	list	dmap.container		an arbitrary container
		mcti	int		dmap.containeritemid	the id of an item in its container
		
		msts	string	dmap.statusstring
		
		mudl	list	dmap.deletedidlisting	used in updates?  (document soon)
		
		abro	list	daap.databasebrowse	
		
		abal	list	daap.browsealbumlistung	  
		abar	list	daap.browseartistlisting   
		abcp	list	daap.browsecomposerlisting
		abgn	list	daap.browsegenrelisting
		
		aeNV	int		com.apple.itunes.norm-volume
		*/
		
		final static int ABPL = 0x6162706c;	// daap.baseplaylist
		final static int ADBS = 0x61646273;	// daap.playlistsongs
		final static int AESP = 0x61655350;	// com.apple.itunes.smart-playlist
		final static int APLY = 0x61706c79;	// daap.databaseplaylists response to /databases/id/containers
		final static int APRO = 0x6170726f;	// daap.protocolversion
		final static int APSO = 0x6170736f;	// daap.databaseplaylistssongs response to /databases/id/containers/pid/
		final static int ASAL = 0x6173616c;	// daap.songalbum		the song ones should be self exp.
		final static int ASAR = 0x61736172;	// daap.songartist
		final static int ASDK = 0x6173646b; // daap.songdatakind
		final static int ASFM = 0x6173666d;	// daap.songformat
		final static int ASSA = 0x61737361; // daap.sortartist
		final static int ASSC = 0x61737363; // daap.sortcomposer
		final static int ASSL = 0x6173736c; // daap.sortalbumartist
		final static int ASSN = 0x6173736e;	// daap.sortname
		final static int ASSU = 0x61737375; // daap.???
		final static int ASTN = 0x6173746e;	// daap.songtracknumber
		final static int AVDB = 0x61766462;	// daap.serverdatabases	response to a /databases
		final static int MBCL = 0x6d62636c;	// dmap.bag
		final static int MCCR = 0x6d636372;	// dmap.contentcodesresponse	the response to the content-codes request
		final static int MCNA = 0x6d636e61;	// dmap.contentcodesname the full name of the code
		final static int MCNM = 0x6d636e6d;	// dmap.contentcodesnumber the four letter code
		final static int MCTC = 0x6d637463;	// dmap.???		number of containers
		final static int MCTY = 0x6d637479;	// dmap.contentcodestype the type of the code (see appendix b for type values)
		final static int MDCL = 0x6d64636c;	// dmap.dictionary - a dictionary entry
		final static int MIID = 0x6d696964;	// dmap.itemid - an item's id
		final static int MIKD = 0x6d696b64; // dmap.itemkind - the kind of item.  So far, only '2' has been seen, an audio file?
		final static int MIMC = 0x6d696d63; // dmap.itemcount - number of items in a container
		final static int MINM = 0x6d696e6d;	// dmap.itemname - an items name
		final static int MLCL = 0x6d6c636c;	// dmap.listing		a list
		final static int MLID = 0x6d6c6964;	// dmap.sessionid		the session id for the login session
		final static int MLIT = 0x6d6c6974;	// dmap.listingitem	a single item in said list
		final static int MLOG = 0x6d6c6f67;	// dmap.loginresponse	response to a /login		
		final static int MPCO = 0x6d70636f;	// dmap.parentcontainerid
		final static int MPER = 0x6d706572;	// dmap.persistentid - a persistend id
		final static int MPRO = 0x6d70726f;	// dmap.protocolversion
		final static int MRCO = 0x6d72636f;	// dmap.returnedcount	number of items returned in a request
		final static int MSAL = 0x6d73616c;	// dmap.supportsuatologout
		final static int MSAU = 0x6d736175;	// dmap.authenticationmethod (should be self explanitory)
		final static int MSBR = 0x6d736272;	// dmap.supportsbrowse
		final static int MSDC = 0x6d736463;	// dmap.databasescount
		final static int MSEX = 0x6d736578;	// dmap.supportsextensions
		final static int MSIX = 0x6d736978;	// dmap.supportsindex
		final static int MSLR = 0x6d736c72;	// dmap.loginrequired		
		final static int MSPI = 0x6d737069;	// dmap.supportspersistentids
		final static int MSRS = 0x6d737273;	// dmap.supportsresolve
		final static int MSRV = 0x6d737276;	// dmap.serverinforesponse	response to a /server-info
		final static int MSTM = 0x6d73746d;	// dmap.timeoutinterval
		final static int MSTT = 0x6d737474;	// dmap.status - response status code
		final static int MSUP = 0x6d737570;	// dmap.supportsupdate
		final static int MSQY = 0x6d737179;	// dmap.supportsquery		
		final static int MTCO = 0x6d74636f;	// dmap.specifiedtotalcount number of items in response to a request
		final static int MUPD = 0x6d757064;	// dmap.updateresponse	response to a /update
		final static int MUSR = 0x6d757372;	// dmap.serverrevision	revision to use for requests
		final static int MUTY = 0x6d757479;	// dmap.updatetype
	};

	public interface OnConnectedListener {
		void onConnected();
	};
	
	public interface OnErrorListener {
		void onError(Exception ex);
	};
	
	public interface OnAuthorizationListener {
		void onAuthorizationRequired();
	};

	public interface OnCatalogUpdateListener {
		
		void onCatalogEmpty();
		
		void onCatalogUpdated(int count, int total);
		
		void onPlaylistUpdated(int count, int total, String name);
		
		void onCatalogComplete();
		
		void onCatalogError(Exception ex);
	}
	
	public interface OnDownloadListener {
		
		void onDownloadStart();
		
		void onDownloadComplete();
		
		void onDownloadError(Exception ex);
	}
	
	private class EventHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CLIENT_AUTHORIZATION:
				if (_onAuthorizationListener != null) {
					_onAuthorizationListener.onAuthorizationRequired();
				}
				break;
				
			case CLIENT_ERROR:
				if (_onErrorListener != null) {
					_onErrorListener.onError((Exception)msg.obj);
				}
				break;
				
			case CLIENT_CONNECTED:
				if (_onConnectedListener != null) {
					_onConnectedListener.onConnected();					
				}
				break;
				
			case CLIENT_CATALOG_EMPTY:
				if (_onCatalogUpdateListener != null) {
					_onCatalogUpdateListener.onCatalogEmpty();
				}
				break;				

			case CLIENT_CATALOG_UPDATED:
				if (_onCatalogUpdateListener != null) {
					_onCatalogUpdateListener.onCatalogUpdated(msg.arg1, msg.arg2);
				}
				break;				

			case CLIENT_PLAYLISTS_UPDATED:
				if (_onCatalogUpdateListener != null) {
					_onCatalogUpdateListener.onPlaylistUpdated(msg.arg1, msg.arg2, (String)msg.obj);
				}
				break;
				
			case CLIENT_CATALOG_COMPLETE:
				if (_onCatalogUpdateListener != null) {
					_onCatalogUpdateListener.onCatalogComplete();
				}
				break;				
				
			case CLIENT_CATALOG_ERROR:
				if (_onCatalogUpdateListener != null) {
					_onCatalogUpdateListener.onCatalogError((Exception)msg.obj);
				}
				break;
				
			case CLIENT_DOWNLOAD_START:
				if (_onDownloadListener != null) {
					_onDownloadListener.onDownloadStart();
				}
				break;

			case CLIENT_DOWNLOAD_COMPLETE:
				if (_onDownloadListener != null) {
					_onDownloadListener.onDownloadComplete();
				}
				break;

			case CLIENT_DOWNLOAD_ERROR:
				if (_onDownloadListener != null) {
					_onDownloadListener.onDownloadError((Exception)msg.obj);
				}
				break;

			default:
				super.handleMessage(msg);
				break;
			}
		}
	};
		
	private class UpdateThread extends StoppableThread {
				
		@Override
		public void run() {
			Message m;
			long then = System.currentTimeMillis();
			int count;

			try {
				_handler.obtainMessage(CLIENT_CATALOG_EMPTY).sendToTarget();
				_catalog.clear();
				_catalog.prepare();

				if (!isStopped() && LOAD_SONGS) {
					count = beginSongList(DATABASE_ID);
					logger.info("Download time: " + (System.currentTimeMillis() - then));
	
					for (int i = 0; i < count; i++) {
						if (isStopped()) {
							break;
						}
						
						if (i % 25 == 0) {
							m = _handler.obtainMessage(CLIENT_CATALOG_UPDATED);
							m.arg1 = i;
							m.arg2 = count;
							m.sendToTarget();
						}
						
						if (i > 0 && i % 500 == 0) {
							// Flush to keep transaction size small
							_catalog.flush(true);							
						}
		
						getSong(_catalog);
					}
									
					endSongList();
				}
				
				if (!isStopped() && LOAD_PLAYLISTS) {
					List<Playlist> playlists = getPlaylists(DATABASE_ID, _catalog);

					count = playlists.size();
					for (int i = 0; i < count; i++){
						Playlist pl = playlists.get(i);
						// notify UI that we are updating playlists
						m = _handler.obtainMessage(CLIENT_PLAYLISTS_UPDATED);
						m.arg1 = i;
						m.arg2 = count;
						m.obj = pl.getName();
						m.sendToTarget();
						loadPlaylist(DATABASE_ID, pl.getId(), pl.getServerId(), _catalog);
					}
				}

				// Finalize catalog
				_catalog.commit(true);
				_server.setStale(false);
					
				logger.info("Finished catalog update: " + _catalog.getArtistCount() + " artists, " + 
						_catalog.getAlbumCount() + " albums, " + _catalog.getSongCount() + " songs, " + 
						_catalog.getPlaylistCount() + " playlists.");
				
				// need to send this as the last action, otherwise we will lock up the db
				_handler.obtainMessage(CLIENT_CATALOG_COMPLETE).sendToTarget();
			} catch (Exception ex) {
				logger.error("Error in update thread.", ex);
				if (_catalog != null) {
					_catalog.commit(false);
				}
				_handler.obtainMessage(CLIENT_CATALOG_ERROR, ex).sendToTarget();
			} finally {
				logger.info("Catalog update time: " + (System.currentTimeMillis() - then));			
			}
		}	
	};	
	
	private class DownloadThread extends StoppableThread {
		
		private Long _id;
		
		public DownloadThread(final Long id) {
			_id = id;
		}
		
		@Override
		public void run() {
			if (isStopped())
				return;
			
			try {
				_handler.obtainMessage(CLIENT_DOWNLOAD_START).sendToTarget();

				Item song = _catalog.getSongItem(_id);
				if (song == null)
					return;
								
				final String address = String.format(getPlaybackURL(), song.getId());
				final URL url = new URL(address);
		
		        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		        urlConnection.setRequestMethod("GET");
		        urlConnection.connect();

		        final String filename = String.format("%02d - %s - %s - %s.mp3", song.getTrack(), song.getArtist(), song.getAlbum(), song.getName());
		        File SDCardRoot = Environment.getExternalStorageDirectory();
		        File file = new File(SDCardRoot, Filename.clean(filename));		
		        FileOutputStream fileOutput = new FileOutputStream(file);
		        InputStream inputStream = urlConnection.getInputStream();
		
		        byte[] buffer = new byte[1024];
		        int bufferLength = 0;
		        while ((bufferLength = inputStream.read(buffer)) > 0) {
		        	if (isStopped())
		        		break;
		        	
		        	fileOutput.write(buffer, 0, bufferLength);
		        }
		        fileOutput.close();
		        
				_handler.obtainMessage(CLIENT_DOWNLOAD_COMPLETE).sendToTarget();

			} catch (Exception ex) {
				logger.error("Error downloading file.", ex);
				_handler.obtainMessage(CLIENT_DOWNLOAD_ERROR, ex).sendToTarget();
			}
		}
	}

	private static byte[] _buffer = new byte[BUFFER_SIZE];
	private static int _offset = 0;
	private static int _read = 0;                               

	private long _sessionId;
	private DataInputStream _in;
	private List<ContentCode> _contentCodes;
	private Catalog _catalog;
	private Server _server;
	private StoppableThread _worker;
	private StoppableThread _downloader;
	private boolean _connected;
	private Handler _handler;
	private OnConnectedListener _onConnectedListener;
	private OnErrorListener _onErrorListener;
	private OnAuthorizationListener _onAuthorizationListener;
	private OnCatalogUpdateListener _onCatalogUpdateListener;
	private OnDownloadListener _onDownloadListener;
	private URLConnection _connection;
	
	public void setOnConnectedListener(final OnConnectedListener listener) {
		_onConnectedListener = listener;
	}
	
	public void setOnAuthorizationListener(final OnAuthorizationListener listener) {
		_onAuthorizationListener = listener;
	}
	
	public void setOnErrorListener(final OnErrorListener listener) {
		_onErrorListener = listener;
	}
	
	public void setOnCatalogUpdateListener(final OnCatalogUpdateListener listener) {
		_onCatalogUpdateListener = listener;
	}
	
	public void setOnDowloadListener(final OnDownloadListener listener) {
		_onDownloadListener = listener;
	}
	
	public Client() {
		_handler = new EventHandler();
		_connected = false;
		_contentCodes = new ArrayList<ContentCode>();
	}
	
	@Override 
	public void connect(final DBHelper db, final Server server) {
		_server = server;

		Thread t = new Thread() {
			
			public void run() {
				
				try {
					logger.info("Connecting to: " + _server.getName());
					getServerInfo();
					
					if (_server.requiresAuthorization() && _server.getPasswordHash() == null) {
						throw new AuthorizationException();
					}
					
					login();					
					update();
					_catalog = new DatabaseCatalog(db, _server);

					_connected = true;
					_handler.obtainMessage(CLIENT_CONNECTED).sendToTarget();
					
				} catch (AuthorizationException ex) {
					_catalog = null;
					_connected = false;
					_handler.obtainMessage(CLIENT_AUTHORIZATION).sendToTarget();
				} catch (Exception ex) {
					logger.error("Error connecting client.", ex);
					_catalog = null;
					_connected = false;
					_handler.obtainMessage(CLIENT_ERROR, ex).sendToTarget();
				}
			}
		};	
		
		t.start();
	}
		
	@Override
	public boolean isConnected() {
		return _connected;
	}
	
	@Override
	public Catalog getCatalog() {
		return _catalog;
	}
	
	@Override
	public String getPlaybackURL() {
		return String.format(SONG_PATH, 
				_server.getAddress(), 
				_server.getPort(), 
				DATABASE_ID, 
				_sessionId);
	}	
	
	@Override
	public Server getServer() {
		return _server;
	}
	
	@Override
	public void updateCatalog(final boolean force) {

		if (!_server.isStale() && !force) {
			logger.debug("Catalog is fresh.");
			return;
		}
		
		if (_worker != null && _worker.isAlive()) {
			logger.debug("Worker is still running, not starting another.");
		}
		
		logger.debug("Catalog is stale.");
				
		_worker = new UpdateThread();
		_worker.start();
	}
	
	@Override
	public void cancelUpdate() {
		if (_worker != null && _worker.isAlive()) {
			logger.debug("Interrupting worker thread.");
			_worker.requestStop();
		}
	}
	
	private DataInputStream beginRequest(final String request, final int response) throws HttpException, IOException, DAAPException {
		final URL url = new URL(PROTOCOL, _server.getAddress(), _server.getPort(), request);
		_connection = url.openConnection();

		if (_server.requiresAuthorization()) {
			final String hash = _server.getPasswordHash();
			_connection.setRequestProperty(PARAM_AUTHORIZATION, String.format(VALUE_AUTHORIZATION, hash));
		}
		
		_connection.connect();
		
		InputStream is = _connection.getInputStream();
		DataInputStream in = new DataInputStream(is);
		if (response > 0) {
			matchCode(in, response);
			matchCode(in, IResponse.MSTT);
			int code = in.readInt();
			if (code != HTTP_OK) {
				throw new HttpException("Invalid response code: " + code);
			}
		}
					
		return in;
	}
	
	private void endRequest(final DataInputStream in) throws IOException {
		if (in != null) {
			in.close();
		}
	}
	
	private int matchCode(final DataInputStream in, final int code) throws DAAPException {

		try {
			final int read = in.readInt();
			if (code != read) {
				throw new DAAPException("Error matching code '" + Integer.toHexString(code) + "' - found: ", read);
			}
			return in.readInt();

		} catch (IOException ex) {			
			try {
				// try to read the next four bytes, since they are what we were looking for
				byte[] buffer = new byte[4];
				in.read(buffer);
				String dword = String.format("%02x%02x%02x%02x", buffer[0], buffer[1], buffer[2], buffer[3]);
				throw new DAAPException("Error matching code '" + Integer.toHexString(code) + "' - next DWORD '" + dword + "'. " + ex.getMessage(), ex);
			} catch (IOException ex2) {
				throw new DAAPException("Error matching code '" + Integer.toHexString(code) + "'", ex);
			}
		}			
	}

	private String readString(int size) throws IOException {
		_offset = 0;
		while ( (_read = _in.read(_buffer, _offset, size)) != size ) {
			if (_read == -1) {
				break;
			}
			_offset = _read;
			size -= _read;
		}
		return new String(_buffer, 0, size);
	}
	
	private void getServerInfo() throws Exception {
		Map<String,Object> flags = new HashMap<String,Object>();
		_in = beginRequest(IRequest.SERVER_INFO, IResponse.MSRV);
		
		while (_in.available() > 0) {
			int code = _in.readInt();
			int size = _in.readInt();
			
			switch (code) {
				case IResponse.MPRO:
					flags.put(Server.FLAG_DMAP_VERSION, _in.readInt());
					break;				
				case IResponse.APRO:
					flags.put(Server.FLAG_DAAP_VERSION, _in.readInt());
					break;
				case IResponse.MINM:
					_server.setName(readString(size));
					break;
				case IResponse.MSAU:
					flags.put(Server.FLAG_AUTHENTICATION, _in.readBoolean());
					break;
				case IResponse.MSLR:
					flags.put(Server.FLAG_LOGIN_REQUIRED, _in.readBoolean());
					break;
				case IResponse.MSTM:
					flags.put(Server.FLAG_TIMEOUT_INTERVAL, _in.readInt());
					break;
				case IResponse.MSAL:
					flags.put(Server.FLAG_AUTO_LOGOUT, _in.readBoolean());
					break;
				case IResponse.MSUP:
					flags.put(Server.FLAG_UPDATE, _in.readBoolean());
					break;
				case IResponse.MSPI:
					flags.put(Server.FLAG_PERSISTENT_IDS, _in.readBoolean());
					break;
				case IResponse.MSEX:
					flags.put(Server.FLAG_EXTENSIONS, _in.readBoolean());
					break;
				case IResponse.MSBR:
					flags.put(Server.FLAG_BROWSING, _in.readBoolean());
					break;
				case IResponse.MSQY:
					flags.put(Server.FLAG_QUERIES, _in.readBoolean());
					break;
				case IResponse.MSIX:
					flags.put(Server.FLAG_INDEXING, _in.readBoolean());
					break;
				case IResponse.MSRS:
					flags.put(Server.FLAG_RESOLVE, _in.readBoolean());
					break;
				case IResponse.MSDC:
					_server.setDatabaseCount(_in.readInt());
					break;
				default:
					//Log.d(Constants.LOG_SOURCE, "Unexpected code: " + Integer.toHexString(code));
					_in.skipBytes(size);
					break;
			}			
		}

		endRequest(_in);
		
		_server.setFlags(flags);
	}

	@SuppressWarnings("unused")
	private void getContentCodes() throws Exception {
		ContentCode cc = null;
		
		_in = beginRequest(IRequest.CONTENT_CODES, IResponse.MCCR);
		
		while (_in.available() > 0) {
			int code = _in.readInt();
			int size = _in.readInt();
			
			switch (code) {
				case IResponse.MDCL:
					if (cc != null) {
						_contentCodes.add(cc);
					}
					
					cc = new ContentCode();
					break;
				case IResponse.MCNM:
					cc.setNumber(_in.readInt());
					break;
				case IResponse.MCNA:
					byte[] buf = new byte[size];
					_in.read(buf);			
					cc.setName(new String(buf));
					break;
				case IResponse.MCTY:
					cc.setCode(_in.readShort());
					break;
				default:
					//Log.d(Constants.LOG_SOURCE, "Unexpected code: " + Integer.toHexString(code));
					_in.skipBytes(size);
					break;
			}
		}
			
		if (cc != null) {
			_contentCodes.add(cc);
		}
		
		StringBuilder sb = new StringBuilder();
		for (ContentCode x : _contentCodes) {
			sb.append(x);
			sb.append("\n");
		}
		
		logger.debug(sb.toString());
		
		endRequest(_in);
	}

	private void login() throws Exception {
		_in = beginRequest(IRequest.LOGIN, IResponse.MLOG);
		matchCode(_in, IResponse.MLID);
		
		// session id is an unsigend int
		int id = _in.readInt();
		_sessionId = id & 0xffffffffL;
		
		endRequest(_in);
	}
	
	@SuppressWarnings("unused")
	private void logout() throws Exception {
		_in = beginRequest(String.format(IRequest.LOGOUT, _sessionId), 0);
		endRequest(_in);
	}
	
	private void update() throws Exception {		
		_in = beginRequest(String.format(IRequest.UPDATE, _sessionId, INITIAL_REVISION), IResponse.MUPD);
		matchCode(_in, IResponse.MUSR);
		_server.setRevision(_in.readInt());
		endRequest(_in);
	}
	
	@SuppressWarnings("unused")
	private void getDatabaseList() throws Exception {

		_in = beginRequest(String.format(IRequest.DATABASE_LIST, _sessionId, _server.getRevision()),
				  IResponse.AVDB);
		
		//  update type - always 0 for now
		matchCode(_in, IResponse.MUTY);
		_in.readByte();
		
		//  total number of matching records
		matchCode(_in, IResponse.MTCO);
		int totalCount = _in.readInt();
		
		//  total number of records returned
		matchCode(_in, IResponse.MRCO);
		int returnedCount = _in.readInt();
		
		//  listing of records
		matchCode(_in, IResponse.MLCL);
		
		for (int i = 0; i < returnedCount; i++) {

			// single record
			matchCode(_in, IResponse.MLIT);
			
			// database id (<dbid> in subsequent requests)
			matchCode(_in, IResponse.MIID);
			int dbid = _in.readInt();
			
			// database persistent id
			matchCode(_in, IResponse.MPER);
			long dperid = _in.readLong();

			// database name
			int size = matchCode(_in, IResponse.MINM);
			String dbname = readString(size);
			
			// number of items (songs) in the database
			matchCode(_in, IResponse.MIMC);
			int countSongs = _in.readInt();
			
			// number of containers (playlists) in the database
			matchCode(_in, IResponse.MCTC);
			int countContainers = _in.readInt();
		}
		
		endRequest(_in);
	}
		
	private int beginSongList(final int database) throws Exception {		
		logger.debug("Getting song list from server.");

		int returnedCount = 0;
		
		_in = beginRequest(String.format(IRequest.SONG_LIST, database, SONG_META_FIELDS, _sessionId, _server.getRevision()),
				IResponse.ADBS);

		matchCode(_in, IResponse.MUTY);
		@SuppressWarnings("unused")
		byte updateType = _in.readByte();

		matchCode(_in, IResponse.MTCO);
		@SuppressWarnings("unused")
		int totalCount = _in.readInt();
			
		matchCode(_in, IResponse.MRCO);
		returnedCount = _in.readInt();
		
		logger.debug(String.format("Server returned %d of %d total songs.", returnedCount, totalCount));
			
		matchCode(_in, IResponse.MLCL);			
		
		// gobble up the first MLIT
		if (returnedCount > 0) {
			matchCode(_in, IResponse.MLIT);
		}
		
		return returnedCount;
	}
	
	private void getSong(final Catalog catalog) throws Exception {
		
		int size, id = 0, track = 0;
		String name = null, 
			album = null, 
			artist = null,
			format = null;

		boolean done = false;
		while (!done && _in.available() > 0) {
			int code = _in.readInt();
			size = _in.readInt();

			switch (code) {
				case IResponse.MLIT:
					done = true;
					break;
				case IResponse.MIID:
					id = _in.readInt();
					break;				
				case IResponse.MIKD: // item kind - currently not in use
					_in.skipBytes(size);
					break;
			    case IResponse.MINM:
					name = readString(size);
					break;
				case IResponse.ASAL:
					album = readString(size);
					break;
				case IResponse.ASAR:
					artist = readString(size);
					break;
				case IResponse.ASTN:
					track = _in.readShort();
					break;
				case IResponse.ASFM:
					format = readString(size);
					if (format != null) {
						if (format.length() > 3) {
							format = format.substring(0, 3);
						}
						
						format = format.toLowerCase();
					}
					break;
				default:
					logger.warn(String.format("Unexpected code: %s", Integer.toHexString(code)));
					_in.skipBytes(size);
					break;
			}
		}
		
		if (artist == null || artist.trim().length() == 0) {
			artist = DEFAULT_ARTIST;
		}
		
		if (album == null || album.trim().length() == 0) {
			album = DEFAULT_ALBUM;
		}
		
		if (SUPPORTED_FORMATS.contains(format)) {
			//logger.debug(String.format("Adding song (%d): %s - %s - %s", id, artist, album, name));
			catalog.addSong(name, track, id, album, artist);
		} else {
			logger.warn(String.format("Unsupported format '%s' - not adding song.", format));
		}
	}
	
	private void endSongList() throws Exception {
		endRequest(_in);
	}
	
	private List<Playlist> getPlaylists(final int database, final Catalog catalog) throws Exception {
		logger.debug("Getting playlists from server.");
		List<Playlist> result = new ArrayList<Playlist>();
		
		_in = beginRequest(String.format(IRequest.PLAYLIST_LIST, database, PLAYLIST_LIST_META_FIELDS, _sessionId, _server.getRevision()),
				  IResponse.APLY);
		
		// update type
		matchCode(_in, IResponse.MUTY);
		_in.readByte();
		// total count
		matchCode(_in, IResponse.MTCO);
		_in.readInt();
		// returned count
		matchCode(_in, IResponse.MRCO);
		_in.readInt();
		// begin list
		matchCode(_in, IResponse.MLCL);			
		// gobble up the first MLIT
		matchCode(_in, IResponse.MLIT);

		// It appears that the counts returned from rhythmbox are 
		// always 0 - just read until there is no more data...
		while (_in.available() > 0) {
			int size;
			Playlist pl = new Playlist();
			
			boolean done = false;
			while (!done && _in.available() > 0) {
				
				int code = _in.readInt();
				size = _in.readInt();
		
				switch (code) {
					case IResponse.MLIT:
						done = true;
						break;
		
					case IResponse.MIID:
						pl.setServerId((long)_in.readInt());
						break;
						
				    case IResponse.MINM:
						pl.setName(readString(size));
						break;
						
				    case IResponse.MIMC:
				    	pl.setCount(_in.readInt());
				    	break;
						
				    case IResponse.ABPL:
				    	pl.setBaselist(_in.readByte() == 1);
				    	break;
				    	
					default:
						//Log.d(Constants.LOG_SOURCE, "Unexpected code: " + Integer.toHexString(code));
						_in.skipBytes(size);
						break;
				}
			}
			
			pl.setId(catalog.addPlaylist(pl.getName(), pl.getServerId(), pl.isBaselist(), pl.getCount()));
			
			if (!pl.isBaselist()) {
				result.add(pl);
			}
		}
		
		endRequest(_in);
	
		return result;
	}
	
	/**
	 * Loads playlist entries.
	 * 
	 * @param database DAAP Database Id
	 * @param playlistId Local playlist id
	 * @param playlist DAAP playlist id
	 * @param catalog Catalog
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private void loadPlaylist(final int database, final long playlistId, final long playlist, final Catalog catalog) throws Exception {
		//Log.d(Constants.LOG_SOURCE, "Getting playlist: " + playlist);
		
		try {
			_in = beginRequest(String.format(IRequest.PLAYLIST, database, playlist, 
					PLAYLIST_META_FELDS, _sessionId, _server.getRevision()), IResponse.APSO);
		} catch (IOException e) {
			//Log.w(Constants.LOG_SOURCE, "Error getting playlist.", e);
			return; // this has been seen to occur with certain playlists from mt-daapd
		}
		
		matchCode(_in, IResponse.MUTY);
		byte updateType = _in.readByte();

		int code, size, totalCount, returnedCount;

		try {
			// Total count
			matchCode(_in, IResponse.MTCO);
			totalCount = _in.readInt();
			
			// Returned count
			matchCode(_in, IResponse.MRCO);
			returnedCount = _in.readInt();

			//Log.d(Constants.LOG_SOURCE, "Total: " + totalCount + ", returned: " + returnedCount);
			
			// begin list
			matchCode(_in, IResponse.MLCL);

		} catch (DAAPException ex) {
			// Protocol defines that we should get MTCO and MRCO at this point,
			// however, rhythmbox doesn't provide that.
			if (ex.getLastCode() == IResponse.MLCL) {
				_in.readInt();
			} else {
				throw ex;
			}
		}
		
		boolean inItem = false;
		int id = 0;

		while (_in.available() > 0) {
			code = _in.readInt();
			size = _in.readInt();

			switch (code) {
				case IResponse.MLIT:
					if (inItem)
						catalog.addPlaylistEntry(playlistId, id);
					else
						inItem = true;
					break;
	
				case IResponse.MIID:
					id = _in.readInt();
					break;
										
				default:
					//Log.d(Constants.LOG_SOURCE, "Unexpected code: " + Integer.toHexString(code));
					_in.skipBytes(size);
					break;
			}
		}
		
		if (inItem) {
			catalog.addPlaylistEntry(playlistId, id);			
		}
		
		endRequest(_in);
	}
	
	@Override
	public void downloadSong(final long id) {
		if (_downloader != null && _downloader.isAlive()) {
			logger.debug("Download is still running, not starting another.");
		}
						
		_downloader = new DownloadThread(id);
		_downloader.start();
	}
	
	@Override
	public void cancelDownload() {
		if (_downloader != null && _downloader.isAlive()) {
			logger.debug("Interrupting download thread.");
			_downloader.requestStop();
		}
	}
	
	public long getSessionId() {
		return _sessionId;
	}
	
	public URLConnection getConnection() {
		return _connection;
	}
}
