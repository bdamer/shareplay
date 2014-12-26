package com.afqa123.shareplay;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;
import com.afqa123.shareplay.common.Constants;
import com.afqa123.shareplay.common.CustomFeedback;
import com.afqa123.shareplay.common.DBHelper;
import com.afqa123.shareplay.common.ListWrapper;
import com.afqa123.shareplay.impl.Client;
import com.afqa123.shareplay.impl.Client.OnAuthorizationListener;
import com.afqa123.shareplay.impl.Client.OnCatalogUpdateListener;
import com.afqa123.shareplay.impl.Client.OnConnectedListener;
import com.afqa123.shareplay.impl.Client.OnDownloadListener;
import com.afqa123.shareplay.impl.Client.OnErrorListener;
import com.afqa123.shareplay.impl.Server;
import com.afqa123.shareplay.impl.ServerProvider;
import com.afqa123.shareplay.interfaces.IClient;
import com.afqa123.shareplay.interfaces.IServerProvider;
import com.afqa123.shareplay.interfaces.MediaPlayerInterface;

public class SelectionActivity extends TabActivity {
	
	public static final String PARAM_SERVER_ID = "server_id";
	private static final String PARAM_ARTIST_ID = "artist_id";
	private static final String PARAM_ALBUM_ID = "album_id";
	private static final String PARAM_PLAYLIST_ID = "playlist_id";
	private static final String PARAM_IDX_ARTIST = "idx_artist";
	private static final String PARAM_IDX_ALBUM = "idx_album";
	private static final String PARAM_IDX_PLAYLIST = "idx_playlist";
	private static final String PARAM_IDX_SONG = "idx_song";	
	private static final String PARAM_POS_ARTIST = "pos_artist";
	private static final String PARAM_POS_ALBUM = "pos_album";
	private static final String PARAM_POS_PLAYLIST = "pos_playlist";
	private static final String PARAM_POS_SONG = "pos_song";
	private static final String PARAM_FILTER_ARTIST = "filter_artist";
	private static final String PARAM_FILTER_ALBUM = "filter_album";
	private static final String PARAM_FILTER_PLAYLIST = "filter_playlist";
	private static final String PARAM_FILTER_SONG = "filter_song";
	private static final Logger logger = LoggerFactory.getLogger(SelectionActivity.class);
	
	private Long serverId;
	private Long artistId;
	private Long albumId;
	private Long songId;
	private Long playlistId;
	private ListWrapper artistList;
	private ListWrapper albumList;
	private ListWrapper playlistList;
	private ListWrapper songList;
	private IClient client;
	private TabHost tabHost;
	private ImageView imagePause;
	private ImageView imagePlay;
	private TextView textArtist;
	private TextView textSong;
	private MediaPlayerInterface player;
	private IServerProvider provider;
	private DBHelper db;
	private boolean restoreState;
	private CustomFeedback feedback;
	
	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			try {
				player = MediaPlayerInterface.Stub.asInterface(service);
				if (player == null) {
					throw new Exception("Error getting MediaPlayerInterface.");
				}
				player.setServer(serverId, client.getPlaybackURL());
				player.update();
			} catch (Exception ex) {
				logger.warn("Error setting playback URL.", ex);
				showDialogSafe(DialogFactory.DIALOG_ERROR);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			player = null;
		}
	};

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent i) {
			String song = null;
			String artist = null;
			Bundle extras = i.getExtras();
			final String msg = extras.getString(MediaPlayerService.PARAM_MSG);
			if (MediaPlayerService.MSG_PLAY.equals(msg)) {
				song = extras.getString(MediaPlayerService.PARAM_SONG);
				artist = extras.getString(MediaPlayerService.PARAM_ARTIST);
				imagePause.setVisibility(ImageView.VISIBLE);
				imagePlay.setVisibility(ImageView.GONE);
			} else if (MediaPlayerService.MSG_PAUSE.equals(msg)) {
				imagePlay.setVisibility(ImageView.VISIBLE);
				imagePause.setVisibility(ImageView.GONE);
			}
			textSong.setText(song);
			textArtist.setText(artist);
		}
	};
	
	private final OnTabChangeListener onTabChange = new OnTabChangeListener() {
		@Override
		public void onTabChanged(String tabId) {
			if (getString(R.string.tab_artists).equals(tabId)) {
				artistId = null;
				albumId = null;					
				playlistId = null;
			} else if (getString(R.string.tab_albums).equals(tabId)) {
				albumList.clearFilters();
				albumId = null;		
				playlistId = null;
			} else if (getString(R.string.tab_playlists).equals(tabId)) {
				playlistList.clearFilters();
				playlistId = null;
			} else if (getString(R.string.tab_songs).equals(tabId)) {
				songList.clearFilters();
			}			
			refreshList();
		}
    };
	
    private final OnItemClickListener onListArtistsClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
			artistId = id;
			albumList.setStale(true);
			tabHost.setCurrentTab(1);
		}
	};
		
	private final OnItemClickListener onListAlbumsClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
			albumId = id;
			songList.setStale(true);
			tabHost.setCurrentTab(3);
		}
	};
	
	private final OnItemClickListener onListPlaylistsClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
			artistId = null;
			albumId = null;
			artistList.clearFilters();
			albumList.clearFilters();
			songList.clearFilters();
			// first entry is always "all songs", so skip playlist in
			// that case
			if (pos == 0) {
				playlistId = null;
			} else {
				playlistId = id;
			}
			songList.setStale(true);
			tabHost.setCurrentTab(3);							
		}
	};

	private final OnItemClickListener _onListSongsClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
			try {		
				final String constraint = DBHelper.prepareFilter(songList.getFilter());
				player.load(client.getCatalog().getSongItems(artistId, albumId, playlistId, constraint));
				player.select(id);
			} catch (RemoteException ex) {
				logger.warn("Error playing starting playback.", ex);
			}
		}
	};

	private final OnItemLongClickListener _onListSongsLongClick = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
			songId = id;
			showDialog(DialogFactory.DIALOG_ITEM_ACTIONS);
			return true;
		}
	};

	private final OnClickListener _onImagePauseClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (player == null)
				return;
			try {
				player.pause();
				imagePlay.setVisibility(ImageView.VISIBLE);
				imagePause.setVisibility(ImageView.GONE);
			} catch (RemoteException e) {

			}
		}
	};
	
	private final OnClickListener _onImagePlayClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (player == null)
				return;
			try {
				player.play();
				imagePause.setVisibility(ImageView.VISIBLE);
				imagePlay.setVisibility(ImageView.GONE);					
			} catch (RemoteException e) {

			}
		}
	};
		
	private void createTab(int tabId, int contentId) {
		String tabText = getString(tabId);
		View view = LayoutInflater.from(this).inflate(R.layout.tab_indicator, null);
		TextView tv = (TextView)view.findViewById(R.id.tab_text);
		tv.setText(tabId);
	    tabHost.addTab(tabHost.newTabSpec(tabText)
	    		.setIndicator(view)
	    		.setContent(contentId));
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		logger.info("Creating activity.");
		setContentView(R.layout.activity_selection);
		
		tabHost = getTabHost();
	    createTab(R.string.tab_artists, R.id.tab_artists);
		createTab(R.string.tab_albums, R.id.tab_albums);
		createTab(R.string.tab_playlists, R.id.tab_playlists);
		createTab(R.string.tab_songs, R.id.tab_songs);
	    tabHost.setOnTabChangedListener(onTabChange);    
	    
	    artistList = new ListWrapper(findViewById(R.id.list_artists), onListArtistsClick, null);
	    albumList = new ListWrapper(findViewById(R.id.list_albums), onListAlbumsClick, null);
	    playlistList = new ListWrapper(findViewById(R.id.list_playlists), onListPlaylistsClick, null);
	    songList = new ListWrapper(findViewById(R.id.list_songs), _onListSongsClick, _onListSongsLongClick);

        textSong = (TextView)findViewById(R.id.text_song);
		textArtist = (TextView)findViewById(R.id.text_artist);

		imagePause = (ImageView)findViewById(R.id.image_pause);
		imagePause.setOnClickListener(_onImagePauseClick);
		imagePlay = (ImageView)findViewById(R.id.image_play);
		imagePlay.setOnClickListener(_onImagePlayClick);

		ImageView imagePrevious = (ImageView)findViewById(R.id.image_previous);
		imagePrevious.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (player == null)
					return;
				try {
					player.rewind();
				} catch (RemoteException e) {
				}
			}
		});
		ImageView imageNext = (ImageView)findViewById(R.id.image_next);
		imageNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (player == null)
					return;
				try {
					player.forward();
				} catch (RemoteException e) {
				}
			}
		});
		ImageButton imageSearch = (ImageButton)findViewById(R.id.image_search);
		imageSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onSearchRequested();
			}			
		});
		
       	db = new DBHelper(this);
        provider = new ServerProvider(this, db, null);

        // set client to null, so it isn't used until we reconnect to the server
        client = null;
        
   		final Bundle extras = getIntent().getExtras();
		serverId = extras.getLong(PARAM_SERVER_ID);
		
   		if (savedInstanceState != null) {
    		if (savedInstanceState.containsKey(PARAM_ARTIST_ID)) {
    			artistId = savedInstanceState.getLong(PARAM_ARTIST_ID);
    		}
    		if (savedInstanceState.containsKey(PARAM_ALBUM_ID)) {
    			albumId = savedInstanceState.getLong(PARAM_ALBUM_ID);
    		}
    		if (savedInstanceState.containsKey(PARAM_PLAYLIST_ID)) {
    			playlistId = savedInstanceState.getLong(PARAM_PLAYLIST_ID);
    		}
    		// update list position after orientation change
    		restoreState = true;
      		artistList.setFilter(savedInstanceState.containsKey(PARAM_FILTER_ARTIST) ? savedInstanceState.getString(PARAM_FILTER_ARTIST) : null);
      		albumList.setFilter(savedInstanceState.containsKey(PARAM_FILTER_ALBUM) ? savedInstanceState.getString(PARAM_FILTER_ALBUM) : null);
      		playlistList.setFilter(savedInstanceState.containsKey(PARAM_FILTER_PLAYLIST) ? savedInstanceState.getString(PARAM_FILTER_PLAYLIST) : null);
      		songList.setFilter(savedInstanceState.containsKey(PARAM_FILTER_SONG) ? savedInstanceState.getString(PARAM_FILTER_SONG) : null);
      		artistList.setPosition(savedInstanceState.getInt(PARAM_IDX_ARTIST), savedInstanceState.getInt(PARAM_POS_ARTIST));
      		albumList.setPosition(savedInstanceState.getInt(PARAM_IDX_ALBUM), savedInstanceState.getInt(PARAM_POS_ALBUM));
      		playlistList.setPosition(savedInstanceState.getInt(PARAM_IDX_PLAYLIST), savedInstanceState.getInt(PARAM_IDX_PLAYLIST));
      		songList.setPosition(savedInstanceState.getInt(PARAM_IDX_SONG), savedInstanceState.getInt(PARAM_POS_SONG));
		}   	
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		logger.info("Starting activity.");

		if (client != null) {
			return;
		}
		
		final ProgressDialog connectDialog = 
			ProgressDialog.show(this, null, getString(R.string.dialog_connecting), true, true, new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					// if the user cancels out of the connection dialog,
					// end activity
					finish();
				}
			});
		
		Client c = new Client();
		c.setOnAuthorizationListener(new OnAuthorizationListener() {
			@Override
			public void onAuthorizationRequired() {
				hideDialogSafe(connectDialog);
				showDialogSafe(DialogFactory.DIALOG_PASSWORD);
			}
		});
		c.setOnConnectedListener(new OnConnectedListener() {
			@Override
			public void onConnected() {
				hideDialogSafe(connectDialog);
				
                // make sure there are any artists at all
                if (client.getCatalog().getArtistCount() == 0) {
                	client.updateCatalog(true);
                } else {
                	refreshList();        	
                } 

                // always start service, in case it was killed
               	startService(new Intent(SelectionActivity.this, MediaPlayerService.class));

    			// Bind to service
    			bindService(new Intent(SelectionActivity.this, MediaPlayerService.class),
    					connection, Context.BIND_AUTO_CREATE);
    	
    			// Register update receiver
    	        registerReceiver(receiver, new IntentFilter(MediaPlayerService.STATUS_UPDATE));	
			}
		});
		c.setOnErrorListener(new OnErrorListener() {
			@Override
			public void onError(Exception ex) {
				logger.error("Error starting activity.", ex);
				hideDialogSafe(connectDialog);
				showErrorDialog("SelectionActivity.onStart.Client.OnErrorListener", ex);
			}
		});		
		c.setOnCatalogUpdateListener(new OnCatalogUpdateListener() {

			private int _requestedOrientation;

			private ProgressDialog _pd;
			
			@Override
			public void onCatalogEmpty() {
				_requestedOrientation = SelectionActivity.this.getRequestedOrientation();
				SelectionActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				
				_pd = new ProgressDialog(SelectionActivity.this);
		    	_pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		    	_pd.setTitle(getString(R.string.dialog_catalog_update));
		    	_pd.setIndeterminate(false);
		    	_pd.setCancelable(true);
		    	_pd.setMessage(getString(R.string.label_requesting_data));
		    	_pd.setProgress(0);
		    	_pd.setMax(1);
		    	_pd.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						client.cancelUpdate();
					}
		    	});
		    	
		    	if (!isFinishing())
		    		_pd.show();
			}

			@Override
			public void onCatalogUpdated(int count, int total) {
				_pd.setProgress(count);
				if (count == 0) {
			    	_pd.setMax(total);
			    	_pd.setMessage(getString(R.string.label_adding_songs));					
				}
			}
			
			@Override
			public void onPlaylistUpdated(int count, int total, String name) {
				if (count == 0) {
					_pd.setMax(total);
				}
				
				_pd.setProgress(count + 1);
				_pd.setMessage(getString(R.string.label_adding_playlist) + name);
			}

			@Override
			public void onCatalogComplete() {
				// get rid of cursor, so it'll be refreshed after
				// the catalog update
				artistList.releaseCursor();
				
				int count = client.getCatalog().getArtistCount();
				if (count > 0) {
					if (tabHost.getCurrentTab() != 0) {
						tabHost.setCurrentTab(0);
					} else {
						refreshList();
					}
					
					SelectionActivity.this.setRequestedOrientation(_requestedOrientation);
					hideDialogSafe(_pd);
				} else {
					tabHost.setCurrentTab(0);
					SelectionActivity.this.setRequestedOrientation(_requestedOrientation);
					hideDialogSafe(_pd);
					showDialogSafe(DialogFactory.DIALOG_ERROR);
				}
			}
			
			@Override
			public void onCatalogError(Exception ex) {
				tabHost.setCurrentTab(0);
				SelectionActivity.this.setRequestedOrientation(_requestedOrientation);
				hideDialogSafe(_pd);
				showErrorDialog("SelectionActivity.onStart.Client.onCatalogError", ex);
			}
		});
		c.setOnDowloadListener(new OnDownloadListener() {

			private int _requestedOrientation;

			private ProgressDialog _pd;

			@Override
			public void onDownloadStart() {
				_requestedOrientation = SelectionActivity.this.getRequestedOrientation();
				SelectionActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				
				_pd = new ProgressDialog(SelectionActivity.this);
		    	_pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    	_pd.setIndeterminate(true);
		    	_pd.setCancelable(true);
		    	_pd.setMessage(getString(R.string.label_download_song));
		    	_pd.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						client.cancelDownload();
					}
		    	});
		    	
		    	if (!isFinishing())
		    		_pd.show();
			}

			@Override
			public void onDownloadComplete() {
				SelectionActivity.this.setRequestedOrientation(_requestedOrientation);
				hideDialogSafe(_pd);
			}

			@Override
			public void onDownloadError(Exception ex) {
				SelectionActivity.this.setRequestedOrientation(_requestedOrientation);
				hideDialogSafe(_pd);
				showErrorDialog("SelectionActivity.onStart.Client.OnDownloadListener", ex);
			}
		});
		
	    tabHost.setCurrentTab(0);

		client = c;

		// Added as a potential fix for issue #8
		Server server = provider.getServer(serverId);
		if (server == null) {
			String payload = ((ServerProvider)provider).getServerListDebug();
			showErrorDialog("SelectionActivity.onStart", 
					new Exception("No server found for id: " + (serverId == null ? "null" : serverId.toString()) + "\n" + payload));
		} else {
	   		client.connect(db, server);
		}		
	}
	
	@Override
	protected void onDestroy() {
		logger.info("Destroying activity.");
		try {
			// Unregister update receiver
			unregisterReceiver(receiver);
			// Unbind service
			unbindService(connection);
		} catch (Exception ex) {
			logger.warn("Error stopping activity.", ex);
		}
		// make sure we cancel the update thread, in case it is still
		// running
		if (client.isConnected()) {
			client.cancelUpdate();
		}
		artistList.releaseCursor();
		albumList.releaseCursor();
		playlistList.releaseCursor();
		songList.releaseCursor();
		db.close();
		super.onDestroy();
	}

	private Cursor refreshListCursor(CharSequence textFilter) throws Exception {
		String constraint = DBHelper.prepareFilter(textFilter);
		switch (tabHost.getCurrentTab()) {
		case 0:
			return client.getCatalog().getArtists(constraint);
		case 1:
			return client.getCatalog().getAlbums(artistId, constraint);
		case 2:
			return client.getCatalog().getPlaylists(constraint);
		case 3:
			return client.getCatalog().getSongs(artistId, albumId, playlistId, constraint);
		default:
			throw new Exception("Invalid tab selection.");
		}	
	}
	
	private void refreshList() {
		if (client == null || !client.isConnected())
			return;
		
		try {
			ListWrapper lw;			
			switch (tabHost.getCurrentTab()) {
			case 0:
				lw = artistList;
				break;
			case 1:
				lw = albumList;
				break;
			case 2:
				lw = playlistList;
				break;
			case 3:
				lw = songList;
				break;
			default:
				throw new Exception("Invalid tab.");
			}
			
			if (lw.isStale()) {
				lw.setCursor(refreshListCursor(lw.getFilter()));
				SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
		        		R.layout.row_simple, lw.getCursor(),
		        		new String[] { "name" },
		        		new int[] { R.id.text1 });
	
				// Filter by NAME
				adapter.setStringConversionColumn(0);
				adapter.setFilterQueryProvider(new FilterQueryProvider() {
					@Override
					public Cursor runQuery(CharSequence constraint) {
						Cursor c;
						try {
							c = refreshListCursor(constraint);
						} catch (Exception ex) {
							logger.error("Error running list query.", ex);
							c = null;
						}
						return c;
					}
				});
									
				lw.getView().setAdapter(adapter);
				lw.setStale(false);				
				if (restoreState) {
					lw.restoreState();
					restoreState = false;
				}
			}			
		} catch (Exception ex) {
			logger.error("Error refreshing list.", ex);
		}		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.selection, menu);
	    final MenuItem shuffleItem = menu.getItem(0);
	    final MenuItem repeatItem = menu.getItem(1);	    
	    try {
	    	boolean shuffleMode = player.isShuffleMode();
	    	shuffleItem.setChecked(shuffleMode);
	    	if (shuffleMode) {
	    		shuffleItem.setIcon(R.drawable.icon48_shuffle_on);
	    	} else {
	    		shuffleItem.setIcon(R.drawable.icon48_shuffle_off);					
	    	}
	    	boolean repeatMode = player.isRepeatMode();
	    	repeatItem.setChecked(repeatMode);
	    	if (repeatMode) {
	    		repeatItem.setIcon(R.drawable.icon48_repeat_on);
	    	} else {
	    		repeatItem.setIcon(R.drawable.icon48_repeat_off);					
	    	}
	    } catch (Exception ex) {
	    	logger.warn("Error creating options menu.", ex);
	    }	    
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			switch (item.getItemId()) {
				case R.id.item_refresh:
					artistList.clearFilters();
					client.updateCatalog(true);
					return true;
					
				case R.id.item_clear_filters:
					artistId = null;
					albumId = null;
					artistList.clearFilters();
					albumList.clearFilters();
					playlistList.clearFilters();
					songList.clearFilters();
					tabHost.setCurrentTab(0);
					return true;
					
				case R.id.item_shuffle:
				{
					boolean checked = !item.isChecked(); 
					player.setShuffleMode(checked);
					item.setChecked(checked);
			    	if (checked) {
			    		item.setIcon(R.drawable.icon48_shuffle_on);
			    	} else {
			    		item.setIcon(R.drawable.icon48_shuffle_off);					
			    	}
				}
				return true;
					
				case R.id.item_repeat:
				{
					boolean checked = !item.isChecked(); 
					player.setRepeatMode(checked);
					item.setChecked(checked);
			    	if (checked) {
			    		item.setIcon(R.drawable.icon48_repeat_on);
			    	} else {
			    		item.setIcon(R.drawable.icon48_repeat_off);					
			    	}
			    }
				return true;
				
				case R.id.item_info:
					showDialog(DialogFactory.DIALOG_INFO);
					return true;					
				default:
					return super.onOptionsItemSelected(item);
			}
			
		} catch (Exception ex) {
			logger.error("Error selecting options item.", ex);
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		switch (tabHost.getCurrentTab()) {
		case 0:
			// this leaves the app, so end music
			try {
				player.quit();
			} catch (Exception ex) {
				logger.warn("Error pausing player.", ex);
			}
			super.onBackPressed();
			break;			
		case 1:
			tabHost.setCurrentTab(0);
			break;
		case 2:
			tabHost.setCurrentTab(0);
			break;
		case 3:
			if (playlistId != null) {
				tabHost.setCurrentTab(2);				
			} else if (albumId != null) {
				tabHost.setCurrentTab(1);
			} else {
				tabHost.setCurrentTab(0);
			}
			break;			
		}
	}
	
	/**
	 * Show soft keyboard to filter current list view.
	 */
	@Override
	public boolean onSearchRequested() {
		switch (tabHost.getCurrentTab()) {
		case 0:
			artistList.focus();
			break;
		case 1:
			albumList.focus();
			break;
		case 2:
			playlistList.focus();
			break;
		case 3:
			songList.focus();
			break;
		}				
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (artistId != null)
			outState.putLong(PARAM_ARTIST_ID, artistId);		
		if (albumId != null)
			outState.putLong(PARAM_ALBUM_ID, albumId);
		if (playlistId != null)
			outState.putLong(PARAM_PLAYLIST_ID, playlistId);
		
		int index = artistList.getView().getFirstVisiblePosition();
		View child = artistList.getView().getChildAt(index);
		outState.putInt(PARAM_IDX_ARTIST, index);
		outState.putInt(PARAM_POS_ARTIST, child == null ? 0 : child.getTop());

		index = albumList.getView().getFirstVisiblePosition();
		child = albumList.getView().getChildAt(index);
		outState.putInt(PARAM_IDX_ALBUM, index);
		outState.putInt(PARAM_POS_ALBUM, child == null ? 0 : child.getTop());

		index = playlistList.getView().getFirstVisiblePosition();
		child = playlistList.getView().getChildAt(index);
		outState.putInt(PARAM_IDX_PLAYLIST, index);
		outState.putInt(PARAM_POS_PLAYLIST, child == null ? 0 : child.getTop());

		index = songList.getView().getFirstVisiblePosition();
		child = songList.getView().getChildAt(index);
		outState.putInt(PARAM_IDX_SONG, index);
		outState.putInt(PARAM_POS_SONG, child == null ? 0 : child.getTop());

		String tf = artistList.getFilter();
		if (tf != null && tf.length() > 0) {
			outState.putString(PARAM_FILTER_ARTIST, tf.toString());
		}

		tf = albumList.getFilter();
		if (tf != null && tf.length() > 0) {
			outState.putString(PARAM_FILTER_ALBUM, tf.toString());
		}
		
		tf = playlistList.getFilter();
		if (tf != null && tf.length() > 0) {
			outState.putString(PARAM_FILTER_PLAYLIST, tf.toString());
		}
		
		tf = songList.getFilter();
		if (tf != null && tf.length() > 0) {
			outState.putString(PARAM_FILTER_SONG, tf.toString());
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog result;
		AlertDialog.Builder ab = new AlertDialog.Builder(this);
	   	
		switch (id) {
		case DialogFactory.DIALOG_ERROR:
	   		result = 
	   			ab.setTitle(R.string.dialog_error)
	   			.setMessage(R.string.message_connect_server)
	   			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
	   			})
	   			.create();
	   		break;
	   		
		case DialogFactory.DIALOG_ERROR_ITUNES:
			result = 
				ab.setTitle(R.string.dialog_error)
					.setMessage(R.string.message_connect_itunes)
					.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					})
					.create();
			break;
			
		case DialogFactory.DIALOG_ERROR_CONNECTION:
			result = 
				ab.setTitle(R.string.dialog_error)
					.setMessage(R.string.message_error_connection)
					.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					})
					.create();
			break;
	   		
		case DialogFactory.DIALOG_ERROR_FEEDBACK:
			result = 
				ab.setTitle(R.string.dialog_error)
				.setMessage(R.string.message_connect_server_fb)
				.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (feedback == null) {
							feedback = new CustomFeedback(SelectionActivity.this, "unknown!", null);
						}
						
						feedback.setClient(client);
						feedback.setServer(client.getServer());
						feedback.submit();

						finish();
					}
	   			})
				.setNeutralButton(R.string.button_no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// nothing to do
						finish();
					}
	   			})
				.setNegativeButton(R.string.button_dont_ask_again, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences prefs = getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
						SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean(Constants.PREFERENCE_FEEDBACK, false);
						editor.commit();
						finish();
					}
	   			})
				.create();
			break;
			
		case DialogFactory.DIALOG_PASSWORD:
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

			result = 
				ab.setTitle(R.string.dialog_enter_password)
				.setMessage(R.string.label_enter_password)
				.setView(input)
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						input.setText("");
					}
				})
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Server server = provider.getServer(serverId);
						server.setPassword(input.getText().toString());
						provider.updateServer(server);
						onStart();
					}
				})
				.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						input.setText("");
						finish();
					}
				})
				.create();
			break;
			
		case DialogFactory.DIALOG_INFO:
			ab.setTitle(R.string.dialog_info)
			  .setMessage(R.string.message_help)
		       .setCancelable(true)
		       .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.dismiss();
		           }
		       });
			result = ab.create();
			break;
			
		case DialogFactory.DIALOG_ITEM_ACTIONS:
			final String options[] = { getString(R.string.label_download) };
			
			ab.setTitle(R.string.dialog_actions);
			ab.setItems(options, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						dialog.dismiss();
						// if we ever move to 2.3, we can use DownloadManager for this
						client.downloadSong(songId);
						break;
												
					default:
						break;
					}
				}
			});
			result = ab.create();
			break;
						
		default:
			result = super.onCreateDialog(id);
			break;
		}
			
		return result;
	}

	private void showDialogSafe(int id) {
		try {
			if (!isFinishing()) {
				showDialog(id);
			}
		} catch (BadTokenException ex) {
			logger.warn("Error showing dialog.", ex);
		}
	}
	
	/**
	 * We have seen errors in market that were caused by being
	 * unable to properly dismiss the progress dialog. 
	 * 
	 * These are likely caused by the app being destroyed.
	 */
	private void hideDialogSafe(final ProgressDialog pd) {
		try {
			if (pd != null)
				pd.dismiss();
		} catch (IllegalArgumentException ex) {
			logger.warn("Error dismissing progress dialog.", ex);
		}
	}
	
	private void showErrorDialog(final String src, final Exception ex) {
		// check if user is trying to connect to iTunes, if so, show specific error
		if (ex instanceof FileNotFoundException && client != null) {
			URLConnection uc = ((Client)client).getConnection();
			
			if (uc != null) {
				String server = uc.getHeaderField("daap-server");
				if (server != null && server.startsWith("iTunes")) {
					showDialogSafe(DialogFactory.DIALOG_ERROR_ITUNES);
					return; // we are done
				}
			}
		// Generic network errors
		} else if (ex instanceof ConnectException || 
				ex instanceof SocketException ||
				ex instanceof SocketTimeoutException) {
			showDialogSafe(DialogFactory.DIALOG_ERROR_CONNECTION);
			return; // we are done
		} 
			
		// handle all other errors
		SharedPreferences prefs = getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
		if (prefs.getBoolean(Constants.PREFERENCE_FEEDBACK, true)) {
			feedback = new CustomFeedback(this, src, ex);
			showDialogSafe(DialogFactory.DIALOG_ERROR_FEEDBACK);
		} else {
			showDialogSafe(DialogFactory.DIALOG_ERROR);			
		}
	}
}