package sip4me.gov.nist.siplite.stack;

/** 
* Event listener for timeout events. Register this with the stack if you want
* to handle your own transaction timeout events (i.e want control of 
* the transaction state machine.
*/

public interface SIPTimerListener {
	/** 
	* Invoked for clients who want to handle their own transaction
	* state machine.
	*
	*@param timerEvent is the timer event.
	*
	*/
	public void timerEvent (
		SIPTimerEvent timerEvent
	);

}
