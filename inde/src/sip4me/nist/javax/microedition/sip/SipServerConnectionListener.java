/*
 * Created on Jan 28, 2004
 *
 */
package sip4me.nist.javax.microedition.sip;

/**
 * Listener interface for incoming SIP requests.
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public interface SipServerConnectionListener {
	
	/**
	 * This method will notify the listener that a new request is received. 
	 * This method gives the SipConnectionNotifier instance. 
	 * The user has to call the SipConnectionNotifier.acceptAndOpen() to get 
	 * the SipServerConnection object that holds the server transaction and 
	 * the request received.
	 * @param ssc - SipConnectionNotifier carrying SipServerConnection
	 */
	public void notifyRequest(sip4me.nist.javax.microedition.sip.SipConnectionNotifier ssc);
}
