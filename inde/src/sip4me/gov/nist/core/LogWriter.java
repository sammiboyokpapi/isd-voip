/***************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).    *
***************************************************************************/

package sip4me.gov.nist.core;
import java.io.PrintStream;

/**
*  Log System Errors. Also used for debugging log.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public class LogWriter
{
	
	/** 
	 * Don't trace
	 */    
	public static int TRACE_NONE = 0;
	/**
	 * Just errors
	 */
	public static int TRACE_EXCEPTION = 2; 
	/**
	 * Errors and flow information
	 */
	public static int TRACE_MESSAGES = 16;
	/**
	 * Most verbose
	 */
	public static int TRACE_DEBUG = 32;
	private static String   logFileName;
	public static PrintStream traceWriter = System.out;

/** Flag to indicate that logging is enabled. This needs to be
* static and public in order to globally turn logging on or off.
* This is static for efficiency reasons (the java compiler will not
* generate the logging code if this is set to false).
*/
	public static boolean needsLogging = false;

	/**
	*  Debugging trace stream.
	*/
	private    static  PrintStream trace = System.out;
	/** trace level
	 */        
	protected     static int traceLevel = TRACE_NONE;


	/**
	 * Given an Exception object, print its stack trace
	 * @param ex
	 */
	public static void logException(Exception ex) {
	    if (needsLogging && traceLevel >= TRACE_EXCEPTION)  {
	    	System.err.println(ex.getClass() + ": " + ex.getMessage());
	    	ex.printStackTrace();
	    }
	}
		
	/**
	 * FIXME: incorporate remote loggers (file, socket, etc) 
	 * @param message
	 * @param remoteLogger
	 * @deprecated until someone writes this functionality ?
	 */
	public synchronized static void logMessage(String message,
			String remoteLogger) {
		if (needsLogging)
			System.out.println(System.currentTimeMillis() + " :: " + message);

	}
	

	/** Log a message into the log file, only if its level is 
	 * higher than the currently configured <i>traceLevel</i>
         * @param message message to log into the log file.
         */
	public static void logMessage(int level, String message) {
		if (needsLogging && traceLevel >= level)
			System.out.println(System.currentTimeMillis() + " :: " + message);
	}
	
	/** Log a message into the log file, assuming a MESSAGES level.
	 * and depending on current traceLevel value.
         * @param message message to log into the log file.
         */
	public static void logMessage(String message) {
		if (needsLogging && traceLevel >= TRACE_MESSAGES)
			System.out.println(System.currentTimeMillis() + " :: " + message);
	}
	
    
	
        /** Set the trace level for the stack.
         */
        public static void setTraceLevel(int level) {
            traceLevel = level;
        }
        
        /** Get the trace level for the stack.
         */
        public static int getTraceLevel() { return traceLevel; }
        
        
        public static void setLogFileName( String logFileName) {                
        }
	

}
