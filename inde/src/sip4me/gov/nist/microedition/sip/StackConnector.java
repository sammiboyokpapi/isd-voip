/*
 * StackConnector.java
 * 
 * Created on Feb 11, 2004
 *
 */
package sip4me.gov.nist.microedition.sip;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.Vector;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.siplite.ConfigurationProperties;
import sip4me.gov.nist.siplite.ListeningPoint;
import sip4me.gov.nist.siplite.ObjectInUseException;
import sip4me.gov.nist.siplite.RequestEvent;
import sip4me.gov.nist.siplite.ResponseEvent;
import sip4me.gov.nist.siplite.SipFactory;
import sip4me.gov.nist.siplite.SipListener;
import sip4me.gov.nist.siplite.SipProvider;
import sip4me.gov.nist.siplite.SipStack;
import sip4me.gov.nist.siplite.TimeoutEvent;
import sip4me.gov.nist.siplite.address.AddressFactory;
import sip4me.gov.nist.siplite.header.HeaderFactory;
import sip4me.gov.nist.siplite.message.MessageFactory;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.ClientTransaction;
import sip4me.gov.nist.siplite.stack.MessageProcessor;
import sip4me.gov.nist.siplite.stack.ServerTransaction;
import sip4me.nist.javax.microedition.sip.SipClientConnection;
import sip4me.nist.javax.microedition.sip.SipConnectionNotifier;
import sip4me.nist.javax.microedition.sip.SipDialog;

/**
 * This class is the connector between the JSR180 and the nist-siplite stack.
 * This class creates a stack from the SipConnector.open(SIP_URI) with the
 * listening point equals to the one specified in the SIP URI. If none is
 * specified, a random one is allowed by the system. This class receives the
 * messages from the stack because it's implementing the SipListener class and
 * transmits them to either SipConnectionNotifier or SipClientConnection or
 * both. This class follows the singleton design pattern and is thread-safe
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 * 
 *         <a href="{@docRoot} /uncopyright.html">This code is in the public
 *         domain.</a>
 */
public class StackConnector implements SipListener {
	/**
	 * The unique instance of this class
	 */
	private static StackConnector instance = null;
	/**
	 * The actual stack
	 */
	protected SipStack sipStack = null;
	/**
	 * listen address
	 */
	private String localAddress = null;
	/**
	 * list of connection notifiers
	 */
	protected Vector connectionNotifiersList = null;
	/**
	 * list of client connections
	 */
	// protected Vector clientConnectionList=null;
	/**
	 * list of all current dialogs
	 */
	protected Vector sipDialogList = null;

	// Factories needed to generate the responses
	public static AddressFactory addressFactory = null;
	public static MessageFactory messageFactory = null;
	public static HeaderFactory headerFactory = null;

	/**
	 * Configuration properties of the SIP stack (fix by ArnauVP)
	 */
	public static ConfigurationProperties properties = new ConfigurationProperties();

	/**
	 * The global Timer Thread for all the Sip Stack
	 */
	private Timer stackTimer;

	static {
		// Creates the factories to help to construct messages
		messageFactory = new MessageFactory();
		addressFactory = new AddressFactory();
		headerFactory = new HeaderFactory();
	}

	/**
	 * Constructor Creates the stack
	 */
	private StackConnector() throws IOException {
		connectionNotifiersList = new Vector();
		// clientConnectionList=new Vector();
		sipDialogList = new Vector();
		stackTimer = new Timer();
		// Create the sipStack
		SipFactory sipFactory = SipFactory.getInstance();
		localAddress = properties.getProperty("javax.sip.IP_ADDRESS");
		properties.setProperty("javax.sip.STACK_NAME", "sip-for-me");
		try {
			// Create SipStack object
			sipStack = sipFactory.createSipStack(properties);
			sipStack.setStackConnector(this);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}

	}

	private void dispose() {
		if (connectionNotifiersList != null)
			connectionNotifiersList.removeAllElements();
		connectionNotifiersList = null;
		if (sipDialogList != null)
			sipDialogList.removeAllElements();
		sipDialogList = null;
		if (stackTimer != null)
			stackTimer.cancel();
		stackTimer = null;
		sipStack = null;
		instance = null;
		
//		if (LogWriter.needsLogging)
//			LogWriter.logMessage("StackConnector disposed " + this);
		
	}

	/**
	 * Get the unique instance of this class
	 * 
	 * @return the unique instance of this class
	 * 
	 * FIXME: Change IOException for a more suitable one. Change all catch blocks
	 * in the calling classes, all the way up the calling chain.
	 */
	public synchronized static StackConnector getInstance() throws IOException {
		if (instance == null) {
			instance = new StackConnector();
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Creating new instance of StackConnector " + instance);
		}
		return instance;
	}

	/**
	 * 
	 * @return the IP of the stack
	 */
	public String getLocalAddress() {
		return localAddress;
	}

	/**
	 * remove the instance of the stack.
	 */
	public synchronized static void releaseInstance() {
		if (instance != null)
			instance.dispose();

		instance = null;
	}

	/**
	 * Create a sip connection notifier on a specific port using or not the sip
	 * secure layer and with some restrictive parameters to receive requests.
	 * 
	 * Fix by ArnauVP (Genaker): If parameters don't include transport=tcp, the
	 * default UDP will be used, but this means that also a TCP listening will
	 * be created on that port. If transport=tcp is included, only a TCP
	 * listening point will be created. <br />
	 * TODO: support secure connections (TLS)
	 * 
	 * @param portNumber
	 *            - the number of the port on which we must listen for incoming
	 *            requests
	 * @param secure
	 *            - flag to specify whether to use or not the secure layer.
	 *            UNUSED.
	 * @param parameters
	 *            - restrictive parameters to filter the incoming requests
	 * @return the sip connection notifier that will receive request
	 * @throws IOException
	 *             - if we cannot create the sip connection notifier for
	 *             whatsoever reason
	 * 
	 */
	public SipConnectionNotifier createSipConnectionNotifier(int portNumber,
			boolean secure, Vector parameters) throws IOException {

		boolean onlyTCP = false;
		ListeningPoint udpLP = null;
		ListeningPoint tcpLP = null;
		SipProvider sipProvider = null;

		// TODO : Use the parameters to restrain the incoming messages

		// According to JSR 180:
		// For any port and interface that a server listens on for UDP, it MUST
		// listen on that
		// same port and interface for TCP. This is because a message may need
		// to be sent using
		// TCP, rather than UDP, if it is too large. As a result, the converse
		// is not true. A
		// server need not listen for UDP on a particular address and port just
		// because it is
		// listening on that same address and port for TCP.
		// This means that for a SipConnectionNotifier listening on UDP
		// transport protocol is not mandatory if
		// all requests have been sent on top of TCP, that is if all the Contact
		// headers sent within a registration or
		// within dialogs indicate transport=tcp.

		// Read transport parameter, if available
		if (parameters != null) {
			for (Enumeration paramEnum = parameters.elements(); paramEnum
					.hasMoreElements();) {
				String nextParam = (String) paramEnum.nextElement();
				if (nextParam.equalsIgnoreCase("transport=tcp"))
					onlyTCP = true;
			}
		}

		if (!onlyTCP) {
			// Creates the UDP listening point
			try {
				udpLP = sipStack.createListeningPoint(portNumber, "udp");
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_DEBUG,
							"Listening point created on : " + localAddress
									+ ":" + portNumber + "/"
									+ udpLP.getTransport().toUpperCase());
			} catch (IllegalArgumentException iae) {
				MessageProcessor msgProc = sipStack.getMessageProcessor("udp");
				if (msgProc != null)
					sipStack.removeMessageProcessor(msgProc);
				udpLP = null;
				throw new IOException(iae.getMessage());
			}

		}

		// Creates the TCP listening point
		try {
			tcpLP = sipStack.createListeningPoint(portNumber, "tcp");
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG,
						"Listening point created on : " + localAddress + ":"
								+ portNumber + "/"
								+ tcpLP.getTransport().toUpperCase());
		} catch (IllegalArgumentException iae) {
			if (udpLP != null) {
				try {
					sipStack.deleteListeningPoint(udpLP);
				} catch (ObjectInUseException e) {
					e.printStackTrace();
				}
				udpLP = null;
			}
			MessageProcessor msgProc = sipStack.getMessageProcessor("tcp");
			if (msgProc != null)
				sipStack.removeMessageProcessor(msgProc);
			tcpLP = null;
			throw new IOException(iae.getMessage());
		}

		// Creates the sip provider
		try {
			sipProvider = sipStack.createSipProvider(tcpLP);
		} catch (ObjectInUseException oiue) {
			throw new IOException(oiue.getMessage());
		}

		// If needed, add the UDP listening point to the Sip Provider
		if (udpLP != null) {
			try {
				sipProvider.addListeningPoint(udpLP);
			} catch (ObjectInUseException oiue) {
				throw new IOException(oiue.getMessage());
			}
		}

		// Add this class as a listener for incoming messages
		try {
			sipProvider.addSipListener(this);
		} catch (IllegalStateException ise) {
			throw new IOException(ise.getMessage());
		}

		SipConnectionNotifierImpl sipConnectionNotifier = new SipConnectionNotifierImpl(
				sipProvider, localAddress, portNumber);

		sipProvider.setConnectionNotifier(sipConnectionNotifier);

		// Add the the newly created sip connection notifier to the list of
		// connection notifiers
		this.connectionNotifiersList.addElement(sipConnectionNotifier);

		return sipConnectionNotifier;
	}

	/**
	 * Creates a sip Client Connection to send a request to the following SIP
	 * URI user@host:portNumber;parameters
	 * 
	 * @param user
	 *            - the user part of the SIP URI
	 * @param host
	 *            - the host part of the SIP URI
	 * @param portNumber
	 *            - the port Number on which to send the request, part of the
	 *            SIP URI
	 * @param parameters
	 *            - the parameters of the SIP URI
	 * @return the sip client connection
	 */
	public SipClientConnection createSipClientConnection(String scheme,
			String user, String host, int portNumber, Vector parameters) {
		SipClientConnection sipClientConnection = new SipClientConnectionImpl(
				scheme, user, host, portNumber, parameters);
		return sipClientConnection;
	}

	/**
	 * generate a random tag that can be used either in the FromHeader or in the
	 * ToHeader
	 * 
	 * @return the randomly generated tag
	 */
	protected static String generateTag() {
		return Utils.generateTag();
	}

	/**
	 * find in the dialog list, the sip dialog with the same dialog ID as the
	 * one in parameter
	 * 
	 * @param dialogID
	 *            - dialogID to test against
	 * @return the sip dialog with the same dialog ID
	 */
	protected SipDialog findDialog(String dialogID) {
		Enumeration e = sipDialogList.elements();
		while (e.hasMoreElements()) {
			SipDialog sipDialog = (SipDialog) e.nextElement();
			if (sipDialog.getDialogID() != null
					&& sipDialog.getDialogID().equals(dialogID))
				return sipDialog;
		}
		return null;
	}

	/************************** SIP LISTENER METHODS **************************/

	/**
	 * 
	 */
	public void processRequest(RequestEvent requestEvent) {
		try {
			Request request = requestEvent.getRequest();
			// Retrieve the SipConnectionNotifier from the transaction
			SipConnectionNotifierImpl sipConnectionNotifier = null;
			ServerTransaction serverTransaction = requestEvent
					.getServerTransaction();
			if (serverTransaction != null)
				sipConnectionNotifier = (SipConnectionNotifierImpl) serverTransaction
						.getApplicationData();
			// If it's a new request coming in, the sipConnectionNotifier will
			// certainly be null, so retrieve it from the sipProvider
			if (sipConnectionNotifier == null) {
				SipProvider sipProvider = (SipProvider) requestEvent
						.getSource();

				sipConnectionNotifier = sipProvider.getConnectionNotifier();

				if (serverTransaction != null)
					serverTransaction.setApplicationData(sipConnectionNotifier);
			}
			if (sipConnectionNotifier != null) {
				sipConnectionNotifier.notifyRequestReceived(request);
			} else {
				new RuntimeException("we cannot find any connection notifier"
						+ "matching to handle this request").printStackTrace();
			}
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void processResponse(ResponseEvent responseEvent) {
		try {
			Response response = responseEvent.getResponse();
			ClientTransaction clientTransaction = responseEvent
					.getClientTransaction();
			SipClientConnectionImpl sipClientConnection = (SipClientConnectionImpl) clientTransaction
					.getApplicationData();
			if (sipClientConnection != null) {
				sipClientConnection.notifyResponseReceived(response);
			} else {
				new RuntimeException("we cannot find any client connection"
						+ "matching to handle this request").printStackTrace();
			}
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void processTimeout(TimeoutEvent timeoutEvent) {
		// In JSR-180, timeouts are not passed to the application.
		// Instead, you should use the scc.receive(TIMEOUT) method.
	}

	protected Timer getStackTimer() {
		return stackTimer;
	}

}
