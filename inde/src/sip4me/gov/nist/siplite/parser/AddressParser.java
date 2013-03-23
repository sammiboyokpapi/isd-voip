package sip4me.gov.nist.siplite.parser;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.URI;

/** Parser for addresses.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public class AddressParser extends Parser {

	protected AddressParser(Lexer lexer) {
		this.lexer = lexer;
		this.lexer.selectLexer("charLexer");
	}

	
	public AddressParser(String address) {
		this.lexer = new Lexer("charLexer",address);
	}

	protected Address nameAddr() throws ParseException {
	    if (debug) dbg_enter("nameAddr");
	    try {
		if (this.lexer.lookAhead(0) == '<') {
		   this.lexer.match('<');
		   this.lexer.selectLexer("sip_urlLexer");
		   this.lexer.SPorHT();
		   URLParser uriParser = new URLParser((Lexer)lexer);
		   URI uri = uriParser.uriReference();
		   Address retval = new Address();
		   retval.setAddressType(Address.NAME_ADDR);
		   retval.setURI(uri);
		   this.lexer.SPorHT();
		   this.lexer.match('>');
		   return retval;
		} else {
		   Address addr = new Address();
		   addr.setAddressType(Address.NAME_ADDR);
		   String name = null;
		   if (this.lexer.lookAhead(0) == '\"')  {
		      name = this.lexer.quotedString();
		      this.lexer.SPorHT();
		   } else  name = this.lexer.getNextToken('<');
		   addr.setDisplayName(name.trim());
		   this.lexer.match('<');
		   this.lexer.SPorHT();
		   URLParser uriParser = new URLParser((Lexer)lexer);
		   URI uri = uriParser.uriReference();
		   addr.setAddressType(Address.NAME_ADDR);
		   addr.setURI(uri);
		   this.lexer.SPorHT();
		   this.lexer.match('>');
		   return addr;
		}
	     } finally {
		if (debug) dbg_leave("nameAddr");

	    }

	}

	public Address address() throws ParseException  {
	    if (debug) dbg_enter("address");
	    Address retval = null;
	    try {
		int k =0;
		while(lexer.hasMoreChars()) {
		   if (lexer.lookAhead(k) == '<' || 
			lexer.lookAhead(k) == '\"' ||
			lexer.lookAhead(k) == ':' ||
			lexer.lookAhead(k) == '/')break;
		   else if (lexer.lookAhead(k) == '\0') 
			throw createParseException("unexpected EOL");
		   else k++;
		}
		if (this.lexer.lookAhead(k) == '<' || 
		    this.lexer.lookAhead(k) == '\"' ) {
			retval =  nameAddr();
		} else if (this.lexer.lookAhead(k) == ':' || 
			this.lexer.lookAhead(k) == '/') {
			retval = new Address();
			URLParser uriParser = new URLParser((Lexer)lexer);
			URI uri = uriParser.uriReference();
			retval.setAddressType(Address.ADDRESS_SPEC);
			retval.setURI(uri);
		} else {
			throw createParseException("Bad address spec");
		}
		return retval;
	     } finally {
		if (debug) dbg_leave("address");
	     }

	}

/**
	public static void main(String[] args) throws ParseException {
	     String[] addresses = { 
		"<sip:user@example.com?Route=%3csip:sip.example.com%3e>",
		"\"M. Ranganathan\"   <sip:mranga@nist.gov>",
		"<sip:+1-650-555-2222@ss1.wcom.com;user=phone>",
		"M. Ranganathan <sip:mranga@nist.gov>" };

	    for (int i = 0; i < addresses.length; i++) {
                System.out.println("parsing " + addresses[i]);
	       	AddressParser addressParser = new AddressParser(addresses[i]);
		Address addr = addressParser.address();
		System.out.println("encoded = " + addr.encode());
	    }

	}
*/


}

