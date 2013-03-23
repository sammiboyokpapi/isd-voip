package sip4me.gov.nist.siplite.parser;
import java.util.Calendar;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.DateHeader;
import sip4me.gov.nist.siplite.header.Header;


/** Parser for SIP Date field. Converts from SIP Date to the
 * internal storage (Calendar)
 */
public class DateParser extends HeaderParser {

	DateParser() {}

        /** Constructor
         * @param String route message to parse to set
         */
        public DateParser (String date) {
		super(date);
	}

        protected DateParser(Lexer lexer) {
		super(lexer);
	}

        

	/** Parse method.
         * @throws ParseException
         * @return  the parsed Date header/
         */
	public Header parse()  throws ParseException {
	   if (debug) dbg_enter("DateParser.parse");
            try {
                headerName(TokenTypes.DATE);
                int w = wkday();
                lexer.match(',');
                lexer.match(' ');
                Calendar cal =date();
                lexer.match(' ');
                time(cal);
                lexer.match(' ');
                String tzone = this.lexer.ttoken().toLowerCase();
                if (!"gmt".equals(tzone)) 
                        throw createParseException("Bad Time Zone " + tzone);
                DateHeader retval = new DateHeader();
                retval.setDate(cal);
                return retval;
            } finally {
		if (debug) dbg_leave("DateParser.parse");
                
            }
            
        }
        
	/** Test program -- to be removed in final version.
        public static void main(String args[]) throws ParseException {
		String date[] = {
			"Date: Sun, 07 Jan 2001 19:05:06 GMT",
			"Date: Mon, 08 Jan 2001 19:05:06 GMT" };
			
		for (int i = 0; i < date.length; i++ ) {
		    System.out.println("Parsing " + date[i]);
		    DateParser dp = 
			  new DateParser(date[i]);
		    DateHeader d = (DateHeader) dp.parse();
		    System.out.println("encoded = " +d.encode());
		}
			
	}
	*/
        

}
