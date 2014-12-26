package com.afqa123.shareplay.data;

public class ContentCode {

	private int _number;
	
	private String _name;
	
	private short _code;
	
	public ContentCode() {
		
	}
	
	public ContentCode(final int number, final String name, final short code) {
		_number = number;
		_name = name;
		_code = code;
	}

	public int getNumber() {
		return _number;
	}

	public void setNumber(int number) {
		_number = number;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public short getCode() {
		return _code;
	}

	public void setCode(short code) {
		_code = code;
	}
	
	@Override
	public String toString() {
		return _number + " " + _name + " " + _code;
	}
}