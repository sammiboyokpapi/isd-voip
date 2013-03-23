/*
 * SessionNameFieldParser.java
 *
 * Created on February 25, 2002, 10:26 AM
 */

package sip4me.gov.nist.javax.sdp.parser;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.javax.sdp.fields.SDPField;
import sip4me.gov.nist.javax.sdp.fields.SessionNameField;

/**
 *
 * @author  deruelle
 * @version 1.0
 */
public class SessionNameFieldParser extends SDPParser {

    /** Creates new SessionNameFieldParser */
    public SessionNameFieldParser(String sessionNameField) {
        this.lexer = new Lexer("charLexer",sessionNameField);
    }

    protected SessionNameFieldParser() { super(); }
    
    /** Get the SessionNameField
     * @return SessionNameField
     */
    public SessionNameField sessionNameField() throws ParseException  {
        try{
            this.lexer.match ('s');
            this.lexer.SPorHT();
            this.lexer.match('=');
            
            // Check if the session name has no meaningful name
            // as stated in RFC 4566:
            // The "s=" field is the textual session name.  There MUST be one and
            // only one "s=" field per session description.  The "s=" field MUST NOT
            // be empty and SHOULD contain ISO 10646 characters (but see also the
            // "a=charset" attribute).  If a session has no meaningful name, the
            // value "s= " SHOULD be used (i.e., a single space as the session name).
            char nextChar = this.lexer.lookAhead();
            
            
            this.lexer.SPorHT();
            SessionNameField sessionNameField=new SessionNameField();
            String rest= lexer.getRest(); 
            
            if (rest != null)
            	sessionNameField.setSessionName(rest.trim());
            else
            if (rest == null && nextChar == ' ')
            	sessionNameField.setSessionName(" ");
            else
            	throw lexer.createParseException();
            System.out.println("s=" + sessionNameField.getSessionName());
            return sessionNameField;
        }
        catch(Exception e) {
            throw lexer.createParseException();
        }  
        
    }

    public SDPField parse() throws ParseException {
	return this.sessionNameField();
    }
    
    public static void main(String[] args) throws ParseException {
	    String session[] = {
			"s=SDP Seminar \n",
                        "s= Session SDP\n"
                };

	    for (int i = 0; i < session.length; i++) {
	      SessionNameFieldParser sessionNameFieldParser=
			new SessionNameFieldParser( session[i] );
	        SessionNameField sessionNameField=
			sessionNameFieldParser.sessionNameField();
		System.out.println("encoded: " +sessionNameField.encode());
	    }

	}



}
