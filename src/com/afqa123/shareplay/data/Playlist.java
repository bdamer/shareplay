package com.afqa123.shareplay.data;

public class Playlist {

	private Long id;	
	private String name;
	private Long serverId;	
	private boolean baselist;
	private int count;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getServerId() {
		return serverId;
	}
	public void setServerId(Long serverId) {
		this.serverId = serverId;
	}
	public boolean isBaselist() {
		return baselist;
	}
	public void setBaselist(boolean baselist) {
		this.baselist = baselist;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
}
