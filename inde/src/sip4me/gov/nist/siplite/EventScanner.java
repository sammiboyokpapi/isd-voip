
package sip4me.gov.nist.siplite;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.ClientTransaction;
import sip4me.gov.nist.siplite.stack.Dialog;
import sip4me.gov.nist.siplite.stack.ServerTransaction;
import sip4me.gov.nist.siplite.stack.Transaction;




public class EventScanner implements Runnable {

	private SipStack sipStack;
	private Vector pendingEvents;
	private SipListener sipListener;
	private boolean isStopped;


	public EventScanner( SipStack sipStack ) {
		this.sipStack = sipStack;
		this.pendingEvents = new Vector();
	}

	public void start() {
		Thread myThread = new Thread(this, "EventScannerThread");
		myThread.setPriority(Thread.MAX_PRIORITY);
		myThread.start();
	}

	public void stop() {
		if (LogWriter.needsLogging)
			LogWriter.logMessage("Stopping EventScanner Thread");

		synchronized (this.pendingEvents) {
			this.isStopped = true;
			this.pendingEvents.notifyAll();
		}
	}
	public void addEvent( EventWrapper eventWrapper ) {
		if (LogWriter.needsLogging)
			LogWriter.logMessage("Adding event to EventScanner queue: " + eventWrapper.transaction);
		synchronized (this.pendingEvents) {
			this.pendingEvents.addElement(eventWrapper);
			this.pendingEvents.notify();
		}

	}

	public void run() {
		while(true) {
			SipEvent sipEvent = null;
			EventWrapper eventWrapper = null;
			synchronized (this.pendingEvents) {
				if (pendingEvents.isEmpty() && !isStopped) {
					try {
						if(LogWriter.needsLogging)
							LogWriter.logMessage("EventScanner going to sleep!");
						pendingEvents.wait();
						if(LogWriter.needsLogging)
							LogWriter.logMessage("EventScanner waking up!");

					} catch (InterruptedException ex) {
						LogWriter.logMessage("Interrupted!" );
						continue;
					}
				}
				if (isStopped)
					return;
				SipListener sipListener = sipStack.getSipListener();
				Enumeration iterator = pendingEvents.elements();
				while (iterator.hasMoreElements()) {
					eventWrapper = (EventWrapper) iterator.nextElement();
					sipEvent = eventWrapper.sipEvent;
					if (LogWriter.needsLogging) {
						LogWriter.logMessage("Processing " + sipEvent +
								" nevents "  + pendingEvents.size());
					}
					if (sipEvent instanceof RequestEvent) {
						// Check if this request has already created a transaction
						Request sipRequest =
							(Request) ((RequestEvent) sipEvent).getRequest();
						// Check if this request has already created a 
						// transaction
						// If this is a dialog creating method for which a server
						// transaction already exists or a method which is
						// not dialog creating and not within an existing dialog 
						// (special handling for cancel) then check to see if
						// the listener already created a transaction to handle
						// this request and discard the duplicate request if a
						// transaction already exists. If the listener chose
						// to handle the request statelessly, then the listener
						// will see the retransmission.
						// Note that in both of these two cases, JAIN SIP will allow
						// you to handle the request statefully or statelessly.
						// An example of the latter case is REGISTER and an example
						// of the former case is INVITE.
						if (sipStack.isDialogCreated(sipRequest.getMethod())) {
							SipProvider sipProvider = (SipProvider)sipEvent.getSource();
							sipProvider.currentTransaction =  (ServerTransaction) eventWrapper.transaction;
							ServerTransaction tr =  
								(ServerTransaction) 
								sipStack.findTransaction(sipRequest, true);
							Dialog dialog = sipStack.getDialog
							(sipRequest.getDialogId(true)) ;

							if (tr != null  &&  ! tr.passToListener() ) {
								if (LogWriter.needsLogging) 
									LogWriter.logMessage(
									"transaction already exists for Dialog-creating request!");
								continue;
							}
						} 
						// Fix: this prevented out-of-dialog requests to arrive to the application
						// MESSAGE, UPDATE, OPTIONS...
//						else if  ( (!sipRequest.getMethod().equals(Request.CANCEL) && !sipRequest.getMethod().equals(Request.MESSAGE)) &&
//								sipStack.getDialog(sipRequest.getDialogId(true)) == null)  {
//							// not dialog creating and not a cancel. 
//							// transaction already processed this message.
//							Transaction tr = sipStack.findTransaction(sipRequest, true);
//							// 
//							// Should this be allowed?
//							// SipProvider sipProvider = (SipProvider) sipEvent.getSource();
//							// sipProvider.currentTransaction = (ServerTransaction) eventWrapper.transaction;
//							// If transaction already exists bail.
//							if (tr != null) {
//								if (LogWriter.needsLogging) {
//									LogWriter.logMessage("Transaction already exists for out-of-dialog request! " + tr);
//									LogWriter.logMessage("Discarding Request because it has transaction (avoid duplicated handling)");
//								}
//								continue;
//							}
//						}

						// Processing incoming CANCEL.
						if (sipRequest.getMethod().equals("CANCEL") ) {
							Transaction tr =
								sipStack.findTransaction(sipRequest,true);
							if (tr != null &&
									tr.getState() ==
										Transaction.TERMINATED_STATE ) {
								// If transaction already exists but it is
								// too late to cancel the transaction then
								// just respond OK to the CANCEL and bail.
								if (LogWriter.needsLogging)
									LogWriter.logMessage
									("Too late to cancel Transaction");
								// send OK and just ignore the CANCEL.
								try {
									tr.sendMessage
									(sipRequest.createResponse(Response.OK));
								} catch (IOException ex) {
									// Ignore?
								}
								continue;
							}
						}
						
						if (LogWriter.needsLogging)
							LogWriter.logMessage("EventScanner passes event "+ sipRequest.getMethod()+" to listener");

						sipListener.processRequest((RequestEvent) sipEvent);
					} else if (sipEvent instanceof ResponseEvent) {
						sipListener.processResponse((ResponseEvent) sipEvent);
						ClientTransaction ct = 
							((ResponseEvent) sipEvent).getClientTransaction();
						ct.clearEventPending();
					} else if (sipEvent instanceof TimeoutEvent) {
						sipListener.processTimeout((TimeoutEvent) sipEvent);
						//Mark that Timeout event has been processed
						if (eventWrapper.transaction != null) {
							if(eventWrapper.transaction instanceof 
									ClientTransaction) {
								((ClientTransaction) eventWrapper.
										transaction).clearEventPending();
							}

						}
					} else {
						if (LogWriter.needsLogging)
							LogWriter.logMessage("bad event" + sipEvent);
					}
				} // Bug report by Laurent Schwitzer
				pendingEvents.removeAllElements();
			}// end of Synchronized block
		} // end While
	}

}
