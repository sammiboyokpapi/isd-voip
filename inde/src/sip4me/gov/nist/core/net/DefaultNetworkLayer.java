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
package sip4me.gov.nist.core.net;

import java.io.IOException;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;

import sip4me.gov.nist.core.LogWriter;


/**
 * basic interface to the Generic Connection Framework
 *
 * @author ArnauVP (Genaker)
 *
 */
public class DefaultNetworkLayer implements NetworkLayer {

	
	public static final DefaultNetworkLayer SINGLETON = new DefaultNetworkLayer();

	public DefaultNetworkLayer() {
		super();
	}
	
    /**
     * Creates a server socket that listens on the specified port. 
     * 
     * @param port
     * @return the server socket
     * @throws SocketException
     */
    public ServerSocketConnection createServerSocket(int port) throws SocketException {
    	String location = "socket://:" + port;
    	try {
			return (ServerSocketConnection) Connector.open(location);
		} catch (SecurityException se) {
			throw new SocketException(se.getMessage());
		} catch (IllegalArgumentException iae) {
			throw new SocketException(iae.getMessage());
		} catch (ConnectionNotFoundException cnfe) {
			throw new SocketException(cnfe.getMessage());
		} catch (IOException ioe) {
			throw new SocketException(ioe.getMessage());
		}
    }

    /**
     * Creates an outbound stream socket and connects it to the specified port number at the specified IP address.
     * 
     * @param targetAddress    
     * 		address or hostname of remote target
     * @param targetPort
     *  	port of the remote target
     * @return the socket
     * @throws SocketException
     */
    public SocketConnection createSocket(String targetAddress, int targetPort) throws SocketException {
    	String location = "socket://" + targetAddress + ":" + targetPort;
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Opening TCP socket to " + location);
    	try {
			return (SocketConnection) Connector.open(location);
		} catch (SecurityException se) {
			throw new SocketException(se.getMessage());
		} catch (IllegalArgumentException iae) {
			throw new SocketException(iae.getMessage());
		} catch (ConnectionNotFoundException cnfe) {
			throw new SocketException(cnfe.getMessage());
		} catch (IOException ioe) {
			throw new SocketException(ioe.getMessage());
		}
    }

    
    /**
	 * Constructs a datagram socket to send packets to a server, and binds it to
	 * any available port on the local host machine.
	 * 
	 * 
	 * @param targetAdress
	 *            address or hostname of remote target
	 * @param targetPort
	 *            port of the remote target
	 * @return the opened datagram socket for sending
	 * @throws SocketException
	 */
    public DatagramConnection createDatagramSocket(String targetAddress, int targetPort) throws SocketException {
    	String location = "datagram://" + targetAddress + ":" + targetPort;
    	if (LogWriter.needsLogging)
    		LogWriter.logMessage(LogWriter.TRACE_DEBUG, "Opening UDP socket to " + location);
    	try {
			return (DatagramConnection) Connector.open(location);
		} catch (SecurityException se) {
			throw new SocketException(se.getMessage());
		} catch (IllegalArgumentException iae) {
			throw new SocketException(iae.getMessage());
		} catch (ConnectionNotFoundException cnfe) {
			throw new SocketException(cnfe.getMessage());
		} catch (IOException ioe) {
			throw new SocketException(ioe.getMessage());
		}
    }

	/**
	 * Constructs a datagram socket to receive packets on the local machine, and
	 * binds it to any available port on the local host machine.
	 * 
	 * @param targetPort
	 *            port where the socket will listen
	 * @return the opened datagram socket
	 * @throws SocketException
	 */
    public DatagramConnection createDatagramInboundSocket(int targetPort) throws SocketException {
    	String location = "datagram://:" + targetPort;
    	try {
			return (DatagramConnection) Connector.open(location);
		} catch (SecurityException se) {
			throw new SocketException(se.getMessage());
		} catch (IllegalArgumentException iae) {
			throw new SocketException(iae.getMessage());
		} catch (ConnectionNotFoundException cnfe) {
			throw new SocketException(cnfe.getMessage());
		} catch (IOException ioe) {
			throw new SocketException(ioe.getMessage());
		}
    }

	public String getDNSresolution(String domainOrIP) {
		return domainOrIP;
	}



}
