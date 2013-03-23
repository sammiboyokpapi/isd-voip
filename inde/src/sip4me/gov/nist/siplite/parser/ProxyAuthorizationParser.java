package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.ProxyAuthorizationHeader;

/** Parser for ProxyAuthorization headers.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class ProxyAuthorizationParser extends ChallengeParser {

	ProxyAuthorizationParser() {}

        /** Constructor
         * @param proxyAuthorization --  header to parse
         */
        public ProxyAuthorizationParser (String proxyAuthorization) {
		super(proxyAuthorization);
	}

        /** Cosntructor
         * @param Lexer lexer to set
         */
        protected ProxyAuthorizationParser (Lexer lexer) {
		super(lexer);
	}
        
        /** parse the String message 
         * @return Header (ProxyAuthenticate object)
         * @throws ParseException if the message does not respect the spec.
         */
	public Header parse() throws ParseException {
	       headerName(TokenTypes.PROXY_AUTHORIZATION);
	       ProxyAuthorizationHeader proxyAuth = 
			new ProxyAuthorizationHeader();
	       super.parse(proxyAuth);
               return proxyAuth;
	}

        /** Test program
        public static void main(String args[]) throws ParseException {
		String paAuth[] = {
     "Proxy-Authorization: Digest realm=\"MCI WorldCom SIP\","+
     "domain=\"sip:ss2.wcom.com\", nonce=\"ea9c8e88df84f1cec4341ae6cbe5a359\","+
     "opaque=\"\", stale=FALSE, algorithm=MD5\n",
                        
     "Proxy-Authenticate: Digest realm=\"MCI WorldCom SIP\","+ 
	"qop=\"auth\" , nonce-value=\"oli\"\n"
                };
			
		for (int i = 0; i < paAuth.length; i++ ) {
		    ProxyAuthorizationParser pap = 
			  new ProxyAuthorizationParser(paAuth[i]);
		    ProxyAuthorizationHeader pa= 
			(ProxyAuthorizationParser) pap.parse();
		    System.out.println("encoded = " + pa.encode());
		}
			
	}
         */
	
       

        
}

