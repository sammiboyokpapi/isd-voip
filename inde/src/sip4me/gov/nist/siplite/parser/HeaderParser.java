package sip4me.gov.nist.siplite.parser;
import java.util.Calendar;
import java.util.TimeZone;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.siplite.header.ExtensionHeader;
import sip4me.gov.nist.siplite.header.Header;


/** Generic header parser class. The parsers for various headers extend this
* class. To create a parser for a new header, extend this class and change
* the createParser class.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public  class HeaderParser extends Parser {
    
    /** Parse the weekday field
     * @return an integer with the calendar content for wkday.
     */
    protected int wkday()  throws ParseException {
	dbg_enter("wkday");
	try {
        	String tok = lexer.ttoken();
        	String id = tok.toLowerCase();
	
        	if ( Utils.equalsIgnoreCase (TokenNames.MON,id)) 
				return Calendar.MONDAY;
        	else if (Utils.equalsIgnoreCase
			(TokenNames.TUE,id)) return Calendar.TUESDAY;
        	else if (Utils.equalsIgnoreCase (TokenNames.WED,id)) 
				return Calendar.WEDNESDAY;
        	else if (Utils.equalsIgnoreCase (TokenNames.THU,id)) 
				return Calendar.THURSDAY;
        	else if (Utils.equalsIgnoreCase
			(TokenNames.FRI,id)) 
				return Calendar.FRIDAY;
        	else if (Utils.equalsIgnoreCase
			( TokenNames.SAT,id)) 
				return Calendar.SATURDAY;
        	else if (Utils.equalsIgnoreCase
			( TokenNames.SUN,id)) return Calendar.SUNDAY;
        	else  throw createParseException("bad wkday" );
	} finally {
		dbg_leave("wkday");
	}
        
    }
    
    /** parse and return a date field.
     *@return a date structure with the parsed value.
     */
    protected Calendar date() throws ParseException {
        try  {
            Calendar retval =
            Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            String s1 = lexer.number();
            int day = Integer.parseInt(s1);
            if (day <= 0 || day > 31)
                throw createParseException("Bad day ");
            retval.set(Calendar.DAY_OF_MONTH,day);
            lexer.match(' ');
            String month = lexer.ttoken().toLowerCase();
            if (month.equals("jan"))  {
                retval.set(Calendar.MONTH,Calendar.JANUARY);
            } else if (month.equals("feb")) {
                retval.set(Calendar.MONTH,Calendar.FEBRUARY);
            } else if (month.equals("mar")) {
                retval.set(Calendar.MONTH,Calendar.MARCH);
            } else if (month.equals("apr")) {
                retval.set(Calendar.MONTH,Calendar.APRIL);
            } else if (month.equals("may")) {
                retval.set(Calendar.MONTH,Calendar.MAY);
            } else if (month.equals("jun")) {
                retval.set(Calendar.MONTH,Calendar.JUNE);
            } else if (month.equals("jul")) {
                retval.set(Calendar.MONTH,Calendar.JULY);
            } else if (month.equals("aug")) {
                retval.set(Calendar.MONTH,Calendar.AUGUST);
            } else if (month.equals("sep")) {
                retval.set(Calendar.MONTH,Calendar.SEPTEMBER);
            } else if (month.equals("oct")) {
                retval.set(Calendar.MONTH,Calendar.OCTOBER);
            } else if (month.equals("nov")) {
                retval.set(Calendar.MONTH,Calendar.NOVEMBER);
            } else if (month.equals("dec")) {
                retval.set(Calendar.MONTH,Calendar.DECEMBER);
            }
            lexer.match(' ');
            String s2 = lexer.number();
            int yr = Integer.parseInt(s2);
            retval.set(Calendar.YEAR,yr);
            return retval;
            
        } catch (Exception ex) {
            throw createParseException("bad date field" );
        }
        
    }
    
    /** Set the time field. This has the format hour:minute:second
     */
    protected void time(Calendar calendar) throws ParseException {
        try {
            String s = lexer.number();
            int hour = Integer.parseInt(s);
            calendar.set(Calendar.HOUR_OF_DAY,hour);
            lexer.match(':');
            s = lexer.number();
            int min = Integer.parseInt(s);
            calendar.set(Calendar.MINUTE,min);
            lexer.match(':');
            s = lexer.number();
            int sec = Integer.parseInt(s);
            calendar.set(Calendar.SECOND,sec);
        } catch (Exception ex) {
            throw createParseException ("error processing time " );
            
        }
        
    }
    
    protected HeaderParser() { }

    public void setHeaderToParse( String header )  throws ParseException {
	if (this.lexer == null) 
        this.lexer = new Lexer("command_keywordLexer",header);
	else throw createParseException("header already set");
    }
	
    /** Creates new HeaderParser
     * @param String to parse.
     */
    protected HeaderParser(String header ) {
        this.lexer = new Lexer("command_keywordLexer",header);
    }
    
    protected HeaderParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("command_keywordLexer");
    }
    
    /** Parse the SIP header from the buffer and return a parsed
     * structure.
     *@throws ParseException if there was an error parsing.
     */
    public Header parse()
    throws ParseException {
            String name = lexer.getNextToken(':');
            lexer.consume(1);
            String body = lexer.getLine().trim();
            // we dont set any fields because the header is
            // ok
            ExtensionHeader retval = new ExtensionHeader(name,body);
            return retval;
            
    }
 
    /** Parse the header name until the colon  and chew WS after that.
     */
    protected void headerName(int tok) throws ParseException {    	
            this.lexer.match (tok);
            this.lexer.SPorHT();
            this.lexer.match(':');
            this.lexer.SPorHT();
    }
	
    
}

