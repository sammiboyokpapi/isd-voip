/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

/**
* Authorization SIP header.
*
* @see ProxyAuthorization
*
* @author M. Ranganathan <mranga@nist.gov>  NIST/ITL/ANTD <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class AuthorizationHeader extends AuthenticationHeader {
	public static Class clazz;
	public static final String NAME = Header.AUTHORIZATION;
	static {
		clazz = new AuthorizationHeader().getClass();
	}
        
	public AuthorizationHeader() {
		super(AUTHORIZATION);
	}

        
}
