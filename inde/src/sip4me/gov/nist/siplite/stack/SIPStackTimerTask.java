/*
 * @author:     Brett Buckingham
 * @author:     Last modified by: $Author: arnauvp $
 * @version:    $Date: 2010-02-23 12:20:26 $ $Revision: 1.2 $
 *
 * This source code has been contributed to the public domain.
 */

package sip4me.gov.nist.siplite.stack;

import java.util.TimerTask;

import sip4me.gov.nist.core.LogWriter;

/**
 * A subclass of TimerTask which runs TimerTask code within a try/catch block to
 * avoid killing the SIPTransactionStack timer thread. Note: subclasses MUST not
 * override run(); instead they should override runTask().
 * 
 * @author Brett Buckingham
 * 
 */
public abstract class SIPStackTimerTask extends TimerTask {
	// / Implements code to be run when the SIPStackTimerTask is executed.
	protected abstract void runTask();

	// / The run() method is final to ensure that all subclasses inherit the
	// exception handling.
	public final void run() {
		try {
			runTask();
		} catch (Throwable e) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "SIP stack timer task failed due to exception: " + e.getClass() + ":" + e.getMessage());
		}
	}
}
