package sip4me.gov.nist.siplite.parser;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.header.AddressParametersHeader;

/** Address parameters parser.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
class AddressParametersParser  extends ParametersParser {

	protected AddressParametersHeader addressParametersHeader;

	protected AddressParametersParser(Lexer lexer) {
		super(lexer);
	}

	protected AddressParametersParser(String buffer) {
		super(buffer);
	}

	protected AddressParametersParser() {}

	protected void parse(AddressParametersHeader addressParametersHeader)
			throws ParseException {
		dbg_enter("AddressParametersParser.parse");
		try {
			this.addressParametersHeader = addressParametersHeader;
			AddressParser addressParser = new AddressParser(this.getLexer());
			Address addr = addressParser.address();
			addressParametersHeader.setAddress(addr);
			super.parse(addressParametersHeader);
		} finally {
			dbg_leave("AddressParametersParser.parse");
		}

	}

}
	
