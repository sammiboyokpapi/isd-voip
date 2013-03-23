/**************************************************************************/
/* Product of NIST Advanced Networking Technologies Division		  */
/**************************************************************************/
package sip4me.gov.nist.siplite.stack;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.microedition.sip.StackConnector;
import sip4me.gov.nist.siplite.SipException;
import sip4me.gov.nist.siplite.TimeoutEvent;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.address.SipURI;
import sip4me.gov.nist.siplite.address.URI;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.ContactHeader;
import sip4me.gov.nist.siplite.header.ContactList;
import sip4me.gov.nist.siplite.header.EventHeader;
import sip4me.gov.nist.siplite.header.ExpiresHeader;
import sip4me.gov.nist.siplite.header.FromHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.RecordRouteHeader;
import sip4me.gov.nist.siplite.header.RecordRouteList;
import sip4me.gov.nist.siplite.header.RequestLine;
import sip4me.gov.nist.siplite.header.RouteHeader;
import sip4me.gov.nist.siplite.header.RouteList;
import sip4me.gov.nist.siplite.header.SupportedHeader;
import sip4me.gov.nist.siplite.header.ToHeader;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.message.Message;
import sip4me.gov.nist.siplite.message.Request;


/** Tracks dialogs. A dialog is a peer to peer association of communicating
 * SIP entities. For INVITE transactions, a Dialog is created when a success
 * message is received (i.e. a response that has a ToHeader tag).
 * The SIP Protocol stores enough state in the
 * message structure to extract a dialog identifier that can be used to
 * retrieve this structure from the SipStack. Bugs against route set
 * management were reported by Antonis Karydas and Brad Templeton.
 *
 *@version  JAIN-SIP-1.1
 *
 *@author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 *
 */

public class Dialog {
    private Object applicationData; // Opaque pointer to application data.
    private Transaction firstTransaction;
    private Transaction lastTransaction;
    private String dialogId;
    private int localSequenceNumber;
    private int remoteSequenceNumber;
    private String myTag;
    private String hisTag;
    private RouteList  routeList;
    private RouteHeader     contactRoute;
    private String user;
    private RouteHeader defaultRoute;
    private SIPTransactionStack sipStack;
    private int dialogState;
    protected boolean ackSeen;
    protected Request lastAck;
    private URI requestURI;
    private int retransmissionTicksLeft;
    private int prevRetransmissionTicks;
	private Address remoteTarget;
    
    public final static int INITIAL_STATE = -1;
    public final static int EARLY_STATE     = 1;
    public final static int CONFIRMED_STATE = 2;
    public final static int COMPLETED_STATE = 3;
    public final static int TERMINATED_STATE = 4;
    
    
    
    /** Set ptr to app data.
     */
    public void setApplicationData(Object applicationData) {
        this.applicationData = applicationData;
    }
    
    /** Get ptr to opaque application data.
     */
    public Object getApplicationData() {
        return this.applicationData;
    }
    
    /**
     *A debugging print routine.
     */
    private void printRouteList() {
        if (LogWriter.needsLogging) {
            LogWriter.logMessage("this : " + this);
            LogWriter.logMessage("printRouteList : " +
            this.routeList.encode());
            if (this.contactRoute != null) {
                LogWriter.logMessage("contactRoute : " +
                this.contactRoute.encode());
            } else {
                LogWriter.logMessage("contactRoute : null");
            }
        }
    }
    
    public Hop getNextHop()  throws SipException {
        // This is already an established dialog so don't consult the router.
        // Route the request based on the request URI.
        
        RouteList rl = this.getRouteList();
        SipURI sipUri = null;
        if (rl != null && !rl.isEmpty()) {
            RouteHeader route = (RouteHeader) this.getRouteList().getFirst();
            sipUri = (SipURI) (route.getAddress().getURI());
        } else if (contactRoute != null) {
            sipUri = (SipURI) (contactRoute.getAddress().getURI());
        } else throw new SipException("No route found!");
        
        String host = sipUri.getHost();
        int port = sipUri.getPort();
        if (port == -1)  port = 5060;
        String transport = sipUri.getTransportParam();
        // Fix by ArnauVP (Genaker): reSUBSCRIBEs were being
        // sent with the wrong transport (UDP by default)
        // if transport is not specified, use the original one
        if (transport == null) 
        	transport = this.firstTransaction.getViaHeader().getTransport();
        
        return new Hop( host,port,transport);
    }
    
    
    
    /** Return true if this is a client dialog.
     *
     *@return true if the transaction that created this dialog is a
     *client transaction and false otherwise.
     */
    public boolean isClientDialog() {
        Transaction transaction =
        this.getFirstTransaction();
        return transaction instanceof ClientTransaction;
    }
    
    /** Set the state for this dialog.
     *
     *@param state is the state to set for the dialog.
     */
    
    public void setState(int state) {
    	if (LogWriter.needsLogging) {
			LogWriter.logMessage("Setting dialog state for " + this + " to " + state);
			if (state != INITIAL_STATE && state != this.dialogState) {
				if (LogWriter.needsLogging) {
					LogWriter.logMessage("New dialog state is " + state
							+ "dialogId = " + this.getDialogId());
				}
			}

		}
        this.dialogState = state;
    }
    
    /** Debugging print for the dialog.
     */
    public void printTags() {
        if (LogWriter.needsLogging) {
            LogWriter.logMessage( "isServer = " + isServer());
            LogWriter.logMessage("localTag = " + getLocalTag());
            LogWriter.logMessage( "remoteTag = " + getRemoteTag());
            LogWriter.logMessage("firstTransaction = " +
            (firstTransaction).getOriginalRequest());
            
        }
    }
    
    
    /** Mark that the dialog has seen an ACK.
     */
    public void ackReceived(Request sipRequest) {
        if (isServer()) {
            ServerTransaction st =
            (ServerTransaction)this.getFirstTransaction();
            if (st == null) return;
            // Suppress retransmission of the final response (in case
            // retransmission filter is being used).
            if (st.getOriginalRequest().getCSeqHeader().getSequenceNumber() ==
            sipRequest.getCSeqHeader().getSequenceNumber()) {
                st.setState(Transaction.TERMINATED_STATE);
                this.ackSeen = true;
		this.lastAck = sipRequest;
            }
        }
    }

    /** Return true if the dialog has been acked. The ack is sent up to the 
    * TU exactly once when retransmission filter is enabled.
    */
    public boolean isAckSeen() {
		return this.ackSeen;
    }

    public Request getLastAck() {
		return this.lastAck;
    }
    
    /**
     * Modify the transaction that created this dialog.
     * Only intended for Dialogs created by NOTIFY,
     * to set the original SUBSCRIBE instead.
     * 
     * @param firstTransaction
     */
	public void setFirstTransaction(Transaction firstTransaction) {
		this.firstTransaction = firstTransaction;
		this.localSequenceNumber = firstTransaction.getOriginalRequest().getCSeqHeaderNumber();
	}
    
    /** Get the transaction that created this dialog.
     */
    public Transaction getFirstTransaction() {
        return this.firstTransaction;
    }
    
    /** Gets the route set for the dialog.
     * When acting as an User Agent Server
     * the route set MUST be set to the list of URIs in the
     * Record-Route header field from the request, taken in order and
     * preserving all URI parameters. When acting as an User Agent
     * Client the route set MUST be set to the list of URIs in the
     * Record-Route header field from the response, taken in
     * reverse order and preserving all URI parameters. If no Record-Route
     * header field is present in the request or response,
     * the route set MUST be set to the empty set. This route set,
     * even if empty, overrides any
     * pre-existing route set for future requests in this dialog.
     * <p>
     * Requests within a dialog MAY contain Record-Route
     * and Contact header fields.
     * However, these requests do not cause the dialog's route set to be
     * modified.
     * <p>
     * The User Agent Client uses the remote target
     * and route set to build the
     * Request-URI and Route header field of the request.
     *
     * @return an Iterator containing a list of route headers to be used for
     *  forwarding. Empty iterator is returned if route has not
     * 	been established.
     */
    public Enumeration getRouteSet() {
        if (this.routeList == null) return null;
        else return this.getRouteList().getElements();
    }
    
    
    
    private RouteList getRouteList() {
        if (LogWriter.needsLogging)
            LogWriter.logMessage("getRouteList " + this.getDialogId());
        // Find the top via in the route list.
        Vector li = routeList.getHeaders();
        RouteList retval = new RouteList();
        
        // If I am a UA then I am not record routing the request.
                
        for (int i = 0; i < li.size(); i++ ) {
            RouteHeader route = (RouteHeader) li.elementAt(i);
            retval.add(route.clone());
        }
        
        
        if (LogWriter.needsLogging) {
            LogWriter.logMessage("----->>> " );
            LogWriter.logMessage("getRouteList for " + this );
            if (retval != null)
                LogWriter.logMessage("RouteList = " +
                retval.encode());
            LogWriter.logMessage("myRouteList = " +
            routeList.encode());
            LogWriter.logMessage("----->>> " );
        }
        return retval;
    }
    
    
    /** Set the stack address.
     * Prevent us from routing messages to ourselves.
     *
     *@param stackAddress the address of the SIP stack.
     *
     */
    public void setStack(SIPTransactionStack sipStack) {
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Setting stack of dialog " + this + " with ID " + dialogId + " to " + sipStack);
        this.sipStack = sipStack;
    }
    
    
    
    
    /**
     * Set the default route (the default next hop for the proxy or
     * the proxy address for the user agent).
     *
     *@param defaultRoute is the default route to set.
     *
     */
    
    public void setDefaultRoute(RouteHeader defaultRoute) {
        this.defaultRoute = (RouteHeader) defaultRoute.clone();
        // addRoute(defaultRoute,false);
    }
    
    /**
     * Set the user name for the default route.
     *
     *@param user is the user name to set for the default route.
     *
     */
    public void setUser(String user) {
        this.user = user;
    }
    
    
    /** Add a route list extracted from a record route list.
     * If this is a server dialog then we assume that the record
     * are added to the route list IN order. If this is a client
     * dialog then we assume that the record route headers give us
     * the route list to add in reverse order.
     *
     *@param recordRouteList -- the record route list from the incoming
     *      message.
     */
    
    private void addRoute(RecordRouteList recordRouteList) {
        if (this.isClientDialog()) {
            // This is a client dialog so we extract the record
            // route from the response and reverse its order to
            // careate a route list.
            this.routeList =  new RouteList();
            // start at the end of the list and walk backwards
            Vector li = recordRouteList.getHeaders();
            for(int i =li.size() -1 ; i >= 0; i-- ) {
                RecordRouteHeader rr = (RecordRouteHeader) li.elementAt(i);
                Address addr = rr.getAddress();
                RouteHeader route = new RouteHeader();
                route.setAddress
                ((Address)(rr.getAddress()).clone());
                route.setParameters
                ((NameValueList)rr.getParameters().clone());
                this.routeList.add(route);
            }
        } else {
            // This is a server dialog. The top most record route
            // header is the one that is closest to us. We extract the
            // route list in the same order as the addresses in the
            // incoming request.
            this.routeList = new RouteList();
            Vector li = recordRouteList.getHeaders();
            for (int i = 0; i < li.size(); i++) {
                RecordRouteHeader rr = (RecordRouteHeader) li.elementAt(i);
                RouteHeader route = new RouteHeader();
                route.setAddress
                ((Address)( rr.getAddress()).clone());
                route.setParameters((NameValueList)rr.getParameters().
                clone());
                routeList.add(route);
            }
        }
    }
    
    
    /** Add a route list extracted from the contact list of the incoming
     *message.
     *
     *@param contactList -- contact list extracted from the incoming
     *  message.
     *
     */
    
    private void addRoute(ContactList contactList) {
        if (contactList == null || contactList.size() == 0)
			return;
        ContactHeader contact = (ContactHeader) contactList.getFirst();
        RouteHeader route = new RouteHeader();
        route.setAddress
        ((Address)((contact.getAddress())).clone());
        this.contactRoute = route;
    }
    
    
    /**
     * Extract the route information from this SIP Message and
     * add the relevant information to the route set.
     *
     *@param sipMessage is the SIP message for which we want
     *  to add the route.
     *
     *@param Message is the incoming SIP message from which we
     * want to extract out the route.
     */
    public synchronized void addRoute(Message sipMessage) {
        // cannot add route list after the dialog is initialized.

    	if (LogWriter.needsLogging) {
                LogWriter.logMessage
                ("addRoute: dialogState: " + this + "state = " +
                this.getState() );
            }
            
    		if ( this.dialogState == CONFIRMED_STATE && 
    				sipMessage instanceof Request &&
    		        Request.isTargetRefresh(((Request)sipMessage).getMethod())) {
    		    this.doTargetRefresh((sipMessage));
    		}
            
            if ( this.dialogState == CONFIRMED_STATE ||
            this.dialogState == COMPLETED_STATE ||
            this.dialogState == TERMINATED_STATE) return;
            
    		// Incoming Request has the route list
            RecordRouteList rrlist = sipMessage.getRecordRouteHeaders();
    		// Add the route set from the incoming response in reverse
    		// order
    		if (rrlist != null) {
    			this.addRoute(rrlist);
    		} else {
    			// Set the route list to the last seen route list.
    			this.routeList = new RouteList();
    		}
    		// put the contact header from the incoming request into
    		// the route set.
    		this.addRoute(sipMessage.getContactHeaders());
    		if (sipMessage.getContactHeaders() != null) {
    			this.setRemoteTarget((ContactHeader) sipMessage.getContactHeaders().getFirst());
    		}
            
//            if (!isServer()) {
//                // I am CLIENT dialog.
//                if (sipMessage instanceof Response) {
//                    Response sipResponse = (Response) sipMessage;
//                    if (sipResponse.getStatusCode() == 100)  {
//                        // Do nothing for trying messages.
//                        return;
//                    }
//                    RecordRouteList rrlist = sipMessage.getRecordRouteHeaders();
//                    // Add the route set from the incoming response in reverse
//                    // order
//                    if (rrlist != null ) {
//                        this.addRoute(rrlist);
//                    } else {
//                        // Set the route list to the last seen route list.
//                        this.routeList = new RouteList();
//                    }
//                    ContactList contactList = sipMessage.getContactHeaders();
//                    if (contactList != null) {
//                        this.addRoute(contactList);
//                    }
//                }
//            }  else  {
//                if (sipMessage instanceof Request) {
//                    // Incoming Request has the route list
//                    RecordRouteList rrlist = sipMessage.getRecordRouteHeaders();
//                    // Add the route set from the incoming response in reverse
//                    // order
//                    if (rrlist != null ) {
//                        
//                        this.addRoute(rrlist);
//                    } else {
//                        // Set the route list to the last seen route list.
//                        this.routeList = new RouteList();
//                    }
//                    // put the contact header from the incoming request into
//                    // the route set.
//                    ContactList contactList = sipMessage.getContactHeaders();
//                    if (contactList != null) {
//                        this.addRoute(contactList);
//                    }
//                }
//            }
//        } finally {
//            if (LogWriter.needsLogging)  {
//                LogWriter.logStackTrace();
//                LogWriter.logMessage
//                ("added a route = " + routeList.encode() +
//                "contactRoute = " + contactRoute);
//                
//            }
//        }
    }
    
    
	/**
	 * Do taget refresh dialog state updates.
	 * 
	 * RFC 3261: Requests within a dialog MAY contain Record-Route and Contact
	 * header fields. However, these requests do not cause the dialog's route
	 * set to be modified, although they may modify the remote target URI.
	 * Specifically, requests that are not target refresh requests do not modify
	 * the dialog's remote target URI, and requests that are target refresh
	 * requests do. For dialogs that have been established with an
	 * 
	 * INVITE, the only target refresh request defined is re-INVITE (see Section
	 * 14). Other extensions may define different target refresh requests for
	 * dialogs established in other ways.
	 */
	private void doTargetRefresh(Message sipMessage) {

		
		ContactList contactList = sipMessage.getContactHeaders();
		/*
		 * INVITE is the target refresh for INVITE dialogs. SUBSCRIBE is the
		 * target refresh for subscribe dialogs from the client side. This
		 * modifies the remote target URI potentially
		 */
		if (contactList != null) {
		   
			ContactHeader contact = (ContactHeader) contactList.getFirst();
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Doing target refresh for " + sipMessage.getFirstLine() + " with contact " + contact.encode());
			this.setRemoteTarget(contact);
			
		}

	}
	
	/**
	 * Add a route list extracted from the contact list of the incoming message.
	 * 
	 * @param contactList --
	 *            contact list extracted from the incoming message.
	 * 
	 */
	private void setRemoteTarget(ContactHeader contact) {
	    this.remoteTarget = contact.getAddress();
	    if (LogWriter.needsLogging) {
	    	LogWriter.logMessage("Dialog.setRemoteTarget: " + this.remoteTarget.encode());
	    }	
	}
	
    
    /** Protected Dialog constructor.
     */
    private Dialog() {
        this.routeList = new RouteList();
        this.dialogState =  -1; // not yet initialized.
        localSequenceNumber = 0;
        remoteSequenceNumber = -1;
    }
    
    
    /** Set the dialog identifier.
     */
    public void setDialogId(String dialogId) {
        this.dialogId = dialogId;
    }
    
    /** Constructor given the first transaction.
     *
     *@param transaction is the first transaction.
     */
    protected Dialog(Transaction transaction) {
        this();
        if (transaction == null)
			throw new NullPointerException("Null tx");
        this.addTransaction(transaction);
    }
    
    
    
    /** Return true if is server.
     *
     *@return true if is server transaction created this dialog.
     */
    public boolean isServer() {
        return this.getFirstTransaction() instanceof ServerTransaction;
        
    }
 
    /** Get the id for this dialog.
     *
     *@return the string identifier for this dialog.
     *
     */
    public String getDialogId() {
        
        if (firstTransaction instanceof ServerTransaction ) {
                Request sipRequest = ((ServerTransaction) firstTransaction).getOriginalRequest();
                this.dialogId = sipRequest.getDialogId(true,this.myTag);
        } else {
            // This is a client transaction. Compute the dialog id
            // from the tag we have assigned to the outgoing
            // response of the dialog creating transaction.
            if ( this.firstTransaction != null &&
            ((ClientTransaction)this.firstTransaction).
            getLastResponse() != null) {
                this.dialogId =
                ((ClientTransaction)getFirstTransaction()).getLastResponse().
                getDialogId(false,this.hisTag);
            }
            
        }
        
        return this.dialogId;
    }
    
    /**
     * Add a transaction record to the dialog.
     *
     *@param transaction is the transaction to add to the dialog.
     */
    public  void addTransaction(Transaction transaction) {
        Request sipRequest =
            transaction.getOriginalRequest();
		//Processing a re-invite.
		//TODO : handle the re-invite
		/*if (firstTransaction != null
			&& firstTransaction != transaction
			&& transaction.getMethod().equals
			(firstTransaction.getMethod())) {
			 this.reInviteFlag = true;
		}*/
		//Set state to Completed if we are processing a 
	 	// BYE transaction for the dialog.
 		// Will be set to TERMINATED after the BYE 
 		// transaction completes.
 
 		if (sipRequest.getMethod().equals(Request.BYE)) {
 				this.setState(COMPLETED_STATE);
 		}
 
        if (firstTransaction == null) {
            // Record the local and remote sequence
            // numbers and the from and to tags for future
            // use on this dialog.
            firstTransaction = transaction;
            //TODO : set the local and remote party
            if (transaction instanceof ServerTransaction ) {
                hisTag = sipRequest.getFromHeader().getTag();
                // My tag is assigned when sending response
            } else {
                setLocalSequenceNumber
                (sipRequest.getCSeqHeader().getSequenceNumber());
                // his tag is known when receiving response
                myTag = sipRequest.getFromHeader().getTag();
                if (myTag == null)
                    throw new RuntimeException("bad message tag missing!");
            }
        } else if (  transaction.getOriginalRequest().getMethod().equals
	           ( firstTransaction.getOriginalRequest().getMethod())  &&
	      (((firstTransaction instanceof ServerTransaction)   &&
	       (transaction instanceof ClientTransaction))  || 
	        ((firstTransaction instanceof ClientTransaction) &&
	        (transaction instanceof ServerTransaction)))) {
	    // Switch from client side to server side for re-invite
	    //  (put the other side on hold).

	     firstTransaction = transaction;

	}

	if ( transaction instanceof ServerTransaction ) 
             setRemoteSequenceNumber
                (sipRequest.getCSeqHeader().getSequenceNumber());

        this.lastTransaction = transaction;
        // set a back ptr in the incoming dialog.
        transaction.setDialog(this);
        if (LogWriter.needsLogging) {
            LogWriter.logMessage("Transaction Added " +
            transaction + " to dialog " + this + " tags: " + myTag + " / " + hisTag);
            LogWriter.logMessage("TID = "
            + transaction.getTransactionId() +
            "/" + transaction.IsServerTransaction() );
        }
    }
    
    /**
     * Set the remote tag.
     *
     *@param hisTag is the remote tag to set.
     */
    public void setRemoteTag(String hisTag) {
        this.hisTag = hisTag;
    }
    
    
    
    
    /**
     * Get the last transaction from the dialog.
     */
    public Transaction getLastTransaction() {
        return  this.lastTransaction;
        
    }
    
    /**
     * Set the last transaction for the dialog.
     * Useful only for Dialogs created by NOTIFY.
     * @param lastTransaction
     */
	public void setLastTransaction(Transaction lastTransaction) {
		this.lastTransaction = lastTransaction;
	}
    
    
    /**
     * Set the local sequence number for the dialog (defaults to 1 when
     * the dialog is created).
     *
     *@param lCseq is the local cseq number.
     *
     */
    protected void setLocalSequenceNumber(int lCseq) {
        this.localSequenceNumber = lCseq;
    }
    
    /**
     * Set the remote sequence number for the dialog.
     *
     * @param rCseq is the remote cseq number.
     *
     */
    public void setRemoteSequenceNumber(int  rCseq) {
        this.remoteSequenceNumber = rCseq;
    }
    
    
    /**
     * Increment the local CSeqHeader # for the dialog.
     *
     *@return the incremented local sequence number.
     *
     */
    public int incrementLocalSequenceNumber() {
        return ++this.localSequenceNumber;
    }
    
    /**
     * Get the remote sequence number (for cseq assignment of outgoing
     * requests within this dialog).
     *
     *@return local sequence number.
     */
    public int getRemoteSequenceNumber() {
        return this.remoteSequenceNumber;
    }
    
    /**
     * Get the local sequence number (for cseq assignment of outgoing
     * requests within this dialog).
     *
     *@return local sequence number.
     */
    
    public int getLocalSequenceNumber() {
        return this.localSequenceNumber;
    }
    
    
    /**
     * Get local identifier for the dialog.
     *  This is used in FromHeader header tag construction
     *  for all outgoing client transaction requests for
     *  this dialog and for all outgoing responses for this dialog.
     *  This is used in ToHeader tag constuction for all outgoing
     *  transactions when we are the server of the dialog.
     *  Use this when constucting ToHeader header tags for BYE requests
     *  when we are the server of the dialog.
     *
     *@return the local tag.
     */
    public String getLocalTag() {
        return this.myTag;
    }
    
    /** Get peer identifier identifier for the dialog.
     * This is used in ToHeader header tag construction for all outgoing
     * requests when we are the client of the dialog.
     * This is used in FromHeader tag construction for all outgoing
     * requests when we are the Server of the dialog. Use
     * this when costructing FromHeader header Tags for BYE requests
     * when we are the server of the dialog.
     *
     *@return the remote tag
     *  (note this is read from a response to an INVITE).
     *
     */
    public String getRemoteTag() {
        return hisTag;
    }
    
    /** Set local tag for the transaction.
     *
     *@param mytag is the tag to use in FromHeader headers client
     * 	transactions that belong to this dialog and for
     *	generating ToHeader tags for Server transaction requests that belong
     * 	to this dialog.
     */
    public void setLocalTag(String mytag) {
        if (LogWriter.needsLogging)  {
            LogWriter.logMessage("set Local tag " + mytag + " "
            + this.dialogId);
        }
        
        this.myTag = mytag;
        
    }
    
    
    
    /** Mark all the transactions in the dialog inactive and ready
     * for garbage collection.
     */
    protected  void deleteTransactions() {
        this.firstTransaction = null;
        this.lastTransaction = null;
    }
    
    
    
    /** This method will release all resources associated with this dialog
     * that are tracked by the Provider. Further references to the dialog by
     * incoming messages will result in a mismatch.
     * Since dialog destruction is left reasonably open ended in RFC3261,
     * this delete method is provided
     * for future use and extension methods that do not require a BYE to
     * terminate a dialogue. The basic case of the INVITE and all dialogues
     * that we are aware of today it is expected that BYE requests will
     * end the dialogue.
     */
    
    public void delete() {
        // the reaper will get him later.
        this.setState(TERMINATED_STATE);
    }
    
    /** Returns the Call-ID for this SipSession. This is the value of the
     * Call-ID header for all messages belonging to this session.
     *
     * @return the Call-ID for this Dialogue
     */
    public CallIdHeader getCallId() {
        Request sipRequest =
        (this.getFirstTransaction()).getOriginalRequest();
        return sipRequest.getCallId();
    }
    
    
    /** Get the local Address for this dialog.
     *
     *@return the address object of the local party.
     */
    
    public Address getLocalParty()  {
        Request sipRequest =
        (this.getFirstTransaction()).getOriginalRequest();
        if (!isServer()) {
            return sipRequest.getFromHeader().getAddress();
        } else  {
            return sipRequest.getTo().getAddress();
        }
    }
    
    /**
     * Returns the Address identifying the remote party.
     * This is the value of the ToHeader header of locally initiated
     * requests in this dialogue when acting as an User Agent Client.
     * <p>
     * This is the value of the FromHeader header of recieved responses in this
     * dialogue when acting as an User Agent Server.
     *
     * @return the address object of the remote party.
     */
    public Address getRemoteParty() {
        Request sipRequest =
        (this.getFirstTransaction()).getOriginalRequest();
        if (!isServer()) {
            return sipRequest.getTo().getAddress();
        } else  {
            return sipRequest.getFromHeader().getAddress();
        }
        
    }
    
    /**
     * Returns the Address identifying the remote target.
     * This is the value of the Contact header of received Responses
     * for Requests or refresh Requests
     * in this dialogue when acting as an User Agent Client <p>
     * This is the value of the Contact header of received Requests
     * or refresh Requests in this dialogue when acting as an User
     * Agent Server.
     * 
     * Modified by ArnauVP (Genaker) according to JAIN-SIP 1.2
     *
     * @return the address object of the remote target.
     */
    public Address getRemoteTarget() {
		return this.remoteTarget;
    }
    
    
    
    /** Returns the current state of the dialogue. The states are as follows:
     * <ul>
     * <li> Early - A dialog is in the "early" state, which occurs when it is
     * created when a provisional response is recieved to the INVITE Request.
     * <li> Confirmed - A dialog transitions to the "confirmed" state when a 2xx
     * final response is received to the INVITE Request.
     * <li> Completed - A dialog transitions to the "completed" state when a BYE
     * request is sent or received by the User Agent Client.
     * <li> Terminated - A dialog transitions to the "terminated" state when it
     * can be garbage collection.
     * </ul>
     * Independent of the method, if a request outside of a dialog generates a
     * non-2xx final response, any early dialogs created through provisional
     * responses to that request are terminated. If no response arrives at all
     * on the early dialog, it also terminates.
     *
     * @return a DialogState determining the current state of the dialog.
     * @see DialogState
     */
    public int getState() {
        return this.dialogState;
    }
    
    /** Returns true if this Dialog is secure i.e. if the request arrived over
     * TLS, and the Request-URI contained a SIPS URI, the "secure" flag is set
     * to TRUE.
     *
     * @return  <code>true</code> if this dialogue was established using a sips
     * URI over TLS, and <code>false</code> otherwise.
     */
    public boolean isSecure() {
        return Utils.equalsIgnoreCase(this.getFirstTransaction().getRequest().
        getRequestURI().getScheme(),"sips");
    }
    
    /** Sends ACK Request to the remote party of this Dialogue.
     *
     * @param ackRequest - the new ACK Request message to send.
     * @throws SipException if implementation cannot send the ACK Request for
     * any other reason
     */
    public void sendAck(Request request) throws SipException {
        Request ackRequest = request;
        if (LogWriter.needsLogging)
            LogWriter.logMessage("sendAck" + this);
        if (this.isServer())
            throw new SipException("Cannot sendAck from Server side of Dialog");
        if (!ackRequest.getMethod().equals(Request.ACK))
            throw new SipException("Bad request method -- should be ACK");
        if (this.getState() == -1 ||
        this.getState()== EARLY_STATE)  {
            throw new SipException
            ("Bad dialog state " + this.getState());
        }
        if (! (this.getFirstTransaction()).
        getOriginalRequest().getCallId().
        getCallId().equals((request).
        getCallId().getCallId())) {
            throw new SipException("Bad call ID in request");
        }
        
        if (LogWriter.needsLogging) {
            LogWriter.logMessage("setting from tag For outgoing ACK= "
            + this.getLocalTag());
            LogWriter.logMessage("setting ToHeader tag for outgoing ACK = "
            + this.getRemoteTag());
        }
        if (this.getLocalTag() != null)
            ackRequest.getFromHeader().setTag(this.getLocalTag());
        if (this.getRemoteTag() != null)
            ackRequest.getTo().setTag(this.getRemoteTag());
        
        // Create the route request and set it appropriately.
        // Note that we only need to worry about being on the client
        // side of the request.
        if (ackRequest.getHeader(Header.ROUTE) == null) {
            RouteList rl = this.getRouteList();
            if (rl.size() > 0 )  {
                RouteHeader route = (RouteHeader) rl.getFirst();
                SipURI sipUri = (SipURI) route.getAddress().getURI();
                if (sipUri.hasLrParam()) {
                    ackRequest.setRequestURI(this.getRemoteTarget().getURI());
                    ackRequest.addHeader(rl);
                } else {
                    // First route is not a lr
                    // Add the contact route to the end.
                    rl.removeFirst();
                    ackRequest.setRequestURI(sipUri);
                    
                    // Bug report from Brad Templeton
                    
                    if (rl.size() > 0) ackRequest.addHeader(rl);
                    if (contactRoute != null)
                        ackRequest.addHeader(contactRoute);
                }
            }  else  {
                if (this.getRemoteTarget() != null)
                    ackRequest.setRequestURI(this.getRemoteTarget().getURI());
            }
        }
        Hop hop = this.getNextHop();
        try {
            MessageChannel messageChannel =
            sipStack.createRawMessageChannel(hop);
            if ( messageChannel == null ) {
            	  if (LogWriter.needsLogging)
            		  LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Still no message channel for ACK. Trying Outbound Proxy");
                // Bug fix from Antonis Karydas
                // At this point the procedures of 8.1.2
                // and 12.2.1.1 of RFC3261 have been tried
                // but the resulting next hop cannot be resolved
                // (recall that the exception thrown
                // is caught and ignored in SIPMessageStack.createMessageChannel()
                // so we end up here  with a null messageChannel
                // instead of the exception handler below).
                // All else failing, try the outbound proxy  in accordance
                // with 8.1.2, in particular:
                // This ensures that outbound proxies that do not add
                // Record-Route header field values will drop out of
                // the path of subsequent requests.  It allows endpoints
                // that cannot resolve the first Route
                // URI to delegate that task to an outbound proxy.
                //
                // if one considers the 'first Route URI' of a
                // request constructed according to 12.2.1.1
                // to be the request URI when the route set is empty.
                Hop outboundProxy = sipStack.getRouter().getOutboundProxy();
                if ( outboundProxy == null )
                    throw new SipException("No route found!");
                messageChannel = sipStack.createRawMessageChannel
                (outboundProxy);
                
            } 
            if (LogWriter.needsLogging)
      		  LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Message channel for ACK: " + messageChannel.getPeerHostPort().encode());
            // Wrap a client transaction around the raw message channel.
            ClientTransaction clientTransaction =
            (ClientTransaction)
            sipStack.createMessageChannel(messageChannel);
            clientTransaction.setOriginalRequest(ackRequest);
            clientTransaction.sendMessage(ackRequest);
            // Do not retransmit the ACK so terminate the transaction
            // immediately.
	    this.lastAck = ackRequest;
            clientTransaction.setState(Transaction.TERMINATED_STATE);
        } catch (Exception ex) {
            if (LogWriter.needsLogging)
                LogWriter.logException(ex);
            throw new SipException("Could not create message channel for ACK (" + ex.getClass() + " : " + ex.getMessage() + ")");
        }
        
        
    }
    /**
     * Creates a new Request message based on the dialog creating request.
     * This method should be used for but not limited to creating Bye's,
     * Refer's and re-Invite's on the Dialog. The returned Request will be
     * correctly formatted that is it will contain the correct CSeqHeader header,
     * Route headers and requestURI (derived from the remote target). This
     * method should not be used for Ack, that is the application should
     * create the Ack from the MessageFactory.
     *
     * If the route set is not empty, and the first URI in the route set
     * contains the lr parameter (see Section 19.1.1), the UAC MUST place
     * the remote target URI into the Request-URI and MUST include a Route
     * header field containing the route set values in order, including all
     * parameters.
     * If the route set is not empty, and its first URI does not contain the
     * lr parameter, the UAC MUST place the first URI from the route set
     * into the Request-URI, stripping any parameters that are not allowed
     * in a Request-URI.  The UAC MUST add a Route header field containing
     * the remainder of the route set values in order, including all
     * parameters.  The UAC MUST then place the remote target URI into the
     * Route header field as the last value.
     *
     * @param method the string value that determines if the request to be
     * created.
     * @return the newly created Request message on this Dialog.
     * @throws SipException if the Dialog is not yet established.
     */
    public Request createRequest(String method)  throws SipException {
    	
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Creating " + method + " inside dialog");
    	
        // Set the dialog back pointer.
		if (method == null)
			throw new NullPointerException("null method");
		
		else if (this.getState() == -1
				|| ((!method.equals(Request.BYE)) && this.getState() == TERMINATED_STATE)
				|| (method.equals(Request.BYE) && this.getState() == EARLY_STATE))
			throw new SipException("Dialog not yet established or terminated " + this.getState());
        
        Request originalRequest =
        this.getFirstTransaction().getRequest();
                
        SipURI sipUri = null;
		if (this.getRemoteTarget() != null) {
			sipUri = (SipURI) this.getRemoteTarget().getURI().clone();
		}
		else {
			sipUri = (SipURI) this.getRemoteParty().getURI().clone();
			sipUri.clearUriParms();
		}
		
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Cloned request with R-URI: " + sipUri);
        
        RequestLine requestLine = new RequestLine();
        requestLine.setUri(sipUri);
        requestLine.setMethod(method);
        sipUri = null;
        
        Request sipRequest =
        originalRequest.createRequest(requestLine,this.isServer());
        
		CSeqHeader cseq;
		try {
			cseq = StackConnector.headerFactory.createCSeqHeader(incrementLocalSequenceNumber(), method);
			sipRequest.setCSeqHeader(cseq);
		} catch (IllegalArgumentException e) {
			InternalErrorHandler.handleException(e);
			e.printStackTrace();
		} catch (ParseException e) {
			InternalErrorHandler.handleException(e);
			e.printStackTrace();
		}
		
		// Contact Header
		sipRequest.addHeader(originalRequest.getContactHeaders());
        
        if (isServer()) {
            // Remove the old via headers.
            sipRequest.removeHeader(Header.VIA);
            // Add a via header for the outbound request based on the
            // transport of the message processor.
            MessageProcessor messageProcessor =
            sipStack.getMessageProcessor
            (firstTransaction.encapsulatedChannel.getTransport());
            ViaHeader via = messageProcessor.getViaHeader();
            sipRequest.addHeader(via);
        }
        

        
		/*
		 * RFC3261, section 12.2.1.1:
		 * 
		 * The URI in the To field of the request MUST be set to the remote URI
		 * from the dialog state. The tag in the To header field of the request
		 * MUST be set to the remote tag of the dialog ID. The From URI of the
		 * request MUST be set to the local URI from the dialog state. The tag
		 * in the From header field of the request MUST be set to the local tag
		 * of the dialog ID. If the value of the remote or local tags is null,
		 * the tag parameter MUST be omitted from the To or From header fields,
		 * respectively.
		 */
        FromHeader from = sipRequest.getFromHeader();
        ToHeader to = sipRequest.getTo();
        
        if (this.getLocalTag() != null) {
        	from.setTag(this.getLocalTag());
        } else {
        	from.removeTag();
        }
        if (this.getRemoteTag() != null) {
        	to.setTag(this.getRemoteTag());
        } else {
        	to.removeTag();
        }
        
        
        // get the route list from the dialog.
        RouteList rl = this.getRouteList();
        
        // Add it to the header.
		if (rl.size() > 0) {
			RouteHeader route = (RouteHeader) rl.getFirst();
			SipURI sipUri2 = (SipURI) route.getAddress().getURI();
			if (sipUri2.hasLrParam()) {
				sipRequest.setRequestURI(this.getRemoteTarget().getURI());
				sipRequest.addHeader(rl);
			} else {
				// First route is not a lr
				// Add the contact route to the end.
				rl.removeFirst();
				sipRequest.setRequestURI(sipUri2);

				// Bug report from Brad Templeton
				if (rl.size() > 0)
					sipRequest.addHeader(rl);
				if (this.contactRoute != null)
					sipRequest.addHeader(contactRoute);
			}
		}  else  {
            // Bug report from Antonis Karydas
            if (this.getRemoteTarget() != null)
                sipRequest.setRequestURI(this.getRemoteTarget().getURI());
        }
        
        // FIXME: These headers shouldn't be checked one by one, but maybe
        // added at Request.createRequest() method.
		if (method.equals(Request.SUBSCRIBE)) {
			if (originalRequest.getHeader(EventHeader.NAME) != null)
				sipRequest.addHeader(originalRequest.getHeader(EventHeader.NAME));
			
			if (originalRequest.getHeader(ExpiresHeader.NAME) != null)
				sipRequest.addHeader(originalRequest.getHeader(ExpiresHeader.NAME));
			
			if (originalRequest.getHeader(SupportedHeader.NAME) != null)
				sipRequest.addHeader(originalRequest.getHeader(SupportedHeader.NAME));
		}
		
		if (originalRequest.getHeader(Header.P_PREFERRED_IDENTITY) != null)
			sipRequest.addHeader(originalRequest.getHeader(Header.P_PREFERRED_IDENTITY));

        
        return sipRequest;
        
    }
   
    
    
    /**
     * Sends a Request to the remote party of this dialog. This method
     * implies that the application is functioning as UAC hence the
     * underlying SipProvider acts statefully. This method is useful for
     * sending Bye's for terminating a dialog or Re-Invites on the Dialog
     * for third party call control.
     * <p>
     * This methods will set the FromHeader and the ToHeader tags for the outgoing
     * request and also set the correct sequence number to the outgoing
     * Request and associate the client transaction with this dialog.
     * Note that any tags assigned by the user will be over-written by this
     * method.
     * <p>
     * The User Agent must not send a BYE on a confirmed INVITE until it has
     * received an ACK for its 2xx response or until the server transaction
     * timeout is received.
     * <p>
     * When the retransmissionFilter is <code>true</code>,
     * that is the SipProvider takes care of all retransmissions for the
     * application, and the SipProvider can not deliver the Request after
     * multiple retransmits the SipListener will be notified with a
     * {@link TimeoutEvent} when the transaction expires.
     *
     * @param request - the new Request message to send.
     * @param clientTransaction - the new ClientTransaction object identifying
     * this transaction, this clientTransaction should be requested from
     * SipProvider.getNewClientTransaction
     * @throws TransactionDoesNotExistException if the serverTransaction does
     * not correspond to any existing server transaction.
     * @throws SipException if implementation cannot send the Request for
     * any reason.
     */
    public void sendRequest(ClientTransaction clientTransactionId) throws
    SipException {
        
        Request dialogRequest =
        (clientTransactionId)
        .getOriginalRequest();
        if (clientTransactionId == null )
            throw new NullPointerException("Original Request for Client Transaction is null");

        if (dialogRequest.getMethod().equals(Request.ACK) ||
        dialogRequest.getMethod().equals(Request.CANCEL))
            throw new SipException("Bad Request Method. "
            + dialogRequest.getMethod());
        
        // Cannot send bye until the dialog has been established.
        if ( this.getState() == -1 ) {
            throw new SipException
            ("Bad dialog state " + this.getState());
            
        }
        
        if (Utils.equalsIgnoreCase(dialogRequest.getMethod(),Request.BYE) &&
        this.getState() == EARLY_STATE)  {
            throw new SipException
            ("Bad dialog state " + this.getState());
        }
        
        if (LogWriter.needsLogging)
            LogWriter.logMessage("dialog.sendRequest " +
            " dialog = " + this + "\ndialogRequest = \n" +
            dialogRequest);
        
        if (dialogRequest.getTopmostVia() == null) {
            ViaHeader via =
            (clientTransactionId).
            getOutgoingViaHeader();
            dialogRequest.addHeader(via);
        }
        
        
        if (! (this.getFirstTransaction()).
        getOriginalRequest().getCallId().
        getCallId().equals(dialogRequest.
        getCallId().getCallId())) {
            throw new SipException("Bad call ID in request");
        }
        
        // Set the dialog back pointer.
        (clientTransactionId).dialog = this;
        
        
        FromHeader from = dialogRequest.getFromHeader();
        ToHeader to = dialogRequest.getTo();
        
        
		// Caller already did the tag assignment -- check to see if the
		// tag assignment is OK.
		if (this.getLocalTag() != null && from.getTag() != null
				&& !from.getTag().equals(this.getLocalTag()))
			throw new SipException("From tag mismatch expecting	 "
					+ this.getLocalTag());

		if (this.getRemoteTag() != null && to.getTag() != null
				&& !to.getTag().equals(this.getRemoteTag()))
			throw new SipException("To header tag mismatch expecting "
					+ this.getRemoteTag());
        
		/*
		 * The application is sending a NOTIFY before sending the response of
		 * the dialog.
		 */
		if (this.getLocalTag() == null
				&& dialogRequest.getMethod().equals(Request.NOTIFY)) {
			if (!(clientTransactionId).getOriginalRequest().getMethod().equals(Request.SUBSCRIBE))
				throw new SipException(
						"Trying to send NOTIFY without SUBSCRIBE Dialog!");
			this.setLocalTag(from.getTag());
			
		}

		if (this.getLocalTag() != null)
			from.setTag(this.getLocalTag());
		if (this.getRemoteTag() != null)
			to.setTag(this.getRemoteTag());
        
		
        // Caller has not assigned the route header - set the route header
        // and the request URI for the outgoing request.
        // Bugs reported by Brad Templeton.
		if (dialogRequest.getHeader(Header.ROUTE) == null) {
			// get the route list from the dialog.
			RouteList rl = this.getRouteList();
			// Add it to the header.
			if (rl.size() > 0 )  {
				RouteHeader route = (RouteHeader) rl.getFirst();
				SipURI sipUri = (SipURI) route.getAddress().getURI();
				if (sipUri.hasLrParam()) {
					dialogRequest.setRequestURI
					(this.getRemoteTarget().getURI());
					((ViaHeader) dialogRequest.getViaHeaders().getFirst()).setProtocolVersion("SIP/2.0/TCP");
					dialogRequest.addHeader(rl);
				} else {
					// First route is not a lr
					// Add the contact route to the end.
					rl.removeFirst();
					dialogRequest.setRequestURI(sipUri);
					if (rl.size() > 0) dialogRequest.addHeader(rl);
					if (contactRoute != null)
						dialogRequest.addHeader(contactRoute);
				}
			}  else {
				// Bug report from Antonis Karydas
				if (this.getRemoteTarget() != null)
					dialogRequest.setRequestURI
					(this.getRemoteTarget().getURI());
			}
		}
        
        if (LogWriter.needsLogging)
            LogWriter.logMessage("At this point, RURI is: " + dialogRequest.getRequestLine().encode());

		Hop hop = (clientTransactionId).getNextHop();
		 if (LogWriter.needsLogging)
	            LogWriter.logMessage("Using hop = " + hop.getHost() + " : "
					+ hop.getPort());
		        
        try {
            MessageChannel messageChannel =
            sipStack.createRawMessageChannel(hop);
            (clientTransactionId).
            encapsulatedChannel = messageChannel;
            
            if ( messageChannel == null ) {
                // Bug fix from Antonis Karydas
                // At this point the procedures of 8.1.2
                // and 12.2.1.1 of RFC3261 have been tried
                // but the resulting next hop cannot be resolved
                // (recall that the exception thrown
                // is caught and ignored in SIPMessageStack.createMessageChannel()
                // so we end up here  with a null messageChannel
                // instead of the exception handler below).
                // All else failing, try the outbound proxy  in accordance
                // with 8.1.2, in particular:
                // This ensures that outbound proxies that do not add
                // Record-Route header field values will drop out of
                // the path of subsequent requests.  It allows endpoints
                // that cannot resolve the first Route
                // URI to delegate that task to an outbound proxy.
                //
                // if one considers the 'first Route URI' of a
                // request constructed according to 12.2.1.1
                // to be the request URI when the route set is empty.
                Hop outboundProxy = sipStack.getRouter().getOutboundProxy();
                if ( outboundProxy == null )
                    throw new SipException("No route found!");
                messageChannel = sipStack.createRawMessageChannel
                (outboundProxy);
                
            }
            (clientTransactionId).
            encapsulatedChannel = messageChannel;
        } catch (Exception ex) {
            if (LogWriter.needsLogging) LogWriter.logException(ex);
            throw new SipException("Could not create message channel for request " + clientTransactionId.getRequest().getFirstLine());
        }

        // Already incremented when creating the request with Dialog.createRequest()
//        // Increment before setting!!
//        incrementLocalSequenceNumber();
//        dialogRequest.getCSeqHeader().
//        setSequenceNumber(getLocalSequenceNumber());
        
        
        if (this.isServer()) {
            ServerTransaction serverTransaction = (ServerTransaction)
            this.getFirstTransaction();
            
            
            from.setTag(this.myTag);
            to.setTag(this.hisTag);
            
            
            try {
                (clientTransactionId).sendMessage
                (dialogRequest);
                // If the method is BYE then mark the dialog completed.
                if (dialogRequest.getMethod().equals(Request.BYE))
                    this.setState(COMPLETED_STATE);
            } catch (IOException ex) {
                throw new SipException("error sending message");
            }
        } else {
            // I am the client so I do not swap headers.
            ClientTransaction clientTransaction = (ClientTransaction)
            this.getFirstTransaction();
            
            
            
            if (LogWriter.needsLogging) {
                LogWriter.logMessage("setting tags from " +
                this.getDialogId());
                LogWriter.logMessage("fromTag " + this.myTag);
                LogWriter.logMessage("toTag " + this.hisTag);
            }
            
            from.setTag(this.myTag);
            to.setTag(this.hisTag);
            
            
            try {
                (clientTransactionId).
                sendMessage(dialogRequest);
				//If the method is BYE then mark the dialog completed.
				if (dialogRequest.getMethod().equals(Request.BYE))
					this.setState(COMPLETED_STATE);
            } catch (IOException ex) {
                throw new SipException("error sending message");
            }
            
        }
    }
    /** Return yes if the last response is to be retransmitted.
     */
    protected boolean toRetransmitFinalResponse() {
	if (--retransmissionTicksLeft == 0)  {
		this.retransmissionTicksLeft = 2*prevRetransmissionTicks;
		this.prevRetransmissionTicks = retransmissionTicksLeft;
		return true;
	} else return false;

    }

    protected void setRetransmissionTicks() {
		this.retransmissionTicksLeft = 1;
		this.prevRetransmissionTicks = 1;
     }



    /** Resend the last ack.
    */
    public void resendAck () {
	// Check for null.
	// Bug report and fix by Antonis Karydas.
	try {
	if (this.lastAck != null)
	   this.sendAck(lastAck); 
	} catch (SipException ex) {
		ex.printStackTrace();
	}
    }

    protected boolean isInviteDialog() {
	return this.getFirstTransaction().getRequest().getMethod().
			equals(Request.INVITE);
    }




    
    
    
}
