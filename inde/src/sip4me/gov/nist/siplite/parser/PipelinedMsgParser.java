/******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD)       *
 ******************************************************************************/
package sip4me.gov.nist.siplite.parser;


import java.io.IOException;
import java.io.InputStream;

import sip4me.gov.nist.core.Debug;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.ContentLengthHeader;
import sip4me.gov.nist.siplite.message.Message;
import sip4me.gov.nist.siplite.stack.TCPMessageChannel;


/* Lamine Brahimi and Yann Duponchel (IBM Zurich) noticed that the parser was
* blocking so I threw out some cool pipelining which ran fast but only worked
* when the phase of the full moon matched its mood. Now things are serialized
* and life goes slower but more reliably.
*/
/**
* This implements a pipelined message parser suitable for use
* with a stream - oriented input such as TCP. The client uses
* this class by instantiating with an input stream from which
* input is read and fed to a message parser.
* It keeps reading from the input stream and process messages in a
* never ending interpreter loop. The message listener interface gets called
* for processing messages or for processing errors. The payload specified
* by the content-length header is read directly from the input stream.
* This can be accessed from the Message using the getContent and
* getContentBytes methods provided by the Message class. 
*
*@author <A href=mailto:mranga@nist.gov > M. Ranganathan  </A>
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*

 * 
 * @version 1.2 $Revision: 1.10 $ $Date: 2010-09-15 14:12:49 $
 * 
 *@author <A href=mailto:mranga@nist.gov > M. Ranganathan  </A>
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 * 
 * @see SIPMessageListener
*/

public final class PipelinedMsgParser implements Runnable {


	/** The message listener that is registered with this parser.
	 * (The message listener has methods that can process correct
	 * and erroneous messages.)
	 */
	protected SIPMessageListener sipMessageListener;
	private Thread mythread; // Preprocessor thread
	private InputStream rawInputStream;
	private int maxMessageSize;
	private int sizeCounter;
	private TCPMessageChannel messageChannel;
	
	private static int uid = 0;

	private static synchronized int getNewUid() {
		return uid++;
	}
	
	/**
	 * default constructor.
	 */
	protected PipelinedMsgParser() {
		super();
	}


	/** Constructor when we are given a message listener and an input stream
	 * (could be a TCP connection or a file)
	 * @param sipMessageListener Message listener which has 
	 * methods that  get called
	 * back from the parser when a parse is complete
	 * @param in Input stream from which to read the input.
	 * @param debug Enable/disable tracing or lexical analyser switch.
	 */
	public PipelinedMsgParser(SIPMessageListener sipMessageListener,
			InputStream in, boolean debug, int maxMessageSize) {
		this();
		this.sipMessageListener = sipMessageListener;
		rawInputStream = in;
		this.maxMessageSize = maxMessageSize;
		mythread = new Thread(this,"PipelineThread-" + getNewUid());

	}

	/**
	 * This is the constructor for the pipelined parser.
	 * @param mhandler a MessageListener implementation that
	 *	provides the message handlers to
	 * 	handle correctly and incorrectly parsed messages.
	 * @param in A Pipeline to read messages from.
	 * @param maxMsgSize maximum message size
	 */

	public PipelinedMsgParser(SIPMessageListener mhandler, InputStream in,
			int maxMsgSize) {
		this(mhandler, in, false, maxMsgSize);
	}

	/**
	 * This is the constructor for the pipelined parser.
	 * @param in - A Pipeline to read messages from.
	 */

	public PipelinedMsgParser(InputStream in) {
		this(null, in, false, 0);
	}

	/**
	 * Start reading and processing input.
	 */
	public void processInput() {
		mythread.start();
	}

	/**
	 * Create a new pipelined parser from an existing one.
	 * 
	 * @return A new pipelined parser that reads from the same input stream.
	 * @deprecated Reading simultaneously from the same IS can only mean
	 *             trouble. I recommend not to use this unless you really know
	 *             what you are doing.
	 */
	protected Object clone() {
		PipelinedMsgParser p = new PipelinedMsgParser();

		p.rawInputStream = this.rawInputStream;
		p.sipMessageListener = this.sipMessageListener;
		Thread mythread = new Thread(p, "PipelineThread-" + getNewUid());
		return p;
	}


	/**
	 * Add a class that implements a MessageListener interface whose
	 * methods get called on successful parse and error conditions.
	 * @param mlistener a MessageListener
	 *	implementation that can react to correct and incorrect
	 * 	pars.
	 */

	public void setMessageListener(SIPMessageListener mlistener) {
		sipMessageListener = mlistener;
	}


	/** 
	 * read a line of input (I cannot use buffered reader because we
	 *may need to switch encodings mid-stream!
	 */
	private String readLine() throws IOException {
		
		StringBuffer retval = new StringBuffer("");
		while (true) {
			char ch;
			if (rawInputStream == null)
				throw new IOException("End of stream, IS null");
			
			int i = rawInputStream.read();
			if (i == -1) {
				throw new IOException("End of stream");
			} else
				ch = (char) i;
			
			// reduce the available read size by 1 ("size" of a char).
			if (this.maxMessageSize > 0) {
				this.sizeCounter--;
				if (this.sizeCounter <= 0)
					throw new IOException("Max size exceeded!");
			}
			if (ch != '\r') {
				retval.append(ch);
			}
			if (ch == '\n') {
				break;
			}
		}
		return retval.toString();
	}


	/**
	 * This is input reading thread for the pipelined parser.
	 * You feed it input through the input stream (see the constructor)
	 * and it calls back an event listener interface for message 
	 * processing or error.
	 * It cleans up the input - dealing with things like line continuation 
	 *
	 */
	public void run() {
		
		try {
			while (messageChannel != null) {
				this.sizeCounter = this.maxMessageSize;
				StringBuffer inputBuffer = new StringBuffer();

				if (Debug.parserDebug)
					Debug.println("Starting parse!");

				String line1;
				String line2 = null;

				while (true) {
					try {
						if (rawInputStream == null)
							throw new IOException("InputStream null at parser " + this);
						
						line1 = readLine();
						// ignore blank lines.
						if (line1.equals("\n")) {
							if (Debug.parserDebug)
								Debug.println("Discarding " + line1);
							continue;
						} else
							break;
					} catch (IOException ex) {
						Debug.printStackTrace("Problem parsing or end of stream!", ex);
						return;

					}
				}
				if (LogWriter.needsLogging)
					LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Parsing message in parser: " + this + " from IS: " + rawInputStream + " of message channel " + messageChannel + "\n" +
							"First line of incoming message: " + line1);
				inputBuffer.append(line1);
				while (true) {
					try {
						if (rawInputStream == null)
							throw new IOException("InputStream null at parser " + this);
						
						line2 = readLine();
						inputBuffer.append(line2);
						if (line2.trim().equals("")) {
							break;
						}
					} catch (IOException ex) {
						Debug.printStackTrace("Problem parsing or end of stream!", ex);
						return;

					}
				}
				inputBuffer.append(line2);
				StringMsgParser smp = new StringMsgParser(sipMessageListener);
				smp.readBody = false;
				Message sipMessage = null;

				try {
					sipMessage = smp.parseSIPMessage(inputBuffer.toString());
					if (sipMessage == null) {
						if (LogWriter.needsLogging)
							LogWriter.logMessage("Discarding null sipMessage after parse");
						continue;
					}
				} catch (ParseException ex) {
					if (LogWriter.needsLogging) {
						LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "Exception parsing incoming SIP Message: \n");
						LogWriter.logException(ex);
					}
					// FIXME: this is wrong, IMHO,
					// as someone should be notified of this exception
					// and reply a 400/500. For this, a handleException method
					// should be added to SIPMessageListener
					continue;
				}

				if (Debug.parserDebug)
					Debug.println("Completed parsing message");
				ContentLengthHeader clhdr = sipMessage.getContentLengthHeader();
				int contentLength = 0;
				if (clhdr != null) {
					contentLength = clhdr.getContentLength();
				} else {
					contentLength = 0;
				}

				if (Debug.parserDebug) {
					Debug.println("contentLength " + contentLength);
					Debug.println("sizeCounter " + this.sizeCounter);
					Debug.println("maxMessageSize " + this.maxMessageSize);
				}

				// Try to read the body of the SIP Message
				if (contentLength == 0) {
					sipMessage.removeContent();
				} else if (maxMessageSize == 0
						|| contentLength < this.sizeCounter) {
					byte[] message_body = new byte[contentLength];
					int nread = 0;
					while (nread < contentLength) {
						try {
							int readlength = rawInputStream.read(message_body,
									nread, contentLength - nread);
							if (readlength > 0) {
								nread += readlength;
							} else {
								if (LogWriter.needsLogging)
									LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
										"PipelinedMsgParser: Warning! We didn't get any more bytes reading from InputStream, " +
										"but it seems we didn't get as many bytes as Content-Length: " + nread);
								break;
							}
						} catch (IOException ex) {
							if (LogWriter.needsLogging)
								LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "IOException while reading message payload from socket. " +
										"Number of bytes read: " + nread + " and Content-Length: " + contentLength + ": " + ex.getMessage());
							break;
						} finally {
						}
					}
					sipMessage.setMessageContent(message_body);
				} else {
					// Content length too large - process the message and
					// return error from there.
				}
				
				if (sipMessageListener != null) {
					try {
						if (LogWriter.needsLogging)
							LogWriter.logMessage("Passing sipMessage to listener (channel)");
						sipMessageListener.processMessage(sipMessage);
					} catch (Exception ex) {
						if (LogWriter.needsLogging) {
							LogWriter.logMessage(LogWriter.TRACE_EXCEPTION, "Exception processing SIP Message: \n");
							LogWriter.logException(ex);
						}
						// fatal error in processing - close the
						// connection.
						break;
					}
				} else {
					if (LogWriter.needsLogging)
						LogWriter.logMessage("No SipListener associated to PipeLinedMessageParser!");
				}
			}
		} finally {
			close();
		}
	
	}
	
	public void close() {
		// this will close the socket and IS
		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Closing parser " + this + " channel " + messageChannel + " IS " + rawInputStream);
		
		if (messageChannel != null) {
			messageChannel.closeFromParser(); 
		}
		// Always close the associated IS just in case
		// (problem with ghost parser threads hanging around)
		try {
			if (rawInputStream != null)
				rawInputStream.close();
			rawInputStream = null;
		} catch (Exception ex) {
		}
	}

	/**
	 * Useful for closing the channel when EOF is reached.
	 * @param messageChannel
	 */
	public void setMessageChannel(TCPMessageChannel messageChannel) {
		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Associated parser " + this + " with channel " + messageChannel);
		this.messageChannel = messageChannel;
	}
	
	
//	public static void main(String[] args) {
//		System.out.println("Testing PipelinedMessageParser");
//		String msg = "put some SIP Message here";
//
//		ByteArrayInputStream bais = new ByteArrayInputStream(msg.getBytes());
//		PipelinedMsgParser parser = new PipelinedMsgParser(null, bais, Integer.MAX_VALUE);
//		parser.processInput();
//	
//	}
	
}
