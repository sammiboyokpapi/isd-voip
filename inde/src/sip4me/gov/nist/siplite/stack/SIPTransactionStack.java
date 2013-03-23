package sip4me.gov.nist.siplite.stack;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.header.EventHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.message.Message;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;

/**
 * Adds a transaction layer to the {@link SIPMessageStack} class. This is done
 * by replacing the normal MessageChannels returned by the base class with
 * transaction-aware MessageChannels that encapsulate the original channels and
 * handle the transaction state machine, retransmissions, etc.
 * 
 *@author Jeff Keyser (original) M. Ranganathan <mranga@nist.gov> <br/>
 *         (Added Dialog table). <a href="{@docRoot} /uncopyright.html">This
 *         code is in the public domain.</a>
 * 
 *@version JAIN-SIP-1.1
 * 
 */
public abstract class SIPTransactionStack extends SIPMessageStack implements
		SIPTransactionEventListener {

	/**
	 * Number of milliseconds between timer ticks.
	 * 
	 * From RFC 3261: The default value for T1 is 500 ms. T1 is an estimate of
	 * the RTT between the client and server transactions. [...]. T1 MAY be
	 * chosen larger, and this is RECOMMENDED if it is known in advance (such as
	 * on high latency access links) that the RTT is larger.
	 * 
	 * As sip-for-me real use is on mobile devices which use GPRS/3G slower
	 * connections, a bit higher value seems sensitive. However, if this value
	 * is too big, timed-out transactions will take longer to be marked as
	 * terminated and, under heavy traffic, this can cause OutOfMemory errors.
	 * 
	 **/
	public static final int BASE_TIMER_INTERVAL = 650;

	// Collection of current client transactions
	private final Vector clientTransactions;
	// Collection or current server transactions
	private final Vector serverTransactions;
	// Table of dialogs.
	private final Hashtable dialogTable;

	// Max number of server transactions concurrent.
	protected int transactionTableSize;

	// A table of assigned dialogs.

	// Retransmission filter - indicates the stack will retransmit 200 OK
	// for invite transactions.
	protected boolean retransmissionFilter;

	// A set of methods that result in dialog creations.
	protected Hashtable dialogCreatingMethods;

	/**
	 * Default constructor.
	 */
	protected SIPTransactionStack() {
		super();
		this.transactionTableSize = -1;
		// a set of methods that result in dialog creation.
		this.dialogCreatingMethods = new Hashtable();
		// Standard set of methods that create dialogs.
		this.dialogCreatingMethods.put(Request.REFER, "");
		this.dialogCreatingMethods.put(Request.INVITE, "");
		this.dialogCreatingMethods.put(Request.SUBSCRIBE, "");
		// Notify may or may not create a dialog. This is handled in
		// the code.
		// this.dialogCreatingMethods.add(Request.NOTIFY);
		// Create the transaction collections
		clientTransactions = new Vector();
		serverTransactions = new Vector();
		// Dialog table.
		this.dialogTable = new Hashtable();

		// Start the timer event thread.
		new Thread(new TransactionScanner()).start();

	}

	/**
	 * Utility method
	 */
	private void printDialogCreatingMethods() {
		System.out.println("PRINTING DIALOGCREATINGMETHODS HASHTABLE");
		Enumeration e = dialogCreatingMethods.keys();
		while (e.hasMoreElements()) {
			System.out.println(e.nextElement());
		}
		System.out.println("DIALOGCREATINGMETHODS HASHTABLE PRINTED");
	}

	/**
	 * Provides whether this Request creates a dialog or not.
	 * 
	 *@return true if extension is supported and false otherwise.
	 * 
	 */
	public boolean isDialogCreated(String method) {
		return dialogCreatingMethods.containsKey(method.toUpperCase());
	}

	/**
	 * Add an extension method.
	 * 
	 *@param extensionMethod
	 *            -- extension method to support for dialog creation
	 */
	public void addExtensionMethod(String extensionMethod) {
		if (!extensionMethod.equals(Request.NOTIFY)) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage("NOTIFY Supported Natively");
		} else
			this.dialogCreatingMethods.put(extensionMethod, "");
	}

	/**
	 * Put a dialog into the dialog table.
	 * 
	 *@param dialog
	 *            -- dialog to put into the dialog table.
	 * 
	 */
	public void putDialog(Dialog dialog) {

		String dialogId = dialog.getDialogId();
		if (LogWriter.needsLogging) {
			LogWriter.logMessage("putDialog " + dialog + " with dialogId="
					+ dialogId);
		}

		if (dialogTable.containsKey(dialogId)) {
			if (LogWriter.needsLogging) {
				LogWriter.logMessage(LogWriter.TRACE_DEBUG,
						"Not adding dialog to table because it is already there\n"
								+ "DialogID: " + dialogId + "\nDialog: "
								+ dialogTable.get(dialogId));
			}
			return;
		}
		dialog.setStack(this);
		synchronized (dialogTable) {
			dialogTable.put(dialogId, dialog);
		}
	}

	public synchronized Dialog createDialog(Transaction transaction) {

		if (LogWriter.needsLogging)
			LogWriter
					.logMessage("[SIPTransactionStack] Creating dialog for transaction "
							+ transaction);

		return new Dialog(transaction);
	}

	/**
	 * Creates a new dialog based on a received NOTIFY. The dialog state is
	 * initialized appropriately. The NOTIFY differs in the From tag
	 * 
	 * Made this a separate method to clearly distinguish what's happening here
	 * - this is a non-trivial case
	 * 
	 * @param subscribeCT
	 *            - the transaction started with the SUBSCRIBE that we sent
	 * @param notifyST
	 *            - the ServerTransaction created for an incoming NOTIFY
	 * @return -- a new dialog created from the subscribe original SUBSCRIBE
	 *         transaction.
	 * 
	 * 
	 */

	/**
	 * Return the dialog for a given dialog ID. If compatibility is enabled then
	 * we do not assume the presence of tags and hence need to add a flag to
	 * indicate whether this is a server or client transaction.
	 * 
	 *@param dialogId
	 *            is the dialog id to check.
	 * 
	 */

	public Dialog getDialog(String dialogId) {
		if (LogWriter.needsLogging)
			LogWriter.logMessage("Getting dialog for " + dialogId);

		synchronized (dialogTable) {
			return (Dialog) dialogTable.get(dialogId);
		}
	}

	/**
	 * Find a matching client SUBSCRIBE to the incoming notify. NOTIFY requests
	 * are matched to such SUBSCRIBE requests if they contain the same
	 * "Call-ID", a "ToHeader" header "tag" parameter which matches the
	 * "FromHeader" header "tag" parameter of the SUBSCRIBE, and the same
	 * "Event" header field. Rules for comparisons of the "Event" headers are
	 * described in section 7.2.1. If a matching NOTIFY request contains a
	 * "Subscription-State" of "active" or "pending", it creates a new
	 * subscription and a new dialog (unless they have already been created by a
	 * matching response, as described above).
	 * 
	 *@param notifyMessage
	 */
	public ClientTransaction findSubscribeTransaction(Request notifyMessage) {

		synchronized (clientTransactions) {
			Enumeration it = clientTransactions.elements();
			String thisToHeaderTag = notifyMessage.getTo().getTag();
			if (thisToHeaderTag == null)
				return null;
			EventHeader eventHdr = (EventHeader) notifyMessage
					.getHeader(Header.EVENT);
			if (eventHdr == null)
				return null;
			while (it.hasMoreElements()) {
				ClientTransaction ct = (ClientTransaction) it.nextElement();
				Request sipRequest = ct.getOriginalRequest();
				String fromTag = sipRequest.getFromHeader().getTag();
				EventHeader hisEvent = (EventHeader) sipRequest
						.getHeader(Header.EVENT);
				// Event header is mandatory but some sloppy clients
				// don't include it.
				if (hisEvent == null)
					continue;
				if (sipRequest.getMethod().equals(Request.SUBSCRIBE)
						&& Utils.equalsIgnoreCase(fromTag, thisToHeaderTag)
						&& hisEvent != null
						&& eventHdr.match(hisEvent)
						&& Utils.equalsIgnoreCase(notifyMessage.getCallId()
								.getCallId(), sipRequest.getCallId()
								.getCallId())) {
					return ct;
				}
			}

		}
		return null;
	}

	/**
	 * Find the transaction corresponding to a given request.
	 * 
	 *@param sipRequest
	 *            -- request for which to retrieve the transaction.
	 * 
	 *@param isServer
	 *            -- search the server transaction table if true.
	 * 
	 *@return the transaction object corresponding to the request or null if no
	 *         such mapping exists.
	 */
	public Transaction findTransaction(Message sipMessage, boolean isServer) {

		if (isServer) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage("searching server transaction for "
						+ sipMessage + " size =  "
						+ this.serverTransactions.size());
			synchronized (this.serverTransactions) {
				Enumeration it = serverTransactions.elements();
				while (it.hasMoreElements()) {
					ServerTransaction sipServerTransaction = (ServerTransaction) it
							.nextElement();
					if (sipServerTransaction
							.isMessagePartOfTransaction(sipMessage))
						return sipServerTransaction;
				}
			}
		} else {
			synchronized (this.clientTransactions) {
				Enumeration it = clientTransactions.elements();
				while (it.hasMoreElements()) {
					ClientTransaction clientTransaction = (ClientTransaction) it
							.nextElement();
					if (clientTransaction
							.isMessagePartOfTransaction(sipMessage))
						return clientTransaction;
				}
			}

		}
		return null;

	}

	/**
	 * Get the transaction to cancel. Search the server transaction table for a
	 * transaction that matches the given transaction.
	 */
	public Transaction findInviteTransactionToCancel(Request cancelRequest,
			boolean isServer) {

		if (LogWriter.needsLogging) {
			LogWriter
					.logMessage("findInviteTransactionToCancel. CANCEL request= \n"
							+ cancelRequest
							+ "\nfindCancelRequest isServer="
							+ isServer);
		}

		if (isServer) {
			synchronized (this.serverTransactions) {
				Enumeration li = this.serverTransactions.elements();
				while (li.hasMoreElements()) {
					Transaction transaction = (Transaction) li.nextElement();
					Request sipRequest = (transaction.getRequest());
					ServerTransaction sipServerTransaction = (ServerTransaction) transaction;
					if (sipServerTransaction
							.doesCancelMatchTransaction(cancelRequest)) {
						if (LogWriter.needsLogging)
							LogWriter.logMessage(LogWriter.TRACE_DEBUG,
									"Found INVITE to cancel: "
											+ sipServerTransaction
													.getOriginalRequest()
													.encode());
						return sipServerTransaction;
					}
				}
			}
		} else {
			synchronized (this.clientTransactions) {
				Enumeration li = this.clientTransactions.elements();
				while (li.hasMoreElements()) {
					Transaction transaction = (Transaction) li.nextElement();
					Request sipRequest = (transaction.getRequest());

					ClientTransaction sipClientTransaction = (ClientTransaction) transaction;
					if (sipClientTransaction
							.doesCancelMatchTransaction(cancelRequest)) {
						if (LogWriter.needsLogging)
							LogWriter.logMessage(LogWriter.TRACE_DEBUG,
									"Found INVITE to cancel: "
											+ sipClientTransaction
													.getOriginalRequest()
													.encode());
						return sipClientTransaction;
					}

				}
			}
		}
		return null;
	}

	/**
	 * Constructor for the stack. Registers the request and response factories
	 * for the stack.
	 * 
	 * @param messageFactory
	 *            User-implemented factory for processing messages.
	 */
	protected SIPTransactionStack(SIPStackMessageFactory messageFactory) {
		this();
		super.sipMessageFactory = messageFactory;
	}

	/**
	 * Thread used to throw timer events for all transactions.
	 * 
	 * @FIXME This loop is wrong, because ticks will typically be fired in a
	 *        time < BASE_TIMER_INTERVAL. It should be changed by a TimerTask in
	 *        each Transaction object that is scheduled for BASE_TIMER_INTERVAL
	 *        to ensure enough time is left for the response to be processed.
	 */
	class TransactionScanner implements Runnable {
		public void run() {

			// Iterator through all transactions
			Enumeration transactionIterator;
			// One transaction in the set
			Transaction nextTransaction;

			// Loop while this stack is running
			while (isAlive()) {

				try {

					// Sleep for one timer "tick"
					Thread.sleep(BASE_TIMER_INTERVAL);

					// System.out.println("clientTransactionTable size " +
					// clientTransactions.size());
					// System.out.println("serverTransactionTable size " +
					// serverTransactions.size());
					// Check all client transactions

					Vector fireList = new Vector();
					Vector removeList = new Vector();

					// Check all server transactions
					synchronized (serverTransactions) {
						transactionIterator = serverTransactions.elements();
						while (transactionIterator.hasMoreElements()) {

							nextTransaction = (Transaction) transactionIterator
									.nextElement();

							// If the transaction has terminated,
							if (nextTransaction.isTerminated()) {
								// Keep the transaction hanging around
								// to catch the incoming ACK.
								// BUG report from Antonis Karydas
								if (((ServerTransaction) nextTransaction).collectionTime == 0) {
									// Remove it from the set
									if (LogWriter.needsLogging)
										LogWriter.logMessage("removing"
												+ nextTransaction);
									removeList.addElement(nextTransaction);
								} else {
									((ServerTransaction) nextTransaction).collectionTime--;
								}
								// If this transaction has not
								// terminated,
							} else {
								// Add to the fire list -- needs to be moved
								// outside the synchronized block to prevent
								// deadlock.
								/**
								 * System.out.println("state = " +
								 * nextTransaction.getState() + "/" +
								 * nextTransaction.getOriginalRequest().
								 * getMethod());
								 **/
								fireList.addElement(nextTransaction);

							}

						}
						for (int j = 0; j < removeList.size(); j++) {
							serverTransactions.removeElement(removeList
									.elementAt(j));
						}
					}

					removeList = new Vector();

					synchronized (clientTransactions) {
						transactionIterator = clientTransactions.elements();
						while (transactionIterator.hasMoreElements()) {

							nextTransaction = (Transaction) transactionIterator
									.nextElement();

							// If the transaction has terminated,
							if (nextTransaction.isTerminated()) {

								// Remove it from the set
								if (LogWriter.needsLogging) {
									LogWriter
											.logMessage("Removing clientTransaction "
													+ nextTransaction);
								}
								removeList.addElement(nextTransaction);

								// If this transaction has not
								// terminated,
							} else {
								// Add to the fire list -- needs to be moved
								// outside the synchronized block to prevent
								// deadlock.
								fireList.addElement(nextTransaction);

							}
						}
						for (int j = 0; j < removeList.size(); j++) {
							clientTransactions.removeElement(removeList
									.elementAt(j));
						}
					}
					removeList = new Vector();

					synchronized (dialogTable) {
						Enumeration values = dialogTable.elements();
						while (values.hasMoreElements()) {
							Dialog d = (Dialog) values.nextElement();
							// System.out.println("dialogState = " +
							// d.getState() +
							// " isServer = " + d.isServer());
							if (d.getState() == Dialog.TERMINATED_STATE) {
								if (LogWriter.needsLogging) {
									String dialogId = d.getDialogId();
									LogWriter.logMessage("Removing Dialog "
											+ dialogId);
								}
								removeList.addElement(d);
							}
							if (d.isServer() && (!d.ackSeen)
									&& d.isInviteDialog()) {
								Transaction transaction = d
										.getLastTransaction();
								// If stack is managing the transaction
								// then retransmit the last response.
								if (transaction.getState() == Transaction.TERMINATED_STATE
										&& transaction instanceof ServerTransaction
										&& ((ServerTransaction) transaction).isMapped) {
									Response response = transaction
											.getLastResponse();
									// Retransmit to 200 until ack received.
									if (response.isSuccessfulResponse()) {
										try {
											if (d.toRetransmitFinalResponse())
												transaction
														.sendMessage(response);
										} catch (IOException ex) {
											/* Will eventully time out */
											d.setState(Dialog.TERMINATED_STATE);
										} finally {
											// Need to fire the timer so
											// transaction will eventually
											// time out whether or not
											// the IOException occurs
											// (bug fix sent in
											// by Christophe).
											fireList.addElement(transaction);
										}
									}
								}
							}

						}
						for (int j = 0; j < removeList.size(); j++) {
							Dialog d = (Dialog) removeList.elementAt(j);
							dialogTable.remove(d.getDialogId());
						}
					}

					for (int i = 0; i < fireList.size(); i++) {
						nextTransaction = (Transaction) fireList.elementAt(i);
						nextTransaction.fireTimer();
					}

				} catch (Exception e) {
					e.printStackTrace();
					// Ignore
				}

			}

		}
	}

	/**
	 * Handles a new SIP request. It finds a server transaction to handle this
	 * message. If none exists, it creates a new transaction.
	 * 
	 * @param requestReceived
	 *            Request to handle.
	 * @param requestMessageChannel
	 *            Channel that received message.
	 * 
	 * @return A server transaction.
	 */
	protected SIPServerRequestInterface newSIPServerRequest(
			Request requestReceived, MessageChannel requestMessageChannel) {

		if (LogWriter.needsLogging)
			LogWriter.logMessage("SIPTransactionStack: creating new Request "
					+ requestReceived.getFirstLine());

		try {
			// Iterator through all server transactions
			Enumeration transactionIterator;
			// Next transaction in the set
			ServerTransaction nextTransaction;
			// Transaction to handle this request
			ServerTransaction currentTransaction;

			// Loop through all server transactions
			synchronized (serverTransactions) {
				transactionIterator = serverTransactions.elements();
				currentTransaction = null;
				while (transactionIterator.hasMoreElements()
						&& currentTransaction == null) {

					nextTransaction = (ServerTransaction) transactionIterator
							.nextElement();

					// If this transaction should handle this request,
					if (nextTransaction
							.isMessagePartOfTransaction(requestReceived)) {
						// Mark this transaction as the one
						// to handle this message
						currentTransaction = nextTransaction;

					}

				}

				// If no transaction exists to handle this message
				if (currentTransaction == null) {
					currentTransaction = createServerTransaction(requestMessageChannel);
					currentTransaction.setOriginalRequest(requestReceived);

					if (LogWriter.needsLogging)
						LogWriter.logMessage("Created server transaction: "
								+ currentTransaction);

					if (!isDialogCreated(requestReceived.getMethod())) {
						// Dialog is not created - can we find the state?
						// If so, then create a transaction and add it.
						String dialogId = requestReceived.getDialogId(true);
						Dialog dialog = getDialog(dialogId);

						if (LogWriter.needsLogging)
							LogWriter
									.logMessage("Non-dialog creating request with dialogID = "
											+ dialogId
											+ ".\n "
											+ "Existing dialog found?: "
											+ dialog);
						// Sequence numbers are supposed to increment.
						// avoid processing old sequence numbers and
						// delivering the same request up to the
						// application if the request has already been seen.
						// Special handling applies to ACK processing.
						if (dialog != null
								&& (requestReceived.getMethod().equals(
										Request.ACK) || requestReceived
										.getCSeqHeader().getSequenceNumber() > dialog
										.getRemoteSequenceNumber())) {
							// Found a dialog.
							if (LogWriter.needsLogging)
								LogWriter
										.logMessage("Adding server transaction for a "
												+ "request inside a dialog: "
												+ currentTransaction);
							serverTransactions.addElement(currentTransaction);
							currentTransaction.isMapped = true;
						} else {
							// Fix by ArnauVP:
							// This is a request outside a dialog
							// that doesn't create a dialog,
							// except for NOTIFYs
							if (!requestReceived.getMethod().equals(
									Request.NOTIFY)) {
								if (LogWriter.needsLogging)
									LogWriter
											.logMessage("Adding server transaction for a request "
													+ "outside a dialog");
							}
							serverTransactions.addElement(currentTransaction);
							currentTransaction.isMapped = true;
						}
					} else {
						// Create the transaction but don't map it.
						String dialogId = requestReceived.getDialogId(true);
						Dialog dialog = getDialog(dialogId);
						// This is a dialog creating request that is part of an
						// existing dialog (eg. re-Invite). Re-invites get a non
						// null server transaction Id (unlike the original
						// invite).
						if (dialog != null
								&& requestReceived.getCSeqHeader()
										.getSequenceNumber() > dialog
										.getRemoteSequenceNumber()) {
							try {
								currentTransaction.map();
							} catch (IOException ex) {
								/** Ignore **/
							}
							serverTransactions.addElement(currentTransaction);
							currentTransaction.toListener = true;
						}
					}
				}

				// Set this transaction's encapsulated request
				// interface from the superclass
				currentTransaction.setRequestInterface(super
						.newSIPServerRequest(requestReceived,
								currentTransaction));
				return currentTransaction;
			}
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			throw ex;
		}

	}

	/**
	 * Handles a new SIP response. It finds a client transaction to handle this
	 * message. If none exists, it sends the message directly to the superclass.
	 * 
	 * @param responseReceived
	 *            Response to handle.
	 * @param responseMessageChannel
	 *            Channel that received message.
	 * 
	 * @return A client transaction.
	 */
	protected SIPServerResponseInterface newSIPServerResponse(
			Response responseReceived, MessageChannel responseMessageChannel) {
		if (LogWriter.needsLogging)
			LogWriter.logMessage("SIPTransactionStack: creating new Response "
					+ responseReceived.getFirstLine());

		// Iterator through all client transactions
		Enumeration transactionIterator;
		// Next transaction in the set
		ClientTransaction nextTransaction;
		// Transaction to handle this request
		ClientTransaction currentTransaction;

		// Loop through all server transactions
		synchronized (clientTransactions) {
			transactionIterator = clientTransactions.elements();
			currentTransaction = null;
			while (transactionIterator.hasMoreElements()
					&& currentTransaction == null) {

				nextTransaction = (ClientTransaction) transactionIterator
						.nextElement();

				// If this transaction should handle this request,
				if (nextTransaction
						.isMessagePartOfTransaction(responseReceived)) {

					// Mark this transaction as the one to
					// handle this message
					currentTransaction = nextTransaction;

				}

			}
		}

		// If no transaction exists to handle this message,
		if (currentTransaction == null) {

			// Pass the message directly to the TU
			// return super.newSIPServerResponse
			// ( responseReceived, responseMessageChannel );

			// Fix by ArnauVP (Genaker)
			// Maybe the transaction has been terminated and removed
			// from our list. This will cause NullPointer in the EventScanner
			// so just stop here
			return null;

		}

		// Set this transaction's encapsulated response interface
		// from the superclass
		currentTransaction.setResponseInterface(super.newSIPServerResponse(
				responseReceived, currentTransaction));
		return currentTransaction;

	}

	/**
	 * Creates a client transaction to handle a new request. Gets the real
	 * message channel from the superclass, and then creates a new client
	 * transaction wrapped around this channel.
	 * 
	 * @param nextHop
	 *            Hop to create a channel to contact.
	 */
	public MessageChannel createMessageChannel(Hop nextHop) {
		synchronized (clientTransactions) {
			// New client transaction to return
			Transaction returnChannel;

			// Create a new client transaction around the
			// superclass' message channel
			MessageChannel mc = super.createMessageChannel(nextHop);
			if (mc == null)
				return null;
			returnChannel = createClientTransaction(mc);
			clientTransactions.addElement(returnChannel);
			((ClientTransaction) returnChannel).setViaPort(nextHop.getPort());
			((ClientTransaction) returnChannel).setViaHost(nextHop.getHost());
			return returnChannel;
		}

	}

	/**
	 * Create a client transaction from a raw channel.
	 * 
	 *@param rawChannel
	 *            is the transport channel to encapsulate.
	 */

	public MessageChannel createMessageChannel(MessageChannel rawChannel) {
		synchronized (clientTransactions) {
			// New client transaction to return
			Transaction returnChannel = createClientTransaction(rawChannel);
			clientTransactions.addElement(returnChannel);
			((ClientTransaction) returnChannel).setViaPort(rawChannel
					.getViaPort());
			((ClientTransaction) returnChannel)
					.setViaHost(rawChannel.getHost());
			return returnChannel;
		}
	}

	/**
	 * Create a client transaction from a raw channel.
	 * 
	 *@param rawChannel
	 *            is the transport channel to encapsulate.
	 */

	public MessageChannel createMessageChannel(Transaction transaction) {
		synchronized (clientTransactions) {
			// New client transaction to return
			Transaction returnChannel = createClientTransaction(transaction
					.getMessageChannel());
			clientTransactions.addElement(returnChannel);
			((ClientTransaction) returnChannel).setViaPort(transaction
					.getViaPort());
			((ClientTransaction) returnChannel).setViaHost(transaction
					.getViaHost());
			return returnChannel;
		}
	}

	/**
	 * Creates a client transaction that encapsulates a MessageChannel. Useful
	 * for implementations that want to subclass the standard
	 * 
	 * @param encapsulatedMessageChannel
	 *            Message channel of the transport layer.
	 */
	public ClientTransaction createClientTransaction(
			MessageChannel encapsulatedMessageChannel) {

		return new ClientTransaction(this, encapsulatedMessageChannel);

	}

	/**
	 * Creates a server transaction that encapsulates a MessageChannel. Useful
	 * for implementations that want to subclass the standard
	 * 
	 * @param encapsulatedMessageChannel
	 *            Message channel of the transport layer.
	 */
	public ServerTransaction createServerTransaction(
			MessageChannel encapsulatedMessageChannel) {

		return new ServerTransaction(this, encapsulatedMessageChannel);

	}

	/**
	 * Creates a raw message channel. A raw message channel has no transaction
	 * wrapper.
	 * 
	 *@param hop
	 *            -- hop for which to create the raw message channel.
	 * 
	 */
	public MessageChannel createRawMessageChannel(Hop hop) {
		return super.createMessageChannel(hop);
	}

	/**
	 * Add a new client transaction to the set of existing transactions.
	 * 
	 *@param clientTransaction
	 *            -- client transaction to add to the set.
	 */

	public void addTransaction(ClientTransaction clientTransaction) {
		synchronized (clientTransactions) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG,
						"Adding client transaction to stack: "
								+ clientTransaction);
			clientTransactions.addElement(clientTransaction);
		}
	}

	/**
	 * Add a new client transaction to the set of existing transactions.
	 * 
	 *@param serverTransaction
	 *            -- server transaction to add to the set.
	 */

	public void addTransaction(ServerTransaction serverTransaction)
			throws IOException {
		synchronized (serverTransactions) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG,
						"Adding server transaction to stack: "
								+ serverTransaction);
			serverTransaction.map();
			this.serverTransactions.addElement(serverTransaction);
		}
	}

	/**
	 * public boolean hasResources() { if (transactionTableSize == -1) return
	 * true; else { return serverTransactions.size() < transactionTableSize; } }
	 **/

}
