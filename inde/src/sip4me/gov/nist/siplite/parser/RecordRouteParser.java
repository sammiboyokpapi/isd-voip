package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.RecordRouteHeader;
import sip4me.gov.nist.siplite.header.RecordRouteList;
/** Parser for a list of route headers.
*
*@version  JAIN-SIP-1.1
*
*@author Olivier Deruelle <deruelle@nist.gov> 
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*@version 1.0
*/
public class RecordRouteParser extends AddressParametersParser {

	RecordRouteParser() { }

       /** Constructor
         * @param String recordRoute message to parse to set
         */
        public RecordRouteParser(String recordRoute) {
		super(recordRoute);
	}

        protected RecordRouteParser(Lexer lexer) {
		super(lexer);
	}
	
        /** parse the String message and generate the RecordRoute List Object
         * @return Header the RecordRoute List object
         * @throws ParseException if errors occur during the parsing
         */
	public Header parse() throws ParseException {
             RecordRouteList recordRouteList=new RecordRouteList();

	     if (debug) dbg_enter("RecordRouteParser.parse");
            
             try {
		this.lexer.match (TokenTypes.RECORD_ROUTE);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();
		while(true)  {
		   RecordRouteHeader recordRoute =  new RecordRouteHeader();
		   super.parse(recordRoute);
		   recordRouteList.add(recordRoute); 
		   this.lexer.SPorHT();
		   if (lexer.lookAhead(0) == ',')  {
			this.lexer.match(',');
			this.lexer.SPorHT();
		   } else if (lexer.lookAhead(0) == '\n') break;
		   else throw createParseException("unexpected char");
		}
		return recordRouteList;
	      } finally {
		if (debug) dbg_leave("RecordRouteParser.parse");
	      }

	}

/**
        public static void main(String args[]) throws ParseException {
		String rou[] = {
			"Record-Route: <sip:bob@biloxi.com;maddr=10.1.1.1>,"+
                        "<sip:bob@biloxi.com;maddr=10.2.1.1>\n",
                        
			"Record-Route: <sip:UserB@there.com;maddr=ss2.wcom.com>\n",
                        
                        "Record-Route: <sip:+1-650-555-2222@iftgw.there.com;"+
                        "maddr=ss1.wcom.com>\n",
                        
                        "Record-Route: <sip:UserB@there.com;maddr=ss2.wcom.com>,"+
                        "<sip:UserB@there.com;maddr=ss1.wcom.com>\n"  
                };
			
		for (int i = 0; i < rou.length; i++ ) {
		    RecordRouteParser rp = 
			  new RecordRouteParser(rou[i]);
		    RecordRouteList recordRouteList = (RecordRouteList) rp.parse();
		    System.out.println("encoded = " +recordRouteList.encode());
		}
			
	}
*/
	
       

        
}
