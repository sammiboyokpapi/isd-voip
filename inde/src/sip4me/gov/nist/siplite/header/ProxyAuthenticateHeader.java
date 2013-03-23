
/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;


/**
* WWW Authenticate SIP (HTTP ) header.
*/
public class ProxyAuthenticateHeader extends AuthenticationHeader {

	public static final String NAME = Header.PROXY_AUTHENTICATE;

	public static Class clazz;

	static {
		clazz = new ProxyAuthenticateHeader().getClass();
	}

    
     	/** Default Constructor
     	*/
    	public ProxyAuthenticateHeader() {
        	super(PROXY_AUTHENTICATE);
    	}
    

        
}
