package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Token;
import sip4me.gov.nist.siplite.header.EventHeader;
import sip4me.gov.nist.siplite.header.Header;

/** Parser for Event header.
*
*@version  JAIN-SIP-1.1
*
*@author Olivier Deruelle <deruelle@nist.gov> 
*@author M. Ranganathan <mranga@nist.gov>  
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
* @version 1.0
*/
public class EventParser extends   ParametersParser{
    
    EventParser() {} 

    /**
     * Creates a new instance of EventParser 
     * @param event the header to parse 
     */
    public EventParser(String event) {
        super(event);
    }
    
    /** Cosntructor
     * @param lexer the lexer to use to parse the header
     */
    protected  EventParser(Lexer lexer) {
        super(lexer);
    }
    
    /** parse the String message
     * @return Header (Event object)
     * @throws SIPParseException if the message does not respect the spec.
     */
    public Header parse() throws ParseException {
        
        if (debug) dbg_enter("EventParser.parse");
        
        try {        	        	
            headerName(TokenTypes.EVENT);			
            this.lexer.SPorHT();
                        
            EventHeader event=new EventHeader();			
            this.lexer.match(TokenTypes.ID);			
            Token token= lexer.getNextToken();
            String value=token.getTokenValue();
            event.setEventType(value);
            super.parse(event);
            
            this.lexer.SPorHT();			
            this.lexer.match('\n');			
         
            return event;
            
        } catch (ParseException ex ) {
             throw createParseException(ex.getMessage());
        } finally {
            if (debug) dbg_leave("EventParser.parse");
        }
    }
    

    public static void main(String args[]) throws ParseException {
        String r[] = {
            "o: presence\n",
            "Event: foo; param=abcd; id=1234\n",
            "Event: foo.foo1; param=abcd; id=1234\n"
        };
        
        for (int i = 0; i < r.length; i++ ) {
            EventParser parser =
            new EventParser(r[i]);
            EventHeader e= (EventHeader) parser.parse();
            System.out.println("encoded = " + e.encode());
	    System.out.println(e.getEventId());
	    System.out.println(e.match(e));
        }    
    }

    
}

