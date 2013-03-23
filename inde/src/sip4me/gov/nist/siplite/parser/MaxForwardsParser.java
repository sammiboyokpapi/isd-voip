package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.MaxForwardsHeader;
/** Parser for Max Forwards Header.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
 * @version 1.0
 */
public class MaxForwardsParser extends HeaderParser {

       MaxForwardsParser() {}

       public  MaxForwardsParser(String contentLength) {
		super(contentLength);
       }

        protected MaxForwardsParser(Lexer lexer) {
		super(lexer);
	}
	
	public  Header  parse() throws ParseException {
	     if (debug) dbg_enter("MaxForwardsParser.enter");
             try {
		MaxForwardsHeader maxForwards = new MaxForwardsHeader();
		headerName (TokenTypes.MAX_FORWARDS);
                String number=this.lexer.number();
                maxForwards.setMaxForwards(Integer.parseInt(number));
                this.lexer.SPorHT();
		this.lexer.match('\n');
                return maxForwards;
              } catch (IllegalArgumentException ex) {
		   throw createParseException(ex.getMessage());
              }  finally {
			if (debug) dbg_leave("MaxForwardsParser.leave");
	      }
	}

/**
        public static void main(String args[]) throws ParseException {
		String content[] = {
			"Max-Forwards: 3495\n",
			"Max-Forwards: 0 \n"
                };
			
		for (int i = 0; i < content.length; i++ ) {
		    MaxForwardsParser cp = 
			  new MaxForwardsParser(content[i]);
		    MaxForwardsHeader c = (MaxForwardsHeader) cp.parse();
		    System.out.println("encoded = " + c.encode());
		}
			
	}
**/
	
       

}
