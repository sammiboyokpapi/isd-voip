package sip4me.gov.nist.siplite.stack;

import java.io.IOException;
import java.util.Vector;

import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.siplite.SIPConstants;
import sip4me.gov.nist.siplite.SIPUtils;
import sip4me.gov.nist.siplite.SipException;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.address.SipURI;
import sip4me.gov.nist.siplite.address.URI;
import sip4me.gov.nist.siplite.header.ContactHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.RecordRouteHeader;
import sip4me.gov.nist.siplite.header.RecordRouteList;
import sip4me.gov.nist.siplite.header.RouteHeader;
import sip4me.gov.nist.siplite.header.RouteList;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.header.ViaList;
import sip4me.gov.nist.siplite.message.Message;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;



/**
 *Represents a client transaction.
 *
 *@author Jeff Keyser
 *@author M. Ranganathan <mranga@nist.gov>
 *(Modified Jeff's code and aligned with JAIN SIP 1.1)<br/>
 *
 *@version  JAIN-SIP-1.1
 *
 *
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 *<pre>
 *
 * Implements the following state machines. (FromHeader RFC 3261)
 *
 *                               |INVITE from TU
 *             Timer A fires     |INVITE sent
 *             Reset A,          V                      Timer B fires
 *             INVITE sent +-----------+                or Transport Err.
 *               +---------|           |---------------+inform TU
 *               |         |  Calling  |               |
 *               +-------->|           |-------------->|
 *                         +-----------+ 2xx           |
 *                            |  |       2xx to TU     |
 *                            |  |1xx                  |
 *    300-699 +---------------+  |1xx to TU            |
 *   ACK sent |                  |                     |
 *resp. to TU |  1xx             V                     |
 *            |  1xx to TU  -----------+               |
 *            |  +---------|           |               |
 *            |  |         |Proceeding |-------------->|
 *            |  +-------->|           | 2xx           |
 *            |            +-----------+ 2xx to TU     |
 *            |       300-699    |                     |
 *            |       ACK sent,  |                     |
 *            |       resp. to TU|                     |
 *            |                  |                     |      NOTE:
 *            |  300-699         V                     |
 *            |  ACK sent  +-----------+Transport Err. |  transitions
 *            |  +---------|           |Inform TU      |  labeled with
 *            |  |         | Completed |-------------->|  the event
 *            |  +-------->|           |               |  over the action
 *            |            +-----------+               |  to take
 *            |              ^   |                     |
 *            |              |   | Timer D fires       |
 *            +--------------+   | -                   |
 *                               |                     |
 *                               V                     |
 *                         +-----------+               |
 *                         |           |               |
 *                         | Terminated|<--------------+
 *                         |           |
 *                         +-----------+
 *
 *                 Figure 5: INVITE client transaction
 *
 *
 *                                   |Request from TU
 *                                   |send request
 *               Timer E             V
 *               send request  +-----------+
 *                   +---------|           |-------------------+
 *                   |         |  Trying   |  Timer F          |
 *                   +-------->|           |  or Transport Err.|
 *                             +-----------+  inform TU        |
 *                200-699         |  |                         |
 *                resp. to TU     |  |1xx                      |
 *                +---------------+  |resp. to TU              |
 *                |                  |                         |
 *                |   Timer E        V       Timer F           |
 *                |   send req +-----------+ or Transport Err. |
 *                |  +---------|           | inform TU         |
 *                |  |         |Proceeding |------------------>|
 *                |  +-------->|           |-----+             |
 *                |            +-----------+     |1xx          |
 *                |              |      ^        |resp to TU   |
 *                | 200-699      |      +--------+             |
 *                | resp. to TU  |                             |
 *                |              |                             |
 *                |              V                             |
 *                |            +-----------+                   |
 *                |            |           |                   |
 *                |            | Completed |                   |
 *                |            |           |                   |
 *                |            +-----------+                   |
 *                |              ^   |                         |
 *                |              |   | Timer K                 |
 *                +--------------+   | -                       |
 *                                   |                         |
 *                                   V                         |
 *             NOTE:           +-----------+                   |
 *                             |           |                   |
 *         transitions         | Terminated|<------------------+
 *         labeled with        |           |
 *         the event           +-----------+
 *         over the action
 *         to take
 *
 *                 Figure 6: non-INVITE client transaction
 *
 *
 *</pre>
 *
 */
public class ClientTransaction
extends Transaction
implements SIPServerResponseInterface {
    
    private Request lastRequest;

    
   private boolean eventPending;
    
    private int viaPort;
    
    private String viaHost;
    
    // Real ResponseInterface to pass messages to
    private SIPServerResponseInterface	respondTo;

	private Hop nextHop;
    
    
    /**
     *	Creates a new client transaction.
     *
     *	@param newSIPMessageStack Transaction stack this transaction
     *      belongs to.
     *	@param newChannelToHeaderUse Channel to encapsulate.
     */
    protected ClientTransaction(
    SIPTransactionStack	newSIPMessageStack,
    MessageChannel		newChannelToHeaderUse
    ) {
        super( newSIPMessageStack, newChannelToHeaderUse );
        setBranch( SIPUtils.generateBranchId());
        if (LogWriter.needsLogging) {
            LogWriter.logMessage("Creating clientTransaction " + this);
        }
        
    }
    
    
    /**
     *	Sets the real ResponseInterface this transaction encapsulates.
     *
     *	@param newRespondToHeader ResponseInterface to send messages to.
     */
    public void setResponseInterface(
    SIPServerResponseInterface	newRespondToHeader
    ) {
        
        respondTo = newRespondToHeader;
        
    }
    
    
    public String getProcessingInfo(
    ) {
        
        return respondTo.getProcessingInfo( );
        
    }
    
    
    /**
     *	Returns this transaction.
     */
    public MessageChannel getRequestChannel(
    ) {
        
        return this;
        
    }
    
    
    /**
     *	Deterines if the message is a part of this transaction.
     *
     *	@param messageToHeaderTest Message to check if it is part of this
     *		transaction.
     *
     *	@return True if the message is part of this transaction,
     * 		false if not.
     */
    public  boolean isMessagePartOfTransaction(
    Message	messageToHeaderTest
    ) {
        
        // List of Via headers in the message to test
        ViaList	viaHeaders = messageToHeaderTest.getViaHeaders( );
        // Flags whether the select message is part of this transaction
        boolean transactionMatches;
        String messageBranch = ((ViaHeader)viaHeaders.getFirst()).getBranch();
        boolean rfc3261Compliant =
        getBranch() != null &&
        messageBranch != null &&
        getBranch().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE) &&
        messageBranch.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE);
        
        /**
         * if (LogWriter.needsLogging)  {
         * LogWriter.logMessage("--------- TEST ------------");
         * LogWriter.logMessage(" testing " + this.getOriginalRequest());
         * LogWriter.logMessage("Against " + messageToHeaderTest);
         * LogWriter.logMessage("isTerminated = " + isTerminated());
         * LogWriter.logMessage("messageBranch = " + messageBranch);
         * LogWriter.logMessage("viaList = " + messageToHeaderTest.getViaHeaders());
         * LogWriter.logMessage("myBranch = " + getBranch());
         * }
         **/
        
        transactionMatches = false;
        if( !isTerminated( ) ) {
            if (rfc3261Compliant) {
                if( viaHeaders != null ) {
                    // If the branch parameter is the
                    //same as this transaction and the method is the same,
                    if( getBranch().equals
                    (((ViaHeader)viaHeaders.getFirst()).
                    getBranch())) {
                        transactionMatches =
                        getOriginalRequest().getCSeqHeader().
                        getMethod().equals
                        ( messageToHeaderTest.getCSeqHeader().
                        getMethod());
                        
                    }
                }
            } else {
                transactionMatches =
                getOriginalRequest().getTransactionId().equals
                (messageToHeaderTest.getTransactionId());
                
            }
            
        }
        return transactionMatches;
        
    }
    
    
    /**
     *  Send a request message through this transaction and
     *  onto the client.
     *
     *	@param messageToHeaderSend Request to process and send.
     */
    public void sendMessage(
    Message	messageToHeaderSend
    ) throws IOException {
        
        // Message typecast as a request
        Request	transactionRequest;
        
        
        transactionRequest = (Request)messageToHeaderSend;
        
        
        // Set the branch id for the top via header.
        ViaHeader topVia =
        (ViaHeader) transactionRequest.getViaHeaders().getFirst();
        // Tack on a branch identifier to match responses.
        
        topVia.setBranch(getBranch());
        
        
        // If this is the first request for this transaction,
        if( getState( ) == -1 ) {
            // Save this request as the one this transaction
            // is handling
            setOriginalRequest( transactionRequest );
            
            // Change to trying/calling state
            if (transactionRequest.getMethod().equals(Request.INVITE)) {
                setState(CALLING_STATE);
            } else if (transactionRequest.getMethod().equals(Request.ACK)) {
                // Acks are never retransmitted.
                setState(TERMINATED_STATE);
            } else {
                setState( TRYING_STATE );
            }
            if( !isReliable( ) ) {
                
                enableRetransmissionTimer( );
                
            }
            
            if (isInviteTransaction()) {
                enableTimeoutTimer( TIMER_B );
            }  else {
                enableTimeoutTimer( TIMER_F );
            }
            
        } else if  (getState() == PROCEEDING_STATE ||
        getState() == CALLING_STATE ) {
            
            // If this is a TU-generated ACK request,
            if( transactionRequest.getMethod().equals( Request.ACK ) ) {
                // Send directly to the underlying
                // transport and close this transaction
                setState( TERMINATED_STATE );
                getMessageChannel().sendMessage
                (transactionRequest );
                return;
                
            }
            
        }
        try {
            
            // Send the message to the server
            lastRequest = transactionRequest;
            getMessageChannel( ).sendMessage( transactionRequest );
            
        } catch( IOException e ) {
            
            setState( TERMINATED_STATE );
            throw e;
            
        }
        
    }
    
    
    /**
     *	Process a new response message through this transaction.
     * If necessary, this message will also be passed onto the TU.
     *
     *	@param transactionResponse Response to process.
     *	@param sourceChannel Channel that received this message.
     */
	public synchronized void processResponse(Response transactionResponse,
			MessageChannel sourceChannel) throws SIPServerException {
		// Log the incoming response in our log file.
		if (ServerLog.needsLogging(ServerLog.TRACE_MESSAGES))
			this.logResponse(transactionResponse, System.currentTimeMillis(),
					"normal processing");
		// Ignore 1xx
		if (getState() == COMPLETED_STATE
				&& transactionResponse.getStatusCode() / 100 == 1) {
			return;
		} else if (PROCEEDING_STATE == this.getState()
				&& transactionResponse.getStatusCode() == 100) {
			// Ignore 100 if received after 180
			// bug report from Peter Parnes.
			return;
		} else if (eventPending) {
			// drop as listener is computing.
			return;
		}

		if (LogWriter.needsLogging)
			LogWriter.logMessage("processing "
					+ transactionResponse.getFirstLine() + "current state = "
					+ getState());

		this.lastResponse = transactionResponse;

		if (dialog != null) {
			// add the route before you process the response.
			// Bug noticed by Brad Templeton.
			dialog.addRoute(transactionResponse);
		}
		String method = transactionResponse.getCSeqHeader().getMethod();

		if (dialog != null) {
			SIPTransactionStack sipStackImpl = (SIPTransactionStack) getSIPStack();

			// A tag just got assigned or changed.
			if (dialog.getRemoteTag() == null
					&& transactionResponse.getTo().getTag() != null) {

				// Don't assign tag on provisional response
				if (transactionResponse.getStatusCode() != 100) {
					dialog.setRemoteTag(transactionResponse.getToTag());
				}
				String dialogId = transactionResponse.getDialogId(false);
				dialog.setDialogId(dialogId);

				if (sipStackImpl.isDialogCreated(method)
						&& transactionResponse.getStatusCode() != 100) {
					sipStackImpl.putDialog(dialog);
					if (transactionResponse.getStatusCode() / 100 == 1)
						dialog.setState(Dialog.EARLY_STATE);
					else if (transactionResponse.isSuccessfulResponse()) {
						dialog.setState(Dialog.CONFIRMED_STATE);
					}
				}

			} else if (dialog.getRemoteTag() != null
					&& transactionResponse.getToTag() != null
					&& !dialog.getRemoteTag().equals(
							transactionResponse.getToTag())) {

				dialog.setRemoteTag(transactionResponse.getToTag());
				String dialogId = transactionResponse.getDialogId(false);
				dialog.setDialogId(dialogId);
				if (sipStackImpl.isDialogCreated(method)) {
					sipStackImpl.putDialog(dialog);
				}
			}

			if (sipStackImpl.isDialogCreated(method)) {
				// Make a final tag assignment.
				if (transactionResponse.getTo().getTag() != null
						&& transactionResponse.isSuccessfulResponse()) {

					// This is a dialog creating method (such as INVITE).
					// 2xx response -- set the state to the confirmed
					// state.
					dialog.setRemoteTag(transactionResponse.getToTag());
					dialog.setState(Dialog.CONFIRMED_STATE);
				} else if ((transactionResponse.getStatusCode() >= 300)
						&& (dialog.getState() == -1 || dialog.getState() == Dialog.EARLY_STATE)) {
					// Invite transaction generated an error.
					dialog.setState(Dialog.TERMINATED_STATE);
				}
			}
			// 200 OK for a bye so terminate the dialog.
			if (transactionResponse.getCSeqHeader().getMethod().equals("BYE")
					&& transactionResponse.isSuccessfulResponse()) {
				dialog.setState(Dialog.TERMINATED_STATE);
			}
		}

		try {
			if (isInviteTransaction())
				inviteClientTransaction(transactionResponse, sourceChannel);
			else
				nonInviteClientTransaction(transactionResponse, sourceChannel);
		} catch (IOException ex) {
			setState(TERMINATED_STATE);
			raiseErrorEvent(SIPTransactionErrorEvent.TRANSPORT_ERROR);
		}
	}
    
    /** Implements the state machine for invite client transactions.
     * @param transactionResponse -- transaction response received.
     * @param sourceChannel - source channel on which the response was received.
     *<pre>
     *
     *                                   |Request from TU
     *                                   |send request
     *               Timer E             V
     *               send request  +-----------+
     *                   +---------|           |-------------------+
     *                   |         |  Trying   |  Timer F          |
     *                   +-------->|           |  or Transport Err.|
     *                             +-----------+  inform TU        |
     *                200-699         |  |                         |
     *                resp. to TU     |  |1xx                      |
     *                +---------------+  |resp. to TU              |
     *                |                  |                         |
     *                |   Timer E        V       Timer F           |
     *                |   send req +-----------+ or Transport Err. |
     *                |  +---------|           | inform TU         |
     *                |  |         |Proceeding |------------------>|
     *                |  +-------->|           |-----+             |
     *                |            +-----------+     |1xx          |
     *                |              |      ^        |resp to TU   |
     *                | 200-699      |      +--------+             |
     *                | resp. to TU  |                             |
     *                |              |                             |
     *                |              V                             |
     *                |            +-----------+                   |
     *                |            |           |                   |
     *                |            | Completed |                   |
     *                |            |           |                   |
     *                |            +-----------+                   |
     *                |              ^   |                         |
     *                |              |   | Timer K                 |
     *                +--------------+   | -                       |
     *                                   |                         |
     *                                   V                         |
     *             NOTE:           +-----------+                   |
     *                             |           |                   |
     *         transitions         | Terminated|<------------------+
     *         labeled with        |           |
     *         the event           +-----------+
     *         over the action
     *         to take
     *
     *                 Figure 6: non-INVITE client transaction
     */
    private void nonInviteClientTransaction(
    Response transactionResponse,
    MessageChannel sourceChannel)
    throws IOException, SIPServerException  {
    	int currentState = getState();
    	if (LogWriter.needsLogging)  {
    		LogWriter.logMessage("nonInviteClientTransaction " +
    				transactionResponse.getFirstLine());
    		LogWriter.logMessage("currentState = " + currentState);
    	}
        int statusCode = transactionResponse.getStatusCode();
        if (currentState == TRYING_STATE) {
            if (statusCode / 100 == 1) {
                setState(PROCEEDING_STATE);
                enableRetransmissionTimer
                (MAXIMUM_RETRANSMISSION_TICK_COUNT);
                enableTimeoutTimer(TIMER_F);
            } else if (transactionResponse.isFinalResponse()) {
                // Send the response up to the TU.
                respondTo.processResponse( transactionResponse, this );
                if (! isReliable() ) {
                    setState(COMPLETED_STATE);
                    enableTimeoutTimer(TIMER_K);
                } else {
                    setState(TERMINATED_STATE);
                }
            }
        } else  if (currentState == PROCEEDING_STATE && transactionResponse.isFinalResponse()) {
            
            respondTo.processResponse( transactionResponse, this );
            
            disableRetransmissionTimer( );
            disableTimeoutTimer( );
            if (! isReliable()) {
                setState(COMPLETED_STATE);
                enableTimeoutTimer(TIMER_K);
            } else {
                setState(TERMINATED_STATE);
            }
        }
        
        
    }
    
    
    
    /** Implements the state machine for invite client transactions.
     * @param transactionResponse -- transaction response received.
     * @param sourceChannel - source channel on which the response was received.
     *<pre>
     *
     *                               |INVITE from TU
     *             Timer A fires     |INVITE sent
     *             Reset A,          V                      Timer B fires
     *             INVITE sent +-----------+                or Transport Err.
     *               +---------|           |---------------+inform TU
     *               |         |  Calling  |               |
     *               +-------->|           |-------------->|
     *                         +-----------+ 2xx           |
     *                            |  |       2xx to TU     |
     *                            |  |1xx                  |
     *    300-699 +---------------+  |1xx to TU            |
     *   ACK sent |                  |                     |
     *resp. to TU |  1xx             V                     |
     *            |  1xx to TU  -----------+               |
     *            |  +---------|           |               |
     *            |  |         |Proceeding |-------------->|
     *            |  +-------->|           | 2xx           |
     *            |            +-----------+ 2xx to TU     |
     *            |       300-699    |                     |
     *            |       ACK sent,  |                     |
     *            |       resp. to TU|                     |
     *            |                  |                     |      NOTE:
     *            |  300-699         V                     |
     *            |  ACK sent  +-----------+Transport Err. |  transitions
     *            |  +---------|           |Inform TU      |  labeled with
     *            |  |         | Completed |-------------->|  the event
     *            |  +-------->|           |               |  over the action
     *            |            +-----------+               |  to take
     *            |              ^   |                     |
     *            |              |   | Timer D fires       |
     *            +--------------+   | -                   |
     *                               |                     |
     *                               V                     |
     *                         +-----------+               |
     *                         |           |               |
     *                         | Terminated|<--------------+
     *                         |           |
     *                         +-----------+
     *</pre>
     */
    
    private void inviteClientTransaction(
    Response transactionResponse,
    MessageChannel sourceChannel) throws IOException, SIPServerException {
        int statusCode = transactionResponse.getStatusCode();
        int currentState = getState();
        if (currentState == TERMINATED_STATE) {
        	// From JAIN SIP 1.2
			boolean ackAlreadySent = false;
			if (dialog != null && dialog.isAckSeen()
					&& dialog.getLastAck() != null) {
				if (dialog.getLastAck().getCSeqHeader().getSequenceNumber() == transactionResponse
						.getCSeqHeader().getSequenceNumber()) {
					// the last ack sent corresponded to this response
					ackAlreadySent = true;
				}
			}
			// retransmit the ACK for this response.
			if (ackAlreadySent
					&& transactionResponse.getCSeqHeader().getMethod().equals(
							dialog.getFirstTransaction().getOriginalRequest().getMethod())) {
				// Found the dialog - resend the ACK and
				// dont pass up the null transaction
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "resending ACK");

				dialog.resendAck();
			}
			return;
		} else if (currentState == CALLING_STATE )  {
            if (transactionResponse.isSuccessfulResponse()) {
            	// From JAIN SIP 1.2, disable retransmissions before notifying the app
                disableRetransmissionTimer( );
                disableTimeoutTimer( );
                setState(TERMINATED_STATE);
                // 200 responses are always seen by TU.
                respondTo.processResponse( transactionResponse, this );

				/**
				 * When in either the "Calling" or "Proceeding" states,
				 * reception of a 2xx response MUST cause the client transaction
				 * to enter the "Terminated" state, and the response MUST be
				 * passed up to the TU. The handling of this response depends on
				 * whether the TU is a proxy core or a UAC core. A UAC core will
				 * handle generation of the ACK for this response, while a proxy
				 * core will always forward the 200 (OK) upstream.
				 */
                // so, no ACK here!

            } else if (statusCode/100 == 1) {
                disableRetransmissionTimer( );
                disableTimeoutTimer( );
                setState(PROCEEDING_STATE);
                respondTo.processResponse( transactionResponse, this );

            } else if (300 <= statusCode  &&  statusCode <= 699) {
                // When in either the "Calling" or "Proceeding" states,
                // reception of response with status code from 300-699
                // MUST cause the client transaction to
                // transition to "Completed".
                // The client transaction MUST pass the received response up to
                // the TU, and the client transaction MUST generate an
                // ACK request.
            	
            	// Send back an ACK request
            	try {
            		sendMessage(createAck());
            	} catch (SipException ex) {
            		InternalErrorHandler.handleException(ex);
            	}
                
                respondTo.processResponse( transactionResponse, this);
                if ( ! isReliable())  {
                    setState(COMPLETED_STATE);
                    enableTimeoutTimer(TIMER_D);
                } else {
                    //Proceed immediately to the TERMINATED state.
                    setState(TERMINATED_STATE);
                }
            }
        } else if (currentState == PROCEEDING_STATE) {
            if (statusCode / 100 == 1) {
                respondTo.processResponse( transactionResponse, this);
            } else if (transactionResponse.isSuccessfulResponse()) {
                setState(TERMINATED_STATE);
                respondTo.processResponse( transactionResponse, this);
            } else if (300 <= statusCode  &&  statusCode <= 699) {
                // Send back an ACK request
                try {
                    sendMessage(createAck());
                } catch (SipException ex) {
                    InternalErrorHandler.handleException(ex);
                }
                if ( ! isReliable())  {
                    setState(COMPLETED_STATE);
                    enableTimeoutTimer(TIMER_D);
                } else {
                    setState(TERMINATED_STATE);
                }
                respondTo.processResponse( transactionResponse, this);

            }
        } else if (currentState == COMPLETED_STATE) {
            if (300 <= statusCode  &&  statusCode <= 699) {
                // Send back an ACK request
                try {
                    sendMessage(createAck());
                } catch (SipException ex) {
                    InternalErrorHandler.handleException(ex);
                }
            }
            
        }
        
    }
    
    /** Sends specified {@link javax.sip.message.Request} on a unique
     * client transaction identifier. This method implies that the application
     * is functioning as either a User Agent Client or a Stateful proxy, hence
     * the underlying SipProvider acts statefully.
     * <p>
     * JAIN SIP defines a retransmission utility specific to user agent
     * behaviour and the default retransmission behaviour for each method.
     * <p>
     * When an application wishes to send a message, it creates a Request
     * message passes that Request to this method, this method returns the
     * cleintTransactionId generated by the SipProvider. The Request message
     * gets sent via the ListeningPoint that this SipProvider is attached to.
     * <ul>
     * <li>User Agent Client - must not send a BYE on a confirmed INVITE until
     * it has received an ACK for its 2xx response or until the server
     * transaction times out.
     * </ul>
     *
     * @throws SipException if implementation cannot send request for any reason
     */
    public void sendRequest()
    throws SipException {
        Request sipRequest =  this.getOriginalRequest();
        try {
            this.sendMessage(sipRequest);
        } catch (IOException ex) {
            throw new SipException(ex.getMessage());
        }
        
    }
    
    
    /**
     * Called by the transaction stack when a retransmission timer
     * fires.
     */
    protected void fireRetransmissionTimer(
    ) {
        
        try {
			// Resend the last request sent
			if (this.getState() == -1)
				return;
			if (this.getState() == CALLING_STATE
					|| this.getState() == TRYING_STATE)
				getMessageChannel().sendMessage(lastRequest);
        } catch( IOException e ) {
            raiseErrorEvent(SIPTransactionErrorEvent.TRANSPORT_ERROR );
        }
        
    }
    
    
    /**
     *	Called by the transaction stack when a timeout timer fires.
     */
    protected void fireTimeoutTimer() {
            	
        Dialog dialogImpl =  this.getDialog();
        if( getState()== CALLING_STATE ||
        getState() == TRYING_STATE   ||
        getState() == PROCEEDING_STATE ) {
        	if (LogWriter.needsLogging)
        		LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Transaction timed out! (no response) " + this);
            // Timeout occured. If this is associated with a transaction
            // creation then kill the dialog.
            if (dialogImpl != null) {
                if (((SIPTransactionStack)getSIPStack()).isDialogCreated
                (this.getOriginalRequest().getMethod())) {
                    // terminate the enclosing dialog.
                    dialogImpl.setState(Dialog.TERMINATED_STATE);
                } else if (getOriginalRequest().getMethod().equals
                (Request.BYE)){
                    // Terminate the associated dialog on BYE Timeout.
                    dialogImpl.setState(Dialog.TERMINATED_STATE);
                }
            }
        }
        if ( getState()!= COMPLETED_STATE ) {
            raiseErrorEvent(SIPTransactionErrorEvent.TIMEOUT_ERROR);
        } else {
            setState(TERMINATED_STATE);
        }
        
    }
    
    /**
     * Creates a new Cancel message from the Request associated with this client
     * transaction. The CANCEL request, is used to cancel the previous request
     * sent by this client transaction. Specifically, it asks the UAS to cease
     * processing the request and to generate an error response to that request.
     *
     *@return a cancel request generated from the original request.
     */
    public Request createCancel() throws SipException {
        Request originalRequest = this.getOriginalRequest();
        if (originalRequest.getMethod().equals(Request.ACK))
            throw new SipException("Cannot Cancel ACK!");
        else return originalRequest.createCancelRequest();
    }
    
    
    /**
     * Creates an ACK request for this transaction
     *
     *@return an ack request generated from the original request.
     *
     *@throws SipException if transaction is in the wrong state to be acked.
     */
    public Request createAck() throws SipException {
        Request originalRequest = this.getOriginalRequest();
        if (originalRequest.getMethod().equals(Request.ACK))
            throw new SipException("Cannot ACK an ACK!");
        else if (  lastResponse == null)
            throw new SipException("bad Transaction state");
        else if (  lastResponse.getStatusCode() < 200 )  {
            if (LogWriter.needsLogging ) {
                LogWriter.logMessage("lastResponse = " +
                lastResponse);
            }
            throw new SipException("Cannot ACK a provisional response!");
        }
        Request ackRequest =
        originalRequest.createAckRequest(lastResponse.getTo());
        // Pull the record route headers from the last reesponse.
        RecordRouteList recordRouteList =
        lastResponse.getRecordRouteHeaders();
        if (recordRouteList == null) return ackRequest;
        ackRequest.removeHeader(Header.ROUTE);
        RouteList routeList =  new RouteList();
        // start at the end of the list and walk backwards
        Vector li = recordRouteList.getHeaders();
        for (int i = li.size() -1 ; i >= 0; i--) {
            RecordRouteHeader rr = (RecordRouteHeader) li.elementAt(i);
            RouteHeader route = new RouteHeader();
            route.setAddress
            ((Address)(rr.getAddress()).clone());
            route.setParameters
            ((NameValueList)rr.getParameters().clone());
            routeList.add(route);
        }
        ContactHeader contact = null;
        if (lastResponse.getContactHeaders() != null) 
        	contact = (ContactHeader) lastResponse.getContactHeaders().getFirst();
        if( ! ((SipURI)((RouteHeader)routeList.getFirst()).getAddress().getURI()).
        hasLrParam() ) {
        	RouteHeader route = new RouteHeader();
        	if (contact != null)            
        		route.setAddress((Address)((contact.getAddress())).clone());
            RouteHeader firstRoute = (RouteHeader) routeList.getFirst();
            routeList.removeFirst();
            sip4me.gov.nist.siplite.address.URI uri =
            firstRoute.getAddress().getURI();
            ackRequest.setRequestURI(uri);
            routeList.add(route);
            ackRequest.addHeader(routeList);
         } else	if (contact != null){
        		URI uri = (URI)                
                contact.getAddress().getURI().clone();
                ackRequest.setRequestURI(uri);
                ackRequest.addHeader(routeList);
        } 
        return ackRequest;
        
        
    }
    
    
	/**
	 * Set the next hop ( if it has already been computed).
	 * 
	 * @param hop --
	 *            the hop that has been previously computed.
	 */
	public void setNextHop(Hop hop) {
		this.nextHop = hop;

	}

	/**
	 * Reeturn the previously computed next hop (avoid computing it twice).
	 * 
	 * @return -- next hop previously computed.
	 */
	public Hop getNextHop() {
		return nextHop;
	}
    
    
    
    /** Set the port of the recipient.
     */
    protected void setViaPort(int port) { this.viaPort = port; }
    
    /** Set the port of the recipient.
     */
    protected void setViaHost(String host) { this.viaHost = host; }
    
    /** Get the port of the recipient.
     */
    public int getViaPort() { return this.viaPort; }
    
    /** Get the host of the recipient.
     */
    public String getViaHost() { return this.viaHost; }
    
    /** get the via header for an outgoing request.
     */
    public ViaHeader getOutgoingViaHeader() {
        return this.getMessageProcessor().getViaHeader();
    }
    
    public boolean isSecure() { return encapsulatedChannel.isSecure(); }

    public void clearEventPending() {
		this.eventPending = false;
    }

    public void setEventPending() {
		this.eventPending = true;
    }

    public void setState(int newState) {
		super.setState(newState);
    }
    
}
