package sip4me.gov.nist.siplite.stack;


import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

import sip4me.gov.nist.core.HostPort;
import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.net.SocketException;



/**
 * Sit in a loop and handle incoming udp datagram messages. For each Datagram
 * packet, a new UDPMessageChannel is created (up to the max thread pool size).
 * Each UDP message is processed in its own thread).
 * 
 * @version 1.2 $Revision: 1.7 $ $Date: 2010-03-23 15:36:33 $
 * 
 * @author M. Ranganathan  <br/>
 * 
 * 
 * 
 * <a href="{@docRoot}/../uml/udp-request-processing-sequence-diagram.jpg">
 * See the implementation sequence diagram for processing incoming requests.
 * </a>
 * 
 * 
 * Acknowledgement: Jeff Keyser contributed ideas on starting and stoppping the
 * stack that were incorporated into this code. Niklas Uhrberg suggested that
 * thread pooling be added to limit the number of threads and improve
 * performance.
 */
public class UDPMessageProcessor  extends MessageProcessor {
    
	
	private static final int HIGHWAT = 100 ; // High water mark for queue size.
	
	private static final int LOWAT =   50 ; // Low water mark for queue size
	
    protected boolean running;
    
    /** Max datagram size.
     */
    public static int MAX_DATAGRAM_SIZE = 1300;
    
    /** Our stack (that created us).
     */
    protected SIPMessageStack sipStack;
    
    protected DatagramConnection dc;

	/**
	 * Incoming messages are queued here.
	 */
	protected Vector messageQueue;

	/**
	 * A list of message channels that we have started.
	 */
	protected Vector messageChannels;

	/**
	 * Max # of udp message channels
	 */
	protected int threadPoolSize;
    
    int port;

    
    /**
     * Constructor.
     * @param srv pointer to the stack.
     * @param notify channel notifier.
     */
    protected  UDPMessageProcessor( SIPMessageStack sipStack , int port) throws IOException  {
        this.sipStack = sipStack;
        this.port = port;
        messageQueue = new Vector();
        useCount = 0;
        
		try {
			// Create a new datagram socket.
			dc = sipStack.getNetworkLayer().createDatagramInboundSocket(port);
			/**
			 * Although according to RFC 3261 we should be able of handling
			 * messages up to 65,535 bytes, most devices have an actual limit 
			 * of around 1450 bytes. The processor will try to use the
			 * system maximum size for incoming datagrams, but bigger ones will
			 * be cut and will probably produce an error at the application level.
			 * 
			 */
			MAX_DATAGRAM_SIZE = dc.getMaximumLength();
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Setting maximum datagram size to " + MAX_DATAGRAM_SIZE);
		} catch (SocketException ex) {
			throw new IOException(ex.getMessage());
		}
    }
    
    /**
     * Start our processor thread.
     */
    public void start() {
    	
        this.running = true;
		Thread thread = new Thread(this, "UDPMessageProcessorThread");
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
    }
    
	/**
	 * Shut down the message processor. Close the socket for receiving incoming
	 * messages.
	 */
	public void stop() {
		
		
        try {            
            this.running = false;
            // Synchronized to solve a bug when stopping 
            // listening points (fix by Pulkit Bhardwaj, Tata Consultancy Services)
            synchronized (this.messageQueue) {
    			this.messageQueue.notifyAll();
			}

            if (dc != null ) {                
                if (LogWriter.needsLogging)
                    LogWriter.logMessage
                    ("Stopping UDPMessageProcessor");
                Thread.sleep(300);
                dc.close();
                if (LogWriter.needsLogging)
                    LogWriter.logMessage
                    ("UDPMessageProcessor stoped");                
            }            
        }
        catch (Exception ex) {
            if (LogWriter.needsLogging)
                LogWriter.logMessage
                ("UDPMessageProcessor, stop(), exception raised:");
            ex.printStackTrace();
        }

	}
    
    
    
    /**
     * Thread main routine.
     */
    public void run(){
    
		// Check for running flag.
    	if (!running)
    		return;
    	
		this.messageChannels = new Vector();
		// start all our messageChannels (unless the thread pool size is
		// infinity.
		if (sipStack.threadPoolSize != -1) {
			for (int i = 0; i < sipStack.threadPoolSize; i++) {
				UDPMessageChannel channel = new UDPMessageChannel(sipStack,this);
				this.messageChannels.addElement(channel);

			}
		}


		// Somebody asked us to exit if "running" is set to false.
		while (running) {
			
			try {
				Datagram packet = dc.newDatagram(MAX_DATAGRAM_SIZE);
				dc.receive(packet);
				
				if (LogWriter.needsLogging && packet.getLength() == MAX_DATAGRAM_SIZE)
					LogWriter.logMessage(
							LogWriter.TRACE_MESSAGES,
							"Incoming datagram is as big as Maximum Datagram Size. " +
							"Some information might have been lost!");
					

			 // This is a simplistic congestion control algorithm.
			 // It accepts packets if queuesize is < LOWAT. It drops
			 // requests if the queue size exceeds a HIGHWAT and accepts
			 // requests with probability p proportional to the difference
			 // between current queue size and LOWAT in the range
			 // of queue sizes between HIGHWAT and LOWAT.
			 // TODO: penalize spammers by looking at the source
			 // port and IP address.
			 if (messageQueue.size() >= HIGHWAT) {
					if (LogWriter.needsLogging) 
						LogWriter.logMessage("Dropping message -- queue length exceeded");
					continue;
				} else if (messageQueue.size() > LOWAT && messageQueue.size() < HIGHWAT ) {
					// Drop the message with a probability that is linear in the range 0 to 1 
					float threshold = ((float)(messageQueue.size() - LOWAT))/ ((float)(HIGHWAT - LOWAT));
					Random rand = new Random(System.currentTimeMillis());
					boolean decision = rand.nextFloat() > (1.0 - threshold);
					if (decision) {
						if (LogWriter.needsLogging)
							LogWriter.logMessage("Dropping message with probability " + (1.0 - threshold));
						continue;
					}
					
				} 

				if (sipStack.threadPoolSize != -1) {
					// Note: the only condition watched for by threads
					// synchronizing on the messageQueue member is that it is
					// not empty. As soon as you introduce some other
					// condition you will have to call notifyAll instead of
					// notify below.

					synchronized (this.messageQueue) {
						messageQueue.addElement(packet);
						messageQueue.notify();
					}
				} else {
					new UDPMessageChannel(packet, sipStack, this);
				}
			} catch (IOException ex) {
				if (running && LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "UDPMessageProcessor: Got an IO Exception");
				running = false;
			} catch (Exception ex) {
				if (running &&  LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "UDPMessageProcessor: Unexpected Exception - quitting");
				running = false;
				InternalErrorHandler.handleException(ex);
				return;
			}
		}
	
    }
    
    /**
     * Return the transport string.
     *@return the transport string
     */
    public String getTransport() {
          return "UDP";
    }
    
    
    /** Get the port from which the UDPMessageProcessor is reading messages
     * @return Port from which the udp message processor is
     * reading messages.
     */
    public int getPort() {
        return this.port;
    }

    
    /** Create and return new TCPMessageChannel for the given host/port.
     */
    public MessageChannel createMessageChannel(HostPort targetHostPort) {
        return new UDPMessageChannel(targetHostPort.getHost().getHostname(),
        targetHostPort.getPort(), sipStack,this);
    }
    
    public MessageChannel createMessageChannel(String host, int port) {
        return new UDPMessageChannel(host, port, sipStack,this);
    }
    
    /** Default target port for UDP
     */
    public int getDefaultTargetPort() {
        return 5060;
    }
    
    /** UDP is not a secure protocol.
     */
    public boolean isSecure() {
        return false;
    }
    
	/**
	 * UDP can handle a message as large as the MAX_DATAGRAM_SIZE.
	 */
	public int getMaximumMessageSize() {
		return MAX_DATAGRAM_SIZE;
	}
    
	/**
	 * Return true if there are any messages in use.
	 */
	public boolean inUse() {
		synchronized (messageQueue) {
			return messageQueue.size() != 0;
		}
	}

	public SIPMessageStack getSIPStack() {
		return sipStack;
	}
    
}
