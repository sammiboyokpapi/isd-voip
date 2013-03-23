/**
 *	An event that indicates that a transaction has encountered an error.
 *
 *@author Added by M. Ranganathan
 */

package sip4me.gov.nist.siplite.stack;

public class SIPTimerEvent  {

	public static int RETRANSMISSION = 1;
        
        private Transaction source;


	private int eventId;
        
        public Transaction getSource() { 
               return this.source;
        }


	/**
	 *	Creates a transaction error event.
	 *
	 *	@param sourceTransaction Transaction which is raising the error.
	 *	@param transactionErrorID ID of the error that has ocurred.
	 */
	SIPTimerEvent (
		Transaction	sourceTransaction, int eventId
	) {

		this.source = sourceTransaction;
		this.eventId = eventId;

	}


}
