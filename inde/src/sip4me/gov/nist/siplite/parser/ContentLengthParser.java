package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.ContentLengthHeader;
import sip4me.gov.nist.siplite.header.Header;
/** Parser for Content-Length Header.
*@version  JAIN-SIP-1.1
*
*@author Olivier Deruelle  <br/>
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class ContentLengthParser extends HeaderParser {

       public  ContentLengthParser() { }

       public  ContentLengthParser(String contentLength) {
		super(contentLength);
       }

        protected ContentLengthParser(Lexer lexer) {
		super(lexer);
	}
	
	public  Header  parse() throws ParseException {
	     if (debug) dbg_enter("ContentLengthParser.enter");
             try {
		ContentLengthHeader contentLength= new ContentLengthHeader();
		headerName (TokenTypes.CONTENT_LENGTH);
                String number=this.lexer.number();
                contentLength.setContentLength(Integer.parseInt(number));
                this.lexer.SPorHT();
		this.lexer.match('\n');
                return contentLength;
              } catch (Exception ex) {
		   throw createParseException(ex.getMessage());
              }  finally {
			if (debug) dbg_leave("ContentLengthParser.leave");
	      }
	}

/**
        public static void main(String args[]) throws ParseException {
		String content[] = {
			"l: 345\n",
			"Content-Length: 3495\n",
			"Content-Length: 0 \n"
                };
			
		for (int i = 0; i < content.length; i++ ) {
		    ContentLengthParser cp = 
			  new ContentLengthParser(content[i]);
		    ContentLengthHeader c = (ContentLength) cp.parse();
		    System.out.println("encoded = " + c.encode());
		}
			
	}
**/
	
       

}
