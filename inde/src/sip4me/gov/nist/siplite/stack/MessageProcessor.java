package sip4me.gov.nist.siplite.stack;


import java.io.IOException;

import sip4me.gov.nist.core.Host;
import sip4me.gov.nist.core.HostPort;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.ListeningPoint;
import sip4me.gov.nist.siplite.header.ViaHeader;



/*
 * Enhancements contributed by Jeff Keyser.
 */
/**
 * This is the Stack abstraction for the active object that waits for messages
 * to appear on the wire and processes these messages by calling the
 * MessageFactory interface to create a ServerRequest or ServerResponse object.
 * The main job of the message processor is to instantiate message channels for
 * the given transport.
 * 
 * @version 1.2 $Revision: 1.3 $ $Date: 2010-03-23 15:36:34 $
 * 
 * @author M. Ranganathan <br/>
 * 
 */
public abstract class MessageProcessor implements Runnable  {
	
	/**
	 * A string containing the 0.0.0.0 IPv4 ANY address.
	 */
	protected static final String IN_ADDR_ANY = "0.0.0.0";

	/**
	 * A string containing the ::0 IPv6 ANY address.
	 */
	protected static final String IN6_ADDR_ANY = "::0";
	
    /**
     * My Sent by string ( which I use to set the outgoing via header)
     */
    private  String sentBy;

    private HostPort sentByHostPort;
    
	private boolean sentBySet;

	private ListeningPoint listeningPoint;
    
	int useCount;
	
	
	/** 
	 * Get the Via header to assign for this message processor.
	*/
	public ViaHeader getViaHeader() {
	 
		ViaHeader via = new ViaHeader();
		Host host = new Host();
		host.setHostname(this.getSIPStack().getHostAddress());
		via.setHost(host);
		via.setPort(this.getPort());
		via.setTransport(this.getTransport());
		return via;
	   
	}

	public void setListeningPoint(ListeningPoint listeningPoint) {
//		if (LogWriter.needsLogging)
//			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Setting Listening Point " + listeningPoint + " to MessageProcessor " + this + " stack " + this.getSIPStack());
		this.listeningPoint = listeningPoint;
	}

	public ListeningPoint getListeningPoint() {
		return this.listeningPoint;
	}
	
	/** Return true if there are pending messages to be processed
	* (which prevents the message channel from being closed).
	*/
	public boolean inUse() {
        return this.useCount != 0;
	}
	
    ////////////////////////////////////////////////////////////////////////////////////////
    // Abstract methods
    ///////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Get the transport string.
	 * @return A string that indicates the transport. 
	 * (i.e. "tcp" or "udp") 
	 */
	public abstract String getTransport();

	/**
	* Get the port identifier.
	*@return the port for this message processor. This is where you
        * receive messages.
	*/
	public abstract int getPort();

	/**
	* Get the SIP Stack.
	*@return the sip stack.
	*/
	public abstract SIPMessageStack getSIPStack();

	/**
	* Create a message channel for the specified host/port.
	*@return New MessageChannel for this processor.
	*/
	public abstract MessageChannel 
		createMessageChannel(HostPort targetHostPort) 
		throws IOException;
	/**
	* Create a message channel for the specified host/port.
	*@return New MessageChannel for this processor.
	*/
	public abstract 
	  MessageChannel createMessageChannel(String targetHost,
			int port) throws IOException;

	/** Start our thread.
	*/
	public abstract void start() throws IOException;

	/** Stop method.
	*/
	public abstract void stop();

	/** Default target port used by this processor.  This is
	 * 5060 for TCP / UDP
	 */
	public abstract int getDefaultTargetPort();

	/** Flags whether this processor is secure or not.
	 */
	public abstract boolean isSecure();

	
    /**
     * Maximum number of bytes that this processor can handle.
     */
    public abstract int getMaximumMessageSize();


}
