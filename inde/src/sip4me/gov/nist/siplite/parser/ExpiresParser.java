package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.ExpiresHeader;
import sip4me.gov.nist.siplite.header.Header;

/** Parser for SIP Expires Parser. Converts from SIP Date to the
* internal storage (Calendar).
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
 */
public class ExpiresParser extends HeaderParser {
    
	public ExpiresParser() {}
    
    
    /** protected constructor.
     *@param text is the text of the header to parse
     */
    public ExpiresParser(String text) {
        super(text);
    }
    
    /** constructor.
     *@param lexer is the lexer passed in from the enclosing parser.
     */
    protected ExpiresParser(Lexer lexer) {
        super(lexer);
    }
    
    /** Parse the header.
     */
    public Header parse() throws ParseException {
        ExpiresHeader expires = new ExpiresHeader();
	if (debug) dbg_enter("parse");
        try {
            lexer.match(TokenTypes.EXPIRES);
            lexer.SPorHT();
            lexer.match(':');
            lexer.SPorHT();
            String nextId = lexer.getNextId();
	    lexer.match('\n');
            try {
                int delta = Integer.parseInt(nextId);
                expires.setExpires(delta);
                return expires;
            } catch (NumberFormatException ex) {
		throw createParseException("bad integer format");
	    }
        } finally  {
		if (debug) dbg_leave("parse");
        }
        
        
    }
    
    /** Test program -- to be removed in final version.
    public static void main(String args[]) throws ParseException {
        String expires[] = {
            "Expires: 1000\n" };
            
            for (int i = 0; i < expires.length; i++ ) {
		try {
                	System.out.println("Parsing " + expires[i]);
                	ExpiresParser ep = new ExpiresParser(expires[i]);
                	ExpiresHeader e = (ExpiresHeader) ep.parse();
                	System.out.println("encoded = " +e.encode());
		} catch (ParseException ex) {
		  	System.out.println(ex.getMessage());
		}
            }
            
    }
     */
    
    
}
