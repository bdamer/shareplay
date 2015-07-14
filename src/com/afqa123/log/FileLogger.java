package com.afqa123.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

class FileLogger implements Logger {

	private static final String LEVEL_ERROR = "ERROR";
	private static final String LEVEL_DEBUG = "DEBUG";
	private static final String LEVEL_WARNING = "WARN";
	private static final String LEVEL_INFO = "INFO";
	private static BufferedWriter writer;
	private final String className;
    
	public FileLogger(String forClass) {
		className = forClass;
        synchronized (FileLogger.class) {
            if (writer == null) {
    			try {
                    File f = openFile();
                    // BufferedWriter for performance, true to set append to file flag
		            writer = new BufferedWriter(new FileWriter(f, true)); 
                } catch (IOException e) {
                    Log.e(className, "Could not create log file.", e);
                }
            }
        }
	}
	
    private File openFile() throws IOException {
        File SDCardRoot = Environment.getExternalStorageDirectory();
        File logFile = new File(SDCardRoot, "shareplay.log");		
		if (!logFile.exists()) {
            logFile.createNewFile();
		}
        return logFile;
    }
    
	private void log(String level, String msg, Throwable t) {
        synchronized (FileLogger.class) {
            try {
                writer.append(new Date().toGMTString());
                writer.append(',');
                writer.append(level);
                writer.append(',');
                writer.append(className);
                writer.append(',');
                writer.append(msg);
                writer.newLine();

                if (t != null) {
                    writer.append(t.getClass().getName());
                    writer.append(t.getMessage());
                    writer.newLine();

                    StackTraceElement elements[] = t.getStackTrace();
                    for (StackTraceElement el : elements) {
                        writer.append("at " + el.getClassName() + "." + el.getMethodName() + "(" + el.getFileName() + ":" + el.getLineNumber() + ")");
                        writer.newLine();
                    }
                }

                writer.flush();
            } catch (IOException e) {
                Log.e(className, "Could not write to log file.", e);
            }
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
