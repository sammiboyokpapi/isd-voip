/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.core.Separators;

/**
* Call ID Header
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class CallIdHeader extends Header {

	public static final String NAME = Header.CALL_ID;

	public static Class clazz;

    
        /** callIdentifier field
         */    
	protected CallIdentifier callIdentifier;


	static {
		clazz = new CallIdHeader().getClass();
	}

        /** Default constructor
         */        
	public CallIdHeader () { 
            super(CALL_ID);
        }
        
        /**
         * Compare two call ids for equality.
	 *
         * @param other Object to set
         * @return true if the two call ids are equals, false otherwise
         */
        public boolean equals(Object other) {
            if (! this.getClass().equals(other.getClass())) {
                return false;
            }
            CallIdHeader that = (CallIdHeader) other;
	   
            return this.callIdentifier.equals(that.callIdentifier);    
        }
	
        /**
         * Get the encoded version of this id.
	 *
         * @return String.
         */        
	public String encode() { 
		return headerName + Separators.COLON + Separators.SP + 
				callIdentifier.encode() + Separators.NEWLINE;
	}

	/** 
	* Encode the body part of this header (leave out the hdrName).
	*
	*@return String encoded body part of the header.
	*/
	public String encodeBody() {
		if (callIdentifier == null) return "";
		else return callIdentifier.encode();
	}
        
        /** get the CallId field. This does the same thing as
	 * encodeBody 
         * @return String the encoded body part of the 
         */        
	public String getCallId() { 
		return encodeBody();
	}

	/**
         * get the call Identifer member.
         * @return CallIdentifier
         */
	public CallIdentifier getCallIdentifer() {
            return callIdentifier;
        }
        
        /** set the CallId field
         * @param cid String to set. This is the body part of the Call-Id
	  *  header. It must have the form localId@host or localId.
         * @throws IllegalArgumentException if cid is null, not a token, or is 
         * not a token@token.
         */        
	public void setCallId( String cid ) throws IllegalArgumentException {
		callIdentifier = new CallIdentifier(cid);
	}

	/**
         * Set the callIdentifier member.
         * @param cid CallIdentifier to set (localId@host).
         */
	public void setCallIdentifier( CallIdentifier cid ) {
            callIdentifier = cid;
        }

       /** Clone - do a deep copy.
        * @return Object CallIdHeader 
	*/
	public Object clone() {
	    CallIdHeader retval = new CallIdHeader();
            if (this.callIdentifier !=null)
	        retval.callIdentifier=(CallIdentifier)this.callIdentifier.clone();
	    return retval;
	}

	public Object getValue() {
		return callIdentifier;

	}

	public NameValueList getParameters() { return null; }
        
}
