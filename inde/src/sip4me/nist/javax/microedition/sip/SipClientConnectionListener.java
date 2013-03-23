/*
 * Created on Jan 28, 2004
 *
 */
package sip4me.nist.javax.microedition.sip;

/**
 * Listener interface for incoming SIP responses.
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public interface SipClientConnectionListener {

	/**
	 * This method gives the SipClientConnection instance, which has received 
	 * a new SIP response. The application implementing this listener interface 
	 * has to call SipClientConnection.receive() to initialize the 
	 * SipClientConnection object with the new response.
	 * @param scc - SipClientConnection carrying the response
	 */
	public void notifyResponse(sip4me.nist.javax.microedition.sip.SipClientConnection scc);
}
