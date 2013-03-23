/*
 * TCPMessageChannel.java
 *
 * Created on September 3, 2002, 3:47 PM
 * Modified on February 24, 2009, 11:10 AM by ArnauVP (Genaker) 
 */

package sip4me.gov.nist.siplite.stack;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.SocketConnection;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
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
import sip4me.gov.nist.siplite.parser.PipelinedMsgParser;
import sip4me.gov.nist.siplite.parser.SIPMessageListener;

/** Handle a TCP stream connection.
 *
 * @author M. Ranganathan
 * @version 1.1
 */
public class TCPMessageChannel extends  MessageChannel
implements SIPMessageListener, Runnable  {
    
    
    private SocketConnection  mySock;
    private PipelinedMsgParser myParser;
    private InputStream myClientInputStream;   // just to pass to thread.
    private OutputStream myClientOutputStream;
    
    protected String key;
    protected boolean isCached;
    protected boolean isRunning;
    private Thread mythread;

    
    private final SIPMessageStack stack;
    private final String myAddress;
    private final int myPort;
    
    private String peerAddress;
    private int peerPort = -1;
    private String peerProtocol;
    
    private int viaPort = -1;

    private final  TCPMessageProcessor tcpMessageProcessor;
    
    
    /**
     * Constructor - gets called from the SIPMessageStack class with a socket
     * on accepting a new client. All the processing of the message is
     * done here with the stack being freed up to handle new connections.
     * The sock input is the socket that is returned from the accept.
     * Global data that is shared by all threads is accessible in the Server
     * structure.
     * @param sock Socket from which to read and write messages. The socket
     *   is already connected (was created as a result of an accept).
     *
     * @param sipStack the SIP Stack
     * @param channelNotifier Notifier (optional) that gets called when
     * the channel is opened or closed.
     */
    protected TCPMessageChannel( SocketConnection sock, SIPMessageStack sipStack,
    TCPMessageProcessor msgProcessor) throws IOException {

    	stack = sipStack;
        mySock = sock;
        myAddress = sipStack.getHostAddress();
        peerAddress = sock.getAddress();
        myClientInputStream = sock.openInputStream();
        myClientOutputStream = sock.openOutputStream();
        if (LogWriter.needsLogging) {
        	LogWriter.logMessage("Creating new TCPMessageChannel " + this);
        	LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Channel parameters: " + "stack: " + stack + "\n" + "processor: " + msgProcessor + "\n" +
        			"localAddress: " + myAddress + "\n" + "peerAddress: " + peerAddress + "\n" +
        			"IS " + myClientInputStream + " Socket " + mySock);
        }
        stack.ioHandler.putSocket(stack.ioHandler.makeKey(mySock.getAddress(), mySock.getPort()), mySock, myClientOutputStream, myClientInputStream);
        
        mythread = new Thread(this, "TCPMessageChannel - incoming connection");
        
        this.tcpMessageProcessor = msgProcessor;
        this.myPort = this.tcpMessageProcessor.getPort();
        // Bug report by Vishwashanti Raj Kadiayl
        super.messageProcessor =  msgProcessor;
        mythread.start();
    }
    
    /**
     *Constructor - connects to the given inet address.
     * Acknowledgement -- Lamine Brahimi (IBM Zurich) sent in a
     * bug fix for this method. A thread was being unnecessarily created.
     *@param inetAddr inet address to connect to.
     *@param sipStack is the sip stack from which we are created.
     *@param messageProcessor to whom a parsed message is passed 
     *@throws IOException if we cannot connect.
     */
    protected TCPMessageChannel(String inetAddr, int port,
			SIPMessageStack sipStack, TCPMessageProcessor messageProcessor)
			throws IOException {

        this.peerAddress = inetAddr;
        this.peerPort = port;
        this.myPort = messageProcessor.getPort();
        this.peerProtocol = "TCP";
        this.stack = sipStack;
        this.tcpMessageProcessor = messageProcessor;
        this.myAddress = sipStack.getHostAddress();
        // Bug report by Vishwashanti Raj Kadiayl
        super.messageProcessor =  messageProcessor;
        this.key = "TCP" + ":" + stack.ioHandler.makeKey(peerAddress, peerPort);

        
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage("Created new TCP Message Channel " + this + " with key " + key + "\nprocessor: " + messageProcessor);
        
    }
    
    /** Returns "true" as this is a reliable transport.
     */
    public boolean isReliable() {
        return true;
    }
    
    /** Close the message channel.
     */
    public void close() {
        try {
            if (LogWriter.needsLogging)
                LogWriter.logMessage("Closing message Channel " + this);
            
        	if (mySock != null) {
        		try {
        			String addr = mySock.getAddress();
        			int port = mySock.getPort();
        			stack.ioHandler.removeAndCloseSocket(stack.ioHandler.makeKey(addr, port));
        		} catch (IOException e) {
                	if (LogWriter.needsLogging)
                        LogWriter.logMessage("Socket was already closed for " + this);
				}
                mySock = null;
            } else {
            	if (LogWriter.needsLogging)
                    LogWriter.logMessage("Socket was already null for " + this);
            }

        } catch (Exception ex) {
        	if (LogWriter.needsLogging) {
                LogWriter.logMessage("Exception closing message Channel " + this);
                LogWriter.logException(ex);
        	}
        } finally {
            this.isRunning = false;
            uncache();
        }
 
    }
    
    public void closeFromParser() {
        try {
            if (LogWriter.needsLogging)
                LogWriter.logMessage("Closing message Channel " + this + " from Parser");
            
            
        	if (mySock != null) {
        		try {
        			String addr = mySock.getAddress();
        			int port = mySock.getPort();
        			stack.ioHandler.removeAndCloseSocket(stack.ioHandler.makeKey(addr, port));
        		} catch (IOException e) {
                	if (LogWriter.needsLogging)
                        LogWriter.logMessage("Socket was already closed for " + this);
				}
                mySock = null;
            } else {
            	if (LogWriter.needsLogging)
                    LogWriter.logMessage("Socket was already null for " + this);
            }
        	
        } catch (Exception ex) {
        	if (LogWriter.needsLogging && !stack.toExit) {
                LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "Exception closing message Channel " + this);
                LogWriter.logException(ex);
        	}
        } finally {
            this.isRunning = false;
            uncache();
            this.tcpMessageProcessor.useCount--;
        	if (LogWriter.needsLogging)
                LogWriter.logMessage("TCP Message Processor use count: " + tcpMessageProcessor.useCount);
        }
    }
   

	/** Send message to whoever is connected to us.
     * Uses the topmost via address to send to.
     *@param message is the message to send.
     */
    private void sendMessage(byte[] msg, boolean retry) throws IOException {

    	int portToUse;
    	if (viaPort != -1)
    		portToUse = viaPort;
    	else
    		portToUse = peerPort;
    	
    	if (!this.isRunning && !this.isCached) {
    		if (LogWriter.needsLogging)
    			LogWriter
    			.logMessage(LogWriter.TRACE_MESSAGES,
    			"Tried to send message through a Message Channel that is no longer running. Create a new one.");
    		
    		TCPMessageChannel newChannel = (TCPMessageChannel) this.tcpMessageProcessor.createMessageChannel(this.peerAddress, portToUse);
    		newChannel.sendMessage(msg, retry);
    		return;
    	}
    	
        SocketConnection sock = stack.ioHandler.sendBytes(
        		this.peerAddress, portToUse, this.peerProtocol, msg, retry);
    	
        /*Fix: don't replace the original socket (where the incoming
        request came from) with the one created to send the response
        to the port specified by the VIA header. This would cause a
        NPE later, as the 'via socket' will be closed shortly after
        sending the response. Thanks to Janos Vig (Genaker) for
        detecting this. */
        if (sock != null
				&& (mySock == null || (sock != mySock && sock.getPort() == mySock.getPort()))) {
            try {
                if (mySock != null) {
                	if (LogWriter.needsLogging)
						LogWriter
								.logMessage(LogWriter.TRACE_MESSAGES,
										"Closing socket on TCPMessageChannel and replacing with new one");
                	
                	// Closing the IO streams will also stop the parser reading from them
					if (myClientOutputStream != null)
						myClientOutputStream.close();
					if (myClientInputStream != null)
						myClientInputStream.close();
					mySock.close();

                } else {
                	if (LogWriter.needsLogging)
						LogWriter
								.logMessage(LogWriter.TRACE_DEBUG,
										"TCP Msg channel " + this + " socket was null!");
                }
            } catch (IOException ex) {
            	ex.printStackTrace();
            }
            
            
            // replace the old references of socket and IO streams
            // to the ones pointing to the newly created socket.
            mySock = sock;
            myClientOutputStream = stack.ioHandler.getSocketOutputStream(mySock);
            myClientInputStream = stack.ioHandler.getSocketInputStream(mySock);
            
            if (LogWriter.needsLogging)
            	LogWriter
            	.logMessage(LogWriter.TRACE_DEBUG,
            			"Creating new Thread from Message Channel " + this + " and new socket " + sock + " IS " + myClientInputStream);
            Thread thread = new Thread(this);
            thread.start();
        }
    }
    
    /** Return a formatted message to the client.
     * We try to re-connect with the peer on the other end if possible.
     * @param msg Message to send.
     * @throws IOException If there is an error sending the message
     */
    public void sendMessage(Message sipMessage) throws IOException {
    	if (sipMessage == null) throw new NullPointerException("null arg!");
    	
        byte[] msg = sipMessage.encodeAsBytes();
        long time = System.currentTimeMillis();
        this.sendMessage(msg, true);
        if (ServerLog.needsLogging(ServerLog.TRACE_MESSAGES))
            logMessage(sipMessage,peerAddress,peerPort,time); 
    }
    
    
    /** Send a message to a specified address.
     * @param message Pre-formatted message to send.
     * @param receiverAddress Address to send it to.
     * @param receiverPort Receiver port.
     * @param retry try to reopen connection if possible
     * @throws IOException If there is a problem connecting or sending.
     * @deprecated
     */
    public void sendMessage(byte message[], String receiverAddress,
    int receiverPort, boolean retry)
    throws IOException, IllegalArgumentException{
        if (message == null || receiverAddress == null)
            throw new IllegalArgumentException("Null argument");
        
        if ( ! receiverAddress.equals(stack.outboundProxy) )
            throw new IOException("Cannot proxy request");
        
        SocketConnection sock = stack.ioHandler.sendBytes(
                this.peerAddress, this.peerPort, this.peerProtocol, message, retry);
    	
        if (sock != mySock && sock != null) {
            try {
                if (mySock != null)
                    mySock.close();
            } catch (IOException ex) {
            }
            mySock = sock;
            this.myClientOutputStream = stack.ioHandler.getSocketOutputStream(mySock);
            this.myClientInputStream = stack.ioHandler.getSocketInputStream(mySock);
//            Thread thread = new Thread(this, "TCPMessageChannelThread - sender");
            Thread thread = new Thread(this);
            thread.start();
        }
        
    }
    
    /** Exception processor for exceptions detected from the application.
     * @param ex The exception that was generated.
     */
    public void handleException( SIPServerException   ex ) {
        // Return a parse error message to the client on the other end
        // if he is still alive.
        int rc = ex.getRC();
        String msgString = ex.getMessage();
        if (rc != 0 ) {
            // Do we have a valid Return code ? --
            // in this case format the message.
            Request request =
            (Request) ex.getSIPMessage();
            Response response =
            request.createResponse(rc);
            try {
                sendMessage(response);
            } catch (IOException ioex) {
                if (LogWriter.needsLogging)
                    LogWriter.logException(ioex);
            }
        } else {
            // Otherwise, message is already formatted --
            // just return it
            try {
                sendMessage(msgString.getBytes(), false);
            } catch (IOException ioex) {
                if (LogWriter.needsLogging)
                    LogWriter.logException(ioex);
            }
        }
    }
    
    
    /** Exception processor for exceptions detected from the parser. (This
     * is invoked by the parser when an error is detected).
     * @param sipMessage -- the message that incurred the error.
     * @param ex -- parse exception detected by the parser.
     * @param header -- header that caused the error.
     * @throws ParseException Thrown if we want to reject the message.
     */
    public void handleException(ParseException ex,
    Message sipMessage,
    Class hdrClass,
    String header,
    String message)
    throws ParseException {
        if (LogWriter.needsLogging) LogWriter.logException(ex);
        // Log the bad message for later reference.
        
        if (hdrClass .equals(FromHeader.clazz)  ||
        hdrClass.equals(ToHeader.clazz )        ||
        hdrClass.equals(CSeqHeader.clazz)       ||
        hdrClass.equals(ViaHeader.clazz)        ||
        hdrClass.equals(CallIdHeader.clazz)     ||
        hdrClass.equals(RequestLine.clazz)||
        hdrClass.equals(StatusLine.clazz)) {
            stack.logBadMessage(message);
            throw ex;
        }else {
            sipMessage.addUnparsed(header);
        }
        
    }
    
    
    
    
    /** Gets invoked by the parser as a callback on successful message
     * parsing (i.e. no parser errors).
     * @param sipMessage Mesage to process (this calls the application
     * for processing the message).
     */
    public void processMessage( Message sipMessage) {
    	
    	if (!stack.isAlive()) {
    		if (LogWriter.needsLogging)
    			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "MsgChannel " + this +" is dropping message as the stack is closing");
    		return; // drop messages when closing, avoid Exceptions
    	}
    	
        try {
            if (LogWriter.needsLogging)
                LogWriter.logMessage("[TCPMessageChannel]-> Processing incoming message: " + sipMessage.getFirstLine());
            if (sipMessage.getFromHeader() == null      ||
            sipMessage.getTo() == null 	          ||
            sipMessage.getCallId() == null        ||
            sipMessage.getCSeqHeader() == null 	  ||
            sipMessage.getViaHeaders()  == null   ) {
                String badmsg = sipMessage.encode();
                if (LogWriter.needsLogging)  {
                    ServerLog.logMessage("bad message " + badmsg);
                    ServerLog.logMessage(">>> Dropped Bad Msg");
                }
                stack.logBadMessage(badmsg);
                return;
            }
            
            ViaList viaList = sipMessage.getViaHeaders();
            // For a request
            // first via header tells where the message is coming from.
            // For response, this has already been recorded in the outgoing
            // message.
            if (sipMessage instanceof Request) {
                ViaHeader v = (ViaHeader)viaList.first();
                if (v.hasPort() ) {
                	viaPort = v.getPort();
                }  else  {
                	viaPort = SIPMessageStack.DEFAULT_PORT;
                }
                this.peerProtocol = v.getTransport();
                try {
                	if (peerPort == -1)
                		peerPort = mySock.getPort();
					this.peerAddress = mySock.getAddress();
					
					// Log this because it happens when the remote host identifies
					// as a FQDN but this is not resolvable to an IP by the OS.
					// S40 doesn't have DNS settings, for instance, so if the APN
					// is not able to resolve all the addresses of the SIP/IMS core,
					// this problem will appear.
					if (peerAddress == null && LogWriter.needsLogging)
						LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "WARNING! Socket.getAddress() returned 'null'!!!");
					
					
		        	// Be warned, the host comparison may fail if socket.getAddress()
		        	// returns a domain name as the Via Host will be a numeric IP.
		        	// FIXME: No idea. Doing a DNS lookup or reverse DNS lookup 
		        	// can be misleading because they can be non-matching, that is,
		        	// DNS(peerAddressName) != ReverseDNS(peerAddressIP)
					if (v.hasParameter(ViaHeader.RPORT) || !v.getHost().equals(this.peerAddress)) {
						if (LogWriter.needsLogging)
							LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "WARNING! \"Received\" parameter " +
									"has been temporarily disabled. Response will be sent to topmost Via Host: " + v.getHost());
						this.peerAddress = v.getHost();
//		                if (LogWriter.needsLogging) 
//			                   LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Adding \"received\" parameter" +
//			                   		" to incoming request with value: " + peerAddress + 
//			                   		" because it doesn't match the Via host " + v.getHost());
//						v.setParameter(ViaHeader.RECEIVED, this.peerAddress);
						
					}
					
					if (v.hasParameter(ViaHeader.RPORT))
							v.setParameter(ViaHeader.RPORT, Integer.toString(this.peerPort));

					/*
					 * If socket is invalid, close it because it is useless and dangerous.
					 * Also if we ran out of slots for new sockets, as this could prevent
					 * incoming connections from being accepted. 
					 */
					if (mySock.getAddress() == null || (stack.maxConnections != -1 && tcpMessageProcessor.getNumConnections() >= stack.maxConnections)) {
						stack.ioHandler.disposeSocket(mySock, myClientInputStream, myClientOutputStream);
						mySock = null;
						myClientInputStream = null;
						myClientOutputStream = null;
			            if (stack.maxConnections != -1) {
			                synchronized (tcpMessageProcessor) {
			        			tcpMessageProcessor.decreaseNumConnections();
			                    tcpMessageProcessor.notify();
			                }
			            }
					}
					// reuse socket even for outgoing requests
					else if (!this.isCached) {
						((TCPMessageProcessor) this.messageProcessor)
						.cacheMessageChannel(this);
						String key = "TCP" + ":" + stack.ioHandler.makeKey(peerAddress, peerPort);
						stack.ioHandler.putSocket(key, mySock, myClientOutputStream, myClientInputStream);
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
                
            }
            
            // System.out.println("receiver address = " + receiverAddress);
            
            // For each part of the request header, fetch it and process it
            long receptionTime = System.currentTimeMillis();
            
            if ( sipMessage instanceof Request) {
                // This is a request - process the request.
                Request sipRequest = (Request)sipMessage;
                // Create a new sever side request processor for this
                // message and let it handle the rest.
                
                if (LogWriter.needsLogging) {
                    LogWriter.logMessage("----Processing Message---");
                }
                
                // TODO: check maximum size of request
                
                SIPServerRequestInterface sipServerRequest =
                stack.newSIPServerRequest(sipRequest,this);
                
                
				if (sipServerRequest != null) {
	                try {
	                	sipServerRequest.processRequest(sipRequest,this);

	                	ServerLog.logMessage(sipMessage,
	                			sipRequest.getViaHost() + ":" +
	                			sipRequest.getViaPort(),
	                			stack.getHostAddress()  + ":" +
	                			stack.getPort(this.getTransport()),false,
	                			receptionTime);
	                } catch (SIPServerException ex) {
	                    ServerLog.logMessage(sipMessage,
	                    sipRequest.getViaHost() + ":" + sipRequest.getViaPort(),
	                    stack.getHostAddress()  + ":" +
	                    stack.getPort(this.getTransport()),
	                    ex.getMessage(), false, receptionTime);
	                    handleException(ex);
	                }

	                    
				} else {
					if (LogWriter.needsLogging)
						LogWriter.logMessage("Dropping request -- null sipServerRequest");					
				}
  
                
                
            } else {
            	// This is a response message - process it.
            	Response sipResponse = (Response)sipMessage;
            	
            	// TODO: check maximum size of the response
            	
            	SIPServerResponseInterface sipServerResponse =
            		stack.newSIPServerResponse(sipResponse, this);

            	if (LogWriter.needsLogging)
            		LogWriter.logMessage("got a response interface " +
            				sipServerResponse);

            	try {
                    // Responses with no ClienTransaction associated will not be processed
            		// as they may cause a NPE in the EventScanner thread.
            		if (sipServerResponse != null)
            			sipServerResponse.processResponse(sipResponse,this);
            		else {
            			if (LogWriter.needsLogging) {
                            LogWriter.logMessage("null sipServerResponse!");
                        }
            		}
            	} catch (SIPServerException ex) {
            		// An error occured processing the message -- just log it.
            		ServerLog.logMessage(sipMessage,
            				getPeerAddress().toString() + ":" + getPeerPort(),
            				stack.getHostAddress()  + ":" +
            				stack.getPort(this.getTransport()),
            				ex.getMessage(), false, receptionTime);
            		// Ignore errors while processing responses??
            	}
            }
        } catch (Exception ee) {
        	if (stack.isAlive()) {
        		throw new RuntimeException(ee.getClass() + ":" + ee.getMessage());
        	} 
        	// else ignore exceptions
        } finally {
//            this.tcpMessageProcessor.useCount --;
        }
    }
    
    
    /**
     * This gets invoked when thread.start is called from the constructor. Implements a message
     * loop - reading the tcp connection and processing messages until we are done or the other
     * end has closed.
     */
    public void run() {
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "New TCP Message Channel Thread started " + this + " " + Thread.currentThread());
        // Removed by ArnauVP: in MIDP it's not reliable to read in blocks
        // from inputstreams; thus the Pipe was useless for byte-to-byte reads,
        // and was giving sync problems when reading from the pipe (duplicate bytes).
        // Create a pipeline to connect to our message parser.
//        Pipeline hispipe = null;
//        hispipe = new Pipeline(myClientInputStream, stack.readTimeout,
//                ((SIPTransactionStack) stack).getTimer());
    	
        // Create a pipelined message parser to read from inputStream
    	// and parse SIP messages from the bytes. 
        myParser = new PipelinedMsgParser(this, myClientInputStream, this.messageProcessor.getMaximumMessageSize());
        myParser.setMessageChannel(this);
        // Start running the parser thread.
        myParser.processInput();
        // Changed by ArnauVP: using a buffer in some implementations of MIDP, 
        // the call to read(msg) would not return from until BUFFER_SIZE bytes were available,
        // thus delaying the processing of the responses by the application and causing timeout. 
        // bug fix by Emmanuel Proulx
//        int bufferSize = 4096;
        this.tcpMessageProcessor.useCount++;
        this.isRunning = true;
     }
    
        /** Called when the pipelined parser cannot read input because the 
     * other end closed the connection.
     */
    public void handleIOException() {
    }
    
    

    protected void uncache() {
        this.tcpMessageProcessor.remove(this);
        isCached = false;
    }


    /**
     * Equals predicate.
     * @param other is the other object to compare ourselves to for equals
     */
    
    public boolean equals(Object other) {
        
    	if (!this.getClass().equals(other.getClass()))
			return false;
		else {
			TCPMessageChannel that = (TCPMessageChannel) other;
			if (this.mySock != that.mySock)
				return false;
			else
				return true;
		}
    }



    
    
    /**
     * Get an identifying key. This key is used to cache the connection
     * and re-use it if necessary.
     */
    public String getKey() {
    	if (this.key != null) {
    		return this.key;
    	} else {
    		this.key = "TCP" + ":" + stack.ioHandler.makeKey(peerAddress, peerPort);
    		return this.key;
    	}
    }
    
    /**
     * Get the host to assign to outgoing messages.
     *
     *@return the host to assign to the via header.
     */
    public String getViaHost() {
        return myAddress;
    }
    
    /**
     * Get the port for outgoing messages sent from the channel.
     *
     *@return the port to assign to the via header.
     */
    public int getViaPort() {
        return myPort;
    }
    
    
    /** Get my SIP Stack.
     * @return The SIP Stack for this message channel.
     */
    public SIPMessageStack getSIPStack() {
        return stack;
    }
    
    /** get the transport string.
     * @return "TCP" in this case.
     */
    public String getTransport() {
        return "TCP";
    }
    
    
    /** get the address of the client that sent the data to us.
     * @return Address of the client that sent us data
     * that resulted in this channel being
     * created.
     */
    public String getPeerAddress() {
    	return peerAddress;
    }
    
    public String getPeerProtocol() {
		return peerProtocol;
	}
    
    /**
     * Get the port of the peer to whom we are sending messages.
     *
     *@return the peer port.
     */
    public int getPeerPort() {
		return peerPort;
	}
    
    /** TCP Is not a secure protocol.
     */
    public boolean isSecure() {
		return false;
	}
    
    
}
