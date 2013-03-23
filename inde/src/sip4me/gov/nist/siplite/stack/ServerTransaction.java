package sip4me.gov.nist.siplite.stack;


import java.io.IOException;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.siplite.RequestEvent;
import sip4me.gov.nist.siplite.SIPConstants;
import sip4me.gov.nist.siplite.SipException;
import sip4me.gov.nist.siplite.SipListener;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.header.ViaList;
import sip4me.gov.nist.siplite.message.Message;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;



/**
 *	Represents a server transaction.
 *
 *@author Jeff Keyser
 *@author M. Ranganathan <mranga@nist.gov>
 *
 *@version  JAIN-SIP-1.1
 *
 *
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class ServerTransaction
extends Transaction
implements SIPServerRequestInterface{
    
    protected int collectionTime;
    
    // Real RequestInterface to pass messages to
    private SIPServerRequestInterface	requestOf;
    
    protected boolean isMapped; // I am known to the stack.

     private void sendSIPResponse ( Response transactionResponse ) 
	throws IOException {	
    	 
    	 if (LogWriter.needsLogging) {
    		 LogWriter.logMessage(LogWriter.TRACE_DEBUG,
					"Sending response on ServerTransaction: "
							+ transactionResponse.getTransactionId()
							+ " with first line: "
							+ transactionResponse.getFirstLine());
    	 }
    	 
    	 // Bug report by Shanti Kadiyala
    	 if (transactionResponse.getTopmostVia().
    			 getParameter(ViaHeader.RECEIVED) == null)  {
        	 if (LogWriter.needsLogging)
        		 LogWriter.logMessage(LogWriter.TRACE_DEBUG,
    					"\"Received\" parameter is null, sending through existing channel");
        	 
    		 // Send the response back on the same peer as received.
    		 getMessageChannel( ).sendMessage( transactionResponse );
    	 } else {
    		 // Respond to the host name in the received parameter.
    		 ViaHeader via = transactionResponse.getTopmostVia();
    		 String host = via.getParameter(ViaHeader.RECEIVED);
    		 int    port = via.getPort();
    		 if (port == -1) port = 5060;
    		 String   transport = via.getTransport();
        	 if (LogWriter.needsLogging)
        		 LogWriter.logMessage(LogWriter.TRACE_DEBUG,
    					"\"Received\" parameter is NOT null; sending through hop " + host + ":" + port + "/" +transport);
    		 Hop hop = new Hop(host+":"+port+"/" +transport);
    		 MessageChannel messageChannel =
    			 ((SIPTransactionStack)getSIPStack()).
    			 createRawMessageChannel(hop);						
    		 messageChannel.sendMessage(transactionResponse);
    	 }
    	 this.lastResponse = transactionResponse;	
     }
	

	

    /** Delays the sending of the Trying state.
    *
    */
    
    class SendTrying extends Thread {
	ServerTransaction myTransaction;
	public SendTrying
	  (ServerTransaction transaction ) {
		myTransaction    = transaction;
		Thread myThread = new Thread(this);
	        myThread.start();
	}

	public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) { }
                        
		if (myTransaction.getState() == TRYING_STATE) {
		  try {
                     myTransaction.sendMessage
                     ( myTransaction.getOriginalRequest().
                     createResponse( 100, "Trying" ) );
		  } catch (IOException ex) {}
		}
		return;
	}
    }
    
    /**
     *	Creates a new server transaction.
     *
     *	@param newSIPMessageStack Transaction stack this transaction
     * 	belongs to.
     *	@param newChannelToHeaderUse Channel to encapsulate.
     */
    protected ServerTransaction(
    SIPTransactionStack	newSIPMessageStack,
    MessageChannel		newChannelToHeaderUse
    ) {
        
        super( newSIPMessageStack, newChannelToHeaderUse );
        if (LogWriter.needsLogging) {
            LogWriter.logMessage("Creating Server Transaction: " + this);
        }
        
    }
    
    
    /**
     *	Sets the real RequestInterface this transaction encapsulates.
     *
     *	@param newRequestOf RequestInterface to send messages to.
     */
    public void setRequestInterface(
    SIPServerRequestInterface	newRequestOf
    ) {
        
        requestOf = newRequestOf;
        
    }
    
    
    public String getProcessingInfo(
    ) {
        
        return requestOf.getProcessingInfo( );
        
    }
    
    
    
    
    /**
     *	Determines if the message is a part of this transaction.
     *
     *	@param messageToTest Message to check if it is part of this
     *		transaction.
     *
     *	@return True if the message is part of this transaction,
     * 		false if not.
     */
    public  boolean isMessagePartOfTransaction(
    Message	messageToTest ) {
        
        // List of Via headers in the message to test
        ViaList	viaHeaders;
        // ToHeaderpmost Via header in the list
        ViaHeader		topViaHeader;
        // Branch code in the topmost Via header
        String	messageBranch;
        // Flags whether the select message is part of this transaction
        boolean transactionMatches;
        
        
        transactionMatches = false;
		String method = messageToTest.getCSeqHeader().getMethod();

	// Compensation for retransmits after OK has been dispatched  
	// as suggested by Antonis Karydas.
		if (method.equals(Request.INVITE) || !isTerminated()) {
            
            // Get the topmost Via header and its branch parameter
            viaHeaders = messageToTest.getViaHeaders( );
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
                if( messageBranch != null  &&
                this.getBranch() != null ) {
                	
					if (method.equals(Request.CANCEL)) {
						// Cancel is handled as a special case because it
						// shares the same same branch id of the invite
						// that it is trying to cancel.
						transactionMatches = this.getOriginalRequest()
								.getCSeqHeader().getMethod().equals(
										Request.CANCEL)
								&& getBranch().equals(messageBranch)
								&& topViaHeader.getSentBy().equals(
										((ViaHeader) getOriginalRequest()
												.getViaHeaders().getFirst())
												.getSentBy());

					} else {
						// Matching server side transaction with only the
						// branch parameter.
						transactionMatches = getBranch().equals(
								messageBranch) && topViaHeader.getSentBy().equals(
										((ViaHeader) getOriginalRequest()
												.getViaHeaders().getFirst())
												.getSentBy());

					}
                    
					// If this is an RFC2543-compliant message,
                } else {
                    
                    // If RequestURI, ToHeader tag, FromHeader tag,
                    // CallIdHeader, CSeqHeader number, and top Via
                    // headers are the same,
                    String originalFromHeaderTag =
                    getOriginalRequest().getFromHeader().
                    getTag();
                    
                    String thisFromHeaderTag =
                    messageToTest.getFromHeader().getTag();
                    
                    boolean skipFromHeader =
                    ( originalFromHeaderTag == null ||
                    thisFromHeaderTag == null);
                    
                    
                    String originalToHeaderTag =
                    getOriginalRequest().getTo().
                    getTag();
                    
                    String thisToHeaderTag =
                    messageToTest.getTo().getTag();
                    
                    boolean skipToHeader =
                    (originalToHeaderTag == null ||
                    thisToHeaderTag == null);
                    
                    
                    
                    if( getOriginalRequest().
                    getRequestURI().
                    equals( ( (Request)messageToTest ).
                    getRequestURI()) &&
                    
                    ( skipFromHeader ||
                    originalFromHeaderTag.equals(thisFromHeaderTag) ) &&
                    
                    ( skipToHeader ||
                    originalToHeaderTag.equals(thisToHeaderTag) ) &&
                    
                    
                    getOriginalRequest( ).
                    getCallId( ).getCallId( ).
                    equals( messageToTest.getCallId( )
                    .getCallId( ) ) &&
                    getOriginalRequest( ).
                    getCSeqHeader( ).getSequenceNumber( ) ==
                    messageToTest.getCSeqHeader( ).
                    getSequenceNumber( ) &&
                    
                    topViaHeader.equals(
                    getOriginalRequest( ).
                    getViaHeaders().getFirst() ) ) {
                        
                        transactionMatches = true;
                    }
                    
                }
                
            }
            
        }
        return transactionMatches;
        
    }
    
    /** Send out a trying response (only happens when the transaction is
     * mapped). Otherwise the transaction is not known to the stack.
     */
    protected void map() throws IOException  {
        if (getState() == -1 || getState() == TRYING_STATE) {
			if (isInviteTransaction() && !this.isMapped) {
				this.isMapped = true;
				// Has side-effect of setting
				// state to "Proceeding"
				new SendTrying(this);
			} else {
				isMapped = true;
			}
		}
    }
    
    /** Return true if the transaction is known to stack.
     */
    public boolean isTransactionMapped() { return this.isMapped; }
    
    
    /**
     *	Process a new request message through this transaction.
     * If necessary, this message will also be passed onto the TU.
     *
     *	@param transactionRequest Request to process.
     *	@param sourceChannel Channel that received this message.
     */
    public void processRequest(Request transactionRequest,
			MessageChannel sourceChannel) throws SIPServerException {
		boolean toTu = false;
        	
	if (LogWriter.needsLogging)
		LogWriter.logMessage("[ServerTransaction] processRequest: " + transactionRequest.getFirstLine());
	
        try {            
            // If this is the first request for this transaction,
            if( getState( )== -1) {
                // Save this request as the one this
                // transaction is handling
            	
            	if (LogWriter.needsLogging)
            		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "[ServerTransaction] Initializing transaction with original request");
            	
                setOriginalRequest( transactionRequest );
                setState( TRYING_STATE );
				this.setPassToListener();
                toTu = true;
                if( isInviteTransaction( )  && this.isMapped ) {
                    
                    // Has side-effect of setting
                    // state to "Proceeding". 
                    sendMessage( transactionRequest.
                    createResponse( 100, "Trying" ) );
                    
                }
                // If an invite transaction is ACK'ed while in
                // the completed state,
            } else if ( isInviteTransaction()
		&& COMPLETED_STATE == getState()
		&& transactionRequest.getMethod().equals(Request.ACK)) {

		setState(CONFIRMED_STATE);
		disableRetransmissionTimer();
		if (!isReliable()) {
			if (this.lastResponse != null
			&& this.lastResponse.getStatusCode()
			== Response.REQUEST_TERMINATED) {
			// Bug report by Antonis Karydas
				setState(TERMINATED_STATE);
			} else {
				enableTimeoutTimer(TIMER_I);
			}

		} else {

			setState(TERMINATED_STATE);

		}
		// Application should not Ack in CONFIRMED state
		// Bug (and fix thereof) reported by Emil Ivov
		return;

            } else if( transactionRequest.getMethod( ).equals
            ( getOriginalRequest( ).getMethod( ) ) ) {
                if( getState( ) == PROCEEDING_STATE ||
                getState( ) == COMPLETED_STATE ) {
                    
                    // Resend the last response to
                    // the client
                    if (lastResponse != null)  {
                        try {
                            // Send the message to the client
                            getMessageChannel( ).sendMessage
                            ( lastResponse );
                        } catch( IOException e ) {
                            setState( TERMINATED_STATE );
                            throw e;
                            
                        }
                    }
                } else if (transactionRequest.getMethod().
                equals(Request.ACK) ) {                    
                    // This is passed up to the TU to suppress
                    // retransmission of OK
                    requestOf.processRequest
                    ( transactionRequest, this );
                }
                return;
                
            }
            
            
	     // Pass message to the TU
	     if (COMPLETED_STATE != getState()
		&& TERMINATED_STATE != getState()
		&& requestOf != null) {
		if (getOriginalRequest().getMethod()
		   .equals(transactionRequest.getMethod())) {
			// Only send original request to TU once!
			if (toTu)
				requestOf.processRequest(transactionRequest, 
					this);
			} else {
				requestOf.processRequest(transactionRequest, 
					this);
			}
		} else {
			// This seems like a common bug so 
			// I am allowing it through!
			if (((SIPTransactionStack) getSIPStack())
				.isDialogCreated
				(getOriginalRequest().getMethod())
				&& getState() == TERMINATED_STATE
				&& transactionRequest.getMethod().equals
					(Request.ACK)
				&& requestOf != null) {
			        if  (! this.getDialog().ackSeen)  {
				    ( this.getDialog()).ackReceived(
						transactionRequest);
				    requestOf.processRequest
					(transactionRequest, this);
				}
			} else if (
				transactionRequest.getMethod().equals
					(Request.CANCEL)) {
				if (LogWriter.needsLogging)
				 LogWriter.logMessage(
				"Too late to cancel Transaction");
				// send OK and just ignore the CANCEL.
				try {
				  this.sendMessage (transactionRequest.
					 createResponse(Response.OK));
				} catch (IOException ex) {
					// Transaction is already terminated
					// just ignore the IOException.
				}
			}
			if (LogWriter.needsLogging)
			    LogWriter.logMessage(
				"Dropping request " + getState());
		}
            
        } catch( IOException e ) {
            raiseErrorEvent
            ( SIPTransactionErrorEvent.TRANSPORT_ERROR );
        }
        
    }
    
    
    /**
     *	Send a response message through this transaction and onto
     *      the client.
     *
     *	@param messageToSend Response to process and send.
     */
    public void sendMessage(Message messageToSend) throws IOException {

		// Message typecast as a response
		Response transactionResponse;
		// Status code of the response being sent to the client
		int statusCode;

		// Get the status code from the response
		transactionResponse = (Response) messageToSend;
		statusCode = transactionResponse.getStatusCode();

		// super.checkCancel(transactionResponse);
		// Provided we have set the branch id for this we set the BID for the
		// outgoing via.
		if (this.getBranch() != null) {
			transactionResponse.getTopmostVia().setBranch(this.getBranch());
		} else {
			transactionResponse.getTopmostVia().removeParameter(
					ViaHeader.BRANCH);
		}

		// Method of the response does not match the request used to
		// create the transaction - transaction state does not change.
		if (!transactionResponse.getCSeqHeader().getMethod().equals(
				getOriginalRequest().getMethod())) {
			sendSIPResponse(transactionResponse);
			return;
		}

		if (this.dialog != null) {
			if (this.dialog.getRemoteTag() == null
					&& transactionResponse.getTo().getTag() != null
					&& ((SIPTransactionStack) this.getSIPStack())
							.isDialogCreated(transactionResponse
									.getCSeqHeader().getMethod())) {
				this.dialog.setRemoteTag(transactionResponse.getTo().getTag());
				((SIPTransactionStack) this.getSIPStack())
						.putDialog(this.dialog);
				if (statusCode / 100 == 1)
					this.dialog.setState(Dialog.EARLY_STATE);

			} else if (((SIPTransactionStack) this.getSIPStack())
					.isDialogCreated(transactionResponse.getCSeqHeader()
							.getMethod())) {
				if (Response.isSuccessfulResponse(statusCode)) {
					if (!this.isInviteTransaction()) {
						this.dialog.setState(Dialog.CONFIRMED_STATE);
					} else {
						if (this.dialog.getState() == -1)
							this.dialog.setState(Dialog.EARLY_STATE);
					}
				} else if (statusCode >= 300
						&& statusCode <= 699
						&& (this.dialog.getState() == -1 || this.dialog
								.getState() == Dialog.EARLY_STATE)) {
					this.dialog.setState(Dialog.TERMINATED_STATE);
				}
			} else if (transactionResponse.getCSeqHeader().getMethod().equals(
					Request.BYE)
					&& Response.isSuccessfulResponse(statusCode)) {
				// Dialog will be terminated when the transaction is terminated.
				if (!isReliable())
					this.dialog.setState(Dialog.COMPLETED_STATE);
				else
					this.dialog.setState(Dialog.TERMINATED_STATE);
			}
		}

		// If the TU sends a provisional response while in the
		// trying state,
		if (getState() == TRYING_STATE) {
			if (statusCode / 100 == 1) {
				setState(PROCEEDING_STATE);
			} else if (Response.isFinalResponse(statusCode)) {
				// bug report from christophe
				if (!isInviteTransaction()) {
					if (!isReliable()) {
						setState(COMPLETED_STATE);
						enableTimeoutTimer(TIMER_J);
					} else {
						setState(TERMINATED_STATE);
					}
				} else {
					if (Response.isSuccessfulResponse(statusCode)) {
						this.disableRetransmissionTimer();
						this.disableTimeoutTimer();
						this.collectionTime = TIMER_J;
						setState(TERMINATED_STATE);
						if (this.dialog != null)
							this.dialog.setRetransmissionTicks();
					} else {
						setState(COMPLETED_STATE);
						if (!isReliable()) {
							enableRetransmissionTimer();
						}
						enableTimeoutTimer(TIMER_H);
					}
				}

			}

			// If the transaction is in the proceeding state,
		} else if (getState() == PROCEEDING_STATE) {
			if (isInviteTransaction()) {

				// If the response is a success message,
				if (Response.isSuccessfulResponse(statusCode)) {

					// Terminate the transaction
					disableRetransmissionTimer();
					disableTimeoutTimer();
					this.collectionTime = TIMER_J;
					setState(TERMINATED_STATE);
					if (this.dialog != null)
						this.dialog.setRetransmissionTicks();


				} else if (300 <= statusCode && statusCode <= 699) {

					// Set up to catch returning ACKs
					setState(COMPLETED_STATE);
					if (!isReliable()) {

						enableRetransmissionTimer();

					}
					// Changed to TIMER_H as suggested by
					// Antonis Karydas
					enableTimeoutTimer(TIMER_H);

					// If it is not an Invite transaction,
					// and the response is a success message,
				}

				// If the transaction is not an invite transaction
				// and this is a final response,
			} else if (200 <= statusCode && statusCode <= 699) {

				// Set up to retransmit this response,
				// or terminate the transaction
				setState(COMPLETED_STATE);
				if (!isReliable()) {

					disableRetransmissionTimer();
					enableTimeoutTimer(TIMER_J);

				} else {

					setState(TERMINATED_STATE);

				}

			}
			// If the transaction has already completed,
		} else if (getState() == COMPLETED_STATE) {
			return;
		}
		try {
			// Send the message to the client
			// Bug report by Shanti Kadiyala
			lastResponse = transactionResponse;
			sendSIPResponse(transactionResponse);
		} catch (IOException e) {

			setState(TERMINATED_STATE);
			this.collectionTime = 0;
			throw e;

		}

	}
    
    public String getViaHost(
    ) {
        
        return encapsulatedChannel.getViaHost( );
        
    }
    
    
    public int getViaPort() {
        
        return encapsulatedChannel.getViaPort( );
        
    }
    
    /**
     *	Called by the transaction stack when a retransmission
     *  timer fires. This retransmits the last response when the
     *  retransmission filter is enabled.
     */
    protected  void fireRetransmissionTimer(
    ) {
        
        try {
            
            // Resend the last response sent by this transaction
            if (isInviteTransaction() &&
            ((SIPTransactionStack)getSIPStack()).retransmissionFilter )
                getMessageChannel( ).sendMessage( lastResponse );
            
        } catch( IOException e ) {
            raiseErrorEvent
            ( SIPTransactionErrorEvent.TRANSPORT_ERROR );
            
        }
        
    }
    
    
    /**
     *	Called by the transaction stack when a timeout timer fires.
     */
    protected void fireTimeoutTimer(
    ) {
        
        if (LogWriter.needsLogging)
            LogWriter.logMessage("ServerTransaction.fireTimeoutTimer "
            + this.getState() + " method = " +
            this.getOriginalRequest().getMethod() );
        
        Dialog dialog = (Dialog) this.getDialog();
        int mystate = this.getState();
        if (((SIPTransactionStack)getSIPStack()).isDialogCreated
        (this.getOriginalRequest().getMethod()) &&
        ( mystate == super.CALLING_STATE ||
        mystate == super.TRYING_STATE )) {
            dialog.setState(Dialog.TERMINATED_STATE);
        } else if (getOriginalRequest().getMethod().equals(Request.BYE)){
            if (dialog != null)
                dialog.setState(Dialog.TERMINATED_STATE);
        }
        
        if( ( getState( ) == CONFIRMED_STATE ||
            // Bug reported by Antonis Karydas
           getState( ) == COMPLETED_STATE) &&
           isInviteTransaction() ) {
           raiseErrorEvent
           ( SIPTransactionErrorEvent.TIMEOUT_ERROR );
           setState( TERMINATED_STATE);
        } else if(  ! isInviteTransaction() && (
           getState( ) == COMPLETED_STATE  ||
           getState() == CONFIRMED_STATE ) ) {
           setState( TERMINATED_STATE );
        } else if (isInviteTransaction() &&
	   getState() == TERMINATED_STATE) {
	   // This state could be reached when retransmitting
	   // Bug report sent in by Christophe
	   raiseErrorEvent(SIPTransactionErrorEvent.TIMEOUT_ERROR);
           if (dialog != null) dialog.setState(Dialog.TERMINATED_STATE);
	}

        
    }
    
    /** Get the last response.
     */
    public Response getLastResponse() {
        return this.lastResponse;
    }
    
    /** Set the original request.
     */
    public void setOriginalRequest(Request originalRequest) {
        super.setOriginalRequest(originalRequest);
        // ACK Server Transaction is just a dummy transaction.
        if (originalRequest.getMethod().equals("ACK"))
            this.setState(TERMINATED_STATE);
        
    }
    
    /** Sends specified Response message to a Request which is identified by the
     * specified server transaction identifier. The semantics for various
     * application behaviour on sending Responses to Requests is outlined at
     * {@link SipListener#processRequest(RequestEvent)}.
     * <p>
     * Note that when a UAS core sends a 2xx response to an INVITE, the server
     * transaction is destroyed, by the underlying JAIN SIP implementation.
     * This means that when the ACK sent by the corresponding UAC arrives
     * at the UAS, there will be no matching server transaction for the ACK,
     * and based on this rule, the ACK is passed to the UAS application core,
     * where it is processed.
     * This ensures that the three way handsake of an INVITE that is managed by
     * the UAS application and not JAIN SIP.
     *
     * @param response - the Response to send to the Request
     * @throws SipException if implementation cannot send response for any
     * other reason
     * @see Response
     */
    public void sendResponse( Response response)
    throws SipException {
                
        try {
			Dialog dialog = (Dialog) this.getDialog();

			// Fix up the response if the dialog has already been established.
			Response responseImpl = (Response) response;

			if (responseImpl.getStatusCode() == Response.OK
					&& parentStack.isDialogCreated(responseImpl.getCSeqHeader()
							.getMethod()) && dialog.getLocalTag() == null
					&& responseImpl.getTo().getTag() == null)
				throw new SipException("ToHeader tag must be set for OK");

			if (responseImpl.getStatusCode() == Response.OK
					&& responseImpl.getCSeqHeader().getMethod().equals(
							Request.INVITE)
					&& responseImpl.getHeader(Header.CONTACT) == null)
				throw new SipException("Contact Header is mandatory for the OK");

			// Create a dialog if needed (not if it was already created by a
			// provisional response)
			if (dialog == null
					&& parentStack.isDialogCreated(responseImpl.getCSeqHeader()
							.getMethod())
					&& responseImpl.getStatusCode() < 299) {
				dialog = new Dialog(this);
				dialog.addRoute(getOriginalRequest());
				this.dialog = dialog;
			}

			// If sending the response within an established dialog, then
			// set up the tags appropriately.

			if (dialog != null && dialog.getLocalTag() != null)
				responseImpl.getTo().setTag(dialog.getLocalTag());

			String fromTag = ((Request) this.getRequest()).getFromHeader()
					.getTag();

			// Backward compatibility slippery slope....
			// Only set the from tag in the response when the
			// incoming request has a from tag.
			if (fromTag != null)
				responseImpl.getFromHeader().setTag(fromTag);
			else {
				if (LogWriter.needsLogging)
					LogWriter
							.logMessage("WARNING -- Null From tag  Dialog layer in jeopardy!!");
			}

			// Keep Via Headers in the response (fix by Arnau Vazquez @ Genaker)
			if (responseImpl.getViaHeaders() == null) {
				responseImpl.setVia(((Request) this.getRequest())
						.getViaHeaders());
			}
			this.sendMessage((Response) response);

			// Transaction successfully cancelled but dialog has not yet
			// been established so delete the dialog.
			// TODO : Does not apply to re-invite (Bug report by Martin LeClerc)
			if (Utils.equalsIgnoreCase(
					responseImpl.getCSeqHeader().getMethod(), Request.CANCEL)
					&& responseImpl.getStatusCode() == 200
					// && (!dialog.isReInvite())
					&& parentStack.isDialogCreated(getOriginalRequest()
							.getMethod())
					&& (dialog.getState() == -1 || dialog.getState() == Dialog.EARLY_STATE)) {
				dialog.setState(Dialog.TERMINATED_STATE);
			}
			// See if the dialog needs to be inserted into the dialog table
			// or if the state of the dialog needs to be changed.
			if (dialog != null) {
				dialog.printTags();
				if (Utils.equalsIgnoreCase(responseImpl.getCSeqHeader()
						.getMethod(), Request.BYE)) {
					dialog.setState(Dialog.TERMINATED_STATE);
				} else if (Utils.equalsIgnoreCase(responseImpl.getCSeqHeader()
						.getMethod(), Request.CANCEL)) {
					if (dialog.getState() == -1
							|| dialog.getState() == Dialog.EARLY_STATE) {
						dialog.setState(Dialog.TERMINATED_STATE);
					}
				} else if (dialog.getLocalTag() == null
						&& responseImpl.getTo().getTag() != null) {

					if (responseImpl.getStatusCode() != 100)
						dialog.setLocalTag(responseImpl.getTo().getTag());
					if (parentStack.isDialogCreated(responseImpl
							.getCSeqHeader().getMethod())) {
						if (response.getStatusCode() / 100 == 1) {
							dialog.setState(Dialog.EARLY_STATE);
						} else if (response.getStatusCode() / 100 == 2) {
							dialog.setState(Dialog.CONFIRMED_STATE);
						}
						
						// Enter into our dialog table provided this is a
						// dialog creating method.
						if (responseImpl.getStatusCode() != 100)
							parentStack.putDialog(dialog);
					}
				} else if (response.getStatusCode() / 100 == 2) {
					if (parentStack.isDialogCreated(responseImpl
							.getCSeqHeader().getMethod())) {
						dialog.setState(Dialog.CONFIRMED_STATE);
						parentStack.putDialog(dialog);
					}
				}
			}
		} catch (IOException ex) {
            throw new SipException(ex.getMessage());
        
        }
        catch(NullPointerException npe){
        	npe.printStackTrace();
        }
    }
    
   
    
    
    /**
     *	Returns this transaction.
     */
    public MessageChannel getResponseChannel(
    ) {
        
        return this;
        
    }
    
    
}
