/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
 *******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;

import sip4me.gov.nist.core.Host;
import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.javax.sdp.SdpException;
import sip4me.gov.nist.javax.sdp.SdpParseException;

/**
 * Origin Field SDP header
 * 
 *@author Olivier Deruelle <deruelle@antd.nist.gov>
 *@author M. Ranganathan <mranga@nist.gov> <br/>
 * 
 *        <a href="{@docRoot} /uncopyright.html">This code is in the public
 *         domain.</a>
 * 
 */
public class OriginField extends SDPField {
	protected String username;
	protected String sessID;
	protected long sessVersion;
	protected String nettype; // IN
	protected String addrtype; // IPV4/6
	protected Host address;

	public Object clone() {
		OriginField retval = new OriginField();
		retval.username = this.username;
		retval.sessID = this.sessID;
		retval.sessVersion = this.sessVersion;
		retval.nettype = this.nettype;
		retval.addrtype = this.addrtype;
		if (this.address != null)
			retval.address = (Host) this.address.clone();
		return retval;
	}

	public OriginField() {
		super(ORIGIN_FIELD);
	}

	/**
	 * Returns the name of the session originator.
	 * 
	 * @throws SdpParseException
	 * @return the string username.
	 */
	public String getUsername() throws SdpParseException {
		return username;
	}

	/**
	 * Get the netType member.
	 */
	public String getNettype() {
		return nettype;
	}

	/**
	 * Get the address type member.
	 */
	public String getAddrtype() {
		return addrtype;
	}

	/**
	 * Get the host member.
	 */
	public Host getHost() {
		return address;
	}

	/**
	 * Set the nettype member
	 */
	public void setNettype(String n) {
		nettype = n;
	}

	/**
	 * Set the addrtype member
	 */
	public void setAddrtype(String a) {
		addrtype = a;
	}

	/**
	 * Set the address member
	 */
	public void setAddress(Host a) {
		address = a;
	}

	/**
	 * Sets the name of the session originator.
	 * 
	 * @param user
	 *            the string username.
	 * @throws SdpException
	 *             if the parameter is null
	 */
	public void setUsername(String user) throws SdpException {
		if (user == null)
			throw new SdpException("The user parameter is null");
		else {
			this.username = user;
		}
	}

	/**
	 * Returns the unique identity of the session.
	 * 
	 * @throws SdpParseException
	 * @return the session id.
	 */
	public String getSessionId() throws SdpParseException {
		return sessID;
	}

	/**
	 * Sets the unique identity of the session.
	 * 
	 * @param id
	 *            the session id.
	 * @throws SdpException
	 *             if the id is <0
	 */
	public void setSessionId(String id) throws SdpException {
		if (id == null || id.length() == 0)
			throw new SdpException("The SessionID is null or too short");
		else
			sessID = id;
	}

	/**
	 * Returns the unique version of the session.
	 * 
	 * @throws SdpException
	 * @return the session version.
	 */
	public long getSessionVersion() throws SdpParseException {
		return sessVersion;
	}

	/**
	 * Sets the unique version of the session.
	 * 
	 * @param version
	 *            the session version.
	 * @throws SdpException
	 *             if the version is <0
	 */
	public void setSessionVersion(long version) throws SdpException {
		if (version < 0)
			throw new SdpException("The version parameter is <0");
		else
			sessVersion = version;
	}

	/**
	 * Returns the type of the network for this Connection.
	 * 
	 * @throws SdpParseException
	 * @return the string network type.
	 */
	public String getAddress() throws SdpParseException {
		Host addr = getHost();
		if (addr == null)
			return null;
		else
			return addr.getAddress();
	}

	/**
	 * Returns the type of the address for this Connection.
	 * 
	 * @throws SdpParseException
	 * @return the string address type.
	 */
	public String getAddressType() throws SdpParseException {
		return getAddrtype();
	}

	/**
	 * Returns the type of the network for this Connection
	 * 
	 * @throws SdpParseException
	 * @return the string network type.
	 */
	public String getNetworkType() throws SdpParseException {
		return getNettype();
	}

	/**
	 * Sets the type of the address for this Connection.
	 * 
	 * @param addr
	 *            string address type.
	 * @throws SdpException
	 *             if the addr is null
	 */
	public void setAddress(String addr) throws SdpException {
		if (addr == null)
			throw new SdpException("The addr parameter is null");
		else {
			Host host = getHost();
			if (host == null)
				host = new Host();
			host.setAddress(addr);
			setAddress(host);
		}
	}

	/**
	 * Returns the type of the network for this Connection.
	 * 
	 * @param type
	 *            the string network type.
	 * @throws SdpException
	 *             if the type is null
	 */
	public void setAddressType(String type) throws SdpException {
		if (type == null)
			throw new SdpException("The type parameter is <0");
		else
			setAddrtype(type);
	}

	/**
	 * Sets the type of the network for this Connection.
	 * 
	 * @param type
	 *            the string network type.
	 * @throws SdpException
	 *             if the type is null
	 */
	public void setNetworkType(String type) throws SdpException {
		if (type == null)
			throw new SdpException("The type parameter is <0");
		else
			setNettype(type);
	}

	/**
	 * Get the string encoded version of this object
	 * 
	 * @since v1.0
	 */
	public String encode() {
		return ORIGIN_FIELD + username + Separators.SP + sessID + Separators.SP
				+ sessVersion + Separators.SP + nettype + Separators.SP
				+ addrtype + Separators.SP + address.encode()
				+ Separators.NEWLINE;
	}

}
