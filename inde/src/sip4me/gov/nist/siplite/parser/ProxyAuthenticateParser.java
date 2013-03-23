package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.ProxyAuthenticateHeader;

/** Parser for ProxyAuthenticate headers.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class ProxyAuthenticateParser extends ChallengeParser {

	 ProxyAuthenticateParser() {}

        /** Constructor
         * @param String paAuthenticate message to parse
         */
        public ProxyAuthenticateParser(String proxyAuthenticate) {
		super(proxyAuthenticate);
	}

        /** Cosntructor
         * @param Lexer lexer to set
         */
        protected ProxyAuthenticateParser(Lexer lexer) {
		super(lexer);
	}
        
        /** parse the String message 
         * @return Header (ProxyAuthenticate object)
         * @throws ParseException if the message does not respect the spec.
         */
	public Header parse() throws ParseException {
	       headerName(TokenTypes.PROXY_AUTHENTICATE);
	       ProxyAuthenticateHeader proxyAuthenticate = 
			new ProxyAuthenticateHeader();
	       super.parse(proxyAuthenticate);
               return proxyAuthenticate;
	}

/**
        public static void main(String args[]) throws ParseException {
		String paAuth[] = {
     "Proxy-Authenticate: Digest realm=\"MCI WorldCom SIP\","+
     "domain=\"sip:ss2.wcom.com\", nonce=\"ea9c8e88df84f1cec4341ae6cbe5a359\","+
     "opaque=\"\", stale=FALSE, algorithm=MD5\n",
                        
     "Proxy-Authenticate: Digest realm=\"MCI WorldCom SIP\","+ 
	"qop=\"auth\" , nonce-value=\"oli\"\n"
                };
			
		for (int i = 0; i < paAuth.length; i++ ) {
		    ProxyAuthenticateParser pap = 
			  new ProxyAuthenticateParser(paAuth[i]);
		    ProxyAuthenticateHeader pa= (ProxyAuthenticateHeader) 
				pap.parse();
		    System.out.println("encoded = " + pa.encode());
		}
			
	}
	
**/
       

        
}

