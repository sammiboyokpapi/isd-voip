/******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
******************************************************************************/
package sip4me.gov.nist.siplite.stack;


import java.io.IOException;
import java.util.Enumeration;

import sip4me.gov.nist.core.Host;
import sip4me.gov.nist.core.HostPort;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.ContactHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.message.Message;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;


/**
* Message channel abstraction for the SIP stack.
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*/

public abstract class MessageChannel
{

/** Message processor to whom I belong (if set).
*/
	protected MessageProcessor messageProcessor;

   /** Close the message channel.
   */
	
   public abstract void close();
/** Get the SIPMessageStack object from this message channel.
 * @return SIPMessageStack object of this message channel
 */    
	public abstract SIPMessageStack getSIPStack();

/** Get transport string of this message channel.
 * @return Transport string of this message channel.
 */        
	public abstract String 	getTransport();

/** Get whether this channel is reliable or not.
 * @return True if reliable, false if not.
 */
	public abstract boolean isReliable();

/** Return true if this is a secure channel.
*/
	public abstract boolean isSecure();

/** Send the message (after it has been formatted)
 * @param sipMessage Message to send.
 */        
	public abstract void sendMessage(Message sipMessage)
		throws IOException;

/** Get the peer address of the machine that sent us this message.
 * @return  a string contianing the ip address or host name of the sender
 *  of the message.
 */        
	public abstract String 	getPeerAddress();

/** Get the name of the machine that sent us this message.
 * @return  a string contianing the ip address or host name of the sender
 *  of the message.
	public abstract String 	getPeerName();
 */        

    /** Get the sender port ( the port of the other end that sent me 
    * the message).
    */
    public abstract int getPeerPort() ;

    /** 
     * Generate a key which identifies the message channel.
     * This allows us to cache the message channel.
     */
    public abstract String getKey();

    /** Get the host to assign for an outgoing Request via header.
     */
    public abstract String getViaHost();
	

   /** Get the port to assign for the via header of an outgoing message.
    */
    public abstract int getViaPort();
        
/** Send the message (after it has been formatted), to a specified
 * address and a specified port
 * @param message Message to send.
 * @param receiverAddress Address of the receiver.
 * @param receiverPort Port of the receiver.
 * @param reconnectFlag retry if connection was closed
 */        
   	protected abstract void  sendMessage(byte[] message, 
					  String receiverAddress,
					  int receiverPort, boolean reconnectFlag) throws IOException;


/** Get the host of this message channel.
 * @return host of this messsage channel.
 */        
	public  String 	getHost() {
		return this.getSIPStack().getHostAddress();
	}
		
/** Get port of this message channel.
 * @return Port of this message channel.
 */        
	public int getPort() {
		if (this.messageProcessor != null) 
			return messageProcessor.getPort();
		else return -1;
	}
    /** Handle an exception.
    */
    public abstract void handleException(SIPServerException ex);

    /** Send a message given SIP message.
     *@param sipMessage is the messge to send.
     *@param receiverAddress is the address to which we want to send
     *@param receiverPort is the port to which we want to send
     */
    public void sendMessage(Message sipMessage, 
    		String receiverAddress,
    		int receiverPort) throws IOException {
    	long time = System.currentTimeMillis();
    	byte[] bytes = sipMessage.encodeAsBytes();
    	sendMessage(bytes, receiverAddress, receiverPort, sipMessage instanceof Request);
    	logMessage(sipMessage, receiverAddress, receiverPort,time);
    }


    /** Get the hostport structure of this message channel.
    */
     public HostPort getHostPort() {
		HostPort retval  = new HostPort();
		retval.setHost(new Host(this.getHost()));
		retval.setPort(this.getPort());
		return retval;
    }

    /** 
    * Get the peer host and port.
    *
    *@return a HostPort structure for the peer.
    */
    public HostPort getPeerHostPort() {
	HostPort retval = new HostPort();
	retval.setHost(new Host(this.getPeerAddress()));
	retval.setPort(this.getPeerPort());
	return retval;
    }

    /** 
     * Get the Via header for this transport.
     * Note that this does not set a branch identifier.
     *
     * @return a via header for outgoing messages sent from this channel.
     */
    public ViaHeader getViaHeader() {
	ViaHeader	channelViaHeader;

	channelViaHeader = new ViaHeader();

	channelViaHeader.setTransport(getTransport());
	
	channelViaHeader.setSentBy(getHostPort());
	return channelViaHeader;
    }

    /** Get the via header host:port structure.
    * This is extracted from the topmost via header of the request.
    *
    * @return a host:port structure
    *
    */

    public HostPort getViaHostPort() {
	HostPort retval = new HostPort();
	retval.setHost(new Host(this.getViaHost()));
	retval.setPort(this.getViaPort());
	return retval;
    }


   
    
	

    /** Log a message sent to an address and port via the default interface.
    *@param sipMessage is the message to log.
    *@param address is the inet address to which the message is sent.
    *@param port    is the port to which the message is directed.
    */
    protected void logMessage (Message sipMessage,
	String address, int port, long time) {
	String firstLine = sipMessage.getFirstLine();
	CSeqHeader cseq = (CSeqHeader) sipMessage.getCSeqHeader();
	CallIdHeader callid = (CallIdHeader) sipMessage.getCallId();
	String cseqBody = cseq.encodeBody();
	String callidBody = callid.encodeBody();
	// Default port.
	if (port == -1) port = 5060;
	if (ServerLog.needsLogging(ServerLog.TRACE_MESSAGES)) {
	    Enumeration extList = sipMessage.getHeaders("NISTExtension");
	    String status = null;
	    if (extList != null && extList.hasMoreElements()) {
	         Header exthdr = null;
		 exthdr = (Header) extList.nextElement();
		 status = exthdr.getHeaderValue();
	    }
	    ServerLog.logMessage(sipMessage.encode(), 
			     this.getHost()+":"+this.getPort(),
			     address  + 
			     ":" + port, true, callidBody, 
			     firstLine, status, 
			     sipMessage.getTransactionId(), time);
	}
    }

   /** Log a response received at this message channel. 
    * This is used for processing incoming responses to a client transaction.
    *
    *@param receptionTime is the time at which the response was received.
    *@param status is the processing status of the message.
    *
    */
	
    public void 
	logResponse(Response sipResponse, 
	long receptionTime,
	String status) {
	try {
	  int peerport = getPeerPort();
	  if (peerport == 0 && sipResponse.getContactHeaders() != null) {
		ContactHeader contact = 
		(ContactHeader) sipResponse.getContactHeaders().getFirst();
		peerport = ((Address)contact.getAddress()).getPort();

	   }
           String from = getPeerAddress() + ":" + peerport;
	   String to = this.getHost() + ":" + getPort();
           ServerLog.logMessage(sipResponse,
                          from, to, status, false, receptionTime);
	} catch (RuntimeException ex) {
		ex.printStackTrace();
	}
   }


   /** Get the message processor.
    */
    public MessageProcessor getMessageProcessor() { 
		return this.messageProcessor;
    }



}
