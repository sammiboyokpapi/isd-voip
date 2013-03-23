/*
 * Created on Jan 28, 2004
 *
 */
package sip4me.nist.javax.microedition.sip;

import java.io.IOException;

/**
 * SipClientConnection represents SIP client transaction. 
 * application can create a new SipClientConnection with Connector or 
 * SipDialog object.The SipClientConnection has following state chart:
 * 
 * 
 * 
 *  Created, SipClientConnection created from Connector or SipDialog 
 *  Initialized, request has been initialized with initRequest(...) or initAck()
 *  or initCancel() 
 *  Stream Open, OutputStream opened with openContentOutputStream(). 
 * Opening InputStream for received response does not trigger state transition. 
 *  Proceeding, request has been sent, waiting for the response, or provisional 
 * 1xx response received. initCancel() can be called, which will spawn 
 * a new SipClientConnection which is in Initialized state 
 *  Completed, transaction completed with final response 
 * (2xx, 3xx, 4xx, 5xx, 6xx) in this state the ACK can be initialized. 
 * Multiple 200 OK responses can be received. Note different state transition 
 * for responses 401 and 407. 
 *  Unauthorized, transaction completed with response 401 (Unauthorized) or 
 * 407 (Proxy Authentication Required). The application can re-originate 
 * the request with proper credentials by calling setCredentials() method. 
 * After this the SipClientConnection is back in Proceeding state.
 *  Terminated, the final state, in which the SIP connection has been terminated
 *  by error or closed 
 * Note: The state chart of SipClientConnection differs 
 * from the state chart of SIP client transaction, which can be found in 
 * RFC 3261 [1] p.128-133
 * Following methods are restricted to a certain state. 
 * The table shows the list of restricted methods allowed in each state.
 *  Created
 * 		initRequest
 *  Initialized
 * 		addHeader
 * 		setHeader
 * 		removeHeader
 * 		setRequestURI
 * 		openContentOutputStream
 * 		send
 * 		enableRefresh	
 * 		setCredentials
 *  Stream Open
 * 		Methods in OutputStream and SipConnection.send 
 *  Proceeding
 * 		receive
 * 		openContentInputStream
 * 		initCancel
 *  Completed
 * 		openContentInputStream
 * 		initAck
 *  Unauthorized
 * 		setCredentials
 *  Terminated
 * 		The transaction and this connection is closed. 
 * 		The I/O methods above will throw IOException Following methods can be 
 * 		called in every state. 
 * 		The functionality is defined by the method depending of the information 
 * 		availability. 
 *  Can be called in every state
 * 		getHeader
 * 		getHeaders
 * 		getRequestURI
 * 		getMethod
 * 		getStatusCode
 * 		getReasonPhrase
 * 		getDialog
 * 		setListener
 * 		close // causes state transition to Terminated state
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public interface SipClientConnection extends SipConnection {
	
	/**
	 * Initializes SipClientConnection to a specific SIP request method 
	 * (REGISTER, INVITE, MESSAGE, ...). 
	 * The methods are defined in the RFC 3261 [1] and extensional 
	 * specifications from SIP WG and SIMPLE WG [2][3]. 
	 * The default RequestURI and headers will be initialized automatically. 
	 * After this the SipClientConnection is in Initialized state. 
	 * The headers From, Contact are set according to the properties of 
	 * SipConnectionNotifier given as the second parameter. 
	 * If SipConnectionNotifier is null headers From, Contact are not set. 
	 * Headers that will be initialized are as follows:
	 *     	To           // To address constructed from SIP URI given in Connector.open()    
	 * 	   	From         // Set by the system. If SipConnectionNotifier is given and the                     
	 *   		 			SIP identity is shared the value will be set according the                    
	 * 		 			    terminal SIP settings (see SIP Identity  for more details). 
	 * 		 			    If the SipConnectionNotifier is not given                     
	 * 					 	(= null) and/or the SIP identity is not shared the From                     
	 * 						header must be set to a default value e.g. anonymous URI                     
	 * 						(see RFC 3261 [1], chapter 8.1.1.3 From).    
	 * 	  	CSeq         // Set by the system    
	 * 		Call-ID      // Set by the system    
	 * 		Max-Forwards // Set by the system    
	 * 		Via          // Set by the system    
	 * 		Contact      // Set by the system (if SipConnectionNotifier is given) for
	 *                      REGISTER, INVITE and SUBSCRIBE. The value will be set
	 *                      according to the terminal IP settings and the
	 *                      SipConnectionNotifer properties. So the new request
	 *                      is associated with the SipConnectionNotifier. The
	 * 	                    other end can use the contact for subsequent requests. 
	 * Reference RFC 3261 [1] p.35 (8.1.1 Generating the Request) and p.159 (20 Header Fields)
	 * 
	 * @param method - Name of the method
	 * @param scn - SipConnectionNotifier to which the request will be 
	 * associated. If SipConnectionNotifier is null the request will not be 
	 * associated to a user defined listening point.
	 * @throws IllegalArgumentException - if the method is invalid
	 * @throws SipException - INVALID_STATE if the request can not be set, 
	 * because of wrong state in SipClientConnection. Furthermore, ACK and 
	 * CANCEL methods can not be initialized in Created state.
	 */
	public void initRequest(java.lang.String method, sip4me.nist.javax.microedition.sip.SipConnectionNotifier scn)
				throws IllegalArgumentException, SipException;
				
	/**
	 * Sets Request-URI explicitly. Request-URI can be set only in Initialized state.
	 * @param URI - Request-URI
	 * @throws IllegalArgumentException - MAY be thrown if the URI is invalid
	 * @throws SipException - INVALID_STATE if the Request-URI can not be set, 
	 * because of wrong state. 
	 * INVALID_OPERATION if the Request-URI is not allowed to be set.
	 */				
	public void setRequestURI(java.lang.String URI)
				throws IllegalArgumentException, SipException;
				
	/**
	 * Convenience method to initialize SipClientConnection with SIP request 
	 * method ACK. ACK can be applied only to INVITE request. 
	 * The method is available when final response (2xx) has been received. 
	 * The header fields of the ACK are constructed in the same way as for any 
	 * request sent within a dialog with the exception of the CSeq and the header 
	 * fields related to authentication (RFC 3261 [1], p.82). The Request-URI 
	 * and headers will be initialized automatically. 
	 * After this the SipClientConnection is in Initialized state. 
	 * At least Request-URI and following headers will be set by the method 
	 * (RFC 3261 [1] 12.2.1.1 Generating the Request p.73 and 8.1.1 
	 * Generating the Request p.35).
	 * See also RFC 3261 [1] 13.2.2.4 2xx Responses (p.82)
	 * Request-URI  // system uses the remote target and route set to build the Request-URI    
	 * To           // remote URI from the dialog state + remote tag of the dialog ID    
	 * From         // local URI from the dialog state + local tag of the dialog ID    
	 * CSeq         // the sequence number of the CSeq header field MUST be the same as                     
	 * 				   the INVITE being acknowledged, but the CSeq method MUST be ACK.    
	 * Call-ID      // Call-ID of the dialog    
	 * Via          // Via header field indicates the transport used for the transaction
	 *                 and identifies the location where the reponse is to be sent    
	 * Route        // system uses the remote target and route set (if present)                     
	 * 				   to build the Route header    
	 * Contact      // SHOULD include a Contact header field in any target refresh
	 *                 requests within a dialog, and unless there is a need to change                     
	 * 				   it, the URI SHOULD be the same as used in previous requests within                     
	 * 				   the dialog    
	 * Max-Forwards // header field serves to limit the number of hops a request can
	 * 				   transit on the way to its destination. 
	 * The following rules also apply:
	 * a) For error responses (3xx-6xx) the ACK is sent automatically by the 
	 * system in transaction level. If user initializes an ACK which has already 
	 * been sent an Exception will be thrown.
	 * b) Using initRequest( ACK , null) builds the request from scratch and 
	 * does not set the headers according to the current SIP dialog.
	 * @throws SipException INVALID_STATE if the request can not be set, 
	 * because of wrong state, INVALID_OPERATION if the ACK request can not be 
	 * initialized for other reason (already sent or the original request is 
	 * non-INVITE).
	 */				
	public void initAck()
				throws SipException;
		
 	/**
 	 * Convenience method to initialize SipClientConnection with SIP request 
 	 * method CANCEL. The method is available when a provisional response 
 	 * has been received. The CANCEL request starts a new transaction, 
 	 * that is why the method returns a new SipClientConnection. 
 	 * The CANCEL request will be built according to the original INVITE request 
 	 * within this connection. The RequestURI and headers will be initialized 
 	 * automatically. After this the SipClientConnection is in Initialized state.
 	 * The message is ready to be sent. 
 	 * The following information will be set by the method: <br/>
 	 * Request-URI  // copy from original request<br/>    
 	 * To           // copy from original request<br/>    
 	 * From         // copy from original request<br/>    
 	 * CSeq         // same value for the sequence number as was present in the original                     
 	 * 				   request, but the method parameter MUST be equal to  CANCEL <br/>    
 	 * Call-ID      // copy from original request<br/>    
 	 * Via          // single value equal to the top Via header field of the request                     
 	 * 				   being cancelled<br/>    
 	 * Route        // If the request being cancelled contains a Route header field, the                     
 	 * 				   CANCEL request MUST include that Route header field's values<br/>    
 	 * Max-Forwards // header field serves to limit the number of hops a request can                     
 	 * transit on the way to its destination.<br/>  
 	 * Reference RFC 3261 [1] p.53-54 Note: using initRequest( CANCEL , null); 
 	 * builds the request from scratch and does not set the headers according to
 	 * the current SIP dialog.
 	 * A CANCEL request SHOULD NOT be sent to cancel a request other than INVITE.
 	 * @return A new SipClientConnection with preinitialized CANCEL request.
 	 * @throws SipException - INVALID_STATE if the request can not be set, 
 	 * because of wrong state (in SipClientConnection) or the system has already 
 	 * got the 200 OK response (even if not read with receive() method). 
 	 * INVALID_OPERATION if CANCEL method can not be applied to the current 
 	 * request method.
 	 */		
	public sip4me.nist.javax.microedition.sip.SipClientConnection initCancel()
													  throws SipException;
													  
	/**												
	 * Receives SIP response message. The receive method will update the 
	 * SipClientConnection with the last new received response. 
	 * If no message is received the method will block until something is 
	 * received or specified amount of time has elapsed.
	 * @param timeout - the maximum time to wait in milliseconds. 0 = do not wait, just poll
	 * @return Returns true if response was received. Returns false if 
	 * the given timeout elapsed and no response was received.
	 * @throws SipException - INVALID_STATE if the receive can not be called because of wrong state.
	 * @throws IOException - if the message could not be received or because of network failure
	 */	  
	public boolean receive(long timeout)
				   throws SipException, IOException;
				   
	/**
	 * Sets the listener for incoming responses. If a listener is already set it 
	 * will be overwritten. Setting listener to null will remove the current 
	 * listener.
	 * @param sccl - reference to the listener object. Value null will remove the existing listener.
	 * @throws IOException - if the connection is closed.
	 */				   
	public void setListener(sip4me.nist.javax.microedition.sip.SipClientConnectionListener sccl)
				throws IOException;			
				
	/**
	 * Enables the refresh on for the request to be sent. The method return a 
	 * refresh ID, which can be used to update or stop the refresh.
	 * @param srl - callback interface for refresh events, if this is null the 
	 * method returns 0 and refresh is not enabled.
	 * @return refresh ID. If the request is not refreshable returns 0.
	 * @throws SipException - INVALID_STATE if the refresh can not be enabled in this state.
	 */
	public int enableRefresh(sip4me.nist.javax.microedition.sip.SipRefreshListener srl)
			   throws SipException;					   			
			   
	/**
	 * Sets credentials for possible digest authentication. 
	 * The application can set multiple credential triplets 
	 * (username, password, realm) for one SipClientConnection. 
	 * The username and password are specified for certain protection domain, 
	 * which is defined by the realm parameter.
	 * The credentials can be set: 
	 * 	 before sending the original request in Initialized state. 
	 * 	 The API implementation caches the credentials for later use. 
	 *   when 401 (Unauthorized) or 407 (Proxy Authentication Required) response 
	 * 	 is received in the Unauthorized state. The API implementation uses the 
	 *   given credentials to re-originate the request with proper authorization 
	 *   header. After that the SipClientConnection will be in Proceeding state. 
	 * @param username - username (for this protection domain)
	 * @param password - user password (for this protection domain)
	 * @param realm - defines the protection domain
	 * @throws SipException - INVALID_STATE if the credentials can not be set in this state.
	 * @throws NullPointerException - if the username, password or realm is null
	 */		   
	public void setCredentials(java.lang.String username, java.lang.String password, java.lang.String realm)
				throws SipException;			   																		  							
}
