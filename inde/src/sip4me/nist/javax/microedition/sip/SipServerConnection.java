/*
 * Created on Jan 28, 2004
 *
 */
package sip4me.nist.javax.microedition.sip;

/**
 * SipServerConnection represents SIP server transaction. 
 * SipServerConnection is created by the SipConnectionNotifier when a new request 
 * is received.
 * The SipServerConnection has following state chart:
 * 
 * 
 * �Request Received, SipServerConnection returned from SipConnectionNotifier or 
 * provisional response(s) (1xx) sent. 
 * �Initialized, response initialized calling initResponse() 
 * �Stream Open, OutputStream opened with openContentOutputStream(). 
 * Opening InputStream for received request does not trigger state transition. 
 * �Completed, transaction completed with sending final response 
 * (2xx, 3xx, 4xx, 5xx, 6xx) 
 * �Terminated, the final state, in which the SIP connection has been terminated 
 * by error or closed Note: The state chart of SipServerConnection differs from 
 * the state chart of SIP server transaction, which can be found in RFC 3261 [1] 
 * p.136-140
 * Following methods are accessible in each state. 
 * �Request Received
 * 		initResponse
 * 		openContentInputStream
 * �Initialized
 * 		addHeader
 * 		setHeader
 * 		removeHeader
 * 		setReasonPhrase
 * 		openContentOutputStream
 * 		send
 * �Stream Open
 * 		Methods in OutputStream and SipConnection.send 
 * �Completed
 * 		All get-methods, see below 
 * �Terminated
 * 		The transaction and this connection is closed. 
 * 		The I/O methods above will throw IOException 
 * Following methods can be called in every state. 
 * The functionality is defined by the method depending of the information availability. 
 * �Can be called in every state
 * 		getHeader
 * 		getHeaders
 * 		getRequestURI
 * 		getMethod
 * 		getStatusCode
 * 		getReasonPhrase
 * 		getDialog
 * 		close // causes state transition to Terminated state
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public interface SipServerConnection extends SipConnection {

	/**
	 * Initializes SipServerConnection with a specific SIP response to the
	 * received request. The default headers and reason phrase will be
	 * initialized automatically. After this the SipServerConnection is in
	 * Initialized state. The response can be sent. The procedure of generating
	 * the response and header fields is defined in RFC 3261 [1] p. 49-50. At
	 * least following information is set by the method: 
	 * 
	 * From: MUST equal the From header field of the Request <br />
	 * 
	 * Call-ID: MUST equal the Call-ID header field of the request Via: MUST equal the Via Header field values in the
	 * Request and MUST maintain the same ordering <br />
	 * To: MUST copy if exists in the original request <br />
	 * 'tag' must be added if not present<br />
	 * 
	 * Furthermore, if the system has automatically sent the "100 Trying"
	 * response, the 100 response initialized and sent by the user is just
	 * ignored.
	 * 
	 * @param code
	 *            - Response status code 1xx - 6xx
	 * @throws IllegalArgumentException
	 *             - if the status code is out of range 100-699 (RFC 3261
	 *             p.28-29)
	 * @throws SipException
	 *             - INVALID_STATE if the response can not be initialized,
	 *             because of wrong state.
	 */
	public void initResponse(int code)
				throws IllegalArgumentException, SipException;

	/**
	 * Changes the default reason phrase.
	 * @param phrase - the default reason phrase.
	 * @throws SipException - INVALID_STATE if the response can not be initialized, 
	 * because of wrong state. INVALID_OPERATION if the reason phrase can not 
	 * be set.
	 * @throws IllegalArgumentException - if the reason phrase is illegal. 
	 */				
	public void setReasonPhrase(java.lang.String phrase)
				throws SipException, IllegalArgumentException;				
}
