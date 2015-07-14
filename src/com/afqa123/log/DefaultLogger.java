package com.afqa123.log;

import android.util.Log;

class DefaultLogger implements Logger {

	private final String className;
	
	public DefaultLogger(String forClass) {
		className = forClass;
	}
	
	@Override
	public void debug(String msg) {
		Log.d(className, msg);
	}

	@Override
	public void debug(String msg, Throwable t) {
		Log.d(className, msg, t);
	}

	@Override
	public void warn(String msg) {
		Log.w(className, msg);
	}

	@Override
	public void warn(String msg, Throwable t) {
		Log.w(className, msg, t);
	}

	@Override
	public void info(String msg) {
		Log.i(className, msg);
	}

	@Override
	public void info(String msg, Throwable t) {
		Log.i(className, msg, t);
	}

	@Override
	public void error(String msg) {
		Log.e(className, msg);
	}

	@Override
	public void error(String msg, Throwable t) {
		Log.e(className, msg, t);
	}
}
