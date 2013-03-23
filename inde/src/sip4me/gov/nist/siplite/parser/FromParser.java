package sip4me.gov.nist.siplite.parser;
import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.SipURI;
import sip4me.gov.nist.siplite.header.FromHeader;
import sip4me.gov.nist.siplite.header.Header;

/** From header parser.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public class FromParser extends AddressParametersParser {

	FromParser() {}
    
	public FromParser(String from) {
		super(from);
	}

        protected FromParser(Lexer lexer) {
		super(lexer);
	}
	
	public Header parse() throws ParseException {
           
		FromHeader from= new FromHeader();
		
		this.lexer.match (TokenTypes.FROM);
		this.lexer.SPorHT();
		this.lexer.match(':');
		this.lexer.SPorHT();
		super.parse(from);
		this.lexer.match('\n');
		if (((Address)from.getAddress()).getAddressType() ==
			Address.ADDRESS_SPEC) {
			// the parameters are header parameters.
			if (from.getAddress().getURI() instanceof SipURI) {
			  SipURI sipUri = (SipURI) from.getAddress().getURI();
			  NameValueList parms = sipUri.getUriParms();
			  if (parms != null && ! parms.isEmpty() ) {
			      from.setParameters(parms);
			      sipUri.removeUriParms();
			  }
			}
		}
			
                return from;
          
	}

/**

        public static void main(String args[]) throws ParseException {
	String from[] = {
	"From: foobar at com<sip:4855@166.34.120.100 >;tag=1024181795\n",
	"From: sip:user@company.com\n",
	"From: sip:caller@university.edu\n",
        "From: sip:localhost\n",
        "From: \"A. G. Bell\" <sip:agb@bell-telephone.com> ;tag=a48s\n"
         };
			
		for (int i = 0; i < from.length; i++ ) {
		    try {
		       FromParser fp = new FromParser(from[i]);
		       FromHeader f = (FromHeader) fp.parse();
		       System.out.println("encoded = " + f.encode());
		    } catch (ParseException ex) {
			System.out.println(ex.getMessage());
		    } 
		}
			
	}

**/
	
       

        
}
