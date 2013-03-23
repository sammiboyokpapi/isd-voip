/*
 * Protocol.java
 * 
 * Created on Feb 2, 2004
 *
 */
package sip4me.gov.nist.microedition.io.j2me;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.StringTokenizer;
import sip4me.gov.nist.microedition.io.j2me.sip.DistributedRandom;
import sip4me.gov.nist.microedition.sip.ConnectionBaseInterface;
import sip4me.gov.nist.microedition.sip.StackConnector;

/**
 * Class implementing the Sip Protocol for J2ME using the generic Connection
 * framework
 * 
 * @author Jean Deruelle, ArnauVP
 * 
 *         <a href="{@docRoot} /uncopyright.html">This code is in the public
 *         domain.</a>
 */
public abstract class Protocol implements ConnectionBaseInterface {

	protected StackConnector stackConnector;

	/**
	 * 
	 */
	public Protocol() {

	}

	/**
	 * Open a client or server socket connection.
	 * <p>
	 * The name string for this protocol should be:
	 * {scheme}:[{target}][{params}] where: scheme is SIP scheme supported by
	 * the system sip or sips �target is user network address in form of
	 * {user_name}@{target_host}[:{port}] or {telephone_number}, see examples
	 * below. params stands for additional SIP URI parameters like
	 * ;transport=udp <B>Opening new client connection
	 * (SipClientConnection):</B> If the target host is included a,
	 * SipClientConnection will be returned. Examples: �
	 * sip:bob@biloxi.com�sip:alice@10.128.0.8:5060�
	 * sips:alice@company.com:5060;transport=tcp�
	 * sip:+358-555-1234567;postd=pp22@foo.com;user=phone� <B>Opening new server
	 * connection (SipConnectionNotifier):</B> Three forms of SIP URIs are
	 * allowed where the target host is omitted: �sip:nnnn, returns
	 * SipConnectionNotifier listening to incoming SIP requests on port number
	 * nnnn. �sip:, returns SipConnectionNotifier listening to incoming SIP
	 * requests on a port number allocated by the system.
	 * �sip:[nnnn];type=�application/x-type�, returns SipConnectionNotifier
	 * listening to incoming SIP requests that match to the MIME type
	 * �application/x-type� (specified in the URI parameter type). Port number
	 * is optional. Examples of opening a SIP connection:
	 * �SipClientConnection�scn
	 * �=�(SipClientConnection)�Connector.open(�sip:bob@biloxi.com�);����
	 * �SipClientConnection�scn�= (SipClientConnection)
	 * Connector.open(�sips:alice@company.com:5060;transport=tcp�);�
	 * SipConnectionNotifier�scn�=
	 * (SipConnectionNotifier)Connector.open(�sip:5060�);
	 * SipConnectionNotifier�scn�=
	 * (SipConnectionNotifier)Connector.open(�sips:5080�);
	 * SipConnectionNotifier�scn�=
	 * (SipConnectionNotifier)Connector.open(�sip:�);
	 * 
	 * @param name
	 *            the [{target}][{params}] for the connection
	 * @param mode
	 *            I/O access mode, never used
	 * @param timeouts
	 *            a flag to indicate that the caller wants timeout exceptions
	 * 
	 * @return client or server SIP connection
	 * 
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @exception ConnectionNotFoundException
	 *                if the host cannot be connected to
	 * @exception IllegalArgumentException
	 *                if the name is malformed
	 */
	public Connection openPrim(String name, int mode, boolean timeout)
			throws IOException, IllegalArgumentException {
		stackConnector = null;

		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Trying to open connection from String: " + name);
		
		int indexOfAt = name.indexOf("@");
		int indexOfSemiColon = name.indexOf(";");
		int indexOfColon = name.indexOf(":");
		int indexOfPlus = name.indexOf("+");

		if (indexOfSemiColon != -1 && indexOfColon > indexOfSemiColon)
			indexOfColon = -1;
		if (indexOfPlus > 0)
			indexOfPlus = -1;
		if (indexOfSemiColon != -1 && indexOfAt > indexOfSemiColon)
			indexOfAt = -1;
		if (indexOfColon == 0)
			throw new IllegalArgumentException("Bad SIP URI: no colon");
		
		/**
		 * Inbound connections. Only valid for 'sip:' and 'sips:' schemes
		 */

		if (this instanceof sip4me.gov.nist.microedition.io.j2me.sip.Protocol) {

			/* CASE S1 */
			// �sip:, returns SipConnectionNotifier listening to incoming SIP
			// requests on a random port number.
			if (name == null || name.length() < 1 || name.equals("*")) {
				openSipConnectionNotifier(
						new DistributedRandom().nextInt(8975) + 1024, false, null);
			}



			/* CASE S2 */
			// �sip:nnnn, returns SipConnectionNotifier listening to incoming SIP
			// requests on the given port number 'nnnn'.

			if (name.length() == 4 && indexOfAt == -1 && indexOfColon == -1
					&& indexOfPlus == -1 && indexOfSemiColon == -1) {
				try {
					int portNumber = Integer.parseInt(name);
					return openSipConnectionNotifier(portNumber, false, null);
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException(
					"the port you have entered for the SipConnectionNotifier seems to be wrong");
				}
			}

			/* CASE S3 */
			// ;parameter=value;parameter=value;....
			if (indexOfSemiColon == 0) {

				Vector parameters = parseAndCheckParameters(name
						.substring(indexOfSemiColon + 1));
				// FIXME: Removed by ArnauVP on 18/03/2009. Any problem?
				//			 parameters.addElement(parametersString);
				return openSipConnectionNotifier(
						new DistributedRandom().nextInt(8975) + 1024, false,
						parameters);
			}

			/* CASE S4 */
			// sip:nnnn;parameter=value;parameter=value;...
			if (indexOfAt == -1 && indexOfColon == -1 && indexOfSemiColon != -1
					&& indexOfPlus == -1) {
				String port = name.substring(0, indexOfSemiColon);
				Vector parameters = parseAndCheckParameters(name
						.substring(indexOfSemiColon + 1));
				// FIXME: Removed by ArnauVP on 18/03/2009. Any problem?
				// parameters.addElement(parametersString);

				if (port.length() == 4) {
					try {
						int portNumber = Integer.parseInt(port);
						return openSipConnectionNotifier(portNumber, false, parameters);
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException(
						"the port you have entered for the SipConnectionNotifier seems to be wrong");
					}
				} else {
					// don't let the flow continue if it is not going to be
					// successful anyway
					throw new IllegalArgumentException(
					"the port you have entered for the SipConnectionNotifier seems to be wrong");
				}

			}

		}

		/**
		 * Outbound connections. Valid for all schemes: 'sip:', 'sips:' and
		 * 'tel:'
		 */
		/* CASE C1 */
		// sip:bob@biloxi.com
		if (indexOfAt != -1 && indexOfColon == -1 && indexOfSemiColon == -1
				&& indexOfPlus == -1) {
			return openSipClientConnection(name.substring(0, indexOfAt), name
					.substring(indexOfAt + 1), -1, false, null);
		}

		/* CASE C2 */
		// sip:+nnnnnnnnnnn@host.com
		// OR sip:+nnnnnnnnnnn OR tel:+nnnnnnnnnnnnn (global TelURL)
		if (indexOfColon == -1 && indexOfSemiColon == -1 && indexOfPlus != -1) {
			String tel = null;
			String host = null;
			if (indexOfAt != -1) {
				tel = name.substring(0, indexOfAt);
				host = name.substring(indexOfAt + 1);
			} else
				tel = name;

			// Fix by ArnauVP (Genaker)
			// Ordinary telephone numbers are bigger than Integer.MAX_VALUE.
			// Use Long instead, just for validation.

			try {
				Long.parseLong(tel.substring(1));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"the Telephone number you have entered"
								+ " seems to contain characters.");
			}

			return openSipClientConnection(tel, host, -1, false, null);
		}

		/* CASE C3 */
		// sip:alice@company.com:nnnn
		if (indexOfAt != -1 && indexOfColon != -1 && indexOfSemiColon == -1
				&& indexOfPlus == -1) {
			String user = name.substring(0, indexOfAt);
			String host = name.substring(indexOfAt + 1, indexOfColon);
			String port = name.substring(indexOfColon + 1);
			int portNumber = -1;
			try {
				portNumber = Integer.parseInt(port);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"the port you have entered for the SipClientConnection seems to be wrong");
			}
			return openSipClientConnection(user, host, portNumber, false, null);
		}

		/* CASE C4 */
		// sip:+nnnnnnnnnnn@localhost.com:nnnn
		// OR sip:+nnnnnnnnnnn:nnnn
		if (indexOfColon != -1 && indexOfSemiColon == -1 && indexOfPlus != -1) {
			String tel = null;
			String host = null;
			String port = null;
			int portNumber = -1;
			if (indexOfAt != -1) {
				tel = name.substring(0, indexOfAt);
				host = name.substring(indexOfAt + 1, indexOfColon);
				port = name.substring(indexOfColon + 1);
			} else {
				tel = name.substring(0, indexOfColon);
				port = name.substring(indexOfColon + 1);
			}
			try {
				portNumber = Integer.parseInt(port);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"the port you have entered for the SipClientConnection seems to be wrong");
			}
			// Fix by ArnauVP (Genaker)
			// Ordinary telephone numbers are bigger than Integer.MAX_VALUE.
			// Use Long instead, just for validation.

			try {
				Long.parseLong(tel.substring(1));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"the Telephone number you have entered"
								+ " seems to contain characters.");
			}
			return openSipClientConnection(tel, host, portNumber, false, null);
		}

		/* CASE C5 */
		// sip:alice@company.com:5060;transport=udp;...
		// OR sip:alice@company.com;transport=udp;...
		if (indexOfAt != -1 && indexOfSemiColon != -1 && indexOfPlus == -1) {
			String user = name.substring(0, indexOfAt);
			String host = null;
			String port = null;

			if (indexOfColon != -1) {
				host = name.substring(indexOfAt + 1, indexOfColon);
				port = name.substring(indexOfColon + 1, indexOfSemiColon);
			} else {
				host = name.substring(indexOfAt + 1, indexOfSemiColon);
				port = "-1";
			}

			Vector parameters = parseAndCheckParameters(name
					.substring(indexOfSemiColon + 1));
			// FIXME: Removed by ArnauVP on 18/03/2009. Any problem?
			// parameters.addElement(parametersString);

			int portNumber = -1;
			try {
				portNumber = Integer.parseInt(port);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"the port you have entered for the SipClientConnection seems to be wrong");
			}

			return openSipClientConnection(user, host, portNumber, false, parameters);
		}

		/* CASE C6 */
		// sip:+nnnnnnnnnnnnn:nnnn;transport=udp;...
		// OR sip:+nnnnnnnnnnnnnn;transport=udp;...
		// OR tel:+nnnnnnnnnnn;paramName=paramValue;...
		// OR sip:+nnnnnnnnnnnnnn@localhost.com;transport=udp;...
		// OR sip:+nnnnnnnnnnnnnn@localhost.com:nnnn;transport=udp;...

		if (indexOfSemiColon != -1 && indexOfPlus != -1) {
			String tel = null;
			String host = null;
			String port = null;

			// sip:+nnnnnnnnnnnnnn@localhost.com;transport=udp;...
			// OR sip:+nnnnnnnnnnnnnn@localhost.com:nnnn;transport=udp;...
			if (indexOfAt != -1) {
				tel = name.substring(0, indexOfAt);
				// sip:+nnnnnnnnnnnnnn@localhost.com:nnnn;transport=udp;..
				if (indexOfColon != -1) {
					host = name.substring(indexOfAt + 1, indexOfColon);
					port = name.substring(indexOfColon + 1, indexOfSemiColon);
				}
				// sip:+nnnnnnnnnnnnnn@localhost.com;transport=udp;...
				else {
					host = name.substring(indexOfAt + 1, indexOfSemiColon);
					port = "-1";
				}
			}

			// sip:+nnnnnnnnnnnnn:nnnn;transport=udp;...
			// OR sip:+nnnnnnnnnnnnnn;transport=udp;...
			// OR tel:+nnnnnnnnnnn;paramName=paramValue;...
			else {
				// sip:+nnnnnnnnnnnnn:nnnn;transport=udp;...
				if (indexOfColon != -1) {
					tel = name.substring(0, indexOfColon);
					port = name.substring(indexOfColon + 1, indexOfSemiColon);
				}
				// sip:+nnnnnnnnnnnnnn;transport=udp;...
				else {
					tel = name.substring(0, indexOfSemiColon);
					port = "-1";
				}
			}
			// Fix by ArnauVP (Genaker)
			// Ordinary telephone numbers are bigger than Integer.MAX_VALUE.
			// Use Long instead, just for validation.

			try {
				Long.parseLong(tel.substring(1));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"the Telephone number you have entered"
								+ " seems to contain characters.");
			}
			Vector parameters = parseAndCheckParameters(name
					.substring(indexOfSemiColon + 1));

			int portNumber = -1;
			try {
				portNumber = Integer.parseInt(port);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"the port you have entered for the SipClientConnection seems to be wrong");
			}
			
			return openSipClientConnection(tel, host, portNumber, false, parameters);
		}

		/*CASE C7*/
		// tel:0234578901;phonecontext=phonecontextvalue
		// OR tel:003412345678;transport=udp
		// OR tel:003412345678 
		if (indexOfAt == -1 && indexOfColon == -1 && indexOfPlus == -1) {
//			 && name.indexOf("phonecontext") != -1
			String tel = null;
			Vector parameters = null;

			if (indexOfSemiColon != -1) {
				tel = name.substring(0, indexOfSemiColon);
				parameters = parseAndCheckParameters(name
						.substring(indexOfSemiColon + 1));
			} else {
				tel = name;
			}

			// Fix by ArnauVP (Genaker)
			// Ordinary telephone numbers are bigger than Integer.MAX_VALUE.
			// Use Long instead, just for validation.

			try {
				Long.parseLong(tel);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"the Telephone number you have entered"
								+ " seems to contain characters.");
			}
	
			return openSipClientConnection(tel, null, -1, false, parameters);
			
		}

		
		throw new ConnectionNotFoundException(
				"Connection could not be created. No pattern matched.");
	}

	/**
	 * Open an inbound connection to listen to incoming SIP requests (e.g. a
	 * SipConnectionNotifier and SipServerConnection). This is only valid for
	 * 'sip' and 'sips' protocols, not for 'tel', which will always throw an
	 * Exception.
	 * 
	 * @param port
	 *            the port to listen to
	 * @param secure
	 *            true if a secure protocol is to be used, false otherwise.
	 *            IGNORED, always insecure (TCP/UDP) right now.
	 * @param params
	 *            a Vector with parameters to open the connection.
	 * @return a SipConnectionNotifier listening on the specified port.
	 * 
	 */
	protected abstract Connection openSipConnectionNotifier(int port,
			boolean secure, Vector params) throws IOException;

	/**
	 * Open an outbound connection to send SIP requests to a UAS.
	 * 
	 * @param user
	 *            the user name of the UAS.
	 * @param host
	 *            the host name or IP address of the remote party.
	 * @param port
	 *            the port number of the remote party.
	 * @param secure
	 *            true if a secure protocol is to be used, false otherwise.
	 *            IGNORED, always insecure (TCP/UDP) right now.
	 * @param params
	 *            the extra parameters to use in the connection.
	 * @return a SipClientConnection bound to the remote host:port and ready to
	 *         send requests.
	 * @throws IOException
	 *             if there is a problem initializing the stack.
	 * @throws IllegalArgumentException
	 *             if there is any problem with the values provided.
	 */
	protected abstract Connection openSipClientConnection(String user,
			String host, int port, boolean secure, Vector params)
			throws IOException, IllegalArgumentException;

	/**
	 * Parse a concatenation of 'paramName=paramValue' separated by ';' and
	 * returns the pairs in a Vector of Strings.
	 * 
	 * @param parametersString
	 * @return a Vector with String elements containing 'paramName=paramValue'
	 * @throws IllegalArgumentException
	 */
	protected Vector parseAndCheckParameters(String parametersString)
			throws IllegalArgumentException {
		Vector params = new Vector();

		// something wrong happened. Return empty just in case
		if (parametersString == null || parametersString.length() == 0)
			return params;
		
		StringTokenizer st = new StringTokenizer(parametersString, ';');
		while (st.hasMoreChars()) {
			String parameter = st.nextToken();
			// This implementation of StringTokenizer
			// will give back also the delimiter; remove it
			if (parameter.endsWith(";"))
				parameter = parameter.substring(0, parameter.length()-1);
			
			// check if it has "name=value" form
			if (parameter.indexOf("=") == -1)
				throw new IllegalArgumentException(
						"One of the parameters in"
								+ "the URI doesn't follow the expected syntax 'name=value'");
			params.addElement(parameter);
		}
		return params;
	}
}
