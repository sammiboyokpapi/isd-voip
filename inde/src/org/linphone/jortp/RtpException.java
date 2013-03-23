package org.linphone.jortp;

public class RtpException extends Exception {

	public RtpException() {
		// TODO Auto-generated constructor stub
	}

	public RtpException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
	public RtpException(Throwable e) {
		super(e.getMessage());
	}

	public RtpException(String arg0,Throwable e) {
		super( arg0 + e.getMessage());
		// TODO Auto-generated constructor stub
	}
}
