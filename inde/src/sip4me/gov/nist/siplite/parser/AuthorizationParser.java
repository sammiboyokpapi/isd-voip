package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.AuthorizationHeader;
import sip4me.gov.nist.siplite.header.Header;

/** Parser for authorization headers.
*
*@version  JAIN-SIP-1.1
*
*@author Olivier Deruelle <deruelle@nist.gov>  
*@author M. Ranganathan <mranga@nist.gov>  
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class AuthorizationParser extends ChallengeParser {

    public AuthorizationParser() {}

     /** Constructor
      * @param String Authorization message to parse
      */
    public AuthorizationParser(String authorization) {
        super(authorization);
    }

        /** Cosntructor
         * @param Lexer lexer to set
         */
        protected AuthorizationParser(Lexer lexer) {
		super(lexer);
	}
	
        
        
        /** parse the String message 
         * @return Header (Authorization object)
         * @throws ParseException if the message does not respect the spec.
         */
	public Header parse() throws ParseException {
	  dbg_enter("parse");
	  try {
            
            headerName(TokenTypes.AUTHORIZATION);
            AuthorizationHeader auth=new AuthorizationHeader();
	    super.parse(auth);
            return auth;
	   } finally {
		dbg_leave("parse");

	   }
		
            
	}


/**
        public static void main(String args[]) throws ParseException {
		String auth[] = {

"Authorization: Digest username=\"UserB\", realm=\"MCI WorldCom SIP\","+
" nonce=\"ea9c8e88df84f1cec4341ae6cbe5a359\", opaque=\"\","+
" uri=\"sip:ss2.wcom.com\", response=\"dfe56131d1958046689cd83306477ecc\"\n",

"Authorization: Digest username=\"aprokop\",realm=\"Realm\",nonce=\"MTA1MDMzMjE5ODUzMjUwM2QyMzBhOTJlMTkxYjIxYWY1NDlhYzk4YzNiMGYz\",uri=\"sip:nortelnetworks.com:5060\",response=\"dbfba6c0e9664b45b7d224d2b52a1d01\",algorithm=\"MD5\",cnonce=\"VG05eWRHVnNJRTVsZEhkdmNtdHpNVEExTURNek16WTFOREUyTUE9PQ==\",qop=auth-int,nc=00000001\n"

    		};
			
		for (int i = 0; i <auth.length; i++ ) {
		    AuthorizationParser ap = 
			  new AuthorizationParser(auth[i]);
		    AuthorizationHeader a= (AuthorizationHeader) ap.parse();
		    System.out.println("encoded = " + a.encode());
		}
			
	}
	
**/
       

        
}

