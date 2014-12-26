package com.afqa123.shareplay.interfaces;

import com.afqa123.shareplay.data.Item;

interface MediaPlayerInterface {

	boolean rewind();
	
	boolean play();
	
	boolean pause();
	
	boolean forward();
	
	void load(in List<Item> songs);
	
	void select(in long songId);
	
	void setServer(in long serverId, in String url);
	
	void quit();
	
	void update();
	
	void setShuffleMode(in boolean shuffleMode);
	
	void setRepeatMode(in boolean repeatMode);
	
	boolean isShuffleMode();
	
	boolean isRepeatMode();
}