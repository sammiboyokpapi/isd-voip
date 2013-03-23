package sip4me.gov.nist.siplite.stack;


/**
 * Interface implemented by classes that want to be notified of asynchronous
 * transacion events.
 *
 *	@author	Jeff Keyser (Ported to siplite by M. Ranganathan)
 */
public interface SIPTransactionEventListener {

	/**
	 *	Invoked when an error has ocurred with a transaction.
	 *
	 *	@param transactionErrorEvent Error event.
	 */
	public void transactionErrorEvent(
		SIPTransactionErrorEvent	transactionErrorEvent
	);

}
