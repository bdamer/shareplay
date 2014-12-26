package com.afqa123.shareplay.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;
import com.afqa123.shareplay.common.DBHelper;
import com.afqa123.shareplay.data.Item;
import com.afqa123.shareplay.interfaces.Catalog;

public class DatabaseCatalog implements Catalog {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseCatalog.class);
	private static Map<String,Long> artistCache = new HashMap<String,Long>();
	private static Map<String,Long> albumCache = new HashMap<String,Long>();
	private ContentValues values;
	private String serverId;
	private DBHelper helper;	

	public DatabaseCatalog(DBHelper aHelper, final Server server) {
		helper = aHelper;
		values = new ContentValues();
		serverId = server.getId().toString();
	}
	
	/**
	 * Returns an open cursor!
	 */
	@Override 
	public Cursor getArtists(final String filter) {
		String selection = DBHelper.COL_SERVER_ID + "=?";
		String[] selectionArgs;
		if (filter != null) {
			selection += " AND " + DBHelper.COL_NAME + " LIKE ?";
			selectionArgs =  new String[] { serverId, filter };
		} else {
			selectionArgs = new String[] { serverId };
		}
		
		return helper.getReadableDatabase().query(DBHelper.TBL_ARTISTS, 
				new String[] { DBHelper.COL_ID, DBHelper.COL_NAME }, 
				selection, selectionArgs, 
				null, null, DBHelper.COL_NAME);
	}
	
	/**
	 * Returns an open cursor!
	 */
	@Override
	public Cursor getPlaylists(final String filter) {
		String selection = DBHelper.COL_SERVER_ID + "=?";
		String[] selectionArgs;
		if (filter != null) {
			selection += " AND " + DBHelper.COL_NAME + " LIKE ?";
			selectionArgs =  new String[] { serverId, filter };
		} else {
			selectionArgs = new String[] { serverId };
		}
		
		return helper.getReadableDatabase().query(DBHelper.TBL_PLAYLISTS, 
				new String[] { DBHelper.COL_ID, DBHelper.COL_NAME }, 
				selection, selectionArgs, 
				null, null, DBHelper.COL_BASE_LIST + " DESC, " + DBHelper.COL_ID + " ASC");
	}
	
	/**
	 * Returns an open cursor!
	 */
	@Override 
	public Cursor getAlbums(final Long artistId, final String filter) {
		String selection;
		String arg1;
		String[] selectionArgs;
		if (artistId != null && artistId != 0) {
			selection = DBHelper.COL_ARTIST_ID + "=?";
			arg1 = artistId.toString();
		} else {
			selection = DBHelper.COL_SERVER_ID + "=?";
			arg1 = serverId;
		}

		if (filter != null) {
			selection += " AND " + DBHelper.COL_NAME + " LIKE ?";
			selectionArgs = new String[2];
			selectionArgs[1] = filter;
		} else {
			selectionArgs = new String[1];
		}
		selectionArgs[0] = arg1;

		return helper.getReadableDatabase().query(DBHelper.TBL_ALBUMS, 
				new String[] { DBHelper.COL_ID, DBHelper.COL_NAME }, 
				selection, selectionArgs, 
				null, null, DBHelper.COL_NAME);
	}

	private Cursor createSongQuery(final String[] columns, final Long artistId, final Long albumId, final Long playlistId, final String filter) {
		final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		String selection;
		List<String> selectionArgs = new ArrayList<String>();
		String sortBy;

		// regular selection
		if (playlistId == null || playlistId == 0) {
			if (albumId != null && albumId != 0) {
				selection = DBHelper.TBL_SONGS + "." + DBHelper.COL_ALBUM_ID + "=?";
				selectionArgs.add(albumId.toString());
				sortBy = DBHelper.TBL_SONGS + "." + DBHelper.COL_TRACK;
			} else if (artistId != null && artistId != 0) {
				selection = DBHelper.TBL_ALBUMS + "." + DBHelper.COL_ARTIST_ID + "=?";
				selectionArgs.add(artistId.toString());
				sortBy = DBHelper.TBL_SONGS + "." + DBHelper.COL_ALBUM_ID + ", " + DBHelper.TBL_SONGS + "." + DBHelper.COL_TRACK;
			} else {
				selection = DBHelper.TBL_SONGS + "." + DBHelper.COL_SERVER_ID + "=?";
				selectionArgs.add(serverId.toString());
				sortBy = DBHelper.TBL_SONGS + "." + DBHelper.COL_NAME;
			}

			builder.setTables(DBHelper.TBL_SONGS + 
					" JOIN " + DBHelper.TBL_ALBUMS + " ON (" + DBHelper.TBL_SONGS + "." + DBHelper.COL_ALBUM_ID + " = " + DBHelper.TBL_ALBUMS + "." + DBHelper.COL_ID + ")" +
					" JOIN " + DBHelper.TBL_ARTISTS + " ON (" + DBHelper.TBL_ALBUMS + "." + DBHelper.COL_ARTIST_ID + " = " + DBHelper.TBL_ARTISTS + "." + DBHelper.COL_ID + ")");
	
		// use playlist
		} else {
			selection = DBHelper.TBL_SONGS + "." + DBHelper.COL_SERVER_ID + "=? AND " + DBHelper.TBL_SONGS_PLAYLISTS + "." + DBHelper.COL_PLAYLIST_ID + "=?";
			selectionArgs.add(serverId);
			selectionArgs.add(playlistId.toString());
			sortBy = DBHelper.TBL_SONGS_PLAYLISTS + "." + DBHelper.COL_ID;
			
			builder.setTables(DBHelper.TBL_SONGS + 
					" JOIN " + DBHelper.TBL_ALBUMS + " ON (" + DBHelper.TBL_SONGS + "." + DBHelper.COL_ALBUM_ID + " = " + DBHelper.TBL_ALBUMS + "." + DBHelper.COL_ID + ")" +
					" JOIN " + DBHelper.TBL_ARTISTS + " ON (" + DBHelper.TBL_ALBUMS + "." + DBHelper.COL_ARTIST_ID + " = " + DBHelper.TBL_ARTISTS + "." + DBHelper.COL_ID + ")" + 
					" JOIN " + DBHelper.TBL_SONGS_PLAYLISTS + " ON (" + DBHelper.TBL_SONGS + "." + DBHelper.COL_DAAP_ID + "=" + DBHelper.TBL_SONGS_PLAYLISTS + "." + DBHelper.COL_SONG_ID + ")"
			);			
		}
		
		// add name filtering
		if (filter != null) {
			selection += " AND " + DBHelper.TBL_SONGS + "." + DBHelper.COL_NAME + " LIKE ?";
			selectionArgs.add(filter);
		}
		
		return builder.query(helper.getReadableDatabase(), columns, selection, selectionArgs.toArray(new String[0]), null, null, sortBy);
	}
	
	/**
	 * Returns an open cursor!
	 */
	@Override 
	public Cursor getSongs(final Long artistId, final Long albumId, final Long playlistId, final String filter) {
		return createSongQuery(new String[] { DBHelper.TBL_SONGS + "." + DBHelper.COL_DAAP_ID + " " + DBHelper.COL_ID, DBHelper.TBL_SONGS + "." + DBHelper.COL_NAME }, 
				artistId, albumId, playlistId, filter);
	}
	
	@Override
	public List<Item> getSongItems(final Long artistId, final Long albumId, final Long playlistId, final String filter) {
		final Cursor c = createSongQuery(
				new String[] { DBHelper.TBL_SONGS + "." + DBHelper.COL_DAAP_ID, DBHelper.TBL_SONGS + "." + DBHelper.COL_NAME, 
						DBHelper.TBL_SONGS + "." + DBHelper.COL_TRACK, DBHelper.TBL_ALBUMS + "." + DBHelper.COL_NAME,
						DBHelper.TBL_ARTISTS + "." + DBHelper.COL_NAME },
				artistId, albumId, playlistId, filter);

		List<Item> res = new ArrayList<Item>();
		try {
			while (c.moveToNext()) {
				res.add(new Item(c.getLong(0), c.getString(1),	c.getShort(2), c.getString(3), c.getString(4)));
			}	
		} finally {
			c.close();
		}		
		return res;
	}
	
	@Override
	public Item getSongItem(Long songId) {
		final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(DBHelper.TBL_SONGS + 
				" JOIN " + DBHelper.TBL_ALBUMS + " ON (" + DBHelper.TBL_SONGS + "." + DBHelper.COL_ALBUM_ID + " = " + DBHelper.TBL_ALBUMS + "." + DBHelper.COL_ID + ")" +
				" JOIN " + DBHelper.TBL_ARTISTS + " ON (" + DBHelper.TBL_ALBUMS + "." + DBHelper.COL_ARTIST_ID + " = " + DBHelper.TBL_ARTISTS + "." + DBHelper.COL_ID + ")");
				
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c =  builder.query(db, 
				new String[] { DBHelper.TBL_SONGS + "." + DBHelper.COL_DAAP_ID, DBHelper.TBL_SONGS + "." + DBHelper.COL_NAME, 
							   DBHelper.TBL_SONGS + "." + DBHelper.COL_TRACK, DBHelper.TBL_ALBUMS + "." + DBHelper.COL_NAME,
							   DBHelper.TBL_ARTISTS + "." + DBHelper.COL_NAME }, DBHelper.TBL_SONGS + "." + DBHelper.COL_DAAP_ID + "=? AND " + DBHelper.TBL_SONGS + "." + DBHelper.COL_SERVER_ID + "=?", 
							   new String[] { songId.toString(), serverId }, null, null, null);
		try {
			c.moveToNext();
			return new Item(c.getLong(0), c.getString(1), c.getShort(2), c.getString(3), c.getString(4));
		} finally {
			c.close();
			db.close();
		}
	}
	
	@Override 
	public int getArtistCount() {
		SQLiteDatabase db = helper.getReadableDatabase();		
		Cursor c = db.query(DBHelper.TBL_ARTISTS, 
				new String[] { "COUNT(" + DBHelper.COL_ID + ")" }, 
				DBHelper.COL_SERVER_ID + "=?", new String[] { serverId }, 
				null, null, null);
		try {
			c.moveToFirst();
			return c.getInt(0);		
		} finally {
			c.close();
			db.close();
		}
	}
	
	@Override 
	public int getAlbumCount() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.query(DBHelper.TBL_ALBUMS, 
				new String[] { "COUNT(" + DBHelper.COL_ID + ")" }, 
				DBHelper.COL_SERVER_ID + "=?", new String[] { serverId }, 
				null, null, null);
		try {
			c.moveToFirst();
			return c.getInt(0);
		} finally {
			c.close();
			db.close();
		}
	}
	
	@Override 
	public int getSongCount() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.query(DBHelper.TBL_SONGS, 
				new String[] { "COUNT(" + DBHelper.COL_ID + ")" }, 
				DBHelper.COL_SERVER_ID + "=?", new String[] { serverId }, 
				null, null, null);
		try {
			c.moveToFirst();
			return c.getInt(0);
		} finally {
			c.close();
			db.close();
		}
	}
	
	@Override
	public int getPlaylistCount() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.query(DBHelper.TBL_PLAYLISTS, 
				new String[] { "COUNT(" + DBHelper.COL_ID + ")" }, 
				DBHelper.COL_SERVER_ID + "=?", new String[] { serverId }, 
				null, null, null);
		try {
			c.moveToFirst();
			return c.getInt(0);
		} finally {
			c.close();
			db.close();
		}
	}
	
	/**
	 * Returns an open cursor!
	 */
	@Override
	public void prepare() {
		helper.getWritableDatabase().beginTransaction();
	}
	
	@Override
	public void commit(boolean success) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			if (db.inTransaction()) {
				if (success)
					db.setTransactionSuccessful();
	
				db.endTransaction();
			}		
		} finally {			
			db.close();	
		}
	}
		
	@Override
	public void addSong(final String name, final int track, final long id, final String album,
			final String artist) {
		Long artistId, albumId;
				
		try {
			artistId = artistCache.get(artist);
			if (artistId == null) {
				artistId = addArtist(artist);
				artistCache.put(artist, artistId);
			}
			
			final String key = artist + "||" + album;
			albumId = albumCache.get(key);
			if (albumId == null) {
				albumId = addAlbum(album, artistId);
				albumCache.put(key, albumId);
			}
			
			values.clear();
			values.put(DBHelper.COL_SERVER_ID, serverId);
			values.put(DBHelper.COL_ALBUM_ID, albumId);
			values.put(DBHelper.COL_NAME, name);
			values.put(DBHelper.COL_DAAP_ID, id);
			values.put(DBHelper.COL_TRACK, track);
			helper.getWritableDatabase().insertOrThrow(DBHelper.TBL_SONGS, null, values);
					
		} catch (Exception ex) {
			logger.error("Error adding song.");
		}
	}
	
	@Override
	public long addArtist(final String name) {
		values.clear();
		values.put(DBHelper.COL_SERVER_ID, serverId);
		values.put(DBHelper.COL_NAME, name);		
		return helper.getWritableDatabase().insertOrThrow(DBHelper.TBL_ARTISTS, null, values);
	}

	@Override
	public long addAlbum(final String name, final long artistId) {
		values.clear();
		values.put(DBHelper.COL_SERVER_ID, serverId);
		values.put(DBHelper.COL_NAME, name);
		values.put(DBHelper.COL_ARTIST_ID, artistId);
		return helper.getWritableDatabase().insertOrThrow(DBHelper.TBL_ALBUMS, null, values);
	}
	
	@Override
	public long addPlaylist(final String name, final long id, final boolean baseList, final int count) {
		try {
			values.clear();
			values.put(DBHelper.COL_SERVER_ID, serverId);
			values.put(DBHelper.COL_NAME, name);
			values.put(DBHelper.COL_DAAP_ID, id);
			values.put(DBHelper.COL_BASE_LIST, baseList);
			values.put(DBHelper.COL_COUNT, count);
			return helper.getWritableDatabase().insertOrThrow(DBHelper.TBL_PLAYLISTS, null, values);
		} catch (Exception ex) {
			logger.error("Error adding playlist.");
			return 0;
		}
	}
	
	@Override
	public void addPlaylistEntry(final long playlistId, final long songId) {
		try {
			values.clear();
			values.put(DBHelper.COL_SERVER_ID, serverId);
			values.put(DBHelper.COL_SONG_ID, songId);
			values.put(DBHelper.COL_PLAYLIST_ID, playlistId);
			helper.getWritableDatabase().insertOrThrow(DBHelper.TBL_SONGS_PLAYLISTS, null, values);
		} catch (Exception ex) {
			logger.error("Error adding playlist entry.");
		}
	}
	
	@Override
	public void clear() {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.delete(DBHelper.TBL_SONGS, DBHelper.COL_SERVER_ID + "=?", new String[] { serverId });
		db.delete(DBHelper.TBL_ALBUMS, DBHelper.COL_SERVER_ID + "=?", new String[] { serverId });
		db.delete(DBHelper.TBL_ARTISTS, DBHelper.COL_SERVER_ID + "=?", new String[] { serverId });
		db.delete(DBHelper.TBL_PLAYLISTS, DBHelper.COL_SERVER_ID + "=?", new String[] { serverId });
		db.delete(DBHelper.TBL_SONGS_PLAYLISTS, DBHelper.COL_SERVER_ID + "=?", new String[] { serverId });
		db.close();
		artistCache.clear();
		albumCache.clear();
	}	
}
