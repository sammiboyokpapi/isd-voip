
package sip4me.gov.nist.siplite.parser;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Token;
import sip4me.gov.nist.siplite.header.ContentTypeHeader;
import sip4me.gov.nist.siplite.header.Header;

/** Parser for content type header.
*
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
 */
public class ContentTypeParser extends ParametersParser {

	ContentTypeParser(){}

       public  ContentTypeParser(String contentType) {
		super(contentType);
       }

        protected ContentTypeParser(Lexer lexer) {
		super(lexer);
	}
	
	public  Header  parse() throws ParseException {
            
            ContentTypeHeader contentType= new ContentTypeHeader();
	    if (debug) dbg_enter("ContentTypeParser.parse");
            
            try{
		this.headerName(TokenTypes.CONTENT_TYPE);
                
                // The type:
                lexer.match(TokenTypes.ID);
                Token type= lexer.getNextToken();
                this.lexer.SPorHT();
                contentType.setContentType(type.getTokenValue());
               
                
                // The sub-type:
                lexer.match('/');
                lexer.match(TokenTypes.ID);
                Token subType= lexer.getNextToken();
                this.lexer.SPorHT();
                contentType.setContentSubType(subType.getTokenValue());
		super.parse(contentType);
		this.lexer.match('\n');
            }  finally {
		if (debug) dbg_leave("ContentTypeParser.parse");
	    }
            return contentType;
            
        }
        
/**
        public static void main(String args[]) throws ParseException {
		String content[] = {
			"c: text/html; charset=ISO-8859-4\n",
			"Content-Type: text/html; charset=ISO-8859-4\n",
			"Content-Type: application/sdp\n",
                        "Content-Type: application/sdp; o=we ;l=ek ; i=end \n"
                };
			
		for (int i = 0; i < content.length; i++ ) {
		    ContentTypeParser cp = 
			  new ContentTypeParser(content[i]);
		    ContentTypeHeader c = (ContentType) cp.parse();
		    System.out.println("encoded = " + c.encode());
		}
			
	}
**/
	
       

}

