package com.afqa123.shareplay.interfaces;

import java.util.List;

import android.database.Cursor;

import com.afqa123.shareplay.data.Item;

public interface Catalog {

	enum CatalogMessage {
		IsEmpty,
		Updated,
		Complete,
		Error
	};
	
	/**
	 * Returns a cursor for artists whose names match the filter, or
	 * all artists if filter is null.
	 * 
	 * @param filter Name filter (Can be null).
	 * @return Cursor
	 */
	Cursor getArtists(final String filter);
		
	/**
	 * Returns a cursor for albums matching artist and filter.
	 * 
	 * @param artistId Artist id (can be null).
	 * @param filter Name filter (can be null).
	 * @return Cursor
	 */
	Cursor getAlbums(final Long artistId, final String filter);
	
	/**
	 * Returns a cursor for playlists whose names match the filter, or
	 * all playlists if filter is null.
	 * 
	 * @param filter Name filter (can be null).
	 * @return Cursor
	 */
	Cursor getPlaylists(final String filter);
	
	/**
	 * Returns a cursor for songs matching album and filter.
	 * 
	 * @param artistId Artist id (can be null).
	 * @param albumId Album id (can be null).
	 * @param filter Name filter (can be null).
	 * @return Cursor
	 */
	Cursor getSongs(final Long artistId, final Long albumId, final Long playlistId, final String filter);
			
	/**
	 * Returns the number of artists in this catalog.
	 * 
	 * @return Number of artists
	 */
	int getArtistCount();

	/**
	 * Returns the number of albums in this catalog.
	 * 
	 * @return Number of albums
	 */
	int getAlbumCount();
	
	/**
	 * Returns the number of songs in this catalog.
	 * 
	 * @return Number of songs
	 */
	int getSongCount();
	
	/**
	 * Returns the number of playlists in this catalog.
	 * 
	 * @return Number of playlists
	 */
	int getPlaylistCount();
	
	/**
	 * Returns a list of song items for a specific album.
	 * 
	 * @param artistId Artist id (can be null).
	 * @param albumId Album Id (can be null).
	 * @param filter Name filter (can be null).
	 * @return List of SongItem
	 */
	List<Item> getSongItems(final Long artistId, final Long albumId, final Long playlistId, final String filter);	
	
	/**
	 * Returns a song item.
	 * 
	 * @param songId Song id
	 * @return Item
	 */
	Item getSongItem(final Long songId);
		
	/**
	 * Adds a song to the catalog, creating the parent artist and 
	 * album if needed.
	 * 
	 * @param name Song name
	 * @param track Track number
	 * @param id Song id
	 * @param album Album name
	 * @param artist Artist name
	 */
	void addSong(final String name, final int track, final long id, final String album, final String artist);
	
	/**
	 * Adds an artist to the catalog.
	 * 
	 * @param name Artist name
	 * @return Id of the record
	 */
	long addArtist(final String name);
	
	/**
	 * Adds an album to the catalog.
	 * 
	 * @param name Album name
	 * @return Id of the record
	 */
	long addAlbum(final String name, final long artistId);
	
	/**
	 * Adds a playlist to the catalog.
	 * 
	 * @param name Name
	 * @param id ID
	 * @param baseList Base List
	 * @param count Song count
	 * @return Id of the record
	 */
	long addPlaylist(final String name, final long id, final boolean baseList, final int count);
	
	/**
	 * Adds a playlist entry to the catalog.
	 * 
	 * @param playlistId Playlist Id
	 * @param songId Song Id
	 */
	void addPlaylistEntry(final long playlistId, final long songId);
	
	/**
	 * Clears the catalog database and caches.
	 */
	void clear();	
	
	/**
	 * Prepares catalog for updates.
	 */
	void prepare();
	
	/**
	 * Commits pending catalog updates.
	 */
	void commit(boolean success);
}
