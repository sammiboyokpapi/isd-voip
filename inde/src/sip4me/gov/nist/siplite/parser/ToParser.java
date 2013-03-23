package sip4me.gov.nist.siplite.parser;
import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.SipURI;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.ToHeader;

/** To Header parser.
*
*@version  JAIN-SIP-1.1
*
*@author Olivier Deruelle <deruelle@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class ToParser extends AddressParametersParser {

   protected ToParser() {}

    
    /** Creates new ToParser
     * @param String to set
     */
    public ToParser(String to) {
        super(to);
    }
    
    protected ToParser(Lexer lexer) {
        super(lexer);
    }
    public Header parse() throws ParseException {
           
	headerName(TokenTypes.TO);
	ToHeader to = new ToHeader();
	super.parse(to);
	this.lexer.match('\n');
	if (((Address)to.getAddress()).getAddressType() ==
		Address.ADDRESS_SPEC) {
		// the parameters are header parameters.
		if (to.getAddress().getURI() instanceof SipURI) {
		   SipURI sipUri = (SipURI) to.getAddress().getURI();
		   NameValueList parms = sipUri.getUriParms();
		   if (parms != null && ! parms.isEmpty()) {
		   	to.setParameters(parms);
		   	sipUri.removeUriParms();
		   }
		}
	}
        return to;
   }
    
    
    /**
    public static void main(String args[]) throws ParseException {
        String to[] = {
           "To: <sip:+1-650-555-2222@ss1.wcom.com;user=phone>;tag=5617\n",
           "To: T. A. Watson <sip:watson@bell-telephone.com>\n",
           "To: LittleGuy <sip:UserB@there.com>\n",
           "To: sip:mranga@120.6.55.9\n",
           "To: sip:mranga@129.6.55.9 ; tag=696928473514.129.6.55.9\n"
        };
        
        for (int i = 0; i < to.length; i++ ) {
            ToParser tp =
            new ToParser(to[i]);
            To t = (To) tp.parse();
            System.out.println("encoded = " + t.encode());
        }
        
    }
    */
    
    
    
}
