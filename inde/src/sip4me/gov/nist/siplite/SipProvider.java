package sip4me.gov.nist.siplite;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.microedition.sip.SipConnectionNotifierImpl;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.RouteHeader;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.ClientTransaction;
import sip4me.gov.nist.siplite.stack.Dialog;
import sip4me.gov.nist.siplite.stack.MessageChannel;
import sip4me.gov.nist.siplite.stack.MessageProcessor;
import sip4me.gov.nist.siplite.stack.SIPTransactionErrorEvent;
import sip4me.gov.nist.siplite.stack.SIPTransactionEventListener;
import sip4me.gov.nist.siplite.stack.ServerTransaction;
import sip4me.gov.nist.siplite.stack.Transaction;



/** Implementation of the JAIN-SIP provider interface.
 *
 *@version  JAIN-SIP-1.1
 *
 *@author M. Ranganathan <mranga@nist.gov>
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public final class SipProvider implements 
SIPTransactionEventListener {
    
    protected   SipListener sipListener;
    
    protected  SipStack sipStack;
    
    protected  Hashtable listeningPoints;
    
    protected   ServerTransaction currentTransaction;

    private EventScanner eventScanner;
    
    private SipConnectionNotifierImpl connectionNotifier;
    
    
    /** Stop processing messages for this provider. Post an empty message to our
     *message processing queue that signals us to quit.
     * @throws ObjectInUseException 
     */
    protected  void stop() throws ObjectInUseException {
    	// Put an empty event in the queue and post ourselves a message.
    	if(LogWriter.needsLogging)
    		LogWriter.logMessage("Exiting provider " + this);
    	synchronized (this) {
    		for (Enumeration lpEnum = listeningPoints.elements(); lpEnum.hasMoreElements();) {
    			ListeningPoint listeningPoint = (ListeningPoint) lpEnum.nextElement();
    			listeningPoint.removeSipProvider();
    			listeningPoints.remove(listeningPoint);
    			sipStack.deleteListeningPoint(listeningPoint);
    		}
    	}
    	eventScanner = null;
    }

    
    /**
     * Handle the SIP event - because we have only one listener and we are
     * already in the context of a separate thread, we don't need to enqueue
     * the event and signal another thread.
     *
     *@param sipEvent is the event to process.
     *
     */
    public void handleEvent(SipEvent sipEvent, Transaction transaction) {
		if (LogWriter.needsLogging) {
			LogWriter.logMessage("handleEvent " + sipEvent
					+ "\ncurrentTransaction = " + transaction
					+ "\nthis.sipListener = " + this.sipListener);
		}
		if (this.sipListener == null)
			return;
		EventWrapper eventWrapper = new EventWrapper();
		eventWrapper.sipEvent = sipEvent;
		eventWrapper.transaction = transaction;
		if (transaction != null && transaction instanceof ClientTransaction)
			((ClientTransaction) transaction).setEventPending();
		this.eventScanner.addEvent(eventWrapper);
	}
    
    
    
    
    
    
    /** Creates a new instance of SipProvider */
    protected SipProvider(SipStack sipStack) {
		this.sipStack = sipStack;
		this.eventScanner = sipStack.eventScanner;
		listeningPoints = new Hashtable();
    }
    
    
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

	/**
	 * This method registers the SipListener object to this SipProvider, once
	 * registered the SIP Listener can send events on the SipProvider and
	 * recieve events emitted from the SipProvider. As JAIN SIP resticts a
	 * unicast Listener special case, that is, that one and only one Listener
	 * may be registered on the SipProvider concurrently.
	 * <p>
	 * If an attempt is made to re-register the existing SipListener this method
	 * returns silently. A previous SipListener must be removed from the
	 * SipProvider before another SipListener can be registered to the
	 * SipProvider.
	 * 
	 * @param <var>SipListener</var> SipListener to be registered with the
	 *        Provider.
	 * @throws IllegalStateException
	 *             this exception is thrown when a new SipListener attempts to
	 *             register with the SipProvider when another SipListener is
	 *             already registered with this SipProvider.
	 * 
	 */
    public void addSipListener(SipListener sipListener)
    throws IllegalStateException {
        
		synchronized (sipStack) {
			Enumeration it = sipStack.getSipProviders();
			while (it.hasMoreElements()) {
				SipProvider provider = (SipProvider) it.nextElement();
				if (provider.sipListener != null
						&& provider.sipListener != sipListener)
					throw new IllegalStateException("This SipProvider already has a listener associated to it!");
			}
		}
        if (LogWriter.needsLogging)
            LogWriter.logMessage("add SipListener " + sipListener);
        this.sipListener = sipListener;
	sipStack.sipListener = sipListener;
        synchronized(sipStack) {
            Enumeration it = sipStack.getSipProviders();
            while (it.hasMoreElements()) {
                SipProvider provider = (SipProvider) it.nextElement();
                provider.sipListener = sipListener;
            }
        }
    }
    
    
    /**
     * Add a Listening Point to the list of LPs of this SipProvider.
     * Only one LP per transport is allowed.
     * @param lp a listening point
     * @throws ObjectInUseException 
     */
    public void addListeningPoint(ListeningPoint lp) throws ObjectInUseException {
    	
		if (lp.sipProviderImpl != null && lp.sipProviderImpl != this)
			throw new ObjectInUseException(
					"Listening point assigned to another provider");
		String transport = lp.getTransport().toUpperCase();
		
		// Not needed in sip-for-me?
//		if (this.listeningPoints.isEmpty()) {
//			// first one -- record the IP address/port of the LP
//			this.address = lp.getAddress();
//			this.port = lp.getPort();
//		} else {
//			if ((!this.address.equals(lp.getIPAddress()))
//					|| this.port != lp.getPort())
//				throw new ObjectInUseException(
//						"Provider already has different IP Address associated");
//		}
		
		if (this.listeningPoints.containsKey(transport)
				&& this.listeningPoints.get(transport) != lp)
			throw new ObjectInUseException(
					"Listening point already assigned for transport!");

		// This is for backwards compatibility.
		lp.sipProviderImpl = this;

		this.listeningPoints.put(transport, lp);

    }
    
    /**
     * Get the listening point associated to the given transport.
     * @param transport
     * @return
     */
    public ListeningPoint getListeningPoint(String transport) {
		if (transport == null)
			throw new NullPointerException("Null transport param");
		return (ListeningPoint) this.listeningPoints.get(transport
				.toUpperCase());
    }
    
    /**
     * Get the list of ListeningPoints of this SipProvider.
     * @return an array with all the Listening Points of this SipProvider
     */
    public ListeningPoint[] getListeningPoints() {
    	ListeningPoint[] retval = new ListeningPoint[this.listeningPoints.size()];
    	int i = 0;
    	for (Enumeration lpEnum = listeningPoints.elements(); lpEnum.hasMoreElements(); ) {
    		retval[i++] = (ListeningPoint) lpEnum.nextElement();
    	}
    	return retval;

    }
     
    
    /**
     * Removes the specified ListeningPoint from this SipProvider. 
     * @param listeningPoint the LP to remove.
     * @throws ObjectInUseException 
     */
    public void removeListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
    	if (listeningPoint.messageProcessor.inUse())
			throw new ObjectInUseException("Object is in use");
    	listeningPoints.remove(listeningPoint);
    }
    
    
    /** Returns a unique CallIdHeader for identifying dialogues between two
     * SIP applications.
     *
     * @return new CallId unique within the SIP Stack.
     */
    public CallIdHeader getNewCallId() {
        String callId = SIPUtils.generateCallIdentifier
        (this.getSipStack().getIPAddress());
        CallIdHeader callid = new  CallIdHeader();
        callid.setCallId(callId);
        return callid;
        
    }
    
    /** Once an application wants to a send a new request it must first request
     * a new client transaction identifier. This method is called by an
     * application to create the client transaction befores it sends the Request
     * via the SipProvider on that transaction. This methods returns a new
     * unique client transaction identifier that can be passed to the stateful
     * sendRequest method on the SipProvider and the sendAck/sendBye
     * methods on the Dialog in order to send a request.
     *
     * @param request the new Request message that is to be handled 
     * statefully by the Provider.
     * @return a new unique client transaction identifier
     * @see ClientTransaction
     * @since v1.1
     */
    public ClientTransaction getNewClientTransaction(Request request)
    throws TransactionUnavailableException {
        if (request == null)
            throw new NullPointerException("null request");
        
        Request sipRequest = request;
        if (sipRequest.getTransaction() != null)
            throw new TransactionUnavailableException
            ("Transaction already assigned to request");
        if (request.getMethod().equals(Request.CANCEL)) {
            ClientTransaction ct = (ClientTransaction)
            sipStack.findInviteTransactionToCancel(request,false);
            if (ct != null ) {
                ClientTransaction retval =   sipStack.createClientTransaction
                (ct.getMessageChannel());
                ((Transaction)retval).
                setOriginalRequest(request);
                ((Transaction)retval).addEventListener(this);
                sipStack.addTransaction(retval);
                (retval).setDialog(ct.getDialog());
                return  retval;
            }
            
        }
        if (LogWriter.needsLogging)
            LogWriter.logMessage("could not find existing transaction for "
            + (request).getFirstLine());
        
        // Fix by ArnauVP (Genaker):
        // First see if the request belongs to a dialog
        // that can provide the next hop; otherwise turn to the router
        
        String dialogId = sipRequest.getDialogId(false);
        Dialog dialog = sipStack.getDialog(dialogId);
        
        Enumeration it = null;
         
        // Could not find a dialog or the route is not set in dialog.
        if (dialog == null) {
            if (LogWriter.needsLogging)
                LogWriter.logMessage("could not find existing dialog for "
                + (request).getFirstLine() + " and dialogID: " + dialogId);
        	it = sipStack.getRouter().getNextHops(request);
        }

        if (it == null || !it.hasMoreElements() )  {
            // could not route the request as out of dialog.
            // maybe the user has no router or the router cannot resolve
            // the route.
            // If this is part of a dialog then use the route from the dialog
            if (dialog != null)   {
                try {
                    Hop hop = dialog.getNextHop();
                    if (hop != null) {
                        ClientTransaction ct =
                        (ClientTransaction) sipStack.createMessageChannel(hop);
                        String branchId = SIPUtils.generateBranchId();
                        if (sipRequest.getTopmostVia() != null) {
                            sipRequest.getTopmostVia().setBranch(branchId);
                        } else {
                            // Find a message processor to assign this
                            // transaction to.
							MessageProcessor messageProcessor = ((ListeningPoint) this.listeningPoints
									.get(hop.getTransport().toUpperCase())).messageProcessor;
                            ViaHeader via = messageProcessor.getViaHeader();
                            sipRequest.addHeader(via);
                        }
                        ct.setOriginalRequest(sipRequest);
                        ct.setBranch(branchId);
            			ct.setNextHop(hop);
                        ct.setDialog(dialog);
                        ct.addEventListener(this);
                        return ct;
                    }
                } catch (Exception ex) {
                    throw new TransactionUnavailableException(ex.getMessage());
                }
            } else throw new TransactionUnavailableException("no route!");
        } else {
        	// An out of dialog route was found. Assign this to the
        	// client transaction.
        	while (it.hasMoreElements()) {
        		Hop hop = (Hop) it.nextElement();

        		ClientTransaction ct =
        			(ClientTransaction) sipStack.createMessageChannel(hop);
        		if (LogWriter.needsLogging)  {
        			LogWriter.logMessage("hop = " + hop + " ; ct " + ct + 
        					" for topmostVia: " + sipRequest.getTopmostVia());
        		}
				if (ct == null)
					continue;
        		String branchId = SIPUtils.generateBranchId();
        		if (sipRequest.getTopmostVia() != null) {
        			sipRequest.getTopmostVia().setBranch(branchId);
        		} else {
        			// Find a message processor to assign this transaction to.
        			MessageProcessor messageProcessor = ((ListeningPoint) this.listeningPoints
        					.get(hop.getTransport().toUpperCase())).messageProcessor;
        			ViaHeader via = messageProcessor.getViaHeader();
            		if (LogWriter.needsLogging) 
            			LogWriter.logMessage("Creating via header: " + via + " for hop: " + hop); 
            		sipRequest.addHeader(via);
        		}
        		ct.setOriginalRequest(sipRequest);
        		ct.setBranch(branchId);
        		ct.setNextHop(hop); 
        		if (sipStack.isDialogCreated(request.getMethod())) {
        			// create a new dialog to contain this transaction
        			// provided this is necessary.
        			// This could be a re-invite
        			// (noticed by Brad Templeton)
        			if (LogWriter.needsLogging) 
        				LogWriter.logMessage("Creating Client Transaction For Dialog-Initiating request (" + request.getMethod() + ")");

        			if (dialog != null)
        				ct.setDialog(dialog);
        			else 
        				sipStack.createDialog(ct);
        		}  else {
        			ct.setDialog(dialog);
        		}

        		// The provider is the event listener for all transactions.
        		ct.addEventListener(this);
        		return ct;

        	}
        }
        throw new TransactionUnavailableException
        ("Could not create transaction - could not resolve next hop! ");

    }
    
    /** An application has the responsibility of deciding to respond to a
     * Request that does not match an existing server transaction. The method
     * is called by an application that decides to respond to an unmatched
     * Request statefully. This methods return a new unique server transaction
     * identifier that can be passed to the stateful sendResponse methods in
     * order to respond to the request.
     *
     * @param the initial Request message that the doesn't match an existing
     * transaction that the application decides to handle statefully.
     * @return a new unique server transation identifier
     * @throws TransactionAlreadyExistsException if a transaction already exists
     * that is already handling this Request. This may happen if the application
     * gets retransmits of the same request before the initial transaction is
     * allocated.
     * @see ServerTransaction
     * @since v1.1
     */
    public ServerTransaction getNewServerTransaction(Request request)
			throws TransactionAlreadyExistsException,
			TransactionUnavailableException {

	try {		
        ServerTransaction transaction = null;
        Request sipRequest = request;
        if (sipStack.isDialogCreated(sipRequest.getMethod())) {
        	if (sipStack.findTransaction(request, true) != null)
        		throw new TransactionAlreadyExistsException(
        		"server transaction already exists!");
        	transaction = this.currentTransaction;
        	if (transaction == null)
        		throw new TransactionUnavailableException(
        		"Transaction not available");
        	if (!transaction.isMessagePartOfTransaction(request)) {
        		throw new TransactionUnavailableException(
        		"Request Mismatch");
        	}
        	transaction.setOriginalRequest(sipRequest);
        	try {
        		sipStack.addTransaction(transaction);
        	} catch (IOException ex) {
        		throw new TransactionUnavailableException(
        		"Error sending provisional response");
        	}
        	// So I can handle timeouts.
        	transaction.addEventListener(this);
        	String dialogId = sipRequest.getDialogId(true);
        	Dialog dialog = sipStack.getDialog(dialogId);
        	if (dialog == null)
        		dialog = sipStack.createDialog(transaction);
        	else
        		transaction.setDialog(dialog);
        	dialog.setStack(this.sipStack);
        	dialog.addRoute(sipRequest);
        	if (dialog.getRemoteTag() != null
        			&& dialog.getLocalTag() != null) {
        		this.sipStack.putDialog(dialog);
        	}
        } else {
        	transaction = (ServerTransaction) sipStack.findTransaction(
        			request, true);
        	if (transaction != null)
        		throw new TransactionAlreadyExistsException(
        		"Transaction exists! ");
        	transaction = this.currentTransaction;
        	if (transaction == null)
        		throw new TransactionUnavailableException(
        		"Transaction not available!");
        	if (!transaction.isMessagePartOfTransaction(request))
        		throw new TransactionUnavailableException(
        		"Request Mismatch");
        	transaction.setOriginalRequest(sipRequest);
        	// Map the transaction.
        	try {
        		sipStack.addTransaction(transaction);
        	} catch (IOException ex) {
        		throw new TransactionUnavailableException(
        		"Could not send back provisional response!");
        	}
        	String dialogId = sipRequest.getDialogId(true);
        	Dialog dialog = sipStack.getDialog(dialogId);
        	if (dialog != null) {
        		dialog.addTransaction(transaction);
        		dialog.addRoute(sipRequest);
        	}
        }
        return transaction;
	} catch (RuntimeException ex) {		
		ex.printStackTrace();
		throw ex;
	}
        
    }
    
    /** Returns the SipStack that this SipProvider is attached to. A SipProvider
     * can only be attached to a single SipStack object which belongs to
     * the same SIP stack as the SipProvider.
     *
     * @see SipStack
     * @return the attached SipStack.
     */
    public SipStack getSipStack() {
        return  this.sipStack;
    }
    
    /** Removes the SipListener from this SipProvider. This method returns
     * silently if the <var>sipListener</var> argument is not registered
     * with the SipProvider.
     *
     * @param SipListener - the SipListener to be removed from this
     * SipProvider
     */
    public void removeSipListener(SipListener sipListener) {
        if (sipListener == this.sipListener) {
            this.sipListener = null;
        }
    }
    
    
    
    
    /** Sends specified Request and returns void i.e.
     * no transaction record is associated with this action. This method
     * implies that the application is functioning statelessly specific to this
     * Request, hence the underlying SipProvider acts statelessly.
     * <p>
     * Once the Request message has been passed to this method, the SipProvider
     * will forget about this Request. No transaction semantics will be
     * associated with the Request and no retranmissions will occur on the
     * Request by the SipProvider, if these semantics are required it is the
     * responsibility of the application not the JAIN SIP Stack.
     * <ul>
     * <li>Stateless Proxy - A stateless proxy simply forwards every request
     *  it receives downstream and discards information about the request
     *  message once the message has been forwarded. A stateless proxy does not
     *  have any notion of a transaction.
     * </ul>
     *
     * @since v1.1
     * @see Request
     * @param request - the Request message to send statelessly
     * @throws SipException if implementation cannot send request for any reason
     */
    public void sendRequest(Request request) throws SipException {
        Enumeration it = sipStack.getRouter().getNextHops(request);
        if (it == null || !it.hasMoreElements())
            throw new SipException("could not determine next hop!");
        // Will slow down the implementation because it involves
        // a search to see if a transaction exists.
        // This is a common bug so adding some assertion
        // checking under debug.
        Transaction tr = sipStack.findTransaction
        (request,false);
        if (tr != null )
            throw new SipException
            ("Cannot send statelessly Transaction found!");
        
        while (it.hasMoreElements()) {
            Hop nextHop = (Hop) it.nextElement();
            
            Request sipRequest = request;
            String bid = sipRequest.getTransactionId();
            ViaHeader via = sipRequest.getTopmostVia();
            via.setBranch(bid);
            Request newRequest;
            // Do not create a transaction for this request. If it has
            // Multiple route headers then take the first one off the
            // list and copy into the request URI.
            if (sipRequest.getHeader(Header.ROUTE) != null) {
                newRequest = (Request) sipRequest.clone();
                Enumeration rl =
                newRequest.getHeaders(Header.ROUTE);
                RouteHeader route = (RouteHeader) rl.nextElement();
                newRequest.setRequestURI(route.getAddress().getURI());
                sipRequest.removeHeader(Header.ROUTE,true);
            } else newRequest = sipRequest;
            MessageChannel messageChannel =
            sipStack.createRawMessageChannel(nextHop);
            try {
                if (messageChannel != null){
                    messageChannel.sendMessage(newRequest);
                    return;
                }  else throw new SipException("could not forward request");
                
            } catch (IOException ex) {
                continue;
            }
            
        }
        
    }
    
    /** Sends specified {@link javax.sip.message.Response} and returns void i.e.
     * no transaction record is associated with this action. This method implies
     * that the application is functioning as either a stateless proxy or a
     * stateless User Agent Server.
     * <ul>
     *  <li> Stateless proxy - A stateless proxy simply forwards every response
     *  it receives upstream and discards information about the response message
     *  once the message has been forwarded. A stateless proxy does not
     *  have any notion of a transaction.
     *  <li>Stateless User Agent Server - A stateless UAS does not maintain
     *  transaction state. It replies to requests normally, but discards
     *  any state that would ordinarily be retained by a UAS after a response
     *  has been sent.  If a stateless UAS receives a retransmission of a
     *  request, it regenerates the response and resends it, just as if it
     *  were replying to the first instance of the request. A UAS cannot be
     *  stateless unless the request processing for that method would always
     *  result in the same response if the requests are identical. Stateless
     *  UASs do not use a transaction layer; they receive requests directly
     *  from the transport layer and send responses directly to the transport
     *  layer.
     * </ul>
     *
     * @see Response
     * @param sipResponse - the Response to send statelessly.
     * @throws SipException if implementation cannot send
     *   response for any reason
     * @see Response
     * @since v1.1
     */
    public void sendResponse(Response sipResponse)
    throws SipException {
        if (LogWriter.needsLogging) {
            LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Sending response " + sipResponse.getFirstLine());
        }
        ViaHeader via = sipResponse.getTopmostVia();
        if (via == null)
            throw new SipException("No via header in response!");
        int      port = via.getPort();
        String   transport = via.getTransport();
        //Bug report by Shanti Kadiyala
        //check to see if Via has "received" parameter. If so
        //set the host to the via parameter. Else set it to the
        //Via host.
        String host = via.getReceived();
        
        if (LogWriter.needsLogging) {
            LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Host from received: " + host + " is null? " + (host == null));
        }
        if( host == null) {
        	System.out.println("Getting Host from Via, as received is null");
            host = via.getHost() ;
        }
                

        if (port == -1)  port = 5060;
        if (LogWriter.needsLogging) {
            LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Response will be sent to Hop: " + host + ":" + port + "/" + transport);
        }
        Hop hop = new Hop(host+":"+port+"/" +transport);
        try {
            MessageChannel messageChannel =
            sipStack.createRawMessageChannel(hop);
            messageChannel.sendMessage(sipResponse);
        } catch (IOException ex) {
            throw new SipException(ex.getMessage());
        }
    }
    
    /** This method sets the listening point of the SipProvider.
     * A SipProvider can only have a single listening point at any
     * specific time. This method returns
     * silently if the same <var>listeningPoint</var> argument is re-set
     * on the SipProvider.
     * <p>
     * JAIN SIP supports recieving messages from
     * any port and interface that a server listens on for UDP, on that same
     * port and interface for TCP in case a message may need to be sent
     * using TCP, rather than UDP, if it is too large. In order to satisfy this
     * functionality an application must create two SipProviders and set
     * identical listeningPoints except for transport on each SipProvder.
     * <p>
     * Multiple SipProviders are prohibited to listen on the same
     * listening point.
     *
     * @param the <var>listeningPoint</var> of this SipProvider
     * @see ListeningPoint
     * @deprecated since v.1.2
     * @since v1.1
     */
    public void setListeningPoint(ListeningPoint listeningPoint)  {
        if (listeningPoint == null)
            throw new NullPointerException("Null listening point");
        ListeningPoint lp = listeningPoint;
        lp.sipProviderImpl = this;
        String transport = lp.getTransport().toUpperCase();
		
        try {
        	if (this.listeningPoints.containsKey(transport)
        			&& this.listeningPoints.get(transport) != lp) {
        		throw new ObjectInUseException(
        		"Listening point already assigned for transport!"); 
        	}
        	listeningPoints.put(lp.getTransport().toUpperCase(), lp);
        } catch (ObjectInUseException e) {
        	e.printStackTrace();
        }
        
    }
    
    
    /** 	Invoked when an error has ocurred with a transaction.
     *Propagate up to the listeners.
     *
     * 	@param transactionErrorEvent Error event.
     */
    public void transactionErrorEvent
    (SIPTransactionErrorEvent transactionErrorEvent) {
        Transaction transaction =
        transactionErrorEvent.getSource();
        
        if (transactionErrorEvent.getErrorID() ==
        SIPTransactionErrorEvent.TRANSPORT_ERROR) {
            if (LogWriter.needsLogging) {
                LogWriter.logMessage
                ("TransportError occured on " + transaction);
            }
	    // handle Transport error as timeout.
            Object errorObject = transactionErrorEvent.getSource();
            Timeout timeout = Timeout.TRANSACTION;
            TimeoutEvent ev = null;
            
            if (errorObject instanceof ServerTransaction) {
                ev = new TimeoutEvent(this,(ServerTransaction)
                errorObject);
            } else {
                ev = new TimeoutEvent(this,(ClientTransaction) errorObject,
                timeout);
            }
            this.handleEvent(ev,(Transaction) errorObject);
            
        } else {
            // This is a timeout event.
            Object errorObject = transactionErrorEvent.getSource();
            Timeout timeout = Timeout.TRANSACTION;
            TimeoutEvent ev = null;
            
            if (errorObject instanceof ServerTransaction) {
                ev = new TimeoutEvent(this,(ServerTransaction)
                errorObject);
            } else {
                ev = new TimeoutEvent(this,(ClientTransaction) errorObject,
                timeout);
            }
            this.handleEvent(ev,(Transaction) errorObject);
            
        }
    }


	public SipConnectionNotifierImpl getConnectionNotifier() {
		return connectionNotifier;
	}


	public void setConnectionNotifier(SipConnectionNotifierImpl connectionNotifier) {
		this.connectionNotifier = connectionNotifier;
	}
    
}
