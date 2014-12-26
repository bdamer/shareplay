package com.afqa123.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.os.Environment;

class FileLogger implements Logger {

	private static final String LEVEL_ERROR = "ERROR";
	private static final String LEVEL_DEBUG = "DEBUG";
	private static final String LEVEL_WARNING = "WARN";
	private static final String LEVEL_INFO = "INFO";

	private final String className;
	
	public FileLogger(String forClass) {
		className = forClass;
	}
		
	private void log(String level, String msg, Throwable t) {
        File SDCardRoot = Environment.getExternalStorageDirectory();
        // @TODO this implementation should keep a file open and flush each time it logs
        File logFile = new File(SDCardRoot, "shareplay.log");		
		
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
		    } catch (IOException e) {
		    	// tough luck...
		    }
		}
		
		try {
			//BufferedWriter for performance, true to set append to file flag
		    BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
		    buf.append(new Date().toGMTString());
		    buf.append(',');
		    buf.append(level);
		    buf.append(',');
		    buf.append(className);
		    buf.append(',');
		    buf.append(msg);
		    buf.newLine();

		    if (t != null) {
		    	buf.append(t.getClass().getName());
		    	buf.append(t.getMessage());
		    	buf.newLine();
		    	
				StackTraceElement elements[] = t.getStackTrace();
				for (StackTraceElement el : elements) {
					buf.append("at " + el.getClassName() + "." + el.getMethodName() + "(" + el.getFileName() + ":" + el.getLineNumber() + ")");
					buf.newLine();
				}
		    }
		    buf.close();
		} catch (IOException e) {
		
		}
	}
	
	@Override
	public void debug(String msg) {
		log(LEVEL_DEBUG, msg, null);
	}

	@Override
	public void debug(String msg, Throwable t) {
		log(LEVEL_DEBUG, msg, t);
	}

	@Override
	public void warn(String msg) {
		log(LEVEL_WARNING, msg, null);
	}

	@Override
	public void warn(String msg, Throwable t) {
		log(LEVEL_WARNING, msg, t);				
	}

	@Override
	public void info(String msg) {
		log(LEVEL_INFO, msg, null);
	}

	@Override
	public void info(String msg, Throwable t) {
		log(LEVEL_INFO, msg, t);
	}

	@Override
	public void error(String msg) {
		log(LEVEL_ERROR, msg, null);
	}

	@Override
	public void error(String msg, Throwable t) {
		log(LEVEL_ERROR, msg, t);	
	}
}
