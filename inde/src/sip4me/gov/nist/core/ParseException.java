package sip4me.gov.nist.core;

public class ParseException extends Exception {

	public ParseException(String buffer, int position) {
		super(buffer);
	}

}
