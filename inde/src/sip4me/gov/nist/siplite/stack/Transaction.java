package sip4me.gov.nist.siplite.stack;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.SIPConstants;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.header.ViaList;
import sip4me.gov.nist.siplite.message.Message;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.parser.StringMsgParser;



/**
 *	Abstract class to support both client and server transactions.  
 * Provides an encapsulation of a message channel, handles timer events, 
 * and creation of the Via header for a message.
 *
 *	@author	Jeff Keyser 
 *      @author M. Ranganathan 
 * (modified Jeff's original source and aligned   with JAIN-SIP 1.1)
 *      @version JAIN-SIP-1.1
 *
 */
public abstract class Transaction
	extends MessageChannel {

	
            
            protected static final int BASE_TIMER_INTERVAL = 
                        SIPTransactionStack.BASE_TIMER_INTERVAL;
            
            
            /** One timer tick. */
            protected static final int T1 = 1;
            
             /** 5 sec Maximum duration a message will remain in the network  */         
	    protected static final int T4 =  5000/BASE_TIMER_INTERVAL;
            
            
            /** The maximum retransmit interval for non-INVITE
            requests and INVITE responses */
            
	    protected static final int T2 = 4000/BASE_TIMER_INTERVAL;
            
            
            /** INVITE request retransmit interval, for UDP only */
	    protected static final int TIMER_A = 1;

            /** INVITE transaction  timeout timer */
	    protected static final int TIMER_B = 64;
	    
	    protected static final int TIMER_J = 64;

	    protected static final int TIMER_F = 64;

	    protected static final int TIMER_H = 64;

	    protected static final int TIMER_I = T4;

	    protected static final int TIMER_K = T4;
	
	    protected static final int TIMER_D = 32000/BASE_TIMER_INTERVAL ;

	    protected static final int  TIMER_C = 3*60*1000/BASE_TIMER_INTERVAL;
            
            protected Response lastResponse;
            
            protected  Dialog  dialog;
	
	    protected  boolean ackSeenFlag;

	    protected boolean toListener;

            
            
        /** Initialized but no state assigned.
         */
         public static final int INITIAL_STATE = -1;

	/**
	 *	Trying state.
	 */
	public static final int	TRYING_STATE = 1;
 	
	/** CALLING State.
	*/
	public static final int	CALLING_STATE = 2;
	/** 	Proceeding state.
	 */
	public static final int	PROCEEDING_STATE = 3;
	/**	Completed state.
	 */
	public static final int	COMPLETED_STATE = 4;
	/**	Confirmed state.
	 */
	public static final int	CONFIRMED_STATE = 5;
	/** 	Terminated state.  
	*/
	public static final int	TERMINATED_STATE = 6;
	/**
	 *	Maximum number of ticks between retransmissions.
	 */
	protected static final int	MAXIMUM_RETRANSMISSION_TICK_COUNT = 8;

	// Parent stack for this transaction
	protected SIPTransactionStack	parentStack;
	// Original request that is being handled by this transaction
	private Request			originalRequest;
	// Underlying channel being used to send messages for this transaction
	protected MessageChannel   encapsulatedChannel;
	// Transaction branch ID
	private String			branch;
	// Current transaction state
	private int			currentState;
	// Number of ticks the retransmission timer was set to last
	private int		retransmissionTimerLastTickCount;
	// Number of ticks before the message is retransmitted
	private int		retransmissionTimerTicksLeft;
	// Number of ticks before the transaction times out
	private int		timeoutTimerTicksLeft;
	// List of event listeners for this transaction
	private Vector		eventListeners;
     
	// Flag to indcate that this has been cancelled.
	protected boolean	isCancelled;

	//Object representing the connection being held by the JSR180 Implementation
	//It can be either a SipClientConnection in case of a ClientTransaction 
	//or a SipConnectionNotifier in case of a ServerTransaction
	protected Object applicationData; 

	/**
	 * Retrieve the application data
	 * @return Object representing the connection being held by the JSR180 
	 * Implementation. It can be either a SipClientConnection in case of a 
	 * ClientTransaction or a SipConnectionNotifier in case of a ServerTransaction
	 */
	public Object getApplicationData(){
		return applicationData;
	}

	/**
	 * Set the application data
	 * @param applicationData - Object representing the connection being held by the JSR180 
	 * Implementation. It can be either a SipClientConnection in case of a 
	 * ClientTransaction or a SipConnectionNotifier in case of a ServerTransaction
	 */
	public void setApplicationData(Object applicationData){
		this.applicationData=applicationData;
	}

	public String getBranchId() { return this.branch; }
    


	/**
	 *	Transaction constructor.
	 *
	 *	@param newParentStack Parent stack for this transaction.
	 *	@param newEncapsulatedChannel 
	 * 		Underlying channel for this transaction.
	 */
	protected Transaction(
		SIPTransactionStack	newParentStack,
		MessageChannel		newEncapsulatedChannel
	) {

		parentStack = newParentStack;
		encapsulatedChannel = newEncapsulatedChannel;
                
                
                this.currentState = INITIAL_STATE;
		
		disableRetransmissionTimer( );
		disableTimeoutTimer( );
		eventListeners = new Vector();

		// Always add the parent stack as a listener 
		// of this transaction
		addEventListener( newParentStack );

	}


	/**
	 *	Sets the request message that this transaction handles.
	 *
	 *	@param newOriginalRequest Request being handled.
	 */
	public void setOriginalRequest(
		Request	newOriginalRequest
	) {

		// Branch value of topmost Via header
		String	newBranch;


		originalRequest = newOriginalRequest;

		originalRequest.setTransaction(this);

		// If the message has an explicit branch value set,
		newBranch = ( (ViaHeader)newOriginalRequest.getViaHeaders( ).
				getFirst( ) ).getBranch( );
		if( newBranch != null ) {
			if (LogWriter.needsLogging) 
			   LogWriter.logMessage("Setting Branch id : " 
				+ newBranch);

			// Override the default branch with the one 
			// set by the message
			setBranch( newBranch );

		} else {
			if (LogWriter.needsLogging) 
				LogWriter.logMessage("Branch id is null!"
						+ newOriginalRequest.encode());


		}

	}


	/**
	 *	Gets the request being handled by this transaction.
	 *
	 *	@return Request being handled.
	 */
	public  Request getOriginalRequest(
	) {

		return originalRequest;

	}

	/** Get the original request but cast to a Request structure.
	*
	* @return the request that generated this transaction.
	*/
	public Request getRequest() {
		return (Request) originalRequest;
	}


	/**
	 *  Returns a flag stating whether this transaction is for an 
	 *	INVITE request or not.
	 *
	 *	@return True if this is an INVITE request, false if not.
	 */
	protected final boolean isInviteTransaction(
	) {

		return originalRequest.getMethod( ).equals
				( Request.INVITE );

	}

	/** Return true if the transaction corresponds to a CANCEL message.
	*
	*@return true if the transaciton is a CANCEL transaction.
	*/
	protected final boolean isCancelTransaction(
	) {

		return originalRequest.getMethod( ).equals
				( Request.CANCEL );

	}

	/** Return a flag that states if this is a BYE transaction.
	*
	*@return true if the transaciton is a BYE transaction.
	*/
	protected final boolean isByeTransaction(
	) {
		return originalRequest.getMethod( ).equals
				( Request.BYE );
	}
		


	/**
	 *  Returns the message channel used for 
	 * 		transmitting/receiving messages
	 * for this transaction. Made public in support of JAIN dual 
	 * transaction model.
	 *
	 *	@return Encapsulated MessageChannel.
	 *
	 */
	public MessageChannel getMessageChannel(
	) {

		return encapsulatedChannel;

	}


	/**
	 *	Sets the Via header branch parameter used to identify 
	 * this transaction.
	 *
	 *@param newBranch New string used as the branch 
	 * 	for this transaction.
	 */
	public final void setBranch(
		String	newBranch
	) {

		branch = newBranch;

	}


	/**
	 *Gets the current setting for the branch parameter of this transaction.
	 *
	 *@return Branch parameter for this transaction.
	 */
	public final String getBranch(
	) {
		if (this.branch == null) {
		    this.branch = 
		    getOriginalRequest().getTopmostVia().getBranch();
		}
		return branch;

	}


	/**
	 *Changes the state of this transaction.
	 *
	 *@param newState New state of this transaction.
	 */
	public void setState(
		int		newState
	) {

		currentState = newState;
		if (LogWriter.needsLogging)  {
		    LogWriter.logMessage
		    ("setState " + this + " " + newState);
		}

		// If this transaction is being terminated,
		if( newState == TERMINATED_STATE ) {
			if (LogWriter.needsLogging) {
				LogWriter.logMessage("Terminating transaction");
			}

		}

	}

	


	/**
	 *	Gets the current state of this transaction.
	 *
	 *	@return Current state of this transaction.
	 */
	public final int getState() {
	    return this.currentState;
                   

	}


	/**
	 * Enables retransmission timer events for this transaction to begin 
	 * in one tick.
	 * FIXME: Increased to 2 because of how the SIPTransactionStack
	 * TransactionScanner loop is implemented right now
	 */
	protected final void enableRetransmissionTimer() {

		enableRetransmissionTimer( 2 );

	}


	/**
	 *Enables retransmission timer events for this 
	 * transaction to begin after the number of ticks passed to 
	 * this routine.
	 *
	 *@param tickCount Number of ticks before the 
	 * 	next retransmission timer
	 *	event occurs.
	 */
	protected final void enableRetransmissionTimer(int tickCount) {

		retransmissionTimerTicksLeft = 
		Math.min( tickCount, MAXIMUM_RETRANSMISSION_TICK_COUNT );
		retransmissionTimerLastTickCount = 
		retransmissionTimerTicksLeft;

	}


	/**
	 *	Turns off retransmission events for this transaction.
	 */
	protected final void disableRetransmissionTimer() {

		retransmissionTimerTicksLeft = -1;

	}


	/**
	 *Enables a timeout event to occur for this transaction after the number
	 * of ticks passed to this method.
	 *
	 *@param tickCount Number of ticks before this transaction times out.
	 */
	protected final void enableTimeoutTimer(int tickCount) {

		timeoutTimerTicksLeft = tickCount;

	}


	/**
	 *	Disabled the timeout timer.
	 */
	protected final void disableTimeoutTimer(
	) {
//		System.out.println("********************** disabling timeout timer " + this);

		timeoutTimerTicksLeft = -1;

	}

	/**
	 * Fired after each timer tick. Checks the retransmission and timeout timers
	 * of this transaction, and fired these events if necessary.
	 */
	synchronized final void fireTimer() {

		// If the timeout timer is enabled,
		if( timeoutTimerTicksLeft != -1 ) {

			// Count down the timer, and if it has run out,
			if( --timeoutTimerTicksLeft == 0 ) {

				// Fire the timeout timer
				fireTimeoutTimer( );

			}

		}

		// If the retransmission timer is enabled,
		if( retransmissionTimerTicksLeft != -1 ) {

			// Count down the timer, and if it has run out,
			if( --retransmissionTimerTicksLeft == 0 ) {

				// Enable this timer to fire again after 
				// twice the original time
				enableRetransmissionTimer
				( retransmissionTimerLastTickCount * 2 );

				// Fire the timeout timer
				fireRetransmissionTimer( );

			}

		}

	}


	/**
	 *	Tests a message to see if it is part of this transaction.
	 *
	 *	@return True if the message is part of this 
	 * 		transaction, false if not.
	 */
	public abstract boolean isMessagePartOfTransaction(
		Message	messageToHeaderTest
	);


	/**
	 *	This method is called when this transaction's 
	 * retransmission timer has fired.
	 */
	protected abstract void fireRetransmissionTimer(
	);


	/**
	 *	This method is called when this transaction's 
	 * timeout timer has fired.
	 */
	protected abstract void fireTimeoutTimer(
	);


	/**
	 *	Tests if this transaction has terminated.
	 *
	 *	@return True if this transaction is terminated, false if not.
	 */
	protected final boolean isTerminated(
	) {

		return
		      getState( ) == TERMINATED_STATE;

	}


	public String getHost(
	) {

		return encapsulatedChannel.getHost( );

	}


	public String getKey(
	) {

		return encapsulatedChannel.getKey( );

	}


	public int getPort(
	) {

		return encapsulatedChannel.getPort( );

	}


	public SIPMessageStack getSIPStack(
	) {

		return parentStack;

	}


	public String getPeerAddress(
	) {

		return encapsulatedChannel.getPeerAddress( );

	}

	public int getPeerPort (
	)  {
		return encapsulatedChannel.getPeerPort();
	}
		


	

	public String getTransport(
	) {

		return encapsulatedChannel.getTransport( );

	}


	public boolean isReliable(
	) {

		return encapsulatedChannel.isReliable( );

	}


	/**
	 *Returns the Via header for this channel.  Gets the Via header of the
	 *underlying message channel, and adds a branch parameter to it for this
	 *transaction.
	 */
	public ViaHeader getViaHeader(
	) {

		// Via header of the encapulated channel
		ViaHeader	channelViaHeader;


		// Add the branch parameter to the underlying 
		// channel's Via header
		channelViaHeader = super.getViaHeader( );
		
		channelViaHeader.setBranch( branch );
		
		return channelViaHeader;

	}




	public void handleException(
		SIPServerException	ex
	) {

		encapsulatedChannel.handleException( ex );

	}


	/**
	 *	Process the message through the transaction and sends it to the SIP
	 * peer.
	 *
	 *	@param messageToHeaderSend Message to send to the SIP peer.
	 */
	abstract public void sendMessage(
		Message	messageToHeaderSend
	) throws IOException;




	/**
	 *	Parse the byte array as a message, process it through the 
	 * transaction, and send it to the SIP peer.
	 *
	 *	@param messageBytes Bytes of the message to send.
	 *	@param receiverAddress Address of the target peer.
	 *	@param receiverPort Network port of the target peer.
	 *
	 *	@throws IOException If there is an error parsing 
	 * the byte array into an object.
	 */
	protected void sendMessage(
		byte[]		messageBytes,
		String  	receiverAddress,
		int			receiverPort
	) throws IOException {

		// Object representation of the SIP message
		Message		messageToHeaderSend;

		try {
			StringMsgParser messageParser = new StringMsgParser();
			messageToHeaderSend = 
			messageParser.parseSIPMessage( messageBytes );
			sendMessage( messageToHeaderSend );
		} catch( ParseException e ) {
			throw new IOException( e.getMessage( ) );
		}

	}
	
	/**
	 * Parse the byte array as a message, process it through the transaction,
	 * and send it to the SIP peer. This is just a placeholder method -- calling
	 * it will result in an IO exception.
	 * 
	 * @param messageBytes
	 *            Bytes of the message to send.
	 * @param receiverAddress
	 *            Address of the target peer.
	 * @param receiverPort
	 *            Network port of the target peer.
	 * 
	 * @throws IOException
	 *             If called.
	 */
	protected void sendMessage(byte[] messageBytes,
			String receiverAddress, int receiverPort, boolean retry)
			throws IOException {
		throw new IOException(
				"Cannot send unparsed message through Transaction Channel!");
	}


	/**
	 *	Adds a new event listener to this transaction.
	 *
	 *	@param newListener Listener to add.
	 */
	public void addEventListener(
		SIPTransactionEventListener	newListener
	) {

		eventListeners.addElement( newListener );

	}


	/**
	 *	Removed an event listener from this transaction.
	 *
	 *	@param oldListener Listener to remove.
	 */
	public void removeEventListener(
		SIPTransactionEventListener	oldListener
	) {

		eventListeners.removeElement( oldListener );

	}


	/**
	 * Creates a SIPTransactionErrorEvent and sends it 
	 * to all of the listeners of this transaction.  
         * This method also flags the transaction as
	 * terminated.
	 *
	 *	@param errorEventID ID of the error to raise.
	 */
	protected void raiseErrorEvent(
		int	errorEventID
	) {

		// Error event to send to all listeners
		SIPTransactionErrorEvent	newErrorEvent;
		// Iterator through the list of listeners
		Enumeration			listenerIterator;
		// Next listener in the list
		SIPTransactionEventListener	nextListener;

		// Create the error event
		newErrorEvent = new SIPTransactionErrorEvent( this, 
					errorEventID );

		// Loop through all listeners of this transaction
		synchronized( eventListeners ) {
			listenerIterator = eventListeners.elements( );
			while( listenerIterator.hasMoreElements( ) ) {
				// Send the event to the next listener
				nextListener = (SIPTransactionEventListener)
						listenerIterator.nextElement( );
				nextListener.transactionErrorEvent
						( newErrorEvent );

			}

		}
		// Clear the event listeners after propagating the error.
	        eventListeners.removeAllElements();

		// Errors always terminate a transaction
		setState( TERMINATED_STATE );
		
		if (this instanceof ServerTransaction &&
		    this.isByeTransaction() && this.dialog != null )
	 	    this.dialog.setState(Dialog.TERMINATED_STATE);
		

	}


        
        
        /** A shortcut way of telling if we are a server transaction.
         */
        protected boolean IsServerTransaction() {
                return this instanceof ServerTransaction;
        }

        /** Gets the dialog object of this Transaction object. This object
         * returns null if no dialog exists. A dialog only exists for a
         * transaction when a session is setup between a User Agent Client and a
         * User Agent Server, either by a 1xx Provisional Response for an early
         * dialog or a 200OK Response for a committed dialog.
         *
         * @return the Dialog Object of this Transaction object.
         * @see Dialog
         */
        public Dialog getDialog() {
            return this.dialog;
        }
        
        /** set the dialog object.
         *@param dialog -- the dialog to set.
         */
        public void setDialog(Dialog dialog) {
            this.dialog = dialog;
        }
        
        /** Returns the current value of the retransmit timer in 
	 * milliseconds used to retransmit messages over unreliable transports.
         *
         * @return the integer value of the retransmit timer in milliseconds.
         */
        public int getRetransmitTimer() {
            return SIPTransactionStack.BASE_TIMER_INTERVAL;
        }
        
        /** Get the host to assign for an outgoing Request via header.
         */
        public String getViaHost() {
            return this.getViaHeader().getHost();
            
        }
        
        /** Get the last response.
         */
        public Response getLastResponse() { return this.lastResponse; }

	
	

	/** Get the transaction Id.
	*/
	public String getTransactionId() {
		return this.getOriginalRequest().getTransactionId();
	}
        
        
        
        /** Get the port to assign for the via header of an outgoing message.
         */
        public int getViaPort() {
            return this.getViaHeader().getPort();
        }
        
        /** A method that can be used to test if an incoming request
        * belongs to this transction. This does not take the transaction
        * state into account when doing the check otherwise it is identical
        * to isMessagePartOfTransaction. This is useful for checking if
        * a CANCEL belongs to this transaction.
        *
        * @param requestToHeaderTest is the request to test.
        * @return true if the the request belongs to the transaction.
        *
        */
    public boolean doesCancelMatchTransaction(Request requestToHeaderTest) {
        
        // List of Via headers in the message to test
        ViaList	viaHeaders;
        // ToHeaderpmost Via header in the list
        ViaHeader		topViaHeader;
        // Branch code in the topmost Via header
        String	messageBranch;
        // Flags whether the select message is part of this transaction
        boolean transactionMatches;
        
        
        transactionMatches = false;
        
        if (this.getOriginalRequest() == null
				|| this.getOriginalRequest().getMethod().equals(Request.CANCEL))
			return false;
        // Get the topmost Via header and its branch parameter
        viaHeaders = requestToHeaderTest.getViaHeaders( );
        if( viaHeaders != null ) {
            
            topViaHeader = (ViaHeader)viaHeaders.getFirst( );
            messageBranch = topViaHeader.getBranch( );
            if( messageBranch != null ) {
                
                // If the branch parameter exists but
                // does not start with the magic cookie,
                if( !messageBranch.toUpperCase().startsWith
                ( SIPConstants.BRANCH_MAGIC_COOKIE.toUpperCase() ) ) {
                    
                    // Flags this as old
                    // (RFC2543-compatible) client
                    // version
                    messageBranch = null;
                    
                }
                
            }
            
            // If a new branch parameter exists,
			if (messageBranch != null && this.getBranch() != null) {
                
                // If the branch equals the branch in
				// this message,
				if (getBranch().equals(messageBranch)
						&& topViaHeader.getSentBy().equals(
								((ViaHeader) getOriginalRequest()
										.getViaHeaders().getFirst())
										.getSentBy())) {
					transactionMatches = true;
					if (LogWriter.needsLogging)
						LogWriter.logMessage("returning  true");
				}
                
            } else {
                // If this is an RFC2543-compliant message,
                // If RequestURI, ToHeader tag, FromHeader tag,
                // CallIdHeader, CSeqHeader number, and top Via
                // headers are the same,
		if (LogWriter.needsLogging)
			LogWriter.logMessage("testing against " +
				getOriginalRequest());
                
                
                if (

		getOriginalRequest().getRequestURI().equals
		(requestToHeaderTest.getRequestURI()) &&

		getOriginalRequest().getTo().equals
		(requestToHeaderTest.getTo()) &&
		
		 getOriginalRequest().getFromHeader().equals
		(requestToHeaderTest.getFromHeader()) &&

                getOriginalRequest().getCallId( ).
		getCallId( ).equals
		( requestToHeaderTest.getCallId( ) .getCallId( )) &&

                getOriginalRequest( ).
                getCSeqHeader( ).getSequenceNumber( ) ==
                requestToHeaderTest.getCSeqHeader().
                getSequenceNumber() &&

                topViaHeader.equals
		( getOriginalRequest().
                  getViaHeaders().getFirst())

                
		) {
                    
                    transactionMatches = true;
                }
                
            }
            
        }
		// JvB: Need to pass the CANCEL to the listener! Retransmitted INVITEs
		// set it to false
		if (transactionMatches) {
			this.toListener = true;
		}
        return transactionMatches;
    }

    public boolean passToListener() {
		return toListener;
	}
     
	/**
	 * Set the passToListener flag to true.
	 */
	public void setPassToListener() {
		if (LogWriter.needsLogging) {
			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "setPassToListener() on transaction " + this);
		}
		this.toListener = true;
	}
       
     /** Close the encapsulated channel.
      */
     public void close() { this.encapsulatedChannel.close(); }

     public boolean isSecure() { 
	  return encapsulatedChannel.isSecure();
     }

     /** Get the message processor handling this transaction.
     */
     public MessageProcessor getMessageProcessor() {
	 return this.encapsulatedChannel.getMessageProcessor();
     }

     public  void setAckSeen() {
		this.ackSeenFlag = true;
     }

     public boolean isAckSeen() {
		return this.ackSeenFlag;
     }


}
