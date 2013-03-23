package sip4me.gov.nist.siplite.stack;


/**
 *	An event that indicates that a transaction has encountered an error.
 *
 *	@author	Jeff Keyser (ported to j2me by M. Ranganathan).
 */
public class SIPTransactionErrorEvent {
    
        protected Transaction sourceTransaction;

	/**
	 *	This event ID indicates that the transaction has timed out.
	 */
	public static final int	TIMEOUT_ERROR = 1;
	/**
	 *	This event ID indicates that there was an error sending
         *  a message using the underlying transport.
	 */
	public static final int	TRANSPORT_ERROR = 2;

	// ID of this error event
	private int		errorID;


	/**
	 *	Creates a transaction error event.
	 *
	 *@param sourceTransaction Transaction which is raising the error.
	 *@param transactionErrorID ID of the error that has ocurred.
	 */
	SIPTransactionErrorEvent(
		Transaction	sourceTransaction,
		int				transactionErrorID
	) {

		this.sourceTransaction = sourceTransaction;
		errorID = transactionErrorID;

	}
        
        /** Get the error source.
         *@return the source of the error.
         */

        public Transaction getSource() {
            return this.sourceTransaction;
        }

	/**
	 *Returns the ID of the error.
	 *
	 *@return Error ID.
	 */
	public int getErrorID(
	) {

		return errorID;

	}

}
