/*
 * Created on Jan 28, 2004
 *
 */
package sip4me.nist.javax.microedition.sip;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.microedition.io.Connection;

/**
 * This interface defines a SIP server connection notifier. 
 * The SIP server connection is opened with Connector.open()using a generic 
 * SIP URI string with the host and user omitted. 
 * For example, URI sip:5060 defines an inbound SIP server connection on port 5060.
 * The local address can be discovered using the getLocalAddress() method. 
 * If the port number is already reserved the Connector.open() MUST throw 
 * IOException. SipConnectionNotifier can be also opened with sips: protocol 
 * scheme, which indicates that this server connection accepts only requests 
 * over secure transport (as defined in RFC 3261 [1] for SIPS URIs). 
 * SipConnectionNotifier is queueing received messages. 
 * In order to receive incoming requests application calls the acceptAndOpen() 
 * method, which returns a SipServerConnection instance. 
 * If there are no messages in the queue acceptAndOpen() will block until 
 * a new request is received. SipServerConnection holds the incoming SIP request 
 * message. SipServerConnection is used to initialize and send responses. 
 * Access to SIP server connections may be restricted by the security policy of 
 * the device. Connector.open MUST check access for the initial SIP server connection 
 * and acceptAndOpen() MUST check before returning each new SipServerConnection. 
 * A SIP server connection can be used to dynamically select an available port 
 * by omitting both the host and the port parameters in the connection SIP URI 
 * string. The string sip: defines an inbound SIP server connection on a port 
 * which is allocated by the system. To discover the assigned port number use 
 * the getLocalPort() method. The SipConnectionNotifier offers also an asynchronous 
 * callback interface to wait for incoming requests. 
 * The interface is defined in SipServerConnectionListener , 
 * which the user has to implement in order to receive notifications about 
 * incoming requests.
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public interface SipConnectionNotifier extends Connection {

	/**
	 * Accepts and opens a new SipServerConnection in this listening point. 
	 * If there are no messages in the queue method will block until a new request 
	 * is received.
	 * @return SipServerConnection which carries the received request
	 * @throws IOException - if the connection can not be established
	 * @throws InterruptedIOException - if the connection is closed
	 * @throws SipException - TRANSACTION_UNAVAILABLE if the system can not open 
	 * new SIP transactions.
	 */
	public sip4me.nist.javax.microedition.sip.SipServerConnection acceptAndOpen()
													  throws IOException, InterruptedIOException, SipException;
	
	/**
	 * Sets a listener for incoming SIP requests. If a listener is already set it 
	 * will be overwritten. Setting listener to null will remove the current 
	 * listener.
	 * @param sscl - listener for incoming SIP requests
	 * @throws IOException - if the connection was closed
	 */
	public void setListener(sip4me.nist.javax.microedition.sip.SipServerConnectionListener sscl)
				throws IOException;
				
	/**
	 * Gets the local IP address for this SIP connection.
	 * @return local IP address. Returns null if the address is not available.
	 * @throws IOException - if the connection was closed
	 */
	public java.lang.String getLocalAddress()
							throws IOException;

	/**
	 * Gets the local port for this SIP connection.
	 * @return local port number, that the notifier is listening to. 
	 * Returns 0 if the port is not available.
	 * @throws IOException - if the connection was closed
	 */			
	public int getLocalPort()
			   throws IOException;			   				   							
}	
