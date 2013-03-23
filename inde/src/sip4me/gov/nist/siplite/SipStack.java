package sip4me.gov.nist.siplite;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.core.net.NetworkLayer;
import sip4me.gov.nist.microedition.sip.StackConnector;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.address.Router;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.stack.MessageProcessor;
import sip4me.gov.nist.siplite.stack.SIPTransactionErrorEvent;
import sip4me.gov.nist.siplite.stack.SIPTransactionStack;
import sip4me.gov.nist.siplite.stack.ServerLog;
import sip4me.gov.nist.siplite.stack.Transaction;

/**
 * Implementation of SipStack.
 * 
 * The JAIN-SIP stack is initialized by a set of properties (see the JAIN SIP
 * documentation for an explanation of these properties). In addition to these,
 * the following are meaningful properties for the NIST SIP stack (specify these
 * in the property array when you create the JAIN-SIP stack).:
 *<ul>
 * 
 *<li><b>sip4me.gov.nist.javax.sip.TRACE_LEVEL = integer </b><br/>
 * Currently only 16 and 32 is meaningful. If this is set to 16 or above, then
 * incoming valid messages are logged in SERVER_LOG. If you set this to 32 and
 * specify a DEBUG_LOG then vast amounts of trace information will be dumped in
 * to the specified DEBUG_LOG. The server log accumulates the signaling trace.
 * <a href="{@docRoot}/tools/tracesviewer/tracesviewer.html"> This can be viewed
 * using the trace viewer tool .</a> Please send us both the server log and
 * debug log when reporting non-obvious problems.</li>
 * 
 *@version JAIN-SIP-1.1
 * 
 *@author M. Ranganathan <mranga@nist.gov> <br/>
 * 
 *        <a href="{@docRoot} /uncopyright.html">This code is in the public
 *         domain.</a>
 * 
 * 
 */

public class SipStack extends SIPTransactionStack {

	private final Hashtable listeningPoints;
	private final Vector sipProviders;
	protected boolean stackInitialized;
	protected String routerPath;
	protected EventScanner eventScanner;
	protected SipListener sipListener;
	protected StackConnector sipStackConnector;

	public void setStackConnector(StackConnector stackConnector) {
		this.sipStackConnector = stackConnector;

	}

	/**
	 * Creates a new instance of SipStack.
	 */

	protected SipStack() {
		super();
		NistSipMessageFactoryImpl msgFactory = new NistSipMessageFactoryImpl();
		super.setMessageFactory(msgFactory);
		this.listeningPoints = new Hashtable();
		this.sipProviders = new Vector();
	}

	public void stopStack() {
		super.stopStack();
		eventScanner.stop();
		eventScanner = null;
		StackConnector.releaseInstance();
	}

	/**
	 * 
	 * @param configurationProperties
	 * @throws IllegalArgumentException
	 */
	public SipStack(ConfigurationProperties configurationProperties)
			throws IllegalArgumentException {
		this();
		this.eventScanner = new EventScanner(this);
		this.eventScanner.start();
		String address = configurationProperties
				.getProperty("javax.sip.IP_ADDRESS");

		/** Retrieve the stack IP address */
		if (address == null)
			throw new IllegalArgumentException("address not specified");
		super.setHostAddress(address);

		/** Retrieve the stack name */
		String name = configurationProperties
				.getProperty("javax.sip.STACK_NAME");
		if (name == null)
			throw new IllegalArgumentException("stack name is missing");
		super.setStackName(name);

		/** Read timeout */
		// ArnauVP: This has to be different in the mobile stack
		// where connections and processing are slower.
		String readTimeout = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.READ_TIMEOUT");
		if (readTimeout != null) {
			try {
				int rt = Integer.parseInt(readTimeout);
				if (rt >= 2000) {
					super.readTimeout = rt;
				} else {
					System.err.println("Value too low " + readTimeout);
					super.readTimeout = 2000;
				}
			} catch (NumberFormatException nfe) {
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_DEBUG,
							"Bad read timeout " + readTimeout);
			}
		}

		// Network Layer
		// For BlackBerry support, set this property to
		// "sip4me.gov.nist.core.net.BBNetworkLayer"
		String netLayerPath = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.NETWORK_LAYER");
		if (netLayerPath != null) {
			try {
				Class netLayerClass = Class.forName(netLayerPath);
				NetworkLayer nl = (NetworkLayer) netLayerClass.newInstance();
				if (nl != null)
					super.setNetworkLayer(nl);
			} catch (Exception e) {
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
							"Could not load required Network Layer: "
									+ netLayerPath);
			}
		}

		routerPath = "sip4me.gov.nist.siplite.stack.DefaultRouter";

		// String with form proxyAddress + ":" + proxyPort + "/" + transport
		// i.e. 10.1.1.1:5060/UDP
		String outboundProxy = configurationProperties
				.getProperty("javax.sip.OUTBOUND_PROXY");

		try {
			Class routerClass = Class.forName(routerPath);
			Router router = (Router) routerClass.newInstance();
			if (outboundProxy != null)
				router.setOutboundProxy(outboundProxy);
			router.setSipStack(this);
			super.setRouter(router);

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new IllegalArgumentException("Could not instantiate router");
		}

		if (outboundProxy != null) {
			Hop hop = new Hop(outboundProxy);
			this.outboundProxy = hop.getHost();
			this.outboundPort = hop.getPort();
		}

		/*
		 * Retrieve the EXTENSION Methods. These are used for instantiation of
		 * Dialogs.
		 */
		String extensionMethods = configurationProperties
				.getProperty("javax.sip.EXTENSION_METHODS");

		if (extensionMethods != null) {
			sip4me.gov.nist.core.StringTokenizer st = new sip4me.gov.nist.core.StringTokenizer(
					extensionMethods, ':');

			while (st.hasMoreChars()) {
				String em = st.nextToken();
				if (em.toUpperCase().equals(Request.BYE)
						|| em.toUpperCase().equals(Request.ACK)
						|| em.toUpperCase().equals(Request.OPTIONS))
					throw new IllegalArgumentException("Bad extension method "
							+ em);
				else
					this.addExtensionMethod(em.toUpperCase());
			}
		}

		/* Set the retransmission filter. For SIPLite this is always true */
		this.retransmissionFilter = true;

		String debugLog = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.LOG_FILE_NAME");
		if (debugLog != null)
			LogWriter.setLogFileName(debugLog);

		String logLevel = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.TRACE_LEVEL");

		if (logLevel != null) {
			try {
				int ll = Integer.parseInt(logLevel);
				if (ll == 32)
					LogWriter.needsLogging = true;
				ServerLog.setTraceLevel(ll);
			} catch (NumberFormatException ex) {
				System.out.println("WARNING Bad integer " + logLevel);
				System.out.println("logging dislabled ");
				ServerLog.setTraceLevel(0);
			}
		}

		String badMessageLog = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.BAD_MESSAGE_LOG");
		if (badMessageLog != null)
			super.badMessageLog = badMessageLog;

		String serverLog = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.SERVER_LOG");
		if (serverLog != null)
			ServerLog.setLogFileName(serverLog);
		ServerLog.checkLogFile();
		String maxConnections = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.MAX_CONNECTIONS");
		if (maxConnections != null) {
			try {
				this.maxConnections = Integer.parseInt(maxConnections);
			} catch (NumberFormatException ex) {
				System.out.println("max connections - bad value "
						+ ex.getMessage());
			}
		}
		String threadPoolSize = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.THREAD_POOL_SIZE");
		if (threadPoolSize != null) {
			try {
				this.threadPoolSize = Integer.parseInt(threadPoolSize);
			} catch (NumberFormatException ex) {
				System.out.println("thread pool size - bad value "
						+ ex.getMessage());
			}
		}

		String transactionTableSize = configurationProperties
				.getProperty("sip4me.gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS");
		if (transactionTableSize != null) {
			try {
				this.transactionTableSize = Integer
						.parseInt(transactionTableSize);
			} catch (NumberFormatException ex) {
				System.out.println("transaction table size - bad value "
						+ ex.getMessage());
			}
		}

	}

	/**
	 * Get the sip listener for the stack.
	 */
	public SipListener getSipListener() {
		return this.sipListener;
	}

	/**
	 * Creates a new peer ListeningPoint on this SipStack on a specified host,
	 * port and transport and returns a reference to the newly created
	 * ListeningPoint object. The newly created ListeningPoint is implicitly
	 * attached to this SipStack upon execution of this method, by adding the
	 * ListeningPoint to the {@link SipStack#getListeningPoints()} of this
	 * SipStack, once it has been successfully created.
	 * 
	 * @return The peer ListeningPoint attached to this SipStack.
	 * @param <var>port</var> the port of the new ListeningPoint.
	 * @param <var>transport</var> the transport of the new ListeningPoint.
	 *        SipStack.
	 */
	public synchronized ListeningPoint createListeningPoint(int port,
			String transport) throws IllegalArgumentException {

		if (transport == null)
			throw new NullPointerException("null transport");

		if (port <= 0)
			throw new IllegalArgumentException("bad port");

		if (!Utils.equalsIgnoreCase(transport, "UDP")
				&& !Utils.equalsIgnoreCase(transport, "TCP"))
			throw new IllegalArgumentException("Transport not supported: "
					+ transport);

		if (LogWriter.needsLogging)
			LogWriter.logMessage("createListeningPoint " + transport + " / "
					+ port);

		String key = ListeningPoint
				.makeKey(super.stackAddress, port, transport);

		ListeningPoint lip = (ListeningPoint) listeningPoints.get(key);
		if (lip != null) {
			return lip;
		} else {
			try {
				MessageProcessor messageProcessor = this
						.createMessageProcessor(port, transport);
				lip = new ListeningPoint(this, port, transport);
				lip.messageProcessor = messageProcessor;
				messageProcessor.setListeningPoint(lip);
				this.listeningPoints.put(key, lip);
				return lip;
			} catch (java.io.IOException ex) {
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
							"Exception creating listening point. "
									+ "Maybe port is in use? "
									+ ex.getMessage());
				throw new IllegalArgumentException(ex.getMessage());
			}
		}
	}

	/**
	 * Creates a new peer SipProvider on this SipStack on a specified
	 * ListeningPoint and returns a reference to the newly created SipProvider
	 * object. The newly created SipProvider is implicitly attached to this
	 * SipStack upon execution of this method, by adding the SipProvider to the
	 * {@link SipStack#getSipProviders()} of this SipStack, once it has been
	 * successfully created.
	 * 
	 * @return The peer SipProvider attached to this SipStack on the specified
	 *         ListeningPoint.
	 * @param <var>listeningPoint</var> the ListeningPoint the SipProvider is to
	 *        be attached to in order to send and Receive messages.
	 * @throws <var>ListeningPointUnavailableException</var> thrown if another
	 *         SipProvider is already using the ListeningPoint.
	 */
	public SipProvider createSipProvider(ListeningPoint listeningPoint)
			throws ObjectInUseException {
		if (listeningPoint == null)
			throw new NullPointerException("null listeningPoint");
		ListeningPoint listeningPointImpl = listeningPoint;
		if (listeningPointImpl.sipProviderImpl != null)
			throw new ObjectInUseException("Provider already attached!");

		SipProvider provider = new SipProvider(this);
		provider.addListeningPoint(listeningPointImpl);
		listeningPointImpl.sipProviderImpl = provider;
		this.sipProviders.addElement(provider);
		return provider;
	}

	/**
	 * Deletes the specified peer ListeningPoint attached to this SipStack. The
	 * specified ListeningPoint is implicitly detached from this SipStack upon
	 * execution of this method, by removing the ListeningPoint from the
	 * {@link SipStack#getListeningPoints()} of this SipStack.
	 * 
	 * @param <var>listeningPoint</var> the peer SipProvider to be deleted from
	 *        this SipStack.
	 * @exception <var>ObjectInUseException</var> thrown if the specified peer
	 *            ListeningPoint cannot be deleted because the peer
	 *            ListeningPoint is currently in use.
	 * 
	 * @since v1.1
	 */
	public void deleteListeningPoint(ListeningPoint listeningPoint)
			throws ObjectInUseException {
		if (listeningPoint == null)
			throw new NullPointerException("null listeningPoint arg");
		ListeningPoint lip = listeningPoint;
		// Stop the message processing thread in the listening point.
		if (lip.messageProcessor != null) {
			removeMessageProcessor(lip.messageProcessor);
			lip.messageProcessor = null;
		}
		String key = lip.getKey();
		if (key != null)
			this.listeningPoints.remove(key);
	}

	/**
	 * Deletes the specified peer SipProvider attached to this SipStack. The
	 * specified SipProvider is implicitly detached from this SipStack upon
	 * execution of this method, by removing the SipProvider from the
	 * {@link SipStack#getSipProviders()} of this SipStack. Deletion of a
	 * SipProvider does not automatically delete the ListeningPoint from the
	 * SipStack.
	 * 
	 * @param <var>sipProvider</var> the peer SipProvider to be deleted from
	 *        this SipStack.
	 * @exception <var>ObjectInUseException</var> thrown if the specified peer
	 *            SipProvider cannot be deleted because the peer SipProvider is
	 *            currently in use.
	 * 
	 */
	public void deleteSipProvider(SipProvider sipProvider)
			throws ObjectInUseException {

		if (sipProvider == null)
			throw new NullPointerException("null provider arg");
		if (LogWriter.needsLogging)
			LogWriter.logMessage("Deleting SIP provider " + sipProvider + " from Stack");
		sipProvider.stop();
		sipProvider.sipListener = null;
		sipProviders.removeElement(sipProvider);
		if (sipProviders.isEmpty() && listeningPoints.isEmpty()) {
			if (LogWriter.needsLogging)
				LogWriter
						.logMessage("No more SipProviders/Listening Points: stopping stack!");
			this.stopStack();
		}
	}

	/**
	 * Gets the IP Address that identifies this SipStack instance. Every Sip
	 * Stack object must have an IP Address and only a single SipStack object
	 * can service a single IP Address. This value is set using the properties
	 * object passed to the {@link SipFactory#createSipStack(ConfigurationProperties)} method
	 * upon creation of the SIP Stack object.
	 * 
	 * @return a string identifing the IP Address
	 * @since v1.1
	 */
	public String getIPAddress() {
		return super.getHostAddress();
	}

	/**
	 * Returns an Iterator of existing ListeningPoints created by this SipStack.
	 * All of the peer SipProviders of this SipStack will be proprietary objects
	 * belonging to the same stack vendor.
	 * 
	 * @return an Iterator containing all existing peer ListeningPoints created
	 *         by this SipStack. Returns an empty Iterator if no ListeningPoints
	 *         exist.
	 */
	public java.util.Enumeration getListeningPoints() {
		return this.listeningPoints.elements();
	}

	/**
	 * Get the listening point for a given transport and port.
	 * 
	 */
	public ListeningPoint getListeningPoint(int port, String transport) {
		String key = ListeningPoint
				.makeKey(super.stackAddress, port, transport);
		if (LogWriter.needsLogging)
			LogWriter.logMessage("getListeningPoint " + port + "/" + transport);
		return (ListeningPoint) listeningPoints.get(key);
	}

	/**
	 * get the outbound proxy specification. Return null if no outbound proxy is
	 * specified.
	 */
	public String getOutboundProxy() {
		return this.outboundProxy;
	}

	/**
	 * This method returns the value of the retransmission filter helper
	 * function for User Agent Client and User Agent Server applications. This
	 * value is set using the properties object passed to the
	 * {@link SipFactory#createSipStack(ConfigurationProperties)} method upon creation of the
	 * SIP Stack object.
	 * <p>
	 * The default value of the retransmission filter boolean is
	 * <var>false</var>. When retransmissions are handled by the SipProvider the
	 * application will not receive {@link Timeout#RETRANSMIT} notifications
	 * encapsulated in {@link javax.sip.TimeoutEvent}'s. However an application
	 * will get notified when a the underlying transaction expired with
	 * {@link Timeout#TRANSACTION} notifications encapsulated in a
	 * {@link javax.sip.TimeoutEvent}.
	 * </p>
	 * 
	 * @return the value of the retransmission filter, true if the filter is set
	 *         false otherwise.
	 * @since v1.1
	 */
	public boolean isRetransmissionFilterActive() {
		return this.retransmissionFilter;
	}

	/**
	 * Gets the Router object that identifies the default Routing policy of this
	 * SipStack. It also provides means to set an outbound proxy. This value is
	 * set using the properties object passed to the
	 * {@link SipFactory#createSipStack(ConfigurationProperties)} method upon creation of the
	 * SIP Stack object.
	 * 
	 * @return a the Router object identifying the Router policy.
	 * @since v1.1
	 */
	public Router getRouter() {
		return super.getRouter();
	}

	/**
	 * Returns an Iterator of existing peer SipProviders that have been created
	 * by this SipStack. All of the peer SipProviders of this SipStack will be
	 * proprietary objects belonging to the same stack vendor.
	 * 
	 * @return an Iterator containing all existing peer SipProviders created by
	 *         this SipStack. Returns an empty Iterator if no SipProviders
	 *         exist.
	 */
	public Enumeration getSipProviders() {
		return this.sipProviders.elements();
	}

	/**
	 * Gets the user friendly name that identifies this SipStack instance. This
	 * value is set using the properties object passed to the
	 * {@link SipFactory#createSipStack(ConfigurationProperties)} method upon creation of the
	 * SIP Stack object.
	 * 
	 * @return a string identifing the stack instance
	 */
	public String getStackName() {
		return this.stackName;
	}

	/**
	 * The default transport to use for via headers.
	 */
	protected String getDefaultTransport() {
		if (isTransportEnabled("udp"))
			return "udp";
		else if (isTransportEnabled("tcp"))
			return "tcp";
		else
			return null;
	}

	/**
	 * Invoked when an error has ocurred with a transaction.
	 * 
	 * @param transactionErrorEvent
	 *            Error event.
	 */
	public void transactionErrorEvent(
			SIPTransactionErrorEvent transactionErrorEvent) {
		Transaction transaction = transactionErrorEvent.getSource();

	}

}
