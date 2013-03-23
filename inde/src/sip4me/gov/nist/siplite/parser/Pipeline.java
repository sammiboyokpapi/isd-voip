/*
 * Conditions Of Use 
 * 
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 * 
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 * 
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *  
 * .
 * 
 */
package sip4me.gov.nist.siplite.parser;


import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.siplite.stack.SIPStackTimerTask;


/**
 * Input class for the pipelined parser. Buffer all bytes read from the socket
 * and make them available to the message parser.
 * 
 * @author M. Ranganathan (Contains a bug fix contributed by Rob Daugherty (
 *         Lucent Technologies) )
 * @deprecated gave many problems in sip4me, including duplicated bytes when read()-ing
 * from the Pipeline, and also with the timers. As in MIDP it is not possible to read in
 * chunks from an InputStream, it is safer to give the IS to the Parser directly.  
 */

public class Pipeline extends InputStream {
	private Vector buffList;

	private Buffer currentBuffer;

	private boolean isClosed;

	private Timer timer;

	private InputStream pipe;

	private int readTimeout;

	private TimerTask myTimerTask;

	private StringBuffer totalWriteBuffer;

	private StringBuffer totalReadBuffer;

	class MyTimer extends SIPStackTimerTask {
		Pipeline pipeline;

		private boolean isCancelled;

		protected MyTimer(Pipeline pipeline) {
			this.pipeline = pipeline;
		}

		protected void runTask() {
			if (this.isCancelled)
				return;
			
			try {
				System.out.println("Closing pipeline because of timeout");
				pipeline.close();
			} catch (IOException ex) {
				InternalErrorHandler.handleException(ex);
			}
		}

		public boolean cancel() {
			boolean retval = super.cancel();
			this.isCancelled = true;
			return retval;
		}

	}

	class Buffer {
		byte[] bytes;

		int length;

		int ptr;

		public Buffer(byte[] bytes, int length) {
			ptr = 0;
			this.length = length;
			this.bytes = bytes;
		}

		public int getNextByte() {
			int retval = bytes[ptr++] & 0xFF;
			return retval;
		}

	}

	public void startTimer() {
		if (this.readTimeout == -1)
			return;
		this.myTimerTask = new MyTimer(this);
		this.timer.schedule(this.myTimerTask, this.readTimeout);
	}

	public void stopTimer() {
		if (this.readTimeout == -1)
			return;
		if (this.myTimerTask != null)
			this.myTimerTask.cancel();
	}

	public Pipeline(InputStream pipe, int readTimeout, Timer timer) {
		// pipe is the Socket stream
		// this is recorded here to implement a timeout.
		this.timer = timer;
		this.pipe = pipe;
		buffList = new Vector();
		this.readTimeout = readTimeout;
		totalWriteBuffer = new StringBuffer();
		totalReadBuffer = new StringBuffer();
	}
	

	public synchronized void write(byte[] bytes, int start, int length) throws IOException {
		if (this.isClosed)
			throw new IOException("Closed!!");
		Buffer buff = new Buffer(bytes, length);
		buff.ptr = start;
		synchronized (this.buffList) {
			buffList.addElement(buff);
			buffList.notifyAll();
		}
	}

	public synchronized void write(byte[] bytes) throws IOException {
		if (this.isClosed)
			throw new IOException("Closed!!");
		synchronized (this.buffList) {
			Buffer buff = new Buffer(bytes, bytes.length);
			buffList.addElement(buff);
//			System.out.println("Added bufer with value: " + new String(buff.bytes));
			totalWriteBuffer.append(new String(buff.bytes));
			buffList.notifyAll();
		}
	}

	public void close() throws IOException {
		if (this.isClosed)
			return;
		
		this.isClosed = true;
		synchronized (this.buffList) {
			this.buffList.notifyAll();
		}
		
		// JvB: added
		this.pipe.close();
	}

	public int read() throws IOException {
		synchronized (this.buffList) {
			if (currentBuffer != null
					&& currentBuffer.ptr < currentBuffer.length) {
				int retval = currentBuffer.getNextByte();
				totalReadBuffer.append((char) retval);
				if (currentBuffer.ptr == currentBuffer.length)
					this.currentBuffer = null;
				return retval;
			}
			// Bug fix contributed by Rob Daugherty.
			if (this.isClosed && this.buffList.isEmpty()) {
				System.out.println("returning -1 bc pipeline is closed. \n" +
						"TotalWrittenBuffer: " + totalWriteBuffer.toString() +
						"*********************************************" + 
						"TotalreadBuffer: " + totalReadBuffer.toString());
				return -1;
			}
			try {
				// wait till something is posted.
				while (this.buffList.isEmpty()) {
					this.buffList.wait();
					if (this.isClosed) {
						return -1;
					}
				}
				currentBuffer = (Buffer) this.buffList.firstElement();
				this.buffList.removeElementAt(0);
				int retval = currentBuffer.getNextByte();
				totalReadBuffer.append((char) retval);
				if (currentBuffer.ptr == currentBuffer.length)
					this.currentBuffer = null;
				return retval;
			} catch (InterruptedException ex) {
				throw new IOException(ex.getMessage());
			} catch (NoSuchElementException ex) {
				throw new IOException(ex.getMessage());
			}
		}
	}

}
