/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;
import sip4me.gov.nist.core.NameValueList;

/**
* Expires SIP Header.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class ExpiresHeader extends Header  {
    
        /** expires field
         */    
	protected Integer expires;

	public static final String NAME = Header.EXPIRES;

	public static Class clazz;


	static {
		clazz = new ExpiresHeader().getClass();
	}

        /** default constructor
         */        
	public ExpiresHeader() {
            super(EXPIRES);
        }

	

        
        /**
         * Return canonical form.
         * @return String
         */        
        public String encodeBody() {
		return expires.toString();
	}
        
        
        
        /**
         * Gets the expires value of the ExpiresHeader. This expires value is
         *
         * relative time.
         *
         *
         *
         * @return the expires value of the ExpiresHeader.
         *
         * @since JAIN SIP v1.1
         *
         */
        public int getExpires() {
		return expires.intValue();
        }
        
        /**
         * Sets the relative expires value of the ExpiresHeader. 
	 * The expires value MUST be greater than zero and MUST be 
	 * less than 2**31.
         *
         * @param expires - the new expires value of this ExpiresHeader
         *
         * @throws InvalidArgumentException if supplied value is less than zero.
         *
         * @since JAIN SIP v1.1
         *
         */
        public void setExpires(int expires)
            throws IllegalArgumentException {
                if (expires < 0) throw new IllegalArgumentException
                            ("bad argument " + expires);
		this.expires = new Integer(expires);
        }
        
        /** Get the parameters for the header as a nameValue list.
         */
        public NameValueList getParameters() {
            return null;
        }
        
        /** Get the value for the header as opaque object (returned value
         * will depend upon the header. Note that this is not the same as
         * the getHeaderValue above.
         */
        public Object getValue() {
            return expires;
        }
        
}
