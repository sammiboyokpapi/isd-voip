/*
 * Created on Jan 29, 2004
 *
 */
package sip4me.gov.nist.microedition.sip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.AuthenticationHelper;
import sip4me.gov.nist.siplite.TransactionUnavailableException;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.SipURI;
import sip4me.gov.nist.siplite.address.TelURL;
import sip4me.gov.nist.siplite.address.URI;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.ContactHeader;
import sip4me.gov.nist.siplite.header.ContactList;
import sip4me.gov.nist.siplite.header.ContentTypeHeader;
import sip4me.gov.nist.siplite.header.ExpiresHeader;
import sip4me.gov.nist.siplite.header.FromHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.MaxForwardsHeader;
import sip4me.gov.nist.siplite.header.ToHeader;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.ClientTransaction;
import sip4me.gov.nist.siplite.stack.authentication.Credentials;
import sip4me.gov.nist.siplite.stack.authentication.DigestClientAuthentication;
import sip4me.nist.javax.microedition.sip.SipClientConnection;
import sip4me.nist.javax.microedition.sip.SipClientConnectionListener;
import sip4me.nist.javax.microedition.sip.SipConnectionNotifier;
import sip4me.nist.javax.microedition.sip.SipDialog;
import sip4me.nist.javax.microedition.sip.SipException;
import sip4me.nist.javax.microedition.sip.SipRefreshListener;

/**
 * @author Jean Deruelle
 * 
 *         <a href="{@docRoot} /uncopyright.html">This code is in the public
 *         domain.</a>
 */
public class SipClientConnectionImpl implements SipClientConnection {
	// Runnable {
	// Client Transaction States
	/**
	 * Created, SipClientConnection created from Connector or SipDialog
	 */
	protected static final int CREATED = 1;
	/**
	 * Initialized, request has been initialized with initRequest(...) or
	 * initAck() or initCancel()
	 */
	protected static final int INITIALIZED = 2;
	/**
	 * Stream Open, OutputStream opened with openContentOutputStream(). Opening
	 * InputStream for received response does not trigger state transition.
	 */
	protected static final int STREAM_OPEN = 3;
	/**
	 * Proceeding, request has been sent, waiting for the response, or
	 * provisional 1xx response received. initCancel() can be called, which will
	 * spawn a new SipClientConnection which is in Initialized state
	 */
	protected static final int PROCEEDING = 4;
	/**
	 * Unauthorized, transaction completed with response 401 (Unauthorized) or
	 * 407 (Proxy Authentication Required). The application can re-originate the
	 * request with proper credentials by calling setCredentials() method. After
	 * this the SipClientConnection is back in Proceeding state.
	 */
	protected static final int UNAUTHORIZED = 5;
	/**
	 * Completed, transaction completed with final response (2xx, 3xx, 4xx,
	 * 5xx, 6xx) in this state the ACK can be initialized. Multiple 200 OK
	 * responses can be received. Note different state transition for responses
	 * 401 and 407.
	 */
	protected static final int COMPLETED = 6;
	/**
	 * �Terminated, the final state, in which the SIP connection has been
	 * terminated by error or closed
	 */
	protected static final int TERMINATED = 7;

	/**
	 * the sip dialog this client transaction belongs to
	 */
	private SipDialog sipDialog = null;

	/**
	 * Listener to notify when a response will be received
	 */
	private SipClientConnectionListener sipClientConnectionListener = null;
	/**
	 * The Sip Connection notifier associated with this client connection
	 */
	private SipConnectionNotifier sipConnectionNotifier = null;
	/**
	 * Callback interface for refresh events
	 */
	private SipRefreshListener sipRefreshListener = null;
	/**
	 * The refresh ID of the refresh task associated with this client connection
	 * if there is any
	 */
	private String refreshID = null;
	/**
	 * current state of this client transaction
	 */
	protected int state;
	/**
	 * flag to know the state of the connection (open or close)
	 */
	private boolean connectionOpen;
	/**
	 * list of credentials that can be used for authorization
	 */
	private final Vector credentials;
	/**
	 * the request for this client transaction
	 */
	private Request request = null;
	/**
	 * the response to the actual request
	 */
	private Response response = null;
	/**
	 * content of the response body
	 */
	private OutputStream contentOutputStream = null;
	/**
	 * content from the request body
	 */
	private InputStream contentInputStream = null;
	/**
	 * The request URI created from the user, host, port and parameters
	 * attributes
	 */
	private URI requestURI = null;
	
	/**
	 * the scheme used in the request line
	 */
	private String scheme = null;
	
	/**
	 * the user part of the SIP URI
	 */
	private String user = null;
	/**
	 * the host part of the SIP URI
	 */
	private String host = null;
	/**
	 * the port Number on which to send the request, part of the SIP URI
	 */
	private int port;
	/**
	 * the parameters of the SIP URI
	 */
	private Vector parameters = null;
	/**
	 * the sip uri of the user
	 */
	private String userSipURI = "sip:anonymous@anonymous.invalid";
	/**
	 * the client Transaction for an INVITE request
	 */
	private ClientTransaction clientTransaction = null;
	/**
	 * 
	 */
	private Thread listeningThread = null;
	/**
	 * 
	 */
	private StackConnector stackConnector = null;

	/**
	 * It will help us create an Authentication Header 
	 * for those requests that need it.
	 */
	private AuthenticationHelper authHelper;
	
	/**
	 * The last 401/407 response, used by authHelper
	 * to create the Authentication Header. 
	 */
	private Response authResponse;
	
	
	/**
	 * Creates a sip Client Connection to send a request to the following SIP
	 * URI user@host:portNumber;parameters
	 * 
	 * @param scheme
	 *            - the scheme to be used in the request URI ('sip', 'tel' or 'sips')
	 * @param user
	 *            - the user part of the SIP URI
	 * @param host
	 *            - the host part of the SIP URI
	 * @param portNumber
	 *            - the port Number on which to send the request, part of the
	 *            SIP URI
	 * @param parameters
	 *            - the parameters of the SIP URI
	 */
	protected SipClientConnectionImpl(String scheme, String user, String host,
			int port, Vector parameters) throws IllegalArgumentException {
		this.scheme = scheme;
		this.user = user;
		this.host = host;
		this.port = port;
		this.parameters = parameters;
		connectionOpen = true;
		credentials = new Vector();
		try {
			stackConnector = StackConnector.getInstance();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		

		// Create the REQUEST URI of the request
		try {
			String ruri = scheme + ":" + user;
			
			// don't append the host for TelURLs
			if (host != null)
				ruri = ruri + "@" + host;
			
			if (LogWriter.needsLogging)
				LogWriter.logMessage("Creating SipClientConnection from R-URI: " + ruri + " and parameters " + parameters);
			
			requestURI = StackConnector.addressFactory.createURI(ruri);
			
			if (port != -1)
				((SipURI) requestURI).setPort(port);
			// handle the parameters
			if (parameters != null && requestURI.isSipURI()) {
				for (int i = 0; i < parameters.size(); i++) {
					String parameter = (String) parameters.elementAt(i);
					String name = parameter
							.substring(0, parameter.indexOf("=")).trim()
							.toLowerCase();
					String value = parameter
							.substring(parameter.indexOf("=") + 1);
					((SipURI) requestURI).setParameter(name, value);
				}
			}
			
			if (LogWriter.needsLogging)
				LogWriter.logMessage("Created SipClientConnection with R-URI: " + requestURI);
			
		} catch (ParseException pe) {
			throw new IllegalArgumentException(
					"The request URI can not be created"
							+ ", check the URI syntax");
		}
		state = CREATED;
	}

	/*
	 * public void start(){ if(listeningThread==null){ listeningThread=new
	 * Thread(this); listeningThread.start(); } }
	 */

	protected SipClientConnectionImpl(URI requestURI, SipDialog sipDialog)
			throws IllegalArgumentException {
		connectionOpen = true;
		credentials = new Vector();
		// Create the REQUEST URI of the request
		this.requestURI = requestURI;
		this.sipDialog = sipDialog;
		try {
			stackConnector = StackConnector.getInstance();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		state = CREATED;
	}

	protected SipClientConnectionImpl(Request request,
			SipConnectionNotifier sipConnectionNotifier, String sipUserURI)
			throws IllegalArgumentException {
		connectionOpen = true;
		credentials = new Vector();
		// Create the REQUEST of the connection
		this.request = request;
		this.userSipURI = sipUserURI;
		this.sipConnectionNotifier = sipConnectionNotifier;
		try {
			stackConnector = StackConnector.getInstance();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		if (request.getMethod().equals("CANCEL"))
			state = INITIALIZED;
		else
			state = CREATED;
	}

	/**
	 * @see javax.microedition.nist.sip.SipClientConnection#initRequest(java.lang.String,
	 *      javax.microedition.nist.sip.SipConnectionNotifier)
	 */
	public void initRequest(String method, SipConnectionNotifier scn)
			throws IllegalArgumentException, SipException {

		if (state != CREATED)
			throw new SipException(
					"the request can not be initialized, because of wrong state.",
					SipException.INVALID_STATE);
		if (state == CREATED
				&& ((method.equals(Request.ACK)) || (method
						.equals(Request.CANCEL))))
			throw new SipException(
					"the request can not be initialized, because of wrong state.",
					SipException.INVALID_STATE);
		if (method == null)
			throw new IllegalArgumentException("The method can not be null");
		if (sipConnectionNotifier == null && scn == null)
			throw new IllegalArgumentException(
					"The SipConnectionNotifier can not be null");
		
		// Affect the sip connection notifier
		if (scn != null)
			this.sipConnectionNotifier = scn;
		
		if (LogWriter.needsLogging)
			LogWriter.logMessage("Initializing Request with Method: " + method + "and SCN: " + scn);
		
		// redirect the methods ACK and CANCEL towards their helper methods
		if (method.equals(Request.ACK))
			initAck();
		if (method.equals(Request.CANCEL))
			initCancel();
		if (method.equals(Request.BYE)) {
			initBye();
			state = INITIALIZED;
			return;
		}
		if (method.equals(Request.INVITE) && state == CREATED && sipDialog != null) {
			initReInvite();
			state = INITIALIZED;
			return;
		}
		if (method.equals(Request.NOTIFY)) {
			initNotify();
			state = INITIALIZED;
			return;
		}
		// We lookup in a record store to see whether or not there is
		// the user sip uri
		String sipURI = null;
		try {
			RecordStore rs = RecordStore.openRecordStore("UserSipUri", false);
			RecordEnumeration re = rs.enumerateRecords(null, null, false);
			if (re.hasNextElement()) {
				sipURI = new String(re.nextRecord());
			}
			re.destroy();
		} catch (RecordStoreException rse) {
		}
		// if the record store is null the sip uri for the user
		// it is an anonymous sip uri
		if (sipURI != null)
			userSipURI = sipURI;
		else
			userSipURI = "\"Anonymous\" <sip:thisis@anonymous.invalid>";
		
		Address userAddress = null;
		try {
			userAddress = StackConnector.addressFactory
					.createAddress(userSipURI);
		} catch (ParseException pe) {
			throw new IllegalArgumentException(
					"The system property UserSipUri can not be parsed"
							+ ", check the syntax");
		}
		
		// Call ID
		CallIdHeader callIdHeader = ((SipConnectionNotifierImpl) scn)
				.getSipProvider().getNewCallId();
		// CSeq
		CSeqHeader cSeqHeader = null;
		try {
			cSeqHeader = StackConnector.headerFactory.createCSeqHeader(1,
					method);
		} catch (ParseException pe) {
			throw new SipException(
					"Problem during the creation of the CSeqHeader",
					SipException.GENERAL_ERROR);
		}
		
		// From and To
		FromHeader fromHeader = null;
		Address fromAddress = null;
		
		if (((SipConnectionNotifierImpl) sipConnectionNotifier).isSharedMode()) {
			// Retrieve identify from system (e.g. Nokia SIP Profile)
			fromAddress = userAddress;
		} else {
			// The user has to specify the From header in all requests
			try {
				fromAddress = StackConnector.addressFactory.createAddress("\"Anonymous\" <sip:thisis@anonymous.invalid>");
				
			} catch (ParseException e) {
				throw new SipException(
						"Problem during the creation of the FromHeader",
						SipException.GENERAL_ERROR);
			}
		}
		try {
			fromHeader = StackConnector.headerFactory.createFromHeader(
					fromAddress, StackConnector.generateTag());
		} catch (ParseException ex) {
			throw new SipException(
					"Problem during the creation of the FromHeader",
					SipException.GENERAL_ERROR);
		}

	

		
		// ToHeader
		Address toAddress = StackConnector.addressFactory.createAddress(requestURI);
		ToHeader toHeader = null;
		try {
			toHeader = StackConnector.headerFactory.createToHeader(
					toAddress, null);
		} catch (ParseException ex) {
			throw new SipException(
					"Problem during the creation of the ToHeader",
					SipException.GENERAL_ERROR);
		}
		// ViaHeader

		Vector viaHeaders = new Vector();
		try {
			String transport = null;
			if (requestURI instanceof SipURI)
				transport = ((SipURI) requestURI).getTransportParam();
			if (transport == null || 
					(!transport.equalsIgnoreCase("tcp") && !transport.equalsIgnoreCase("udp"))) {
				// Use UDP by default, unless specified in the parameters passed
				// when initializing the connection (this is the case when RURI == TelURL)
				transport = "udp";
				if (parameters != null && parameters.contains("transport=tcp"))
					transport = "tcp";
				
			}
				

			ViaHeader viaHeader = StackConnector.headerFactory.createViaHeader(
					sipConnectionNotifier.getLocalAddress(),
					sipConnectionNotifier.getLocalPort(), transport, null);
			
			viaHeaders.addElement(viaHeader);
		} catch (ParseException ex) {
			throw new SipException(
					"Problem during the creation of the ViaHeaders",
					SipException.GENERAL_ERROR);
		} catch (IOException ioe) {
			throw new SipException(
					"Internal Error, cannot get the local port or address",
					SipException.GENERAL_ERROR);
		} catch (ClassCastException ex) {
			System.err.println("mwahahaha");
			throw new SipException(
					"Problem during the creation of the ViaHeaders",
					SipException.GENERAL_ERROR);
		}
		// Max Forward Header
		MaxForwardsHeader maxForwardsHeader = StackConnector.headerFactory
				.createMaxForwardsHeader(70);
		// If the request is a notify and is created with a dialog
		// the to tag and the call id are set from the dialog
		/*
		 * if(method.equals(Request.NOTIFY)){ if(sipDialog!=null){ Dialog
		 * dialog=((SipDialogImpl)sipDialog).getDialog(); //Call id
		 * callIdHeader= dialog.getCallId();
		 * toHeader.setTag(dialog.getRemoteTag());
		 * 
		 * } }
		 */

		// Request-URI
		if (method.equals(Request.REGISTER)) {
			Address reqUriAddress = null;
			try {
				reqUriAddress = StackConnector.addressFactory
						.createAddress(userSipURI);
				if (reqUriAddress.isSIPAddress()) {
					((SipURI) reqUriAddress.getURI()).removeUser();
					requestURI = reqUriAddress.getURI();
				}
			} catch (ParseException pe) {
				throw new IllegalArgumentException(
						"The system property UserSipUri can not be parsed"
								+ ", check the syntax");
			}
		}

		// generate the request
		try {
			if (sipConnectionNotifier != null)
				request = StackConnector.messageFactory.createRequest(
						requestURI, method, callIdHeader, cSeqHeader,
						fromHeader, toHeader, viaHeaders, maxForwardsHeader);
			else {
				request = StackConnector.messageFactory
						.createRequest(requestURI.toString());
				request.setMethod(method);
				request.setCallId(callIdHeader);
				request.setCSeqHeader(cSeqHeader);
				request.setTo(toHeader);
				request.setVia(viaHeaders);
				request.setHeader(maxForwardsHeader);
			}
		} catch (ParseException ex) {
			throw new SipException(
					"Problem during the creation of the Request " + method,
					SipException.GENERAL_ERROR);
		}

		state = INITIALIZED;

	}

	/**
	 * @see javax.microedition.nist.sip.SipClientConnection#setRequestURI(java.lang.String)
	 */
	public void setRequestURI(String URI) throws IllegalArgumentException,
			SipException {
		if (state != INITIALIZED)
			throw new SipException(
					"the request URI can not be set, because of wrong state.",
					SipException.INVALID_STATE);
		if (URI == null)
			throw new IllegalArgumentException("Invalid URI");
		URI uri = null;
		try {
			uri = StackConnector.addressFactory.createURI(URI);
		} catch (ParseException pe) {
			throw new IllegalArgumentException("Invalid URI");
		}
		request.setRequestURI(uri);
	}

	/**
	 * @see javax.microedition.nist.sip.SipClientConnection#initAck()
	 */
	public void initAck() throws SipException {
		if (state != COMPLETED)
			throw new SipException(
					"the ACK request can not be initialized, because of wrong state.",
					SipException.INVALID_STATE);
		// init the ack request
		try {
			request = clientTransaction.createAck();
		} catch (sip4me.gov.nist.siplite.SipException se) {
			new SipException(se.getMessage(), SipException.GENERAL_ERROR);
		}
		state = INITIALIZED;
	}

	/**
	 * 	 
	 */
	protected void initBye() {
		// Generate Request
		SipDialogImpl sipDialogImpl = ((SipDialogImpl) sipDialog);
		try {
			request = sipDialogImpl.dialog.createRequest(Request.BYE);
			// handle the parameters
			if (parameters != null) {
				for (int i = 0; i < parameters.size(); i++) {
					String parameter = (String) parameters.elementAt(i);
					String name = parameter
							.substring(0, parameter.indexOf("=")).trim()
							.toLowerCase();
					String value = parameter
							.substring(parameter.indexOf("=") + 1);
					((SipURI) requestURI).setParameter(name, value);
				}
			}
			/*
			 * if(sipDialogImpl.proxyAuthorizationHeader!=null){
			 * request.setHeader(sipDialogImpl.proxyAuthorizationHeader); }
			 * if(sipDialogImpl.authorizationHeader!=null)
			 * request.setHeader(sipDialogImpl.authorizationHeader);
			 */
		} catch (sip4me.gov.nist.siplite.SipException ex) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
						"Could not create the BYE request! " + ex.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
		}
	}
	
	/**
	 * 	 
	 */
	protected void initReInvite() {
		// Generate Request
		SipDialogImpl sipDialogImpl = ((SipDialogImpl) sipDialog);
		try {
			request = sipDialogImpl.dialog.createRequest(Request.INVITE);
			// handle the parameters
			if (parameters != null) {
				for (int i = 0; i < parameters.size(); i++) {
					String parameter = (String) parameters.elementAt(i);
					String name = parameter
							.substring(0, parameter.indexOf("=")).trim()
							.toLowerCase();
					String value = parameter
							.substring(parameter.indexOf("=") + 1);
					((SipURI) requestURI).setParameter(name, value);
				}
			}
			/*
			 * if(sipDialogImpl.proxyAuthorizationHeader!=null){
			 * request.setHeader(sipDialogImpl.proxyAuthorizationHeader); }
			 * if(sipDialogImpl.authorizationHeader!=null)
			 * request.setHeader(sipDialogImpl.authorizationHeader);
			 */
		} catch (sip4me.gov.nist.siplite.SipException ex) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
						"Could not create the re-INVITE request! "
								+ ex.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
		}
	}

	/**
	 * 	 
	 */
	protected void initNotify() {
		// Generate Request
		SipDialogImpl sipDialogImpl = ((SipDialogImpl) sipDialog);
		try {
			request = sipDialogImpl.dialog.createRequest(Request.NOTIFY);
			// handle the parameters
			if (parameters != null) {
				for (int i = 0; i < parameters.size(); i++) {
					String parameter = (String) parameters.elementAt(i);
					String name = parameter
							.substring(0, parameter.indexOf("=")).trim()
							.toLowerCase();
					String value = parameter
							.substring(parameter.indexOf("=") + 1);
					((SipURI) requestURI).setParameter(name, value);
				}
			}
			/*
			 * if(sipDialogImpl.proxyAuthorizationHeader!=null){
			 * request.setHeader(sipDialogImpl.proxyAuthorizationHeader); }
			 * if(sipDialogImpl.authorizationHeader!=null)
			 * request.setHeader(sipDialogImpl.authorizationHeader);
			 */
		} catch (sip4me.gov.nist.siplite.SipException ex) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
						"Could not create the NOTIFY request! "
								+ ex.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
		}
	}

	/**
	 * @see javax.microedition.nist.sip.SipClientConnection#initCancel()
	 */
	public SipClientConnection initCancel() throws SipException {
		if (state != PROCEEDING)
			throw new SipException(
					"the CANCEL request can not be initialized, because of wrong state.",
					SipException.INVALID_STATE);
		// init the cancel request
		try {
			Request cancelRequest = clientTransaction.createCancel();
			SipClientConnection sipClientConnectionCancel = new SipClientConnectionImpl(
					cancelRequest, sipConnectionNotifier, userSipURI);
			// stackConnector.clientConnectionList.addElement(
			// sipClientConnectionCancel);
			return sipClientConnectionCancel;
		} catch (sip4me.gov.nist.siplite.SipException se) {
			new SipException(se.getMessage(), SipException.GENERAL_ERROR);
		}

		return null;
	}

	/**
	 * @see javax.microedition.nist.sip.SipClientConnection#receive(long)
	 */
	public boolean receive(long timeout) throws SipException, IOException {
		if (state != PROCEEDING)
			throw new SipException(
					"can not check for any response received, because of wrong state.",
					SipException.INVALID_STATE);
		if (!connectionOpen)
			throw new IOException("The connection has been closed");
		// check for a response
		if (response == null) {
			// wait for a response during the time specified by the timeout
			if (timeout != 0) {
				synchronized (this) {
					try {
						wait(timeout);
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}
				}
			}
		}
				
		changeDialogState();
		changeClientConnectionState();
		// If there is some credentials and the client connection is in an
		// UNAUTHORIZED state, the request is reoriginated automatically
		if (state == UNAUTHORIZED && credentials.size() > 0) {
			reoriginateRequest();
		} else if (state == UNAUTHORIZED) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage("Authorization is required but no Credentials were set for this client connection");
		}
		if (response == null)
			return false;
		return true;
	}

	/**
	 * @see javax.microedition.nist.sip.SipClientConnection#setListener(javax.microedition.nist.sip.SipClientConnectionListener)
	 */
	public void setListener(SipClientConnectionListener sccl)
			throws IOException {
		if (!connectionOpen)
			throw new IOException("The Connection has been closed!");
		this.sipClientConnectionListener = sccl;
	}

	/**
	 * @see javax.microedition.nist.sip.SipClientConnection#enableRefresh(javax.microedition.nist.sip.SipRefreshListener)
	 */
	public int enableRefresh(SipRefreshListener srl) throws SipException {
		if (state != INITIALIZED)
			throw new SipException(
					"can not enable the refresh, because of wrong state.",
					SipException.INVALID_STATE);
		if (srl == null)
			return 0;
		if (!request.getMethod().equals(Request.REGISTER)
				&& !request.getMethod().equals(Request.SUBSCRIBE))
			return 0;

		sipRefreshListener = srl;
		int taskID = RefreshManager.getInstance().createRefreshTask(request,
				sipConnectionNotifier, sipRefreshListener, this);
		refreshID = String.valueOf(taskID);
		return taskID;
	}

	/**
	 * @see javax.microedition.nist.sip.SipClientConnection#setCredentials(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void setCredentials(String username, String password, String realm)
			throws SipException {
		if (state != INITIALIZED && state != UNAUTHORIZED)
			throw new SipException(
					"can set the credentials, because of wrong state.",
					SipException.INVALID_STATE);
		if (username == null || password == null || realm == null)
			throw new NullPointerException();
		Credentials credential = new Credentials(username, password, realm);
		credentials.addElement(credential);
		// reoriginate the requests with the proper credentials
		if (state == UNAUTHORIZED)
			reoriginateRequest();
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#send()
	 */
	public void send() throws IOException, InterruptedIOException, SipException {
		if (state != STREAM_OPEN && state != INITIALIZED && state != UNAUTHORIZED)
			throw new SipException(
					"can not send the request, because of wrong state.",
					SipException.INVALID_STATE);
		if (!connectionOpen)
			throw new IOException("The Connection has been closed!");
		if (contentOutputStream != null) {
			try {
				request.setContent(contentOutputStream,
						(ContentTypeHeader) request
								.getHeader(Header.CONTENT_TYPE));
				contentOutputStream = null;
			} catch (ParseException pe) {
				pe.printStackTrace();
			}
		}
		
		if (LogWriter.needsLogging)
			LogWriter.logMessage("[SipClientConnection] Sending request "
					+ request.getMethod());
		
		// Fix by ArnauVP (Genaker): create a Tag for the From header if missing
		if (!request.getFromHeader().hasTag()) {
			request.getFromHeader().setTag(StackConnector.generateTag());
		}
		
		// Fix by ArnauVP (Genaker): Contact header is only a MUST
		// in dialog-initiating requests (INVITE and SUBSCRIBE)
		if ((request.getMethod().equals(Request.SUBSCRIBE) || request
				.getMethod().equals(Request.INVITE))
				&& request.getHeader(ContactHeader.NAME) == null) {
			// Contact Header
			ContactHeader contactHeader = null;
			try {

				String userPart = "anonymous";

				// TelURL is not valid in Contact Header
				// FIXME: to improve, e.g. add parameter user=phone in contact.
				if (userSipURI.indexOf("tel") != -1) {

					TelURL contactURI = StackConnector.addressFactory.createTelURL(userSipURI);
					userPart = contactURI.getPhoneNumber();

				}

				if (userSipURI.indexOf("sip:") != -1) {
					userPart = userSipURI.substring("sip:".length(), userSipURI
							.indexOf("@"));
				}

				SipURI contactURI = StackConnector.addressFactory.createSipURI(
						userPart, sipConnectionNotifier
						.getLocalAddress());

				// Fix by ArnauVP (Genaker): the transport of Contact doesn't
				// only depend on the
				// transport used to send the REGISTER, but on how we initiated
				// the ConnectionNotifier
				// and if we are listening only on TCP or also on UDP
				if (this.stackConnector.sipStack.getListeningPoint(
						sipConnectionNotifier.getLocalPort(), "udp") != null) {
					// we are also listening on UDP! Don't set transport
					// parameter,
					// to indicate the SIP proxy that we can receive over both
					// transports
				} else {
					// JSR-180, we ALWAYS listen in TCP
					contactURI.setTransportParam("TCP");
				}
				// End of Fix

				contactURI.setPort(sipConnectionNotifier.getLocalPort());

				contactHeader = StackConnector.headerFactory
				.createContactHeader(StackConnector.addressFactory
						.createAddress(contactURI));


			} catch (IOException ioe) {
				throw new SipException(
						"Internal Error, cannot get the local port or address",
						SipException.GENERAL_ERROR);
			} catch (ParseException ex) {
				ex.printStackTrace();
				throw new SipException(
						"Problem during the creation of the Contact Header",
						SipException.GENERAL_ERROR);
			}
			// set the headers
			request.addHeader(contactHeader);
		}
		
		// Creates the Nist-Siplite client Transaction for this request
		try {
			clientTransaction = ((SipConnectionNotifierImpl) sipConnectionNotifier)
					.getSipProvider().getNewClientTransaction(request);
		} catch (TransactionUnavailableException tue) {
			tue.printStackTrace();
			throw new SipException(
					"Cannot create a new Client Transaction for this request",
					SipException.INVALID_MESSAGE);
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		// Set the application data so that when the response comes in,
		// it will retrieve this SipClientConnection
		clientTransaction.setApplicationData(this);
				
		// Send the request
		try {
			if (request.getMethod().equals(Request.ACK)) {
				try {
					clientTransaction.getDialog().sendAck(request);
				} catch (sip4me.gov.nist.siplite.SipException se) {
					throw new SipException(se.getMessage(),
							SipException.DIALOG_UNAVAILABLE);
				} catch (IllegalArgumentException iae) {
					iae.printStackTrace();
				}
				state = COMPLETED;
				return;
			}
			// If the request is a BYE or a NOTIFY, we must send it within the
			// dialog
			else if (request.getMethod().equals(Request.BYE)
					|| request.getMethod().equals(Request.NOTIFY))
				((SipDialogImpl) sipDialog).dialog
						.sendRequest(clientTransaction);
			// If the request is a SUBSCRIBE and is a refresh,
			// NOT a reoriginated request with credentials
			else if (state != UNAUTHORIZED && (request.getMethod().equals(Request.SUBSCRIBE)
					&& refreshID != null && request.getCSeqHeaderNumber() > 1)) {
				if (LogWriter.needsLogging)
					LogWriter.logMessage("Sending refresh through existing dialog");
				((SipDialogImpl) sipDialog).dialog
						.sendRequest(clientTransaction);
			}
			else {
				if (LogWriter.needsLogging)
					LogWriter.logMessage("Sending request directly through Client Transaction");
				// We do not allow reINVITE here
				clientTransaction.sendRequest();
			}
		} catch (sip4me.gov.nist.siplite.SipException se) {
			throw new SipException(se.getMessage(),
					SipException.INVALID_MESSAGE);
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		}
		// An INVITE or a SUBSCRIBE has been sent, so a dialog need to be
		// created
		if (request.getMethod().equals(Request.INVITE)
				|| request.getMethod().equals(Request.SUBSCRIBE)) {
			sipDialog = new SipDialogImpl(clientTransaction.getDialog(),
					sipConnectionNotifier, requestURI);
			((SipDialogImpl) sipDialog).setSipClientConnectionListener(sipClientConnectionListener);
			stackConnector.sipDialogList.addElement(sipDialog);

			/*
			 * ((SipDialogImpl)sipDialog).proxyAuthorizationHeader=
			 * (ProxyAuthorizationHeader
			 * )request.getHeader(ProxyAuthorizationHeader.NAME);
			 * ((SipDialogImpl)sipDialog).authorizationHeader=
			 * (AuthorizationHeader)request.getHeader(AuthorizationHeader.NAME);
			 */
		}
		// If the method is a REGISTER it means that we are using a proxy
		// so we put the route of the proxy in the router (if it is not set
		// already)
		if (request.getMethod().equals(Request.REGISTER)
				&& stackConnector.sipStack.getRouter().getOutboundProxy() == null) {

			SipURI sipURI = (SipURI) request.getRequestURI();

			String transport = sipURI.getTransportParam();
			if (transport == null)
				transport = "UDP";

			int port = sipURI.getPort();
			if (port < 0)
				port = 5060;

			stackConnector.sipStack.getRouter().setOutboundProxy(
					sipURI.getHost() + ":" + port + "/" + transport);
			// outboundProxy=true;
		}

		// Fix by ArnauVP: don't schedule a refresh on send(), just
		// when an affirmative answer is received (notifyResponseReceived)
		state = PROCEEDING;
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#setHeader(java.lang.String,
	 *      java.lang.String)
	 */
	public void setHeader(String name, String value) throws SipException,
			IllegalArgumentException {
		if (state != INITIALIZED)
			throw new SipException(
					"the Header can not be set, because of wrong state.",
					SipException.INVALID_STATE);
		if (name == null)
			throw new IllegalArgumentException(
					"The header name can not be null");
		if (value == null)
			throw new IllegalArgumentException(
					"The header value can not be null");
		Header header = null;
		try {
			header = StackConnector.headerFactory.createHeader(name, value);
		} catch (ParseException pe) {
			throw new IllegalArgumentException(pe.getMessage());
		}
		if (header == null)
			throw new IllegalArgumentException("null header!");
		if (request.getHeader(name) == null)
			request.addHeader(header);
		else
			request.attachHeader(header, true, true);
		
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#addHeader(java.lang.String,
	 *      java.lang.String)
	 */
	public void addHeader(String name, String value) throws SipException,
			IllegalArgumentException {
		if (state != INITIALIZED)
			throw new SipException(
					"the Header can not be added, because of wrong state.",
					SipException.INVALID_STATE);
		if (name == null)
			throw new IllegalArgumentException(
					"The header name can not be null");
		if (value == null)
			throw new IllegalArgumentException(
					"The header value can not be null");
		Header header = null;
		try {
			header = StackConnector.headerFactory.createHeader(name, value);
		} catch (ParseException pe) {
			throw new IllegalArgumentException("The header can not be created,"
					+ " check if it is correct");
		}

		request.addHeader(header);
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#removeHeader(java.lang.String)
	 */
	public void removeHeader(String name) throws SipException,
			IllegalArgumentException {
		if (state != INITIALIZED)
			throw new SipException(
					"the Header can not be removed, because of wrong state.",
					SipException.INVALID_STATE);
		if (name == null)
			throw new IllegalArgumentException(
					"The header name can not be null");
		request.removeHeader(name, true);
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getHeaders(java.lang.String)
	 */
	public String[] getHeaders(String name) {
		Enumeration e = response.getHeaders(name);
		// Check the size of the enumeration
		int size = 0;
		while (e.hasMoreElements()) {
			e.nextElement();
			size++;
		}
		// If there is no elements in the enumeration we return null
		if (size < 1)
			return null;
		// Create the array of headers
		String[] headers = new String[size];
		e = response.getHeaders(name);
		int count = 0;
		while (e.hasMoreElements())
			headers[count++] = ((Header) e.nextElement()).getHeaderValue();
		return headers;
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getHeader(java.lang.String)
	 */
	public String getHeader(String name) {
		// Fix by Arnau Vazquez (Genaker)
		if (response != null) {
			if(response.getHeader(name) != null)
				return response.getHeader(name).getHeaderValue();
			else 
				return null;
		} else if (request != null && request.getHeader(name) != null) {
			return request.getHeader(name).getHeaderValue();
		} else {
			return null;
		}

	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getMethod()
	 */
	public String getMethod() {
		return request.getMethod();
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getRequestURI()
	 */
	public String getRequestURI() {
		return request.getRequestURI().toString();
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getStatusCode()
	 */
	public int getStatusCode() {
		if (response == null)
			return 0;
		return response.getStatusCode();
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getReasonPhrase()
	 */
	public String getReasonPhrase() {
		if (response == null)
			return null;
		return response.getReasonPhrase();
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getDialog()
	 */
	public SipDialog getDialog() {
		return sipDialog;
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#openContentInputStream()
	 */
	public InputStream openContentInputStream() throws IOException,
			SipException {
		if (state != COMPLETED)
			throw new SipException(
					"the content input strean can not be open, because of wrong state.",
					SipException.INVALID_STATE);
		if (!connectionOpen)
			throw new IOException("The Connection has been closed!");
		byte[] buf = response.getRawContent();
		contentInputStream = new ByteArrayInputStream(buf);
		return contentInputStream;
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#openContentOutputStream()
	 */
	public OutputStream openContentOutputStream() throws IOException,
			SipException {
		if (state != INITIALIZED)
			throw new SipException(
					"the content output strean can not be open, because of wrong state.",
					SipException.INVALID_STATE);
		if (request.getHeader(Header.CONTENT_TYPE) == null)
			throw new SipException(
					"Content-Type unknown, set the content-type header first",
					SipException.UNKNOWN_TYPE);
		if (request.getHeader(Header.CONTENT_LENGTH) == null)
			throw new SipException(
					"Content-Length unknown, set the content-length header first",
					SipException.UNKNOWN_LENGTH);
		if (!connectionOpen)
			throw new IOException("The Connection has been closed!");
		contentOutputStream = new SDPOutputStream(this);
		state = STREAM_OPEN;
		return contentOutputStream;
	}

	/**
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		// Removing the connection from the connection list held by the
		// stackConnector
		// StackConnector.getInstance().
		// clientConnectionList.removeElement(this);
		connectionOpen = false;
		listeningThread = null;
		state = TERMINATED;

	}

	/**
	 * Reoriginate the request with the proper credentials
	 */
	private void reoriginateRequest() {
		
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage("Authentication is needed for request: " + request.getFirstLine());
		
    	this.authResponse = (Response) response.clone();
    	
		// Reoriginate the request with the proper credentials
		this.request = authenticateRequest(request);

		// Will be null if no credentials are found, etc.
		if (request != null) {
			if (refreshID != null) {
				RefreshTask task = RefreshManager.getInstance().getTask(refreshID);
				// Fix by Albert Petit (Genaker)
				// When stopping a refresh, if authentication is needed,
				// the task would be null at this point, so check for it.
				if (task != null) {
					task.updateRequest(request);
				}
			}

			// update request will change state to INITIALIZED,
			// so bring it back to UNAUTHORIZED
			state = UNAUTHORIZED;
			try {
				this.send();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			// Fix by ArnauVP (Genaker)
			/* As the state diagram of SipClientConnection
			  in JSR-180 explains, after 401/407 and credentials
			  setting we move into the proceeding state */
			// this is actually done inside send()
			
		} else {
	    	if (LogWriter.needsLogging)
	    		LogWriter.logMessage(LogWriter.TRACE_MESSAGES,
						"Couldn't create authenticated request for: "
								+ request.getFirstLine());
		}
	}

	/**
	 * Change the state of this Client Connection due to an incoming response
	 * 
	 * @param response
	 */
	private void changeClientConnectionState() {
		// Change the Client Connection state
		// If it's a trying, the state is PROCEEDING
		if (response == null)
			state = COMPLETED;
		else if (response.getStatusCode() / 100 == 1 && state == PROCEEDING) {
			// System.out.println("Set state to PROCEEDING");
			state = PROCEEDING;
		}
		// If it's a 401 or 407, the state is UNAUTHORIZED
		else if (state == PROCEEDING
				&& (response.getStatusCode() == Response.UNAUTHORIZED || response
						.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED)) {
			state = UNAUTHORIZED;
		}
		// Otherwise this is COMPLETED
		else {
			state = COMPLETED;
		}
		// System.out.println("Set state to "+state);
	}

	/**
	 * Change the state of the dialog due to an incoming response
	 * 
	 * @param response
	 */
	private void changeDialogState() {
		// Change the dialog state
		if (response == null || 
			response.getCSeqHeader().getMethod().equals(Request.REGISTER) ||
			response.getCSeqHeader().getMethod().equals(Request.MESSAGE))
			return;
		if (response.isSuccessfulResponse()) {
			// handle the un-Subscribe state
			if (response.getCSeqHeader().getMethod().equals(Request.SUBSCRIBE)) {
				ExpiresHeader expiresHeader = (ExpiresHeader) request
						.getHeader(ExpiresHeader.NAME);
				if (expiresHeader.getExpires() == 0) {
					((SipDialogImpl) sipDialog).setState(SipDialog.TERMINATED);
					stackConnector.sipDialogList.removeElement(sipDialog);
				}
			}
			if (!response.getCSeqHeader().getMethod().equals(Request.BYE)) {
				// If it's a REGISTER the dialog can be null
				if (sipDialog != null) {
					// ((SipDialogImpl)sipDialog).dialog.addRoute(response);
					((SipDialogImpl) sipDialog).setState(SipDialog.CONFIRMED);
					((SipDialogImpl) sipDialog).setDialogID(response
							.getDialogId(false));
				}
			} else {
				((SipDialogImpl) sipDialog).setState(SipDialog.TERMINATED);
				stackConnector.sipDialogList.removeElement(sipDialog);
			}
		} 
		// Fix by ArnauVP (Genaker): On a negative response, the dialog
		// may not have been established!
		else if (sipDialog != null && sipDialog.getState() == SipDialog.EARLY
				&& response.getStatusCode() / 100 != 1) {
			((SipDialogImpl) sipDialog).setState(SipDialog.TERMINATED);
			stackConnector.sipDialogList.removeElement(sipDialog);
		} else {
			
		}
	}

	/**
	 * Update the request and the state from the refresh
	 * 
	 * @param request
	 *            - the request to update
	 */
	protected void updateRequestFromRefresh(Request request) {
		this.request = request;
		state = INITIALIZED;
	}

	/**
	 * 
	 */
	/*
	 * public void run(){ while(connectionOpen){ try{ listeningThread.sleep(1);
	 * } catch(InterruptedException ie){ ie.printStackTrace(); } } }
	 */

	protected Request getRequest() {
		return request;
	}

	/**
	 * Return the Call Identifier of this client connection If there is no call
	 * id yet, this method return an empty String
	 * 
	 * @return the call Identifier
	 */
	protected String getCallIdentifier() {
		if (request == null)
			return " ";
		return request.getCallIdentifier();
	}

	/**
	 * The stack connector notifies this class when it receives a new request
	 * 
	 * @param request
	 *            - the new received request
	 */
	protected void notifyResponseReceived(Response response) {		
		if (state != PROCEEDING && response.getStatusCode() / 100 == 1)
			return;
		if (state == COMPLETED && !response.isSuccessfulResponse())
			return;
		this.response = response;
		if (response.getCSeqHeaderNumber() == request.getCSeqHeaderNumber()) {
			synchronized (this) {
				notify();
			}
			// We notify the listener that a response has been received
			if (this.sipClientConnectionListener != null)
				sipClientConnectionListener.notifyResponse(this);
		}
		// If a listener for refresh event has been set, it is notified
		String method = response.getCSeqHeader().getMethod();
		if (sipRefreshListener != null
				&& (method.equals(Request.REGISTER) || method
						.equals(Request.SUBSCRIBE))) {

			if (response.isSuccessfulResponse()) {

				int expValue = -1;
				// If the expires is set, the refresh is scheduled for the
				// duration of the expires
				ExpiresHeader expiresHeader = (ExpiresHeader) response
						.getHeader(ExpiresHeader.NAME);
				
				// Try to get it from Contact Header
				if (expiresHeader == null && method.equals(Request.REGISTER)) {
					ContactList cList = response.getContactHeaders();
					if (cList != null) {
						ContactHeader originalContact = (ContactHeader) request.getContactHeaders().getFirst();
						ContactHeader nextContact;
						int numContacts = cList.size();
						for (int j = 0; j < numContacts; j++) {
							nextContact = (ContactHeader) cList.elementAt(j);
							if (((Address) nextContact.getValue()).equals(originalContact.getValue())) {
								expValue = Integer.parseInt(nextContact.getParameter("expires"));
								if (LogWriter.needsLogging)
									LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Found expires value in Contact Header: " + expValue);
								break;
							} else {
								if (LogWriter.needsLogging)
									LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Contact Headers do not match: " + nextContact.getValue() + " and " + originalContact.getValue());
							}
						}
					}
				}
				
				// if no expires found, it means the original Expires value is valid
				if (expiresHeader == null && expValue == -1) {
					expiresHeader = (ExpiresHeader) request
							.getHeader(ExpiresHeader.NAME);
				}

				// Finally get the value from the header. If it came in a Contact
				// it will already be in expValue
				if (expiresHeader != null) {
					expValue = expiresHeader.getExpires();
				}
				
				// Fix by ArnauVP (Genaker): don't refresh the
				// unsubscribe/unregister
				if (expValue > 0) {
					RefreshManager.getInstance().scheduleTask(refreshID,
						expValue);
				}
				// Notify the listener
				sipRefreshListener.refreshEvent(Integer.parseInt(refreshID),
						response.getStatusCode(), response.getReasonPhrase());

			}
			// Fix by ArnauVP (Genaker): notify the listener of failure
			else if (response.getStatusCode() >= 300 && state != UNAUTHORIZED) {

				// Notify the listener
				sipRefreshListener.refreshEvent(Integer.parseInt(refreshID),
						response.getStatusCode(), response.getReasonPhrase());

			}
		}
	}

	/**
	 * Adds an Authentication header to a request,
	 * using the headers in the last authentication
	 * response received.
	 * 
	 * @return the same request, with authentication header.
	 */
	public Request authenticateRequest(Request req) {
		if (authHelper == null) {
    		authHelper = new DigestClientAuthentication(credentials);
		}
		return authHelper.createNewRequest(stackConnector.sipStack,
				this.request, authResponse);
	}
}
