/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;


/**
* Authorization SIP header 
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*
*/

public class ProxyAuthorizationHeader extends AuthenticationHeader  {
	public static final String NAME = Header.PROXY_AUTHORIZATION;
	public static Class clazz;
	
	static {
		clazz = new ProxyAuthorizationHeader().getClass();
	}
	
    
        /** Default constructor
         */        
	public ProxyAuthorizationHeader() {
		super(Header.PROXY_AUTHORIZATION);
	}

        /** constructor
         * @param hname String to set
         */        
	public ProxyAuthorizationHeader(String hname) {
		super(hname);
	}
	        
              
        
}
