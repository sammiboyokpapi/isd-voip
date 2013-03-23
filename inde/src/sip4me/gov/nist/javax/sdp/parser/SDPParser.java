package sip4me.gov.nist.javax.sdp.parser;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.ParserCore;
import sip4me.gov.nist.javax.sdp.fields.SDPField;

/**
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public abstract class SDPParser extends ParserCore {

	public abstract SDPField parse() throws ParseException;

	protected void setField(String field) {
		this.lexer = new Lexer("charLexer", field);
	}

}


