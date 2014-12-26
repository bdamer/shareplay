package com.afqa123.shareplay.common;

public class AuthorizationException extends Exception {

	private static final long serialVersionUID = -2899034045819323202L;

	public AuthorizationException() {
		super();
	}
	
	public AuthorizationException(String message) {
		super(message);
	}
	
	public AuthorizationException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
