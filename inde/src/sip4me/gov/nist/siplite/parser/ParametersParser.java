package sip4me.gov.nist.siplite.parser;

import sip4me.gov.nist.core.NameValue;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.ParametersHeader;


/** parameters parser header.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public abstract class ParametersParser  extends HeaderParser {

	protected ParametersHeader parametersHeader;

	protected ParametersParser() {} 

	protected ParametersParser(Lexer lexer) {
		super((Lexer)lexer);
	}

	protected ParametersParser(String buffer) {
		super(buffer);
	}

	protected void parse(ParametersHeader parametersHeader)
	throws ParseException {
		this.lexer.SPorHT();
		while (lexer.lookAhead(0) == ';') {
			this.lexer.consume(1);
			//this.lexer.match(';');
			// eat white space
			this.lexer.SPorHT();
			NameValue nv = nameValue();
			parametersHeader.setParameter(nv);
			// eat white space
			this.lexer.SPorHT();		   
		}
	}

}
