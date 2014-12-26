package com.afqa123.shareplay.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Build;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;

public abstract class Feedback {

	private static final Logger logger = LoggerFactory.getLogger(Feedback.class);
	private static final String APP_PARAM = "app";
	private static final String DATA_PARAM = "data";
	
	private Context _context;
	private String _source;
	private Exception _exception;
	
	public Feedback(final Context context, final String source, final Exception ex) {
		_context = context;
		_source = source;
		_exception = ex;
	}
	
	public void submit() {
		new Thread() {
			public void run() {			        				
			    HttpClient httpClient = new DefaultHttpClient();
			    HttpPost httpPost = new HttpPost(Constants.FEEDBACK_URL);
				
				try {
					logger.debug("Submitting feedback request.");
					
					// create post data
			        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
			        params.add(new BasicNameValuePair(APP_PARAM, Constants.APP_NAME));
			        params.add(new BasicNameValuePair(DATA_PARAM, prepareFeedback().toString()));
			        httpPost.setEntity(new UrlEncodedFormEntity(params));

			        // Execute HTTP Post Request
			        httpClient.execute(httpPost);
					
				} catch (Exception ex) {
					logger.warn("Could not submit feedback.", ex);
				}
			}
		}.start();
	}
	
	protected JSONObject prepareFeedback() throws Exception {
		// assemble general information
		JSONObject root = new JSONObject();
		root.put("date", new Date().toGMTString());
		root.put("source", _source);
		root.put("appVersion", _context.getPackageManager().getPackageInfo(_context.getPackageName(), 0).versionName);

		// assemble device information
		JSONObject device = new JSONObject();
		device.put("release", Build.VERSION.RELEASE);
		device.put("sdkVersion", Build.VERSION.SDK_INT);
		device.put("brand", Build.BRAND);
		device.put("device", Build.DEVICE);
		device.put("display", Build.DISPLAY);
		device.put("manufacturer", Build.MANUFACTURER);
		device.put("model", Build.MODEL);
		device.put("product", Build.PRODUCT);
		device.put("user", Build.USER);			
		root.put("device", device);

		// assemble exception information
		JSONObject exception = new JSONObject();
		if (_exception != null) {
			exception.put("class", _exception.getClass().getCanonicalName());
			exception.put("message", _exception.getMessage());
			
			Throwable cause = _exception.getCause();
			if (cause != null) {
				exception.put("cause", cause.getMessage());
			} else {
				exception.put("cause", "none");
			}
			
			JSONArray stack = new JSONArray();
			StackTraceElement elements[] = _exception.getStackTrace();
			for (StackTraceElement el : elements) {
				stack.put("at " + el.getClassName() + "." + el.getMethodName() + "(" + el.getFileName() + ":" + el.getLineNumber() + ")");
			}
			
			exception.put("stackTrace", stack);
		}
		root.put("exception", exception);
			
		return root;
	}
}
