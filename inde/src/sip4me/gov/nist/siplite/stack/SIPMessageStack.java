/******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 ******************************************************************************/
package sip4me.gov.nist.siplite.stack;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import sip4me.gov.nist.core.Host;
import sip4me.gov.nist.core.HostPort;
import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.core.net.DefaultNetworkLayer;
import sip4me.gov.nist.core.net.NetworkLayer;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.address.Router;
import sip4me.gov.nist.siplite.address.SipURI;
import sip4me.gov.nist.siplite.header.RouteHeader;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;


/**
 * This class defines a SIP Stack. In order to build a SIP server (UAS/UAC or
 *  Proxy etc.) you need to extend this class and instantiate it in your
 *  application. After you have done so, call
 *  <a href="SIPMessageStack.html#createMessageProcessor">createMessageProcessor</a>
 *  to create message processors and then start these message processors to
 *  get the stack the process messages.
 *  This will start the necessary threads that wait for incoming SIP messages.
 *  A general note about the handler structures -- handlers are expected to
 *  returnResponse  for successful message processing and throw
 *  SIPServerException for unsuccessful message processing.
 *
 *@author M. Ranganathan <mranga@nist.gov>  <br/>
 *Acknowledgement: Marc Bednarek added code in support of firewall. Jeff
 *Keyser suggested that MessageProcessors be accessible and applications
 *should have control over message processors.
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * IPv6 Support added by Emil Ivov (emil_ivov@yahoo.com)<br/>
 * Network Research Team (http://www-r2.u-strasbg.fr))<br/>
 * Louis Pasteur University - Strasbourg - France<br/>
 *
 */

public abstract class SIPMessageStack {

    protected boolean tcpFlag;
    protected boolean udpFlag;

    /*
     * The socket factory. Can be overriden by applications that want direct access to the
     * underlying socket.
     */
    protected NetworkLayer networkLayer;


    protected String outboundProxy;
    
    protected int outboundPort;

    /** Flag that indicates that the stack is active.
     */
    protected boolean toExit;

    /** Bad message log.  The name of a file that stores bum messages for
     * debugging.
     */
    protected String badMessageLog;


    /** Internal flag for debugging
     */
    protected boolean debugFlag;

    /** Name of the stack.
     */
    protected String stackName;

    /** IP address of stack
     */
    protected String stackAddress; // My host address.
   
    /** Request factory interface (to be provided by the application)
     */
    protected SIPStackMessageFactory sipMessageFactory;

    /** Default UDP port (5060)
     */
    public static final int DEFAULT_PORT = 5060;

    /** Router to determine where to forward the request.
     */
    protected Router router;

    /** start a single processing thread for all UDP messages (otherwise, the
     * stack will start a new thread for each UDP message).
     */
    protected int threadPoolSize;

    /** max number of simultaneous connections.  */
    protected int maxConnections;

    /** A collection of message processors.  */
    private final Vector messageProcessors;

    /*
     * Read timeout on TCP incoming sockets -- defines the time between reads for after delivery
     * of first byte of message.
     */
    protected int readTimeout;
    
    // handle low-level networking
	protected IOHandler ioHandler;

    /** Log a bad message (invoked when a parse exception arises).
     *
     *@param message is a string that contains the bad message to log.
     */
    public void logBadMessage(String message) {
        if (badMessageLog != null)
            LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, message);

    }

    /**
     * Get the file name of the bad message log.
     *
     *@return the file where bad messages are logged.
     *
     */
    public String getBadMessageLog() {
        return this.badMessageLog;
    }


    /** Set the flag that instructs the stack to only start a single
     * thread for sequentially processing incoming udp messages (thus
     * serializing the processing).
     * Caution: If the user-defined function called by the
     * processing thread blocks, then the entire server will block.
     *This feature was requested by Lamine Brahimi (IBM Zurich).
     */
    public void setSingleThreaded() {
        this.threadPoolSize = 1;
    }

    /** Set the thread pool size for processing incoming UDP messages.
     * Limit the total number of threads for processing udp messages.
     * Caution: If the user-defined function called by the
     * processing thread blocks, then the entire server will block.
     */
    public void setThreadPoolSize(int size) {
        this.threadPoolSize = size;
    }


    /** Set the max # of simultaneously handled TCP connections.
     */
    public void setMaxConnections(int nconnections) {
        this.maxConnections = nconnections;
    }


    /** Get the default route string.
     *@param sipRequest is the request for which we want to compute
     *  the next hop.
     */
    public Enumeration getNextHop(Request sipRequest) {
        return router.getNextHops(sipRequest);
    }

   

    /**
     * Constructor for the stack. Registers the request and response
     * factories for the stack.
     * @param messageFactory User-implemented factory for processing
     * 		messages.
     * @param stackAddress -- IP address or host name of the stack.
     * @param stackName -- descriptive name for the stack.
     */

    public SIPMessageStack(SIPStackMessageFactory messageFactory,
                    String stackAddress,
                    String stackName) throws IllegalArgumentException {
        this();
        sipMessageFactory = messageFactory;
        if (stackAddress == null) {
            throw new IllegalArgumentException
                    ("stack Address not set");
        }

        // Set a descriptive name for the message trace logger.
        ServerLog.description = stackName;
        ServerLog.stackIpAddress = stackAddress;
    }

    /**
     * Set the server Request and response factories.
     *@param messageFactory User-implemented factory for processing
     *    messages.
     */
    public void setStackMessageFactory
            (SIPStackMessageFactory messageFactory) {
        sipMessageFactory = messageFactory;
    }

    /** Set the descriptive name of the stack.
     *@param stackName -- descriptive name of the stack.
     */
    public void setStackName(String stackName) {
        this.stackName = stackName;
        ServerLog.setDescription(stackName);
	ServerLog.stackIpAddress = stackAddress;
    }


    /** Get the Stack name.
     *
     *@return name of the stack.
     */
    public String getStackName() {
        return this.stackName;
    }


    /** Set my address.
     *@param stackAddress -- A string containing the stack address.
     */
    public void setHostAddress(String stackAddress) {
        if(    stackAddress.indexOf(':') != stackAddress.lastIndexOf(':')
            && stackAddress.trim().charAt(0) != '['
        )
            this.stackAddress = '[' + stackAddress + ']';
        else
            this.stackAddress = stackAddress;
    }

    /** Get my address.
     *@return hostAddress - my host address.
     */
    public String getHostAddress() {
        return this.stackAddress;
    }


    /** Get the default next hop from the router.
     */
    public Hop getNextHop() {
        return this.router.getOutboundProxy();

    }


    /**
     * get port of the message processor (based on the transport). If
     * multiple ports are enabled for the same transport then the first
     * one is retrieved.
     *
     *@param transport is the transport for which to get the port.
     *
     */
    public int getPort(String transport) throws IllegalArgumentException{
        synchronized (messageProcessors) {
            Enumeration it = messageProcessors.elements();
            while (it.hasMoreElements()) {
                MessageProcessor mp = (MessageProcessor) it.nextElement();
                if (Utils.equalsIgnoreCase(mp.getTransport(),transport))
                    return mp.getPort();
            }
           	throw new IllegalArgumentException
                    ("Transport not supported " + transport);
        }
    }

    /** Return true if a transport is enabled.
     *
     *@param transport is the transport to check.
     */
    public boolean isTransportEnabled(String transport) {
        synchronized (messageProcessors) {
            Enumeration it = messageProcessors.elements();
            while (it.hasMoreElements()) {
                MessageProcessor mp = (MessageProcessor) it.nextElement();
                if (Utils.equalsIgnoreCase(mp.getTransport(),transport))
                    return true;
            }
            return false;
        }
    }

    /** Return true if the transport is enabled for a given port.
     *
     *@param transport transport to check
     *@param port 	port to check transport at.
     */
    public boolean isTransportEnabled(String transport, int port) {
        synchronized (messageProcessors) {
            Enumeration it = messageProcessors.elements();
            while (it.hasMoreElements()) {
                MessageProcessor mp = (MessageProcessor) it.nextElement();
                if (Utils.equalsIgnoreCase(mp.getTransport(),transport) &&
                        mp.getPort() == port)
                    return true;
            }
            return false;
        }
    }


    /**
     * Default constructor.
     */
    public SIPMessageStack() {
        this.toExit = false;
        // Set an infinit thread pool size.
        this.threadPoolSize = -1;
        // Max number of simultaneous connections.
        this.maxConnections = -1;
        // Array of message processors.
        messageProcessors = new Vector();
        
        // Handle IO for this process.
        ioHandler = new IOHandler(this);
        
        this.readTimeout = -1;
    }


    /**
     * Generate a new SIPSeverRequest from the given Request. A
     * SIPServerRequest is generated by the application
     * SIPServerRequestFactoryImpl. The application registers the
     * factory implementation at the time the stack is initialized.
     * @param siprequest Request for which we want to generate
     * this SIPServerRequest.
     * @param msgchan Message channel for the request for which
     * we want to generate the SIPServerRequest
     * @return Generated SIPServerRequest.
     */
    protected SIPServerRequestInterface
            newSIPServerRequest(Request siprequest, MessageChannel msgchan) {
        return sipMessageFactory.newSIPServerRequest
                (siprequest, msgchan);
    }

    /**
     * Generate a new SIPSeverResponse from the given Response.
     * @param sipresponse Response from which the SIPServerResponse
     * is to be generated. Note - this just calls the factory interface
     * to do its work. The factory interface is provided by the user.
     * @param msgchan Message channel for the SIPServerResponse
     * @return SIPServerResponse generated from this SIP
     * Response
     */
    protected SIPServerResponseInterface
            newSIPServerResponse(Response sipresponse, 
		MessageChannel msgchan) {
        return sipMessageFactory.newSIPServerResponse
                (sipresponse, msgchan);
    }


    /** Set the router algorithm.
     *@param router A class that implements the Router interface.
     */
    public void setRouter(Router router) {
        this.router = router;
    }

    /** Get the router algorithm.
     * @return Router router
     */
    public Router getRouter() {
        return router;
    }


    /** Get the default route.
     */
    public Hop getDefaultRoute() {
        return this.router.getOutboundProxy();
    }


    /**
     * Get the route header for this hop.
     *
     *@return the route header for the hop.
     */
    public RouteHeader getRouteHeader(Hop hop) {
        HostPort hostPort = new HostPort();
        Host h = new Host(hop.getHost());
        hostPort.setHost(h);
        hostPort.setPort(hop.getPort());
        sip4me.gov.nist.siplite.address.SipURI uri = new SipURI();
        uri.setHostPort(hostPort);
        uri.setScheme("sip");
        try {
            uri.setTransportParam(hop.getTransport());
        } catch (ParseException ex) {
            InternalErrorHandler.handleException(ex);
        }
        Address address = new Address();
        address.setURI(uri);
        RouteHeader route = new RouteHeader();
        route.setAddress(address);
        return route;

    }

    /**
     * Get the route header corresponding to the default route.
     */
    public RouteHeader getDefaultRouteHeader() {
        if (router.getOutboundProxy() != null) {
            Hop hop = (router.getOutboundProxy());
            return getRouteHeader(hop);
        } else
            return null;
    }


    /** return the status of the toExit flag.
     *@return true if the stack object is alive and false otherwise.
     */
    public synchronized boolean isAlive() {
        return !toExit;
    }
    
    /**
     * Return the network layer (i.e. the interface for socket creation or the socket factory for
     * the stack).
     * 
     * @return -- the registered Network Layer.
     */
    public NetworkLayer getNetworkLayer() {
        if (networkLayer == null) {
            return DefaultNetworkLayer.SINGLETON;
        } else {
            return networkLayer;
        }
    }
    
    /**
     * Set the network layer 
     * 
     * @param nl a suitable implementation of NetworkLayer interface
     */
    public void setNetworkLayer(NetworkLayer nl) {
        networkLayer = nl;
    }


    /** Make the stack close all accept connections and return. This
     * is useful if you want to start/stop the stack several times from
     * your application. Caution : use of this function could cause
     * peculiar bugs as messages are prcessed asynchronously by the stack.
     */

    public void stopStack() {
		if (LogWriter.needsLogging)
			LogWriter.logMessage("Stopping SIPMessageStack!" + this);
		
		synchronized (this.messageProcessors) {
			// Threads must periodically check this flag.
			this.toExit = true;
			for (int i = 0; i < messageProcessors.size(); i++) {
				MessageProcessor mp = (MessageProcessor) messageProcessors
						.elementAt(i);
				mp.stop();
				mp.setListeningPoint(null);

			}
			messageProcessors.removeAllElements();
		}
		ioHandler.closeAll();
		if (LogWriter.needsLogging)
			LogWriter.logMessage("SIPMessageStack stopped" + this);
    }

	/**
	 * Adds a new MessageProcessor to the list of running processors for this
	 * SIPMessageStack and starts it. You can use this method for dynamic stack
	 * configuration. Acknowledgement: This code is contributed by Jeff Keyser.
	 */
	public void addMessageProcessor(MessageProcessor newMessageProcessor)
			throws IOException {
		if (LogWriter.needsLogging)
			LogWriter.logMessage("addMessageProcessor "
					+ newMessageProcessor.getPort() + " / "
					+ newMessageProcessor.getTransport());
		synchronized (messageProcessors) {
			messageProcessors.addElement(newMessageProcessor);
		}
		newMessageProcessor.start();
	}

	/**
	 * Removes a MessageProcessor from this SIPMessageStack. Acknowledgement:
	 * Code contributed by Jeff Keyser.
	 * 
	 * @param oldMessageProcessor
	 */
	public void removeMessageProcessor(MessageProcessor oldMessageProcessor) {
		synchronized (messageProcessors) {
			if (messageProcessors.removeElement(oldMessageProcessor)) {
				oldMessageProcessor.stop();
				oldMessageProcessor.setListeningPoint(null);
				if (LogWriter.needsLogging)
					LogWriter
							.logMessage("Stopped and removed MessageProcessor "
									+ oldMessageProcessor);
			}
		}
	}


    /**
     * Gets an array of running MessageProcessors on this SIPMessageStack.
     *Acknowledgement: Jeff Keyser suggested that applications should
     * have access to the running message processors and contributed
     * this code.
     *@return an array of running message processors.
     *
     */
    public Vector
            getMessageProcessors() {
        
           return  messageProcessors;
          
        
    }

    /**
     * Get a message processor for the given transport.
     */
    public MessageProcessor getMessageProcessor(String transport) {
        synchronized (messageProcessors) {
            Enumeration it = messageProcessors.elements();
            while (it.hasMoreElements()) {
                MessageProcessor mp = (MessageProcessor) it.nextElement();
                if (Utils.equalsIgnoreCase(mp.getTransport(),transport))
                    return mp;
            }
            return null;
        }
    }


    /** Creates the equivalent of a JAIN listening point and attaches
     * to the stack.
     */

    public MessageProcessor createMessageProcessor(int port, String transport) 
    		throws java.io.IOException,IllegalArgumentException {
	if (LogWriter.needsLogging) 
		LogWriter.logMessage("createMessageProcessor : " +
		port + " / " + transport);
	
        if (Utils.equalsIgnoreCase(transport,"udp")) {
            UDPMessageProcessor
                    udpMessageProcessor =
                    new UDPMessageProcessor(this,  port);
            this.addMessageProcessor(udpMessageProcessor);
            this.udpFlag = true;
            return udpMessageProcessor;
        } else if (Utils.equalsIgnoreCase(transport,"tcp")) {
            TCPMessageProcessor
                    tcpMessageProcessor =
                    new TCPMessageProcessor(this,  port);
            this.addMessageProcessor(tcpMessageProcessor);
            this.tcpFlag = true;
            return tcpMessageProcessor;
        } else {
            throw new IllegalArgumentException("bad transport");
        }

    }

    /** Set the message factory.
     *
     *@param messageFactory -- messageFactory to set.
     */
    protected
            void setMessageFactory(SIPStackMessageFactory messageFactory) {
        this.sipMessageFactory = messageFactory;
    }


    /**
     *  Creates a new MessageChannel for a given Hop.
     *
     *  @param nextHop Hop to create a MessageChannel to.
     *
     *  @return A MessageChannel to the specified Hop, or null if
     *  no MessageProcessors support contacting that Hop.
     *
     *  @throws UnknwonHostException If the host in the Hop doesn't
     *  exist.
     */
	public MessageChannel createMessageChannel(Hop nextHop) {
		Host targetHost;
		HostPort targetHostPort;
		MessageProcessor nextProcessor;
		MessageChannel newChannel;

		// Create the host/port of the target hop
		targetHost = new Host();
		targetHost.setHostname(nextHop.getHost());
		targetHostPort = new HostPort();
		targetHostPort.setHost(targetHost);
		targetHostPort.setPort(nextHop.getPort());

		// Search each processor for the correct transport
		newChannel = null;
		synchronized (messageProcessors) {
			Enumeration processorIterator = messageProcessors.elements();
			while (processorIterator.hasMoreElements() && newChannel == null) {
				nextProcessor = (MessageProcessor) processorIterator.nextElement();
				// If a processor that supports the correct
				// transport is found,
				if (Utils.equalsIgnoreCase(nextHop.getTransport(), nextProcessor
						.getTransport())) {
					try {
						if (LogWriter.needsLogging)
							LogWriter.logMessage("createMessageChannel " + nextHop + " processor " + nextProcessor);
						// Create a channel to the target
						// host/port
						newChannel = nextProcessor
						.createMessageChannel(targetHostPort);
					} catch (IOException e) {
						e.printStackTrace();
						// Ignore channel creation error -
						// try next processor
					}
				}
			}
			// Return the newly-created channel
		}
		return newChannel;
	}

}
