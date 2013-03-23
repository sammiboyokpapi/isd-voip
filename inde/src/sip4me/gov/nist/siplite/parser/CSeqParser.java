package sip4me.gov.nist.siplite.parser;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.Header;

/** Parser for CSeq headers.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*@author Olivier Deruelle <deruelle@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class CSeqParser extends HeaderParser {
	CSeqParser() {}


       public   CSeqParser(String cseq) {
		super(cseq);
       }

        protected  CSeqParser(Lexer lexer) {
		super(lexer);
	}
	
	public  Header  parse() throws ParseException {
             try {
		CSeqHeader c= new CSeqHeader();
		
		this.lexer.match (TokenTypes.CSEQ);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();
                String number=this.lexer.number();
                c.setSequenceNumber(Integer.parseInt(number));
                this.lexer.SPorHT();
		String m = method();
                c.setMethod(m);
                this.lexer.SPorHT();
		this.lexer.match('\n');
                  return c;
              } 
              catch (NumberFormatException ex) {
                   
		   throw createParseException("Number format exception");
              } 
	}

/**
        public static void main(String args[]) throws ParseException {
		String cseq[] = {
			"CSeq: 17 INVITE\n",
			"CSeq: 17 ACK\n",
			"CSeq : 18   BYE\n",
                        "CSeq:1 CANCEL\n",
                        "CSeq: 3 BYE\n"
                };
			
		for (int i = 0; i < cseq.length; i++ ) {
		    CSeqParser cp = 
			  new CSeqParser(cseq[i]);
		    CSeqHeader c = (CSeqHeader) cp.parse();
		    System.out.println("encoded = " + c.encode());
		}
			
	}
**/
	
       

}
