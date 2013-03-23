/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

import sip4me.gov.nist.core.GenericObject;
import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.core.Utils;

/**
*  Protocol name and version.
*
*@version  1.1
*
*@author Olivier Deruelle and M. Ranganathan
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class Protocol extends GenericObject {
    
        /** protocolName field
         */    
	protected String protocolName;
        
        /** protocolVersion field
         */        
	protected String protocolVersion;
        
        /** transport field
         */        
	protected String transport;
        
        
	/** 
	* Default constructor.
	*/
	public Protocol() {
		protocolName = "SIP";
		protocolVersion = "2.0";
		transport = "UDP";	
	}
        
        
        /**
         * Compare two protocols for equality.
         * @return true if the two protocols are the same.
         * @param other Object to set
         */   
        public boolean equals(Object other) {
            if (! other.getClass().equals(this.getClass())) {
                return false;
            }
            Protocol that = (Protocol) other;
            if (Utils.compareToIgnoreCase(protocolName,that.protocolName) 
		!= 0 ) {
                return false;
            }
            if (Utils.compareToIgnoreCase(protocolVersion,protocolVersion) 
			!= 0 ) {
                return false;
            }
            if (Utils.compareToIgnoreCase(transport,that.transport) != 0) {
                return false;
            }
            return true;
        }
        
        /**
         * Return canonical form.
         * @return String
         */  
        public String encode() {
            return protocolName.toUpperCase() + Separators.SLASH + 
		protocolVersion +
                Separators.SLASH + transport.toUpperCase();
        }

        /** get the protocol name
         * @return String
         */        
	public String getProtocolName() {
            return protocolName ;
        }
            
        /** get the protocol version
         * @return String
         */        
	public String getProtocolVersion() {
            return protocolVersion ;
        }
        
        /** get the transport
         * @return String
         */        
	public String getTransport() {
            return transport ;
        }
        
	/**
         * Set the protocolName member
         * @param p String to set
         */
	public void setProtocolName(String p) {
            protocolName = p ;
        }
        
	/**
         * Set the protocolVersion member
         * @param p String to set
         */
	public void setProtocolVersion(String p) {
            protocolVersion = p ;
        }
        
	/**
         * Set the transport member
         * @param t String to set
         */
	public void setTransport(String t) {
            transport = t ;
        }

       /** Clone this structure.
        * @return Object Protocol
	*/
	public Object clone() {
		Protocol retval = new Protocol();
   
		if (this.protocolName != null) 
			retval.protocolName = new String(this.protocolName);
		if (this.protocolVersion != null) 
			retval.protocolVersion =new String(this.protocolVersion);
                if (this.transport != null) 
			retval.transport =new String(this.transport);
		return (Object) retval;

	}
        
}
