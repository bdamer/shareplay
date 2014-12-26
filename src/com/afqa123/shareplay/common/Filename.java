package com.afqa123.shareplay.common;

public class Filename {

	// Only allow:
	// alpha-numeric, whitespace, and special: . , - _ 
	private static final String InvalidChars = "[^a-zA-Z0-9 \\.,\\-_]";
	
	public static String clean(final String name) {
		if (name == null)
			return null;
		
		return name.replaceAll(InvalidChars, "");
	}
	
}
