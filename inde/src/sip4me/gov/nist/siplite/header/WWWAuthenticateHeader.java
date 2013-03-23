/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

/**
* WWW Authenticate SIP (HTTP ) header.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public class WWWAuthenticateHeader extends AuthenticationHeader {

   public static final String NAME = Header.WWW_AUTHENTICATE;

   public static Class clazz;

   static {
	clazz = new WWWAuthenticateHeader().getClass();
   }

        /**
         * Default Constructor.
         */
    public WWWAuthenticateHeader() {
        super(WWW_AUTHENTICATE);
    }
    
    
	/** Get the value of the header (just returns the scheme).
	*@return the scheme object.
	*/
	public Object getValue() {
		return getScheme();

	}
	
    
}
