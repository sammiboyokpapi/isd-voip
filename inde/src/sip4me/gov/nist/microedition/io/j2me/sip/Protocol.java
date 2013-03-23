/*
 * Protocol.java
 * 
 * Created on Feb 2, 2004
 *
 */
package sip4me.gov.nist.microedition.io.j2me.sip;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.Connection;

import sip4me.gov.nist.microedition.sip.StackConnector;


/**
 * Extension of the class implementing the Sip Protocol for J2ME using the 
 * generic Connection framework, in the cases of SIP and SIPS URI schemes.
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
	 * Open an inbound connection to listen to incoming SIP requests (e.g. a
	 * SipConnectionNotifier and SipServerConnection)
	 * 
	 * @param port
	 *            the port to listen to
	 * @param secure
	 *            true if a secure protocol is to be used, false otherwise.
	 *            IGNORED, always insecure (TCP/UDP) right now.
	 * @param params
	 *            a Vector with parameters to open the connection
	 */
	protected Connection openSipConnectionNotifier(int port, boolean secure,
			Vector params) throws IOException {
		//Create the stack and listening points
		stackConnector = StackConnector.getInstance();
		return stackConnector.createSipConnectionNotifier(port, false, params);					
	}

	protected Connection openSipClientConnection(String user, String host,
			int port, boolean secure, Vector params) throws IOException, IllegalArgumentException {
		
		if (user == null)
			throw new IllegalArgumentException("'User' parameter can't be null in SipURI");
		if (host == null)
			throw new IllegalArgumentException("'Host' parameter can't be null in SipURI");
		
		stackConnector = StackConnector.getInstance();
		return stackConnector.createSipClientConnection("sip", user, host, port, params);
	}

}
