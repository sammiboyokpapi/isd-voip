/******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).      *
 ******************************************************************************/
package sip4me.gov.nist.siplite;

import java.io.IOException;

import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.ClientTransaction;
import sip4me.gov.nist.siplite.stack.Dialog;
import sip4me.gov.nist.siplite.stack.MessageChannel;
import sip4me.gov.nist.siplite.stack.SIPServerException;
import sip4me.gov.nist.siplite.stack.SIPServerRequestInterface;
import sip4me.gov.nist.siplite.stack.SIPServerResponseInterface;
import sip4me.gov.nist.siplite.stack.ServerTransaction;
import sip4me.gov.nist.siplite.stack.Transaction;

/**
 * An adapter class from the JAIN implementation objects to the NIST-SIP stack.
 * This is the class that is instantiated by the NistSipMessageFactory to
 * create a new SIPServerRequest or SIPServerResponse.
 * Note that this is not part of the JAIN-SIP spec (it does not implement
 * a JAIN-SIP interface). This is part of the glue that ties together the
 * NIST-SIP stack and event model with the JAIN-SIP stack. Implementors
 * of JAIN services need not concern themselves with this class.
 *
 *@version  JAIN-SIP-1.1
 *
 *@author M. Ranganathan <mranga@nist.gov>  <br/>
 * Bug fix Contributions by Lamine Brahimi and  Andreas Bystrom. <br/>
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class NistSipMessageHandlerImpl
implements SIPServerRequestInterface, SIPServerResponseInterface {
    
    protected Transaction transactionChannel;
    protected MessageChannel rawMessageChannel;
    // protected Request sipRequest;
    // protected Response sipResponse;
    protected ListeningPoint listeningPoint;
    /**
     * Process a request.
     *@exception SIPServerException is thrown when there is an error
     * processing the request.
     */
    public void processRequest(Request sipRequest,
			MessageChannel incomingMessageChannel) throws SIPServerException {
		// Generate the wrapper JAIN-SIP object.
		if (LogWriter.needsLogging)
			LogWriter.logMessage("PROCESSING INCOMING REQUEST "
					+ sipRequest.getFirstLine());
		if (listeningPoint == null) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage("Dropping message: No listening point "
						+ "registered!");
			return;
		}
		this.rawMessageChannel = incomingMessageChannel;

		SipStack sipStack = (SipStack) transactionChannel.getSIPStack();
		SipProvider sipProvider = listeningPoint.getProvider();
		if (sipProvider == null) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage("No provider - dropping !!");
			return;
		}
		SipListener sipListener = sipProvider.sipListener;

		Transaction transaction = transactionChannel;
		// Look for the registered SIPListener for the message channel.
		synchronized (sipProvider) {
			String dialogId = sipRequest.getDialogId(true);
			Dialog dialog = sipStack.getDialog(dialogId);
			if (sipRequest.getMethod().equals(Request.ACK)) {
				// Could not find transaction. Generate an event
				// with a null transaction identifier.
				if (LogWriter.needsLogging)
					LogWriter.logMessage("Processing ACK for dialog " + dialog);

				if (dialog == null) {
					if (LogWriter.needsLogging)
						LogWriter.logMessage("Dialog does not exist "
								+ sipRequest.getFirstLine()
								+ " isServerTransaction = " + true);

					// Bug reported by Antonis Karydas
					transaction = sipStack.findTransaction(sipRequest, true);
				} else if (dialog.getLastAck() != null
						&& dialog.getLastAck().equals(sipRequest)) {
					if (sipStack.isRetransmissionFilterActive()) {
						dialog.ackReceived(sipRequest);
						transaction.setDialog(dialog);
						if (LogWriter.needsLogging) {
							LogWriter
									.logMessage("Retransmission Filter enabled - dropping Ack"
											+ " retransmission");
						}
						// filter out retransmitted acks if
						// retransmission filter is enabled.
						return;
					}
					if (LogWriter.needsLogging)
						LogWriter
								.logMessage("ACK retransmission for 2XX response "
										+ "Sending ACK to the TU");
				} else {
					// This could be a re-invite processing.
					// check to see if the ack matches with the last
					// transaction.

					Transaction tr = dialog.getLastTransaction();
					Response sipResponse = tr.getLastResponse();

					if (LogWriter.needsLogging)
						LogWriter.logMessage("TRANSACTION:" + tr);

					if (tr instanceof ServerTransaction
							&& sipResponse != null
							&& sipResponse.isSuccessfulResponse()
							&& sipResponse.getCSeqHeader().getSequenceNumber() == sipRequest
									.getCSeqHeader().getSequenceNumber()) {
						transaction.setDialog(dialog);
						dialog.ackReceived(sipRequest);
						if (sipStack.isRetransmissionFilterActive()
								&& tr.isAckSeen()) {
							if (LogWriter.needsLogging)
								LogWriter
										.logMessage("ACK retransmission for 2XX response --- "
												+ "Dropping ");
							return;
						} else {
							// record that we already saw an ACK for
							// this transaction.
							tr.setAckSeen();
							LogWriter
									.logMessage("ACK retransmission for 2XX response --- "
											+ "sending to TU ");
						}

					} else {
						if (LogWriter.needsLogging)
							LogWriter
									.logMessage("ACK retransmission for non 2XX response "
											+ "Discarding ACK");
						// Could not find a transaction.
						if (tr == null) {
							if (LogWriter.needsLogging)
								LogWriter
										.logMessage("Could not find transaction ACK dropped");
							return;
						}
						transaction = tr;
						if (transaction instanceof ClientTransaction) {
							if (LogWriter.needsLogging)
								LogWriter.logMessage("Dropping late ACK");
							return;
						}
					}
				}
			} else if (sipRequest.getMethod().equals(Request.BYE)) {
				transaction = this.transactionChannel;
				// If the stack has not mapped this transaction because
				// of sequence number problem then just drop the BYE
				if (transaction != null
						&& ((ServerTransaction) transaction)
								.isTransactionMapped()) {
					// Get the dialog identifier for the bye request.
					if (LogWriter.needsLogging)
						LogWriter.logMessage("dialogId = " + dialogId);
					// Find the dialog identifier in the SIP stack and
					// mark it for garbage collection.
					if (dialog != null) {
						// Remove dialog marks all
						dialog.addTransaction(transaction);
					} else {
						dialogId = sipRequest.getDialogId(false);
						if (LogWriter.needsLogging)
							LogWriter.logMessage("dialogId = " + dialogId);
						dialog = sipStack.getDialog(dialogId);
						if (LogWriter.needsLogging)
							LogWriter.logMessage("dialog = " + dialog);
						if (dialog != null) {
							dialog.addTransaction(transaction);
						} else {
							transaction = null;
							// pass up to provider for
							// stateless handling.
						}
					}
				} else if (transaction != null) {
					// This is an out of sequence BYE
					// transaction was allocated but
					// not mapped to the stack so
					// just discard it.
					if (dialog != null) {
						if (LogWriter.needsLogging)
							LogWriter
									.logMessage("Dropping out of sequence BYE");
						return;
					} else
						transaction = null;
				}
				// note that the transaction may be null (which
				// happens when no dialog for the bye was fund.
			} else if (sipRequest.getRequestLine().getMethod().equals(
					Request.CANCEL)) {

				// The ID refers to a previously sent
				// INVITE therefore it refers to the
				// server transaction table.
				// Bug reported by Andreas Byström
				// Find the transaction to cancel.
				// Send a 487 for the INVITE to inform the
				// other side that we've seen it but do not send the
				// request up to the application layer.

				// Get rid of the CANCEL transaction -- we pass the
				// transaction we are trying to cancel up to the TU.

				// Antonis Karydas: Suggestion
				// 'transaction' here refers to the transaction to
				// be cancelled. Do not change
				// its state because of the CANCEL.
				// Wait, instead for the 487 from TU.
				// transaction.setState(Transaction.TERMINATED_STATE);

				ServerTransaction serverTransaction = (ServerTransaction) sipStack
						.findInviteTransactionToCancel(sipRequest, true);

				if (serverTransaction == null) {
					// Could not find the invite transaction. Reply 481
					if (LogWriter.needsLogging) {
						LogWriter.logMessage("INVITE transaction "
								+ " does not exist for CANCEL "
								+ sipRequest.getFirstLine()
								+ "isServerTransaction = " + true);
					}
					
					try {
						Response resp = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
						transaction.sendMessage(resp);
						if (LogWriter.needsLogging)
							LogWriter.logMessage("481 sent for non-matching CANCEL");
					} catch (IOException e) {
						if (LogWriter.needsLogging) {
							LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
											"Could not send 481 response to non-matching CANCEL");
							LogWriter.logException(e);
						}
					}
					return;
				} else {
					
					// From JAIN SIP
					// If the CANCEL comes in too late, there's not
					// much that the Listener can do so just do the
					// default action and avoid bothering the listener.
					if (serverTransaction.getState() == Transaction.TERMINATED_STATE) {

						if (LogWriter.needsLogging)
							LogWriter.logMessage(LogWriter.TRACE_MESSAGES,
									"Too late to cancel INVITE Transaction: "
											+ serverTransaction.getBranchId());
						try {
							// send OK and just ignore the CANCEL.
							transaction.sendMessage(sipRequest.createResponse(Response.OK));
						} catch (Exception ex) {
							if (LogWriter.needsLogging) {
								LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
												"Could not send 200 OK response to late CANCEL");
								LogWriter.logException(ex);
							}
						}
						return;
					}

					if (LogWriter.needsLogging)
						LogWriter.logMessage(LogWriter.TRACE_MESSAGES,
								"Cancelling INVITE Transaction: "
										+ serverTransaction.getBranchId());
					
	                
	                // Don't do this!!! CANCEL MUST be processed in a new transaction!
					// transaction = serverTransaction;
					
	                dialog = serverTransaction.getDialog();
	                transaction.setDialog(dialog);

	                if (transaction != null) {
	                	try {
	                		sipStack.addTransaction((ServerTransaction) transaction);
	                		
	                	} catch (IOException e) {
	                		e.printStackTrace();
	                		InternalErrorHandler.handleException(e);
	                	}
	                }

				}
			} else if (sipRequest.getRequestLine().getMethod().equals(Request.INVITE)) {
				if (dialog == null) {
					try {
						sipStack.addTransaction((ServerTransaction) transaction);
					} catch (IOException ex) {
						ex.printStackTrace();
						InternalErrorHandler.handleException(ex);
						return;
					}
				}

			}

			if (dialog != null && transaction != null
					&& !sipRequest.getMethod().equals(Request.BYE)
					&& !sipRequest.getMethod().equals(Request.CANCEL)
					&& !sipRequest.getMethod().equals(Request.ACK)) {
				// already dealt with bye above.
				// Note that route updates are only effective until
				// Dialog is in the confirmed state.
				if (dialog.getRemoteSequenceNumber() >= sipRequest
						.getCSeqHeader().getSequenceNumber()) {
					if (LogWriter.needsLogging) {
						LogWriter
								.logMessage("Dropping out of sequence message "
										+ dialog.getRemoteSequenceNumber()
										+ " " + sipRequest.getCSeqHeader());
					}

					return;
				}
				
	            try {
	                    sipStack.addTransaction((ServerTransaction) transaction);
	                    // This will set the remote sequence number.
	                    dialog.addTransaction(transaction);
	                    dialog.addRoute(sipRequest);
	                    transaction.setDialog(dialog);
	            } catch (IOException ex) {
	                InternalErrorHandler.handleException(ex);
	                return;
	            }
			}

			RequestEvent sipEvent = null;
			if (dialog == null && sipRequest.getMethod().equals(Request.NOTIFY)) {
				if (LogWriter.needsLogging)
					LogWriter.logMessage("NOTIFY has no associated dialog.");
				ClientTransaction ct = sipStack
						.findSubscribeTransaction(sipRequest);
				if (LogWriter.needsLogging)
					LogWriter
							.logMessage("NOTIFY has client transaction? " + ct);

				// From RFC 3265
				// If the server transaction cannot be found or if it
				// already has a dialog attached to it then just assign the
				// notify to this dialog and pass it up.
				if (ct != null) {
					Dialog subscribeDialog = ct.getDialog();
					transaction.setDialog(subscribeDialog);

					if (subscribeDialog.getState() == Dialog.INITIAL_STATE) {
						// Fix by ArnauVP (Genaker): NOTIFYs can establish
						// a dialog if the SUBSCRIBE OK has not been received
						// yet
						subscribeDialog.setRemoteTag(transaction.getRequest()
								.getFromHeaderTag());
						subscribeDialog.addTransaction(transaction);
						subscribeDialog.setFirstTransaction(ct);
						subscribeDialog.setLastTransaction(ct);
						subscribeDialog.addRoute(sipRequest);
						subscribeDialog.setDialogId(sipRequest
								.getDialogId(true));
						subscribeDialog.setLocalTag(sipRequest.getToTag());
						subscribeDialog.setRemoteTag(sipRequest
								.getFromHeaderTag());
						subscribeDialog.setRemoteSequenceNumber(sipRequest
								.getCSeqHeaderNumber());
						subscribeDialog.setLocalTag(sipRequest.getToTag());
						subscribeDialog.setState(Dialog.CONFIRMED_STATE);
						sipStack.putDialog(subscribeDialog);
					}
					sipEvent = new RequestEvent(sipProvider,
							(ServerTransaction) transaction, sipRequest);

				} else {
					// Fix by ArnauVP (Genaker): Don't process NOTIFYs
					// that don't have associated Subscribe transactions.
					// Just reply 481.

					if (LogWriter.needsLogging)
						LogWriter.logMessage("Trying to reply 481 to out-of-dialog NOTIFY");

					try {
						Response resp = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
						// Send response directly through the message channel
						// ('statelessly');
						// otherwise the Transaction will switch its state to
						// TERMINATED
						// and we will not be able to continue receiving
						// requests.
						transaction.getMessageChannel().sendMessage(resp);
						if (LogWriter.needsLogging)
							LogWriter.logMessage("481 sent for out-of-dialog NOTIFY");
					} catch (IOException e) {
						if (LogWriter.needsLogging) {
							LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
											"Could not send 481 response to out-of-dialog NOTIFY");
							LogWriter.logException(e);
						}
					}
					// don't continue processing the request
					return;
				}

			} else if (transaction != null && sipRequest.getMethod().equals(Request.CANCEL)) { 
				if (LogWriter.needsLogging) {
					LogWriter.logMessage(LogWriter.TRACE_DEBUG,
									"Creating request event for CANCEL from transaction: " + transaction.getBranchId());
				}
				sipEvent = new RequestEvent(sipProvider,
						(ServerTransaction) transaction, sipRequest);
			} else {
				// For a dialog creating event - set the transaction to null.
				// The listener can create the dialog if needed.
				if (transaction != null
						&& ((ServerTransaction) transaction)
								.isTransactionMapped()) {
					if (LogWriter.needsLogging) {
						LogWriter.logMessage(LogWriter.TRACE_DEBUG,
										"Creating request event from transaction: " + transaction.getBranchId());
					}
					sipEvent = new RequestEvent(sipProvider,
							(ServerTransaction) transaction, sipRequest);
				} else {
					if (LogWriter.needsLogging) {
						LogWriter.logMessage(LogWriter.TRACE_DEBUG,
								"Creating request event with no transaction.");
					}
					sipEvent = new RequestEvent(sipProvider, null, sipRequest);
				}
			}
			sipProvider.handleEvent(sipEvent, transaction);
		}

	}
    
    /**
     *Process the response.
     *@exception SIPServerException is thrown when there is an error
     * processing the response
     *@param incomingMessageChannel -- message channel on which the
     * response is received.
     */
    public void processResponse(Response sipResponse,
    MessageChannel incomingMessageChannel )
    throws SIPServerException {
        try {
            if (LogWriter.needsLogging) {
                LogWriter.logMessage("PROCESSING INCOMING RESPONSE" +
                sipResponse.encode());
            }
            if (listeningPoint == null) {
                if (LogWriter.needsLogging)
                    LogWriter.logMessage
                    ("Dropping message: No listening point"
                    + " registered!");
                return;
            }
            
            Transaction transaction = this.transactionChannel;
            SipProvider sipProvider = listeningPoint.getProvider();
            if (sipProvider == null) {
                if (LogWriter.needsLogging)  {
                    LogWriter.logMessage
                    ("Dropping message:  no provider");
                }
                return;
            }
            
            SipStack sipStack = sipProvider.sipStack;
            
            if (LogWriter.needsLogging)
                LogWriter.logMessage("Transaction = " + transaction);
            
            if (this.transactionChannel == null) {
                String dialogId = sipResponse.getDialogId(false);
                Dialog dialog = sipStack.getDialog(dialogId);
                //  Have a dialog but could not find transaction.
                if (sipProvider.sipListener == null)
                    return;
                else if ( dialog != null  ) {
                    // Bug report by Emil Ivov
                    if ( !sipResponse.isSuccessfulResponse() ) {
                        return;
                    } else if (sipStack.isRetransmissionFilterActive()) {
                        // 200  retransmission for the final response.
                        if ( sipResponse.getCSeqHeader().equals(
                        dialog.getFirstTransaction().getRequest().
                        getHeader(Header.CSEQ)) ) {
                            dialog.resendAck();
                            return;
                        }
                    }
                }
                // long receptionTime = System.currentTimeMillis();
                // Pass the response up to the application layer to handle
                // statelessly.
                
                // Dialog is null so this is handled statelessly
                ResponseEvent sipEvent =
                new ResponseEvent(sipProvider,null, sipResponse);
                sipProvider.handleEvent(sipEvent,transaction);
                //transaction.logResponse(sipResponse,
                //       receptionTime,"Retransmission");
                return;
            }
            
            this.rawMessageChannel = incomingMessageChannel;
            
            String method = sipResponse.getCSeqHeader().getMethod();
            // Retrieve the client transaction for which we are getting
            // this response.
            ClientTransaction clientTransaction =
            (ClientTransaction) this.transactionChannel;
            
            Dialog dialog = null;
            if (transaction != null) {
                dialog =  transaction.getDialog();
                if (LogWriter.needsLogging && dialog == null) {
                    LogWriter.logMessage("dialog not found for " +
                    sipResponse.getFirstLine());
                }
            }
            
            
            
            SipListener sipListener = sipProvider.sipListener;
            
			ResponseEvent responseEvent = new ResponseEvent(sipProvider,
					(ClientTransaction) transaction, sipResponse);
			sipProvider.handleEvent(responseEvent, transaction);
            
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        
    }
    /** Get the sender channel.
     */
    public MessageChannel getRequestChannel() {
        return this.transactionChannel;
    }
    
    /** Get the channel if we want to initiate a new transaction to
     * the sender of  a response.
     *@return a message channel that points to the place from where we got
     * the response.
     */
    public MessageChannel getResponseChannel() {
        if (this.transactionChannel != null)
            return this.transactionChannel;
        else return this.rawMessageChannel;
    }
    
    /** Just a placeholder. This is called from the stack
     * for message logging. Auxiliary processing information can
     * be passed back to be  written into the log file.
     *@return auxiliary information that we may have generated during the
     * message processing which is retrieved by the message logger.
     */
    public String getProcessingInfo() {
        return null;
    }
    
    
    
}
