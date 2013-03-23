package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.WWWAuthenticateHeader;

/** Parser for WWW authenitcate header.
*
*@version  JAIN-SIP-1.1
*
*@author Olivier Deruelle <deruelle@nist.gov>
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
* @version 1.0
*/
public class WWWAuthenticateParser extends ChallengeParser {

	protected WWWAuthenticateParser() {}


        /** Constructor
         * @param wwwAuthenticate -  message to parse
         */
        public WWWAuthenticateParser(String wwwAuthenticate) {
		super(wwwAuthenticate);
	}

        /** Cosntructor
         * @param  lexer - lexer to use.
         */
        protected WWWAuthenticateParser(Lexer lexer) {
		super(lexer);
	}
        
        /** parse the String message 
         * @return Header (WWWAuthenticate object)
         * @throws ParseException if the message does not respect the spec.
         */
	public Header parse() throws ParseException {
		if (debug) dbg_enter("parse");
		try {
	       		headerName(TokenTypes.WWW_AUTHENTICATE);
	       		WWWAuthenticateHeader 
			wwwAuthenticate = new WWWAuthenticateHeader();
	       		super.parse(wwwAuthenticate);
               		return wwwAuthenticate;
		} finally {
		   if (debug) dbg_leave("parse");
		}
	}

	
       

        
}

