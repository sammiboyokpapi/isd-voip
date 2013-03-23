/*
 * Protocol.java
 * 
 * Created on Feb 2, 2004
 *
 */
package sip4me.gov.nist.microedition.io.j2me.tel;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.Connection;

import sip4me.gov.nist.microedition.sip.StackConnector;


/**
 * Extension of the class implementing the Sip Protocol for J2ME using the 
 * generic Connection framework, in the particular case of TEL URI scheme.
 * 
 * @author ArnauVP
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class Protocol extends sip4me.gov.nist.microedition.io.j2me.Protocol {

	/**
	 * 
	 */
	public Protocol() {	
			
	}

	/**
	 * This method always throws an exceptions, as the application
	 * is not supposed to open a SIP inbound connection with the "tel:"
	 * protocol but with the "sip:" protocol.
	 */
	protected Connection openSipConnectionNotifier(int port, boolean secure,
			Vector params) throws IOException {
		throw new IOException(
				"Opening a SipConnectionNotifier is not supported with the 'tel' protocol");
	}

	/**
	 * Creates a SipClientConnection with a TelURL Request URL. 
	 * User can't be null. Host must be null. 
	 */
	protected Connection openSipClientConnection(String user, String host,
			int port, boolean secure, Vector params) throws IOException, IllegalArgumentException {
		
		if (user == null)
			throw new IllegalArgumentException("'User' parameter can't be null in tel URL");
		if (host != null)
			throw new IllegalArgumentException("'Host' parameter must be null in tel URL");
		if (port != -1)
			throw new IllegalArgumentException("'Port' parameter can't be specified in tel URL");
		
		stackConnector = StackConnector.getInstance();
		return stackConnector.createSipClientConnection("tel", user, null, port, params);
	}

}
