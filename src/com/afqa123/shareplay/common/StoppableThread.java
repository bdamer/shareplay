package com.afqa123.shareplay.common;

public abstract class StoppableThread extends Thread {

	private volatile boolean stop = false;
	
	protected boolean isStopped() {
		return stop;
	}
	
	public synchronized void requestStop() {
		stop = true;
	}
}
