package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.Header;
/** Parser for CALL ID header.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*@author  Olivier Deeruelle <deruelle@nist.gov> <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class CallIDParser extends HeaderParser {

    CallIDParser() {}

    
    /** Creates new CallIdHeaderParser
     * @param String callID message to parse to set
     */
    public CallIDParser(String callID) {
        super(callID);
    }
    
    /** Constructor
     * @param Lexer lexer to set
     */
    protected CallIDParser(Lexer lexer) {
        super(lexer);
    }
    
        /** parse the String message
         * @return Header (CallIdHeader object)
         * @throws ParseException if the message does not respect the spec.
         */
    public Header parse() throws ParseException {
      if (debug) dbg_enter("parse");
      try {  
        this.lexer.match (TokenTypes.CALL_ID);
        this.lexer.SPorHT();
        this.lexer.match(':');
        this.lexer.SPorHT();
        
        CallIdHeader callID=new CallIdHeader();
        
        this.lexer.SPorHT();
        String rest=lexer.getRest();
        callID.setCallId(rest.trim());
        return callID;
       }finally {
		if (debug) dbg_leave("parse");
	}
    }
    
        /** Test program
    public static void main(String args[]) throws ParseException {
        String call[] = {
            "Call-ID: f81d4fae-7dec-11d0-a765-00a0c91e6bf6@foo.bar.com\n",
            "i:f81d4fae-7dec-11d0-a765-00a0c91e6bf6@foo.bar.com\n",
            "Call-ID: 1@10.0.0.1\n",
            "Call-ID: kl24ahsd546folnyt2vbak9sad98u23naodiunzds09a3bqw0sdfbsk34poouymnae0043nsed09mfkvc74bd0cuwnms05dknw87hjpobd76f\n",
            "Call-Id: 281794\n"
        };
        
        for (int i = 0; i <call.length; i++ ) {
            CallIdHeaderParser cp =
            new CallIdHeaderParser(call[i]);
            CallIdHeader callID= (CallIdHeader) cp.parse();
            System.out.println("encoded = " + callID.encode());
        }
        
    }
     */
    
    
    
    
}


