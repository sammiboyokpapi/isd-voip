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

import javax.microedition.io.DatagramConnection;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;


/**
 * basic interface to the network layer
 *
 * @author m.andrews
 *
 */
public interface NetworkLayer {

    /**
     * Creates a server socket that listens on the specified port. 
     * 
     * @param port
     * @return the server socket
     * @throws SocketException
     */
    public ServerSocketConnection createServerSocket(int port) throws SocketException;

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
    public SocketConnection createSocket(String targetAddress, int targetPort) throws SocketException;

    
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
    public DatagramConnection createDatagramSocket(String targetAddress, int targetPort) throws SocketException;

	/**
	 * Constructs a datagram socket to receive packets on the local machine, and
	 * binds it to any available port on the local host machine.
	 * 
	 * @param targetPort
	 *            port where the socket will listen
	 * @return the opened datagram socket
	 * @throws SocketException
	 */
    public DatagramConnection createDatagramInboundSocket(int targetPort) throws SocketException;

	/**
	 * Allows the network layer to have an alternate DNS resolution mechanism,
	 * be it a fixed table or some dynamic method like communication with an
	 * online service. A mock method could always return the given parameter.
	 * 
	 * @param domainOrIP
	 *            typically, a FQDN to which a suitable IP cannot be found.
	 * @return The string containing the IP or domain name that maps to a given
	 *         FQDN or IP, in each case. It could also be the given parameter,
	 *         unchanged, if no mechanism is implemented.
	 */
    public String getDNSresolution(String domainOrIP);

}
