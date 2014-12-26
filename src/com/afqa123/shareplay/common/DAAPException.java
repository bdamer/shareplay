package com.afqa123.shareplay.common;

public class DAAPException extends Exception {

	private static final long serialVersionUID = -2899034045819323202L;

	private int _lastCode;
	
	public DAAPException() {
		super();
	}
	
	public DAAPException(final String message) {
		super(message);
	}
	
	public DAAPException(final String message, final int lastCode) {
		super(message);
		_lastCode = lastCode;
	}
	
	public DAAPException(final String message, final Throwable cause) {
		super(message, cause);
	}
	
	public int getLastCode() {
		return _lastCode;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + Integer.toHexString(_lastCode);
	}
}
