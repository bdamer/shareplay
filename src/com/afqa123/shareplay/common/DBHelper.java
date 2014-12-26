package com.afqa123.shareplay.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;

public class DBHelper extends SQLiteOpenHelper {

	public static final int DB_VERSION = 16;
	public static final String DB_NAME = "shareplay";
	public static final String TBL_SERVERS = "servers";
	public static final String TBL_ARTISTS = "artists";
	public static final String TBL_ALBUMS = "albums";
	public static final String TBL_SONGS = "songs";
	public static final String TBL_PLAYLISTS = "playlists";
	public static final String TBL_SONGS_PLAYLISTS = "songs_playlists";
	public static final String IDX_SONGS_DAAP_ID = "idx_songs_daap_id";
	public static final String COL_ID = "_id";
	public static final String COL_NAME = "name";
	public static final String COL_HOST = "host";
	public static final String COL_PORT = "port";
	public static final String COL_REVISION = "revision";
	public static final String COL_DISCOVERED = "discovered";
	public static final String COL_ARTIST_ID = "artist_id";
	public static final String COL_ALBUM_ID = "album_id";
	public static final String COL_TRACK = "track";
	public static final String COL_DAAP_ID = "daap_id";
	public static final String COL_SERVER_ID = "server_id";
	public static final String COL_ADDRESS = "address";
	public static final String COL_PASSWORD_HASH = "pw_hash";
	public static final String COL_COUNT = "track";
	public static final String COL_BASE_LIST = "base_list";
	public static final String COL_SONG_ID = "song_id";
	public static final String COL_PLAYLIST_ID = "playlist_id";

	public static final String[] COLS_SERVER = new String[] { COL_ID, COL_NAME, COL_HOST, COL_ADDRESS, 
		COL_PORT, COL_REVISION, COL_PASSWORD_HASH, COL_DISCOVERED };
	
	private static final String DB_CREATE_SERVERS =
		"CREATE TABLE " + TBL_SERVERS + " (" + 
		COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		COL_NAME + " TEXT NOT NULL, " +
		COL_HOST + " TEXT NOT NULL, " +
		COL_ADDRESS + " TEXT NOT NULL, " +
		COL_PORT + " INTEGER NOT NULL, " +
		COL_REVISION + " INTEGER DEFAULT 0, " +
		COL_PASSWORD_HASH + " TEXT DEFAULT NULL, " +
		COL_DISCOVERED + " TEXT NOT NULL)";
	
	private static final String DB_CREATE_ARTISTS = 
		"CREATE TABLE " + TBL_ARTISTS + " (" +
		COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
		COL_SERVER_ID + " INTEGER NOT NULL, " +
		COL_NAME + " TEXT NOT NULL)";
	
	private static final String DB_CREATE_ALBUMS =
		"CREATE TABLE " + TBL_ALBUMS + " (" +
		COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		COL_SERVER_ID + " INTEGER NOT NULL, " +
		COL_ARTIST_ID + " INTEGER NOT NULL, " + 
		COL_NAME + " TEXT NOT NULL)";
	
	private static final String DB_CREATE_SONGS = 
		"CREATE TABLE " + TBL_SONGS + " (" +
		COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		COL_SERVER_ID + " INTEGER NOT NULL, " +
		COL_ALBUM_ID + " INTEGER NOT NULL, " +
		COL_NAME + " TEXT NOT NULL, " + 
		COL_DAAP_ID + " INTEGER NOT NULL, " + 
		COL_TRACK + " INTEGER)";
	
	private static final String DB_CREATE_PLAYLISTS = 
		"CREATE TABLE " + TBL_PLAYLISTS + " (" +
		COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
		COL_SERVER_ID + " INTEGER NOT NULL, " +
		COL_NAME + " TEXT NOT NULL, " + 
		COL_DAAP_ID + " INTEGER NOT NULL, " + 
		COL_BASE_LIST + " INTEGER DEFAULT 0, " + 
		COL_COUNT + " INTEGER)";
	
	private static final String DB_CREATE_SONGS_PLAYLISTS = 
		"CREATE TABLE " + TBL_SONGS_PLAYLISTS + " (" +
		COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
		COL_SERVER_ID + " INTEGER NOT NULL, " +
		COL_SONG_ID + " INTEGER NOT NULL, " +
		COL_PLAYLIST_ID + " INTEGER NOT NULL)";
	
	private static final String DB_CREATE_SONG_IDX = 
		"CREATE INDEX " + IDX_SONGS_DAAP_ID + " ON " + TBL_SONGS + " (" + COL_DAAP_ID + ", " + COL_SERVER_ID + ");";
	
	private static final String DB_DROP_SERVERS = 
		"DROP TABLE IF EXISTS " + TBL_SERVERS;

	private static final String DB_DROP_ARTISTS = 
		"DROP TABLE IF EXISTS " + TBL_ARTISTS;
	
	private static final String DB_DROP_ALBUMS = 
		"DROP TABLE IF EXISTS " + TBL_ALBUMS;
	
	private static final String DB_DROP_SONGS = 
		"DROP TABLE IF EXISTS " + TBL_SONGS;

	private static final String DB_DROP_PLAYLISTS = 
		"DROP TABLE IF EXISTS " + TBL_PLAYLISTS;	

	private static final String DB_DROP_SONGS_PLAYLISTS = 
		"DROP TABLE IF EXISTS " + TBL_SONGS_PLAYLISTS;	
	
	private static final String DB_DROP_SONG_IDX = 
		"DROP INDEX IF EXISTS " + IDX_SONGS_DAAP_ID;
	
	private static final Logger logger = LoggerFactory.getLogger(DBHelper.class);
	
	public DBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
		
	@Override
	public void onCreate(SQLiteDatabase db) {
		logger.info("Creating new database.");
		db.execSQL(DB_CREATE_SERVERS);
		db.execSQL(DB_CREATE_ARTISTS);
		db.execSQL(DB_CREATE_ALBUMS);
		db.execSQL(DB_CREATE_SONGS);
		db.execSQL(DB_CREATE_PLAYLISTS);
		db.execSQL(DB_CREATE_SONGS_PLAYLISTS);
		db.execSQL(DB_CREATE_SONG_IDX);		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion >= newVersion) {
			return;
		}
		
		logger.info("Upgrading database from version " + oldVersion + 
				" to " + newVersion + ".");

		db.execSQL(DB_DROP_SONG_IDX);
		db.execSQL(DB_DROP_SERVERS);
		db.execSQL(DB_DROP_ARTISTS);
		db.execSQL(DB_DROP_ALBUMS);
		db.execSQL(DB_DROP_SONGS);
		db.execSQL(DB_DROP_PLAYLISTS);
		db.execSQL(DB_DROP_SONGS_PLAYLISTS);
		onCreate(db);
	}
	
	public static String Date2DB(final Date date) {
		final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		return fmt.format(date);
	}
	
	public static Date DB2Date(final String date) {
		Date result;
		try {
			final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
			result = fmt.parse(date);
		} catch (Exception ex) {
			logger.warn("Error parsing date.", ex);
			result = null;
		}
		return result;
	}
		
	public static String prepareFilter(final CharSequence filter) {
		String result = null;
		if (filter != null) {
			result = prepareFilter(filter.toString());
		}
		return result;
	}
	
	public static String prepareFilter(final String filter) {
		String result = null;
		if (filter != null && filter.length() > 0) {
			result = "%" + filter + "%";
		}
		return result;
	}
}
