package com.afqa123.shareplay.data;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Serializable, Parcelable {

	private static final long serialVersionUID = -209321053886816131L;

	private byte _kind;
	
	private long _id;
	
	private String _name;
	
	private String _artist;
	
	private String _album;
	
	private short _track;
			
	public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
		public Item createFromParcel(Parcel in) {
			return new Item(in);
		}
		
		public Item[] newArray(int size) {
			return new Item[size];
		}
	};

	public Item() {
		
	}
	
	public Item(long id, String name, short track, String album, String artist) {
		_id = id;
		_name = name;
		_artist = artist;
		_album = album;
		_track = track;
	}

	public Item(Parcel in) {
		readFromParcel(in);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte(_kind);
		dest.writeLong(_id);
		dest.writeString(_name);
		dest.writeInt(_track);
		dest.writeString(_artist);
		dest.writeString(_album);
	}
	
	public void readFromParcel(Parcel in) {
		_kind = in.readByte();
		_id = in.readLong();
		_name = in.readString();
		_track = (short)in.readInt();
		_artist = in.readString();
		_album = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public byte getKind() {
		return _kind;
	}

	public void setKind(byte kind) {
		this._kind = kind;
	}

	public long getId() {
		return _id;
	}

	public void setId(long id) {
		this._id = id;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		this._name = name;
	}

	public String getArtist() {
		return _artist;
	}

	public void setArtist(String artist) {
		this._artist = artist;
	}

	public String getAlbum() {
		return _album;
	}

	public void setAlbum(String album) {
		this._album = album;
	}

	public short getTrack() {
		return _track;
	}

	public void setTrack(short track) {
		_track = track;
	}

	@Override
	public String toString() {
		return _name + " by " + _artist + " from " + _album;
	}
}
