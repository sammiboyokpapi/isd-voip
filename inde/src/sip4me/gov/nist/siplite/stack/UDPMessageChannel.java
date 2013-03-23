/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 ******************************************************************************/

package sip4me.gov.nist.siplite.stack;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.SocketConnection;

import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.FromHeader;
import sip4me.gov.nist.siplite.header.RequestLine;
import sip4me.gov.nist.siplite.header.StatusLine;
import sip4me.gov.nist.siplite.header.ToHeader;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.header.ViaList;
import sip4me.gov.nist.siplite.message.Message;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.parser.ParseExceptionListener;
import sip4me.gov.nist.siplite.parser.StringMsgParser;


/*
 * Kim Kirby (Keyvoice) suggested that duplicate checking should be added to the
 * stack (later removed). Lamine Brahimi suggested a single threaded behavior
 * flag be added to this. Niklas Uhrberg suggested that thread pooling support
 * be added to this for performance and resource management. Peter Parnes found
 * a bug with this code that was sending it into an infinite loop when a bad
 * incoming message was parsed. Bug fix by viswashanti.kadiyala@antepo.com.
 * Hagai Sela added fixes for NAT traversal. Jeroen van Bemmel fixed up for
 * buggy clients (such as windows messenger) and added code to return
 * BAD_REQUEST. David Alique fixed an address recording bug. Jeroen van Bemmel
 * fixed a performance issue where the stack was doing DNS lookups (potentially
 * unnecessary). Ricardo Bora (Natural Convergence ) added code that prevents
 * the stack from exiting when an exception is encountered.
 * 
 */


/**
 * This is the UDP Message handler that gets created when a UDP message needs to
 * be processed. The message is processed by creating a String Message parser
 * and invoking it on the message read from the UDP socket. The parsed structure
 * is handed off via a SIP stack request for further processing. This stack
 * structure isolates the message handling logic from the mechanics of sending
 * and receiving messages (which could be either udp or tcp).
 * 
 * 
 * @author M. Ranganathan <br/>
 * 
 * 
 * 
 * @version 1.2 $Revision: 1.4 $ $Date: 2010-09-15 14:12:48 $
 */

public class UDPMessageChannel
extends  MessageChannel
implements ParseExceptionListener, Runnable {
    
    
    /** SIP Stack structure for this channel.
     */
    protected SIPMessageStack stack;
    
    /** Sender address (from getPeerName())
     */
    private final String myAddress;
    private final int myPort;
    
	/**
	 * The parser we are using for messages received from this channel.
	 */
	protected StringMsgParser myParser;
    
    private String peerAddress;
    private int peerPort;
    private String peerProtocol;    
	private int peerPacketSourcePort;
	private String peerPacketSourceAddress;
        
    private Datagram incomingPacket;
    private long receptionTime;

	private String key;

    protected static Hashtable duplicates;

    
	/**
	 * Constructor of a channel for the thread pool.
	 * 
	 * @param stack is the shared SIPStack structure
	 * @param messageProcessor is the creating message processor.
	 */
	public UDPMessageChannel(SIPMessageStack stack,
			UDPMessageProcessor messageProcessor) {
		super.messageProcessor = messageProcessor;
		this.stack = stack;
		this.myAddress = messageProcessor.getSIPStack().getHostAddress();
		this.myPort = messageProcessor.getPort();
		
		Thread mythread = new Thread(this, "UDPMessageChannelThread");
		mythread.start();

	}
    
    /**
     * Constructor - takes a datagram packet and a stack structure
     * Extracts the address of the other from the datagram packet and
     * stashes away the pointer to the passed stack structure.
     * @param packet   is the UDP Packet that contains the request.
     * @param sipStack stack is the shared SipStack structure
     * @param notifier Channel notifier (not very useful for UDP).
     *
     */
    public UDPMessageChannel(Datagram packet,
    SIPMessageStack sipStack, MessageProcessor messageProcessor ) {
        incomingPacket = packet;
        stack = sipStack;
        this.messageProcessor = messageProcessor;

		myAddress = sipStack.getHostAddress();
		myPort = messageProcessor.getPort();
		
		Thread mythread = new Thread(this, "UDPMessageChannelThread");
		mythread.start();
        
    }

	/**
	 * Constructor. We create one of these when we send out a message.
	 * 
	 * @param targetAddr
	 *            internet address of the place where we want to send messages.
	 *@param port
	 *            target port (where we want to send the message).
	 *@param stack
	 *            our SIP Stack.
	 */
	public UDPMessageChannel(String targetAddr, int port,
    SIPMessageStack sipStack, UDPMessageProcessor processor) {
    	if(LogWriter.needsLogging)
            LogWriter.logMessage(LogWriter.TRACE_DEBUG, "DEBUG, UDPMessageChannel, UDPMessageChannel(),"+
                    " Creating message channel on " +targetAddr + "/" + port);
        
        stack = sipStack;
        this.peerPort = port;
        this.peerAddress = targetAddr;
        this.messageProcessor = processor;
        this.myPort = processor.getPort();
        this.myAddress = sipStack.getHostAddress();
        this.peerProtocol = "UDP";
                
    }
    

    
    /**
     * Run method specified by runnable.
     */
    public void run() {

		while (true) {
			// Create a new string message parser to parse the list of messages.
			if (myParser == null) {
				myParser = new StringMsgParser();
				myParser.setParseExceptionListener(this);
			}
			
			// messages that we write out to the peer.
			Datagram packet;

			if (stack.threadPoolSize != -1) {
				synchronized (((UDPMessageProcessor) messageProcessor).messageQueue) {
					while (((UDPMessageProcessor) messageProcessor).messageQueue
							.isEmpty()) {
						
						// Check to see if we need to exit.
						if (!((UDPMessageProcessor) messageProcessor).running)
							return;
						
						try {
							// Wait for packets
							((UDPMessageProcessor) messageProcessor).messageQueue.wait();
						} catch (InterruptedException ex) {
							if (!((UDPMessageProcessor) messageProcessor).running)
								return;
						}
					}
					packet = (Datagram) ((UDPMessageProcessor) messageProcessor).messageQueue.firstElement();
					((UDPMessageProcessor) messageProcessor).messageQueue.removeElementAt(0);
				}
				incomingPacket = packet;
			} else {
				packet = incomingPacket;
			}

			if (LogWriter.needsLogging) {
				LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Processing new incoming datagram " + packet.getLength());
			}
			// Process the packet. Catch and log any exception we may throw.
			try {
				processIncomingDataPacket(packet);
			} catch (Exception e) {
				if (LogWriter.needsLogging) {
					LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "Exception processing incoming UDP packet");
					LogWriter.logException(e);
				}
			}

			if (stack.threadPoolSize == -1) {
				return;
			}
		}

    }
    

	/**
	 * Process an incoming datagram
	 * 
	 * @param packet
	 *            is the incoming datagram packet.
	 */
	private void processIncomingDataPacket(Datagram packet) throws Exception {
		
        // For a request first via header tells where the message
        // is coming from.
        // For response, just get the port from the packet.
        // format: address:port
        String address = packet.getAddress();
        try {
            int firstColon = address.indexOf("://");
            int secondColon = address.indexOf(":", firstColon+1);
            this.peerAddress = address.substring(firstColon+3,secondColon);
            if (LogWriter.needsLogging)
            	LogWriter.logMessage(LogWriter.TRACE_DEBUG, "UDPMessageChannel, run(), sender address:"+ peerAddress);
            String senderPortString=address.substring(secondColon+1);
            this.peerPacketSourcePort = Integer.parseInt(senderPortString);
            if (LogWriter.needsLogging)
            	LogWriter.logMessage(LogWriter.TRACE_DEBUG, "UDPMessageChannel, run(), sender port:"+ peerPacketSourcePort);
        }
        catch(NumberFormatException e) {
        	
        	if (LogWriter.needsLogging)
            	LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "UDPMessageChannel, run(), exception raised: " + e.getMessage());
        	e.printStackTrace();
            peerPacketSourcePort =-1;
        }

		int packetLength = packet.getLength();
		// Read bytes and put it in a queue.
		byte[] msgBytes = packet.getData();


		// Do debug logging.
		if (LogWriter.needsLogging) {
			LogWriter.logMessage(LogWriter.TRACE_DEBUG,
					"UDPMessageChannel: processIncomingDataPacket : peerAddress = "
							+ peerAddress + "/"
							+ peerPacketSourcePort + " Length = " + packetLength + " msgBytes " + msgBytes);
		}

		Message sipMessage = null;
		try {
			receptionTime = System.currentTimeMillis();
			sipMessage = myParser.parseSIPMessage(msgBytes);
			myParser = null;
		} catch (ParseException ex) {
			myParser = null; // let go of the parser reference.
			if (LogWriter.needsLogging) {
				LogWriter.logMessage(LogWriter.TRACE_DEBUG,
						"Rejecting message !  " + new String(msgBytes));
				LogWriter.logMessage(LogWriter.TRACE_DEBUG, "error message "
						+ ex.getMessage());
				LogWriter.logException(ex);
			}
			
			// TODO: do this on TCP too
			// JvB: send a 400 response for requests (except ACK)
			String msgString = new String(msgBytes, 0, packetLength);
			if (!msgString.startsWith("SIP/") && !msgString.startsWith("ACK ")) {

				String badReqRes = create400Response(msgString, ex);
				if (badReqRes != null) {
					if (LogWriter.needsLogging)
						LogWriter.logMessage(LogWriter.TRACE_DEBUG, 
								"Sending automatic 400 Bad Request: " + badReqRes);
					try {
						this.sendMessage(badReqRes.getBytes(), peerAddress,
								peerPacketSourcePort, "UDP", false);
					} catch (IOException e) {
						LogWriter.logException(e);
					}
				} else {
					if (LogWriter.needsLogging)
						LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Could not formulate automatic 400 Bad Request");
				}
			}

			return;
		}
		// No parse exception but null message - reject it and
		// march on (or return).
		// exit this message processor if the message did not parse.
		if (sipMessage == null) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Rejecting message !  + Null message parsed.");
			return;
		}
		
		
		ViaList viaList = sipMessage.getViaHeaders();
		
        // Check for the required headers.
        if (sipMessage.getFromHeader()   == null  	 	||
        //sipMessage.getFromHeader().getTag() == null  ||
        sipMessage.getTo()     == null  	 	||
        sipMessage.getCallId() == null 	 	||
        sipMessage.getCSeqHeader()   == null 	 	||
        sipMessage.getViaHeaders()    == null
        ){
            String badmsg = new String(msgBytes);
            if (LogWriter.needsLogging)  {
                LogWriter.logMessage("bad message " + badmsg);
                LogWriter.logMessage(">>> Dropped Bad Msg " +
                "FromHeader = " + sipMessage.getFromHeader() 	+
                "ToHeader = " + sipMessage.getTo() 		+
                "CallId = " + sipMessage.getCallId() 	+
                "CSeqHeader = " + sipMessage.getCSeqHeader() 	+
                "Via = " + sipMessage.getViaHeaders() 	);
            }
            
            stack.logBadMessage(badmsg);
            return;
            
        }
		

		// For a request first via header tells where the message
        // is coming from.
        // For response, just get the port from the packet.
        if (sipMessage instanceof Request) {

        	ViaHeader v = (ViaHeader)viaList.first();
        	if (v.hasPort() )
        		this.peerPort = v.getPort();
        	else 
        		this.peerPort = SIPMessageStack.DEFAULT_PORT;

        	this.peerProtocol = v.getTransport();

        	boolean hasRPort = v.hasParameter(ViaHeader.RPORT);
        	// Be warned, the host comparison may fail if socket.getAddress()
        	// returns a domain name as the Via Host will be a numeric IP.
        	// FIXME: No idea. Doing a DNS lookup or reverse DNS lookup 
        	// can be misleading because they can be non-matching, that is,
        	// DNS(peerAddressName) != ReverseDNS(peerAddressIP)
        	if (hasRPort || !this.peerAddress.equals(v.getHost())) {
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "WARNING! \"Received\" parameter " +
							"has been temporarily disabled. Response will be sent to topmost Via Host: " + v.getHost());
				this.peerAddress = v.getHost();
//                if (LogWriter.needsLogging) 
//	                   LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Adding \"received\" parameter" +
//	                   		" to incoming request with value: " + peerAddress + 
//	                   		" because it doesn't match the Via host " + v.getHost());
//				v.setParameter(ViaHeader.RECEIVED, this.peerAddress);
        	}

        	if (hasRPort) {
        		v.setParameter(ViaHeader.RPORT, Integer.toString(peerPacketSourcePort));
        		this.peerPort = peerPacketSourcePort;
        	}

        } else {
        	this.peerPort = this.peerPacketSourcePort;
        	this.peerProtocol = ((ViaHeader) viaList.getFirst()).getTransport();
        }

        processMessage(sipMessage);

	}
	
	/**
	 * Actually process the parsed SIP message.
	 * 
	 * @param sipMessage
	 */
	public void processMessage(Message sipMessage) {

		if (sipMessage instanceof Request) {
			Request sipRequest = (Request) sipMessage;

			// This is a request - process it.                
			SIPServerRequestInterface sipServerRequest =
				stack.newSIPServerRequest(sipRequest,this);
			// Drop it if there is no request returned
			if (sipServerRequest == null) {
				if (LogWriter.needsLogging) {
					LogWriter.logMessage
					("Null request interface returned");
				}
				return;
			}
			try {
				if (LogWriter.needsLogging)
					LogWriter.logMessage("About to process " +
							sipRequest.getFirstLine() + "/" +
							sipServerRequest);
				sipServerRequest.processRequest(sipRequest,this);
				if (LogWriter.needsLogging)
					LogWriter.logMessage("Done processing " +
							sipRequest.getFirstLine() + "/" +
							sipServerRequest);

				// So far so good -- we will commit this message if
				// all processing is OK.
				if ( ServerLog.needsLogging(ServerLog.TRACE_MESSAGES)) {
					if (sipServerRequest.getProcessingInfo() == null) {
						ServerLog.logMessage(sipMessage,
								sipRequest.getViaHost() + ":" +
								sipRequest.getViaPort(),
								stack.getHostAddress()  + ":" +
								stack.getPort(this.getTransport()),false,
								new Long(receptionTime).toString());
					} else {
						ServerLog.logMessage(sipMessage,
								sipRequest.getViaHost() + ":" +
								sipRequest.getViaPort(),
								stack.getHostAddress()  + ":" +
								stack.getPort(this.getTransport()),
								sipServerRequest.getProcessingInfo(),
								false,new Long(receptionTime).toString());
					}
				}
			} catch (SIPServerException ex) {

				if ( ServerLog.needsLogging(ServerLog.TRACE_MESSAGES)) {
					ServerLog.logMessage(sipMessage,
							sipRequest.getViaHost() + ":" +
							sipRequest.getViaPort(),
							stack.getHostAddress()  + ":" +
							stack.getPort(this.getTransport()),
							ex.getMessage(), false,
							new Long(receptionTime).toString());
				}
				handleException(ex);
			}

		} else {

			// Handle a SIP Response message.
			Response sipResponse = (Response) sipMessage;
			SIPServerResponseInterface sipServerResponse =
				stack.newSIPServerResponse(sipResponse,this);
			try {
				if (sipServerResponse != null) {
					sipServerResponse.processResponse(sipResponse,this);
					// Normal processing of message.
				} else {
					if (LogWriter.needsLogging) {
						LogWriter.logMessage("null sipServerResponse!");
					}
				}

			} catch (SIPServerException ex) {
				if (ServerLog.needsLogging
						(ServerLog.TRACE_MESSAGES)){
					this.logResponse(sipResponse,
							receptionTime,
							ex.getMessage()+ "-- Dropped!");
				}

				ServerLog.logException(ex);
			}
		}

	}
    
    /** Return a reply from a pre-constructed reply. This sends the message
     * back to the entity who caused us to create this channel in the
     * first place.
     * @param msg Message string to send.
     * @throws IOException If there is a problem with sending the
     * message.
     */
    public void sendMessage(Message sipMessage) throws IOException {		
        byte[] msg = sipMessage.encodeAsBytes();		
        sendMessage(msg, peerAddress, peerPort, peerProtocol, sipMessage instanceof Request);		
    }
    
    
    /** Send a message to a specified receiver address.
     * @param msg message string to send.
     * @param receiverAddress Address of the place to send it to.
     * @param receiverPort the port to send it to.
     * @throws IOException If there is trouble sending this message.
     */
    protected void sendMessage(byte[] msg, String receiverAddress,
			int receiverPort, boolean reconnect) throws IOException {
        // msg += "\r\n\r\n";
        // Via is not included in the request so silently drop the reply.
        if (receiverPort == -1) {
        	if(LogWriter.needsLogging)
	            LogWriter.logMessage("DEBUG, UDPMessageChannel, sendMessage(),"+
	            " The message is not sent: the receiverPort=-1");
            throw new IOException("Receiver port not set ");
        }
        
        

		try {
			DatagramConnection socket;
			boolean created = false;

			if (stack.udpFlag) {
				// Use the socket from the message processor (for firewall
				// support use the same socket as the message processor
				// socket -- feature request # 18 from java.net). This also
				// makes the whole thing run faster!
				socket = ((UDPMessageProcessor) messageProcessor).dc;

			} else {
				// bind to any interface and port.
	            // format: datagram://address:port
	            String url = "datagram://" + peerAddress + ":" + peerPort;
	            socket =(DatagramConnection) Connector.open(url);
				created = true;
			}
			
			Datagram reply = socket.newDatagram(msg, msg.length);
			socket.send(reply);
			if (created)
				socket.close();
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			InternalErrorHandler.handleException(ex);
		}
        
    }
    /** Send a message to a specified receiver address.
     * @param msg message string to send.
     * @param receiverAddress Address of the place to send it to.
     * @param receiverPort the port to send it to.
     * @param receiverProtocol protocol to use to send.
     * @param retry try if connection was not successful at fi
     * @throws IOException If there is trouble sending this message.
     */
    protected void sendMessage(byte[] msg, String receiverAddress,
    int receiverPort, String receiverProtocol, boolean retry) throws IOException {
        // msg += "\r\n\r\n";
        // Via is not included in the request so silently drop the reply.
        if (receiverPort == -1) {
        	if(LogWriter.needsLogging)
	            LogWriter.logMessage("DEBUG, UDPMessageChannel, sendMessage(),"+
	            " The message is not sent: the receiverPort=-1");
            throw new IOException("Receiver port not set ");
        }
        if (Utils.compareToIgnoreCase(receiverProtocol,"UDP") == 0) {
            

    		try {
    			DatagramConnection socket;
    			Datagram reply;
    			boolean created = false;

    			if (stack.udpFlag) {
    				// Use the socket from the message processor (for firewall
    				// support use the same socket as the message processor
    				// socket -- feature request # 18 from java.net). This also
    				// makes the whole thing run faster!
    				socket = ((UDPMessageProcessor) messageProcessor).dc;
    				
    				// ArnauVP: this has the problem that the datagram created from this (inbound) connection
    				// doesn't have an address assigned. Let's do it.
	    			reply = socket.newDatagram(msg, msg.length);
	    			reply.setAddress("datagram://" + peerAddress + ":" + peerPort);


    			} else {
    				// bind to any interface and port.
    	            // format: datagram://address:port
					socket = stack.getNetworkLayer().createDatagramSocket(peerAddress, peerPort);
	    			reply = socket.newDatagram(msg, msg.length);
    				created = true;
    			}
            	if(LogWriter.needsLogging)
    	            LogWriter.logMessage(LogWriter.TRACE_DEBUG, "UDPMessageChannel, sendMessage(),"+
    	            " Sending message over UDP, using socket " + socket + " created " + created + 
    	            " destination " + reply.getAddress());
    			socket.send(reply);
    			if (created)
    				socket.close();
    		} catch (IOException ex) {
    			throw ex;
    		} catch (Exception ex) {
    			InternalErrorHandler.handleException(ex);
    		}

        	
        } else {
        	// Use TCP to talk back to the sender.
			SocketConnection outputSocket = stack.ioHandler.sendBytes(
					peerAddress, peerPort, "tcp", msg, retry);
        	OutputStream myOutputStream = stack.ioHandler.getSocketOutputStream(outputSocket);
        	myOutputStream.write(msg, 0, msg.length);
        	myOutputStream.flush();
        	// The socket is cached (don't close it!);
        }
    }
    
    /** get the stack pointer.
     * @return The sip stack for this channel.
     */
    public SIPMessageStack getSIPStack() {
        return stack;
    }
    
    /**
     * Return a transport string.
     * @return the string "udp" in this case.
     */
    
    public String getTransport() {
        return "udp";
    }
    
    /**
     * get the stack address for the stack that received this message.
     * @return The stack address for our stack.
     */
    public String getHost() {
        return stack.stackAddress;
    }
    /** get the port.
     * @return Our port (on which we are getting datagram
     * packets).
     */
    public int getPort() {
        return this.myPort;
    }
    
    
    /** Handle an exception - construct a sip reply and send it back to the
     * caller.
     * @param ex The exception thrown at us by our
     * application.
     */
    public void handleException(SIPServerException ex) {
        // Return a parse error message to the client on the other end
        // if he is still alive.
        // ex.printStackTrace();
        int rc = ex.getRC();
        Request request = (Request) ex.getSIPMessage();
        Response response;
        String msgString = ex.getMessage();
        if (rc != 0) {
            response = request.createResponse(rc,msgString);
            // messageFormatter.newResponse(rc,request,msgString);
            try {
                sendMessage(response);
            } catch (IOException ioex) {
                ServerLog.logException(ioex);
            }
        }  else {
            // Assume that the message has already been formatted.
            try {
                sendMessage(msgString);
            } catch (IOException ioex) {
                ServerLog.logException(ioex);
            }
        }
    }
    
    
    /** Compare two UDP Message channels for equality.
     *@param other The other message channel with which to compare oursleves.
     */
    public boolean equals(Object other) {
        if (other == null ) return false;
        boolean retval;
        if (!this.getClass().equals(other.getClass())) {
            retval =  false;
        } else {
            UDPMessageChannel that = (UDPMessageChannel) other;
            retval =  this.peerAddress.equals(that.peerAddress);
        }
        
        return retval;
    }
    
    
    
    public String getKey() {
		if (key == null) {
    		this.key = "UDP" + ":" + stack.ioHandler.makeKey(peerAddress, peerPort);
		}	
		return key;
	}

    
	/**
	 * Creates a response to a bad request (ie one that causes a ParseException)
	 * 
	 * @param badReq
	 * @return message bytes, null if unable to formulate response
	 */
	private final String create400Response(String badReq, ParseException pe) {

		StringBuffer buf = new StringBuffer(512);
		buf.append("SIP/2.0 400 Bad Request (" + pe.getMessage() + ')');

		// We need the following headers: all Vias, CSeq, Call-ID, From, To
		if (!copyViaHeaders(badReq, buf))
			return null;
		if (!copyHeader(CSeqHeader.NAME, badReq, buf))
			return null;
		if (!copyHeader(CallIdHeader.NAME, badReq, buf))
			return null;
		if (!copyHeader(FromHeader.NAME, badReq, buf))
			return null;
		if (!copyHeader(ToHeader.NAME, badReq, buf))
			return null;

		// Should add a to-tag if not already present...
		int toStart = buf.toString().indexOf(ToHeader.NAME);
		if (toStart != -1 && buf.toString().indexOf("tag", toStart) == -1) {
			buf.append(";tag=badreq");
		}
		return buf.toString();
	}
	
	

	/**
	 * Copies a header from a request
	 * 
	 * @param name
	 * @param fromReq
	 * @param buf
	 * @return
	 * 
	 * Note: some limitations here: does not work for short forms of headers, or
	 * continuations; problems when header names appear in other parts of the
	 * request
	 */
	private static final boolean copyHeader(String name, String fromReq,
			StringBuffer buf) {
		int start = fromReq.indexOf(name);
		if (start != -1) {
			int end = fromReq.indexOf("\r\n", start);
			if (end != -1) {
				// XX Assumes no continuation here...
				buf.append(fromReq.substring(start - 2, end)); // incl CRLF
				// in front
				return true;
			}
		}
		return false;
	}

	/**
	 * Copies all via headers from a request
	 * 
	 * @param fromReq
	 * @param buf
	 * @return
	 * 
	 * Note: some limitations here: does not work for short forms of headers, or
	 * continuations
	 */
	private static final boolean copyViaHeaders(String fromReq, StringBuffer buf) {
		int start = fromReq.indexOf(ViaHeader.NAME);
		boolean found = false;
		while (start != -1) {
			int end = fromReq.indexOf("\r\n", start);
			if (end != -1) {
				// XX Assumes no continuation here...
				buf.append(fromReq.substring(start - 2, end)); // incl CRLF
				// in front
				found = true;
				start = fromReq.indexOf(ViaHeader.NAME, end);
			} else {
				return false;
			}
		}
		return found;
	}
    
    /**
	 * Used for sending responses in case of internal exception 
	 * when handling requests
	 * 
	 * @param msg
	 * @throws IOException
	 */
    private void sendMessage(String msg) throws IOException {
		sendMessage(msg.getBytes(), peerAddress, peerPort, peerProtocol, false);
	}
    
    /** Get the logical originator of the message (from the top via header).
     *@return topmost via header sentby field
     */
    public String getViaHost() {
        return this.myAddress;
    }
    
    /** Get the logical port of the message orginator (from the top via hdr).
     *@return the via port from the topmost via header.
     */
    public int getViaPort() {
        return this.myPort;
    }
    
    /**
     * UDP is not a reliable protocol
     */
    public boolean isReliable() {
		return false;
	}
    
    /** Close the message channel.
     */
    public void close() {
    }
    
    /** Get the peer address of the machine that sent us this message.
     * @return  a string contianing the ip address or host name of the sender
     *  of the message.
     */
    public String getPeerAddress() {
        return this.peerAddress;
    }
    
    /** Get the sender port ( the port of the other end that sent me
     * the message).
     */
    public int getPeerPort() {
        return this.peerPort;
    }
    
    /** Return true if this is a secure channel.
     */
    public boolean isSecure() {
        
        return false;
    }
    
    
    
    /** This gets called from the parser when a parse error is generated.
     * The handler is supposed to introspect on the error class and
     * header name to handle the error appropriately. The error can
     * be handled by :
     * <ul>
     * <li>1. Re-throwing an exception and aborting the parse.
     * <li>2. Ignoring the header (attach the unparseable header to
     * the Message being parsed).
     * <li>3. Re-Parsing the bad header and adding it to the sipMessage
     * </ul>
     *
     * @param  ex - parse exception being processed.
     * @param  sipMessage -- sip message being processed.
     * @param headerText --  header/RL/SL text being parsed.
     * @param messageText -- message where this header was detected.
     */
    public void handleException(ParseException ex, Message sipMessage,
    Class hdrClass,
    String headerText,
    String messageText) throws ParseException {
        if (LogWriter.needsLogging) LogWriter.logException(ex);
        // Log the bad message for later reference.
        
        if ( hdrClass .equals(FromHeader.clazz)    ||
        hdrClass.equals(ToHeader.clazz)            ||
        hdrClass.equals(CSeqHeader.clazz)          ||
        hdrClass.equals(ViaHeader.clazz)           ||
        hdrClass.equals(CallIdHeader.clazz)        ||
        hdrClass.equals(RequestLine.clazz)   	   ||
        hdrClass.equals(StatusLine.clazz)) {
            stack.logBadMessage(messageText);
            throw ex;
        }else {
            sipMessage.addUnparsed(headerText);
        }
        
    }
    
}
