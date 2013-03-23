
package sip4me.gov.nist.javax.sdp.parser;
import sip4me.gov.nist.core.LexerCore;

public class Lexer extends LexerCore {
    public Lexer(String lexerName, String buffer) {
	super(lexerName,buffer);

    }

    

    public void selectLexer(String lexerName) {}

    public static String getFieldName(String line) {
	int i = line.indexOf("=");
	if (i == -1 ) return null;
	else return  line.substring(0,i);
    }


}

