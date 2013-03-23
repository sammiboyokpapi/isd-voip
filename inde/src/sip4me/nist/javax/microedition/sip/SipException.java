/*
 * Created on Jan 28, 2004
 */
package sip4me.nist.javax.microedition.sip;

import java.io.IOException;

/**
 * This is an exception class for SIP specific errors. The exception includes 
 * free format textual error message and error code to categorize the error.
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SipException extends IOException {
	/**
	 * The requested transport is not supported
	 */
	public static final byte TRANSPORT_NOT_SUPPORTED=1;
	
	/**
	 * Thrown for example when SIP connection does not belong to any Dialog.
	 */
	public static final byte DIALOG_UNAVAILABLE=2;
	
	/**
	 * Used when for example Content-Type is not set before filling the message body.
	 */
	public static final byte UNKNOWN_TYPE=3;
	
	/**
	 * Used when for example Content-Length is not set before filling the message body
	 */
	public static final byte UNKNOWN_LENGTH=4;
	
	/**
	 * Method call not allowed, because of wrong state in SIP connection.
	 */
	public static final byte INVALID_STATE=5;
	
	/**
	 * The system does not allow particular operation. NOTICE! This error does 
	 * not handle security exceptions.
	 */
	public static final byte INVALID_OPERATION=6;
	
	/**
	 * System can not open any new transactions.
	 */
	public static final byte TRANSACTION_UNAVAILABLE=7;
	
	/**
	 * The message to be sent has invalid format.
	 */
	public static final byte INVALID_MESSAGE=8;
	
	/**
	 * Other SIP error
	 */
	public static final byte GENERAL_ERROR=9;
	
	/**
	 * Current error code for thos sip exception
	 */
	public byte error_code;
	
	/**
	 * Construct SipException with error code.
	 * @param errorCode - error code. If the error code is none of the specified 
	 * codes the Exception is initialized with default GENERAL_ERROR.
	 */
	public SipException(byte errorCode){
		super();
		this.error_code=errorCode;
	}
	
	/**
	 * Construct SipException with textual message and error code.
	 * @param message - error message.
	 * @param errorCode - error code. If the error code is none of the specified 
	 * codes the Exception is initialized with default GENERAL_ERROR.
	 */
	public SipException(java.lang.String message, byte errorCode) {
		super(message);
		this.error_code=errorCode;		
	}
	
	/**
	 * Gets the error code
	 * @return error code
	 */
	public byte getErrorCode() {
		return error_code;
	}
}
