package sip4me.gov.nist.siplite.stack;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;

import sip4me.gov.nist.core.HostPort;
import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.net.SocketException;


/**
 * Sit in a loop waiting for incoming tcp connections and start a new thread to handle each new
 * connection. This is the active object that creates new TCP MessageChannels (one for each new
 * accept socket).
 * 
 * @version 1.2 $Revision: 1.9 $ $Date: 2010-03-23 15:36:35 $
 * 
 * @author M. Ranganathan <br/>
 * 
 * 
 */
public class TCPMessageProcessor extends MessageProcessor {

	private int nConnections = 0;
	
	public boolean isRunning;
	
    private final Hashtable tcpMessageChannels;
    
    private final Vector incomingTcpMessageChannels;

    protected int port;
    
    protected  ServerSocketConnection serverSocket;
    
    public boolean ERROR_SOCKET=false;
    
    /** Our stack
     */
    protected SIPMessageStack sipStack;
    
    
    /** Creates new TCPMessageProcessor */
    public TCPMessageProcessor(SIPMessageStack sipStack, int port) {
        this.sipStack = sipStack;
        this.port = port;
        this.tcpMessageChannels = new Hashtable();
        this.incomingTcpMessageChannels = new Vector();
        
        if (LogWriter.needsLogging)
            LogWriter.logMessage("Created TCPMessageProcessor " + this);
        
    }
	
    
    /** Start the processor
     */
    public void start() throws IOException {
    	Thread thread = new Thread (this, "TCPMessageProcessorThread");
    	thread.setPriority(Thread.MAX_PRIORITY);
    	
        try {
			this.serverSocket = sipStack.getNetworkLayer().createServerSocket(getPort());
            if (LogWriter.needsLogging)
                LogWriter.logMessage("Created server socket on " + serverSocket.getLocalAddress() + ":" + serverSocket.getLocalPort());
		} catch (SocketException e) {
			throw new IOException(e.getClass() + " ::: " + e.getMessage());
		}
        this.isRunning = true;

    	thread.start();
    
    }
    
    /**
     * Run method for the thread that accepts incoming connections
     */
    public void run()  {
    	
        // Accept new connections on our socket.
        while (this.isRunning) {
            try {
                synchronized (this) {
                	                	
                    // sipStack.maxConnections == -1 means we are
                    // willing to handle an "infinite" number of
                    // simultaneous connections (no resource limitation).
                    // This is the default behavior.
                    while (sipStack.maxConnections != -1
                            && this.nConnections >= sipStack.maxConnections) {
                        try {
                            this.wait();
                            if (!this.isRunning)
                                return;
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                    increaseNumConnections();
                }
                
                if (LogWriter.needsLogging)
                    LogWriter.logMessage("Server socket blocked waiting for connections");
                SocketConnection newsock = (SocketConnection) serverSocket.acceptAndOpen();
                if (LogWriter.needsLogging)
                    LogWriter.logMessage("New socket created from incoming connection from host: " + newsock.getAddress());

                // Note that for an incoming message channel, the
                // thread is already running
                incomingTcpMessageChannels.addElement(new TCPMessageChannel(newsock, sipStack, this));
            } catch (IOException ex) {
                // Problem accepting connection, or loop killed by
            	// a stopping message processor (don't print Exception in that case)
                if (this.isRunning && LogWriter.needsLogging)
                    LogWriter.logException(ex);
                continue;
            } catch (Exception ex) {
            	if (this.isRunning) {
            		ex.printStackTrace();
            		InternalErrorHandler.handleException(ex);
            	}
            }
        }
    
    }


    /**
     * Get the transport string.
     * @return A string that indicates the transport.
     */
    public String getTransport() {
        return "tcp";
    }
    
    
    public SIPMessageStack getSIPStack() {
        return this.sipStack;
    }
     

    /**
     * Get the port identifier.
     * @return the port for this message processor.
     */
    public int getPort() {
        return this.port;
    }
    
    /**
     * Get the SIP Stack.
     * @return the sip stack.
     */
    public SIPMessageStack getSipStack() {
        return sipStack;
    }
    
    /**
     * Stop the message processor. Feature suggested by Jeff Keyser.
     * Synchronized to solve a bug when stopping 
     * listening points (fix by Pulkit Bhardwaj, Tata Consultancy Services)
     */
    public synchronized void stop() {
        if (LogWriter.needsLogging)
            LogWriter.logMessage
            ("Stopping TCPMessageProcessor");
    	
        isRunning = false;
        // this.listeningPoint = null;

        if (serverSocket != null) {
        	try {
        		serverSocket.close();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }

        
        for (Enumeration en = tcpMessageChannels.elements(); en.hasMoreElements();) {
            TCPMessageChannel next = (TCPMessageChannel) en.nextElement();
            next.close();
        }
        
        // RRPN: fix
        for (Enumeration incomingMCIterator = incomingTcpMessageChannels.elements(); 
        	incomingMCIterator.hasMoreElements();) {
            TCPMessageChannel next = (TCPMessageChannel) incomingMCIterator.nextElement();
            next.close();
        }

        this.notify();
        
        if (LogWriter.needsLogging)
            LogWriter.logMessage
            ("TCPMessageProcessor stopped");

    }

    protected synchronized void remove(TCPMessageChannel tcpMessageChannel) {

        String key = tcpMessageChannel.getKey();
        if (LogWriter.needsLogging) {
			LogWriter.logMessage(LogWriter.TRACE_DEBUG, this
					+ " removing Channel " + tcpMessageChannel + " with key "
					+ key);
        }

        /** May have been removed already */
        if (tcpMessageChannels.get(key) == tcpMessageChannel) {
            this.tcpMessageChannels.remove(key);
            LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Message Channel " + tcpMessageChannel + " correctly uncached");
        } else {
            LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Message Channel " + tcpMessageChannel + " not indexed with key " + key + ". Instead: " + tcpMessageChannels.get(key));
        }

        incomingTcpMessageChannels.removeElement(tcpMessageChannel);
    }
    
    /** Create and return new TCPMessageChannel for the given host/port.
     */
    public MessageChannel createMessageChannel(HostPort targetHostPort) 
        throws IOException {
    	
    	String key = "TCP" + ":" + sipStack.ioHandler.makeKey(targetHostPort.getHost().getAddress(), targetHostPort.getPort());
        if (tcpMessageChannels.get(key) != null) {
            if (LogWriter.needsLogging) {
                LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Returning existing channel " + this.tcpMessageChannels.get(key) + " with key " + key);
            }
            return (TCPMessageChannel) this.tcpMessageChannels.get(key);
        } else {
            TCPMessageChannel msgChannel = new TCPMessageChannel(targetHostPort.getHost().getAddress(),
                    targetHostPort.getPort(), sipStack, this);
    		cacheMessageChannel(msgChannel);
            if (LogWriter.needsLogging) {
                LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Msg channel created " + msgChannel + " with key " + key);
            }
            return msgChannel;
        }

    }
    
    public MessageChannel createMessageChannel(String host, int port)
    throws IOException {
    	
    	String key = "TCP" + ":" + sipStack.ioHandler.makeKey(host, port);
    	if (tcpMessageChannels.get(key) != null) {
            if (LogWriter.needsLogging) {
                LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Returning existing channel " + this.tcpMessageChannels.get(key) + " with key " + key);
            }
    		return (TCPMessageChannel) this.tcpMessageChannels.get(key);
    	} else {
    		TCPMessageChannel msgChannel = new TCPMessageChannel(host, port, sipStack, this);
    		cacheMessageChannel(msgChannel);
    		if (LogWriter.needsLogging) {
                LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Msg channel created " + msgChannel + " with key " + key);
    		}
    		return msgChannel;
    	}
    }
    
    protected synchronized void cacheMessageChannel(TCPMessageChannel messageChannel) {
        String key = messageChannel.getKey();
        if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Caching Message Channel " + messageChannel + " with key " + key);
        
        TCPMessageChannel currentChannel = (TCPMessageChannel) tcpMessageChannels.get(key);
        if (currentChannel != null) {
    		if (LogWriter.needsLogging)
    			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Replacing old channel. Closing " + currentChannel + " with key " + key);
            currentChannel.close();
        }
        messageChannel.isCached = true;
        this.tcpMessageChannels.put(key, messageChannel);

    }
    
    /** Default target port
     */
    public int getDefaultTargetPort() {
        return 5060;
    }
    
    
    public boolean isSecure() {
        return false;
    }
    
    /**
     * TCP can handle an unlimited number of bytes.
     */
    public int getMaximumMessageSize() {
        return Integer.MAX_VALUE;
    }
    
    public synchronized int increaseNumConnections() {
    	nConnections++;
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "TCP connections increased to " + nConnections);
    	return nConnections;
    }
    
    public synchronized int decreaseNumConnections() {
    	nConnections--;
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "TCP connections reduced to " + nConnections);
    	return nConnections;
    }
    
    public synchronized int getNumConnections() {
    	return nConnections;
    }

  
}
