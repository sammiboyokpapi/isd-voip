/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 ******************************************************************************/

package sip4me.gov.nist.siplite.stack;
import java.io.PrintStream;
import java.util.Enumeration;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.message.Message;


/** Log file wrapper class.
 * Log messages into the message trace file and also write the log into the
 * debug file if needed. This class keeps an XML formatted trace around for
 * later access via RMI. The trace can be viewed with a trace viewer (see
 * tools.traceviewerapp).
 *
 *@version  JAIN-SIP-1.1
 *
 *@author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 *
 */

public class ServerLog {
    
    /** Dont trace */
    public static int TRACE_NONE = 0;

    public static int TRACE_MESSAGES = 16;
    /** Trace exception processing
     */
    public static int TRACE_EXCEPTION = 17;
    /** Debug trace level (all tracing enabled).
     */
    public static int TRACE_DEBUG = 32;
    /** Name of the log file in which the trace is written out
     * (default is null)
     */
    /** Name of the log directory in which the messages are written out
     */
    /** print stream for writing out trace
     */
    protected static PrintStream printWriter = null;

    protected static PrintStream traceWriter = null;
    
    
    /** Set auxililary information to log with this trace.
     */
    protected static String auxInfo;
    
    protected static String description;

    protected static String stackIpAddress;
    
    /** default trace level
     */
    protected     static int traceLevel = TRACE_NONE;
    
    public static void checkLogFile()  {
            // Append buffer to the end of the file.
    	if (printWriter == null) {
    		printWriter = traceWriter;
    		if (printWriter == null)
    			printWriter = System.out;
    		if (auxInfo != null) 
    			printWriter.println
    			("<description\n logDescription=\""+description+
    					"\"\n name=\"" + stackIpAddress  + 
    					"\"\n auxInfo=\"" + auxInfo  + 
    			"\"/>\n ");
    		else 
    			printWriter.println("<description\n logDescription=\""
    					+description+ "\"\n name=\"" + stackIpAddress  + 
    			"\" />\n");
    	}
    }
    
    
    private static String getStatusHeader(Message message) {
        // If this message has a "NISTStatus" extension then we extract
        // it for logging.
        Enumeration statusHeaders = message.getHeaders("NISTExtension");
        String status = null;
        if (statusHeaders.hasMoreElements()) {
            Header statusHdr =  (Header) statusHeaders.nextElement();
            status = statusHdr.getHeaderValue();
        }
        return status;
    }
    
    /**
     *Check to see if logging is enabled at a level (avoids
     * unecessary message formatting.
     *@param logLevel level at which to check.
     */
    public static boolean needsLogging(int logLevel) {
        return traceLevel >= logLevel;
    }
    
    
    /** Global check for whether to log or not. ToHeader minimize the time
     *return false here.
     *
     *@return true -- if logging is globally enabled and false otherwise.
     *
     */
    
    public static boolean needsLogging() {
        return traceLevel >= 16;
    }
    
    /** Set the log file name
     *@param name is the name of the log file to set.
     */
    public static void setLogFileName(String  loggerURL) {

    }
    
    
    
    /** Log a message into the log file.
     * @param message message to log into the log file.
     */
    public static void logMessage( String message) {
        // String tname = Thread.currentThread().getName();
    	if (needsLogging()) {
    		checkLogFile();
    		String logInfo = message;
    		printWriter.println(logInfo);
    		if (LogWriter.needsLogging) {
    			LogWriter.logMessage(logInfo);
    		}
    	}
    }
    
    /** Log a message into the log directory.
     * @param message a Message to log
     * @param from from header of the message to log into the log directory
     * @param to to header of the message to log into the log directory
     * @param sender is the server the sender (true if I am the sender).
     * @param callId CallId of the message to log into the log directory.
     * @param firstLine First line of the message to display
     * @param status Status information (generated while processing message).
     * @param time the reception time (or date).
     */
    public synchronized static void logMessage(String message,
    String from,
    String to,
    boolean sender,
    String callId,
    String firstLine,
    String status,
    String tid,
    String  time) { 
    	if (needsLogging()) {

        MessageLog log = new MessageLog(message, from, to, time,
            sender,  firstLine, status, tid,callId);
        logMessage(log.flush());
    	}
    }

    public synchronized static void logMessage(String message,
    String from,
    String to,
    boolean sender,
    String callId,
    String firstLine,
    String status,
    String tid,
    long  time) {
        
    	if (needsLogging()) {

    		MessageLog log = new MessageLog(message, from, to, time,
    				sender,  firstLine,
    				status, tid, callId);
    		logMessage(log.flush());
    	}
    }
    
    /** Log a message into the log directory.
     * @param message a Message to log
     * @param from from header of the message to log into the log directory
     * @param to to header of the message to log into the log directory
     * @param sender is the server the sender
     * @param callId CallId of the message to log into the log directory.
     * @param firstLine First line of the message to display
     * @param status Status information (generated while processing message).
     * @param tid    is the transaction id for the message.
     */
    public static void logMessage(String message,
    String from, String to,
    boolean sender,
    String callId,
    String firstLine,
    String status,
    String tid) {
    	if (needsLogging()) {

    		String time = new Long(System.currentTimeMillis()).toString();
    		logMessage
    		(message,from, to,sender,callId,firstLine,status,
    				tid,time);
    	}
    }
    
    
    /** Log a message into the log directory. Status information is extracted
     * from the NISTExtension Header.
     * @param message a Message to log
     * @param from from header of the message to log into the log directory
     * @param to to header of the message to log into the log directory
     * @param sender is the server the sender
     * @param time is the time to associate with the message.
     */
    public static void logMessage(Message message, String from,
    String to, boolean sender, String time) {
    	if (needsLogging()) {

    		checkLogFile();
    		CallIdHeader cid = (CallIdHeader)message.getCallId();
    		String callId = null;
    		if (cid != null) callId = ((CallIdHeader)message.getCallId()).getCallId();
    		String firstLine = message.getFirstLine();
    		String inputText = message.encode();
    		String status = getStatusHeader(message);
    		String tid = message.getTransactionId();
    		logMessage( inputText , from, to,  sender,
    				callId, firstLine,status,tid,time);
    	}
    }
    
    /** Log a message into the log directory.
     * Status information is extracted from the NISTExtension Header.
     * @param message a Message to log
     * @param from from header of the message to log into the log directory
     * @param to to header of the message to log into the log directory
     * @param sender is the server the sender
     * @param time is the time to associate with the message.
     */
    public static void logMessage(Message message, String from,
    String to, boolean sender, long time) {
    	if (needsLogging()) {
    		checkLogFile();
    		CallIdHeader cid =(CallIdHeader )message.getCallId();
    		String callId = null;
    		if (cid != null) callId = cid.getCallId();
    		String firstLine = message.getFirstLine().trim();
    		String inputText = message.encode();
    		String status = getStatusHeader(message);
    		String tid = message.getTransactionId();
    		logMessage( inputText , from, to, sender,
    				callId, firstLine,status,tid,time);
    	}
    }
    
    /** Log a message into the log directory. Status information is extracted
     * from SIPExtension header. The time associated with the message is the
     * current time.
     * @param message a Message to log
     * @param from from header of the message to log into the log directory
     * @param to to header of the message to log into the log directory
     * @param sender is the server the sender
     */
    public static void logMessage(Message message, String from,
    String to, boolean sender) {
    	if (needsLogging()) {
    		logMessage(message,from,to,sender,
    				new Long(System.currentTimeMillis()).toString());
    	}
    }
    
    /** Log a message into the log directory.
     * @param message a Message to log
     * @param from from header of the message to log into the log directory
     * @param to to header of the message to log into the log directory
     * @param status the status to log. This is appended to any NISTExtension
     *	  header present in the message.
     * @param sender is the server the sender or receiver (true if sender).
     * @param time is the reception time.
     */
    public static void logMessage(Message message, String from,
    String to, String status,
    boolean sender, String time) {
    	if (needsLogging()) {

    		checkLogFile();
    		CallIdHeader cid =(CallIdHeader) message.getCallId();
    		String callId = null;
    		if (cid != null) callId = cid.getCallId();
    		String firstLine = message.getFirstLine().trim();
    		String encoded = message.encode();
    		String tid = message.getTransactionId();
    		String shdr = getStatusHeader(message);
    		if (shdr != null) {
    			status = shdr + "/" + status;
    		}
    		logMessage( encoded , from, to,  sender,
    				callId, firstLine,status,tid,time);
    	}
    }
    /** Log a message into the log directory.
     * @param message a Message to log
     * @param from from header of the message to log into the log directory
     * @param to to header of the message to log into the log directory
     * @param status the status to log. This is appended to any NISTExtension
     *	  header present in the message.
     * @param sender is the server the sender or receiver (true if sender).
     * @param time is the reception time.
     */
    public static void logMessage(Message message, String from,
    String to, String status,
    boolean sender, long time) {
    	
    	if (needsLogging()) {

    		checkLogFile();
    		CallIdHeader cid = (CallIdHeader) message.getCallId();
    		String callId = null;
    		if (cid != null) callId = cid.getCallId();
    		String firstLine = message.getFirstLine().trim();
    		String encoded = message.encode();
    		String tid = message.getTransactionId();
    		String shdr = getStatusHeader(message);
    		if (shdr != null) {
    			status = shdr + "/" + status;
    		}
    		logMessage( encoded , from, to,  sender,
    				callId, firstLine,status,tid,time);
    	}
    }
    
    /** Log a message into the log directory. Time stamp associated with the
     * message is the current time.
     * @param message a Message to log
     * @param from from header of the message to log into the log directory
     * @param to to header of the message to log into the log directory
     * @param status the status to log.
     * @param sender is the server the sender or receiver (true if sender).
     */
    public static void logMessage(Message message, String from,
    String to, String status,
    boolean sender) {
    	if (needsLogging()) {
    		logMessage(message,from,to,status,sender,
    				System.currentTimeMillis());
    	}
    }
    
    
    
    /** Log an exception stack trace.
     * @param ex Exception to log into the log file
     */
    
    public static void logException(Exception ex) {
        if (traceLevel >= TRACE_EXCEPTION) {
            checkLogFile();
            if (printWriter != null) ex.printStackTrace();
        }
    }
    
    
    
    
    /** Set the trace level for the stack. Use only for
     * development and debugging, as this can degrade the performance A LOT,
     * specially for big messages (e.g. NOTIFYs). 
     *
     *@param level -- the trace level to set. The following trace levels are
     *supported:
     *<ul>
     *<li>
     *0 -- no tracing
     *</li>
     *
     *<li>
     *16 -- trace messages only
     *</li>
     *
     *<li>
     *32 Full tracing including debug messages.
     *</li>
     *
     *</ul>
     */
    public static void setTraceLevel(int level) {
        traceLevel = level;
    }
    
    /** Get the trace level for the stack.
     *
     *@return the trace level
     */
    public static int getTraceLevel() { return traceLevel; }
    
    
    /** Set aux information. Auxiliary information may be associated
     *with the log file. This is useful for remote logs.
     *
     *@param auxInfo -- auxiliary information.
     *
     */
    public static void setAuxInfo(String auxInfo) {
        ServerLog.auxInfo = auxInfo;
    }
    
    /** Set the descriptive String for the log.
     *
     *@param desc is the descriptive string.
     */
    public static void setDescription(String desc) {
		description = desc;
    }
    
    
    
}
