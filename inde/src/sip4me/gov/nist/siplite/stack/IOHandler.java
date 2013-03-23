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
/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 *******************************************************************************/
package sip4me.gov.nist.siplite.stack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.SocketConnection;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.net.SocketException;



/**
 * Low level Input output to a socket. Caches TCP connections and takes care of
 * re-connecting to the remote party if the other end drops the connection
 * 
 * @version 1.2
 * 
 * @author M. Ranganathan <br/>
 * 
 * 
 */

class IOHandler {

	private final SIPMessageStack sipStack;

	private static String TCP = "tcp";
	

	/**
	 * A cache of client sockets that can be re-used for
	 * sending tcp messages.
	 * <br />
	 *  (String, SocketConnection) : (key,socket) pair
	 */
	private final Hashtable socketTable;
	
	/**
	 * As in MIDP we don't have socket.getOutputStream() method,
	 * we need to open them once and keep them for future use. 
	 * 
	 * (SocketConnection, OutputStream)
	 */
	private final Hashtable outputStreamsTable;
	
	/**
	 * As in MIDP we don't have socket.getInputStream() method,
	 * we need to open them once and keep them for future use. 
	 * 
	 * (SocketConnection, InputStream)
	 */
	private final Hashtable inputStreamsTable;

	/**
	 * Improves socket handling by being able to match targets specified with
	 * FQDN or IPs. This way, an outbound proxy can be specified by a FQDN and
	 * the IOHandler won't be confused when the socket is cached using the IP
	 * instead.
	 * (String,String) equivalent keys (host:port)
	 */
	private final Hashtable dnsResolution;

	private String makeKey(String knownAddress, String socketAddress, int port) {
		String socketKey = makeKey(socketAddress, port);
		String knownKey = makeKey(knownAddress, port);
		if (!socketKey.equals(knownKey)) {
			// Target mismatch! socket.getAddress() returns an unexpected value (e.g. FQDN vs IP)
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_MESSAGES,
						"Target address " + knownAddress + " does not match socket address " + socketAddress + ". Indexing this pair.");
			dnsResolution.put(knownKey, socketKey);
		}
		return socketKey;
	}
	
	protected String makeKey(String addr, int port) {
		if (addr == null)
			addr = "null";
		String alternative = sipStack.getNetworkLayer().getDNSresolution(addr); 
		if (alternative != null)
			addr = alternative;
		return addr + ":" + port;
	}

	protected IOHandler(SIPMessageStack messageStack) {
		this.sipStack = messageStack;
		this.socketTable = new Hashtable(5);
		outputStreamsTable = new Hashtable(5);
		inputStreamsTable = new Hashtable(5);
		dnsResolution = new Hashtable(3);
	}

	protected void putSocket(String key, SocketConnection sock, OutputStream os, InputStream is) {
		
		if (key == null || sock == null) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG,
						"IOHandler NOT saving socket and IO streams because some value is null.\nKey: "
								+ key + " socket " + sock + " IS " + is
								+ " OS " + os);
			return;
		}
		
		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "IOHandler saving socket and IO streams. Key: " + key + " socket " + sock + " IS " + is + " OS " + os);
		if (socketTable.get(key) != null) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "There is already a socket: " + sock + "with key: " + key + ". Closing it.");
//			System.out.println("There is already a socket: " + sock + "with key: " + key + ". Closing it.");
			removeAndCloseSocket(key);
		}
		socketTable.put(key, sock);
		inputStreamsTable.put(sock, is);
		outputStreamsTable.put(sock, os);
//		System.out.println("Socket table: " + socketTable + " " + socketTable.size());
	}

	protected SocketConnection getSocket(String key) {
		if (socketTable.containsKey(key))
			return (SocketConnection) socketTable.get(key);
		else if (dnsResolution.get(key) != null)
			return (SocketConnection) socketTable.get(dnsResolution.get(key));
		else
			return null;
	}
	
	public OutputStream getSocketOutputStream(SocketConnection socket) {
		return (OutputStream) outputStreamsTable.get(socket);
	}

	public InputStream getSocketInputStream(SocketConnection socket) {
		return (InputStream) inputStreamsTable.get(socket);
	}

	/**
	 * Safely close a socket and its I/O Streams
	 * 
	 * @param sock
	 * @param is
	 * @param os
	 */
	protected void disposeSocket(SocketConnection sock, InputStream is, OutputStream os) {
		
		if (sock == null)
			return;
		
		try {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG, "IOHandler disposing socket and associated I/O streams: " + sock.getAddress() + " " + sock.getPort());
//			System.out.println("IOHandler disposing socket and associated I/O streams: " + sock.getAddress() + " " + sock.getPort());
			if (is != null)
				is.close();
			is = null;
		} catch (IOException e) {
		}
		try {
			if (os != null)
				os.close();
			os = null;
		} catch (IOException e) {
		}
		try {
			sock.close();
			sock = null;
		} catch (IOException e) {
		}
	}
	
	
	/**
	 * Remove the socket from the cached list; also close it and the associated
	 * in/out streams
	 * 
	 * @param key
	 */
	protected void removeAndCloseSocket(String key) {
		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "IOHandler closing/removing socket and I/O streams with key: " + key);
		SocketConnection sock = (SocketConnection) socketTable.remove(key);
		if (sock == null) {
			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Socket was already null for key: " + key);
			return;
		}
		InputStream is = (InputStream) inputStreamsTable.remove(sock);
		OutputStream os = (OutputStream) outputStreamsTable.remove(sock);
		
		// We could also check for a match in dnsResolution table
		// and remove it, but we may need to enumerate and check all entries in
		// the table so we don't do it (it's harmless anyway)
		
		disposeSocket(sock, is, os);
		// Do this after closing the socket, as the phone will throw exception
		// on acceptAndOpen() when the maximum number of threads has been reached
		if (sipStack.maxConnections != -1) {
			TCPMessageProcessor tcpProc = (TCPMessageProcessor) sipStack.getMessageProcessor(TCP);
			if (tcpProc == null)
				return;
			synchronized (tcpProc) {
				tcpProc.decreaseNumConnections();
				tcpProc.notify();
			}
		}
	}

	/**
	 * A private function to write things out. This needs to be synchronized as
	 * writes can occur from multiple threads. We write in chunks to allow the
	 * other side to synchronize for large sized writes.
	 */
	private void writeChunks(OutputStream outputStream, byte[] bytes, int length)
			throws IOException {

		synchronized (outputStream) {
			int chunksize = 512;
			for (int p = 0; p < length; p += chunksize) {
				int chunk = p + chunksize < length ? chunksize : length - p;
				outputStream.write(bytes, p, chunk);
			}
			outputStream.flush();
		}
		bytes = null;
	}

	/**
	 * Send an array of bytes.
	 * 
	 * @param receiverAddress --
	 *            inet address
	 * @param receiverPort --
	 *            port to connect to.
	 * @param transport --
	 *            tcp or udp.
	 * @param retry --
	 *            retry to connect if the other end closed connection
	 * @throws IOException --
	 *             if there is an IO exception sending message.
	 */

	public SocketConnection sendBytes(String receiverAddress, int receiverPort, String transport,
			byte[] bytes, boolean retry) throws IOException {
		int retry_count = 0;
		int max_retry = retry ? 2 : 1;
		int length = bytes.length;
		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_DEBUG, "sendBytes START" + transport + " rcvrAddr "
					+ receiverAddress + " port = "
					+ receiverPort + " length = " + length);

		if (transport.equalsIgnoreCase(TCP)) {
			String key = makeKey(receiverAddress, receiverPort);

			SocketConnection clientSock = null;


			clientSock = getSocket(key);

			while (retry_count < max_retry) {
				if (clientSock == null) {
					if (LogWriter.needsLogging)
						LogWriter.logMessage(LogWriter.TRACE_DEBUG, "No socket cached for rcvraddr = "
								+ receiverAddress + " port = " + receiverPort);

					// Check if we have enough sockets and wait otherwise
					TCPMessageProcessor tcpProc = (TCPMessageProcessor) sipStack.getMessageProcessor(TCP);
					if (tcpProc == null)
						return null;
					
					synchronized (tcpProc) {
						while (sipStack.maxConnections != -1
								&& tcpProc.getNumConnections() >= sipStack.maxConnections) {
							try {
								if (LogWriter.needsLogging)
									LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Not enough sockets available. Waiting...");
								System.out.println("Not enough sockets available. Waiting...");
								tcpProc.wait();
								if (!sipStack.isAlive())
									return null;
							} catch (InterruptedException ex) {
								break;
							}
						}
						tcpProc.increaseNumConnections();
					}

					try {
						if (LogWriter.needsLogging)
							LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Creating socket for rcvraddr = "
									+ receiverAddress + " port = " + receiverPort);
						clientSock = sipStack.getNetworkLayer().createSocket(receiverAddress, receiverPort);
						key = makeKey(receiverAddress, clientSock.getAddress(), clientSock.getPort()); // check for address divergence (FQDN vs IP maybe)
					} catch (SocketException e) {
						throw new IOException(e.getClass() + " ::: " + e.getMessage());
					}

					OutputStream outputStream = clientSock.openOutputStream();
					InputStream inputStream = clientSock.openInputStream();
					putSocket(key, clientSock, outputStream, inputStream);
					writeChunks(outputStream, bytes, length);
					break;
				} else {
					try {
						OutputStream outputStream = (OutputStream) this.outputStreamsTable.get(clientSock);
						writeChunks(outputStream, bytes, length);
						break;
					} catch (IOException ex) {
						if (LogWriter.needsLogging)
							LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "IOException occured writing to socket. RetryCount " + retry_count + " max " + max_retry);
						// old connection is bad.
						// remove from our table.
						removeAndCloseSocket(key);
						clientSock = null;
						retry_count++;
					}
				}
			}


			if (LogWriter.needsLogging)
				LogWriter.logMessage(LogWriter.TRACE_DEBUG, "sendBytes END" + transport + " rcvrAddr "
						+ receiverAddress + " port = "
						+ receiverPort + " length = " + length);

			if (clientSock == null) {
				throw new IOException("Could not connect to " + receiverAddress
						+ ":" + receiverPort);
			} else
				return clientSock;
		} 
		else {
			// This is a UDP transport...
			DatagramConnection datagramSock;
			try {
				datagramSock = sipStack.getNetworkLayer().createDatagramSocket(receiverAddress, receiverPort);
				Datagram dgPacket = datagramSock.newDatagram(bytes, length);
				datagramSock.send(dgPacket);
				datagramSock.close();
				return null;
			} catch (SocketException e) {
				throw new IOException(e.getClass() + " ::: " + e.getMessage());

			}

		}
	}

	/**
	 * Close all the cached connections.
	 */
	public void closeAll() {
		if (LogWriter.needsLogging)
			LogWriter.logMessage("IOHandler closing all connections!");

		for (Enumeration keys = socketTable.keys(); keys
				.hasMoreElements();) {
			String key = (String) keys.nextElement();
			removeAndCloseSocket(key);	
		}
		
		dnsResolution.clear();

	}

}
