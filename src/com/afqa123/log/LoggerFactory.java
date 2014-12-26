package com.afqa123.log;

import com.afqa123.shareplay.BuildConfig;

public class LoggerFactory {

	public static Logger getLogger(@SuppressWarnings("rawtypes") Class claz) {
		if (BuildConfig.DEBUG) {
			return new DefaultLogger(claz.getName());
		} else {
			return new NullLogger();
		}
	}	
}
