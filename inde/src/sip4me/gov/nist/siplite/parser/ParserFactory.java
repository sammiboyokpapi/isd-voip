package sip4me.gov.nist.siplite.parser;

import java.util.Hashtable;

import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.header.AuthorizationHeader;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.ContactHeader;
import sip4me.gov.nist.siplite.header.ContentLengthHeader;
import sip4me.gov.nist.siplite.header.ContentTypeHeader;
import sip4me.gov.nist.siplite.header.DateHeader;
import sip4me.gov.nist.siplite.header.EventHeader;
import sip4me.gov.nist.siplite.header.ExpiresHeader;
import sip4me.gov.nist.siplite.header.FromHeader;
import sip4me.gov.nist.siplite.header.MaxForwardsHeader;
import sip4me.gov.nist.siplite.header.ProxyAuthenticateHeader;
import sip4me.gov.nist.siplite.header.ProxyAuthorizationHeader;
import sip4me.gov.nist.siplite.header.RecordRouteHeader;
import sip4me.gov.nist.siplite.header.RouteHeader;
import sip4me.gov.nist.siplite.header.ToHeader;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.header.WWWAuthenticateHeader;


/** A factory class that does a name lookup on a registered parser and
* returns a header parser for the given name.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*@author Jean Deruelle <jeand@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public class ParserFactory {

    private static Hashtable parserTable; 
    private static Class[] constructorArgs;
    
    static {
		parserTable = new Hashtable();
				constructorArgs = new Class[1];
				constructorArgs[0] = new String().getClass();				
				
				parserTable.put("t", new ToParser().getClass());
				parserTable.put(ToHeader.NAME.toLowerCase(), new ToParser().getClass());

				parserTable.put(FromHeader.NAME.toLowerCase(), new FromParser().getClass());
				parserTable.put("f", new FromParser().getClass());

				parserTable.put(CSeqHeader.NAME.toLowerCase(), new CSeqParser().getClass());

				parserTable.put(ViaHeader.NAME.toLowerCase(), new ViaParser().getClass());
				parserTable.put("v", new ViaParser().getClass());

				parserTable.put(ContactHeader.NAME.toLowerCase(), new ContactParser().getClass());
				parserTable.put("m", new ContactParser().getClass());

				parserTable.put(
					ContentTypeHeader.NAME.toLowerCase(),
					new ContentTypeParser().getClass());
				parserTable.put("c", new ContentTypeParser().getClass());

				parserTable.put(
					ContentLengthHeader.NAME.toLowerCase(),
					new ContentLengthParser().getClass());
				parserTable.put("l", new ContentLengthParser().getClass());

				parserTable.put(
					AuthorizationHeader.NAME.toLowerCase(),
					new AuthorizationParser().getClass());

				parserTable.put(
					WWWAuthenticateHeader.NAME.toLowerCase(),
					new WWWAuthenticateParser().getClass());

				parserTable.put(CallIdHeader.NAME.toLowerCase(), new CallIDParser().getClass());
				parserTable.put("i", new CallIDParser().getClass());

				parserTable.put(RouteHeader.NAME.toLowerCase(), new RouteParser().getClass());

				parserTable.put(
					RecordRouteHeader.NAME.toLowerCase(),
					new RecordRouteParser().getClass());

				parserTable.put(DateHeader.NAME.toLowerCase(), new DateParser().getClass());

				parserTable.put(
					ProxyAuthorizationHeader.NAME.toLowerCase(),
					new ProxyAuthorizationParser().getClass());

				parserTable.put(
					ProxyAuthenticateHeader.NAME.toLowerCase(),
					new ProxyAuthenticateParser().getClass());
				
				parserTable.put(
					MaxForwardsHeader.NAME.toLowerCase(),
					new MaxForwardsParser().getClass());

				parserTable.put(ExpiresHeader.NAME.toLowerCase(), new ExpiresParser().getClass());
				
				parserTable.put(EventHeader.NAME.toLowerCase(), new EventParser().getClass());
				parserTable.put("o", new EventParser().getClass());

				/*parserTable.put(
					AuthenticationInfoHeader.NAME.toLowerCase(),
					AuthenticationInfoParser.getClass());*/				        

    }
    /** create a parser for a header. This is the parser factory.
     */
    public static HeaderParser createParser(String line) throws ParseException {
            String headerName=Lexer.getHeaderName(line);
            String headerValue=Lexer.getHeaderValue(line);
            if (headerName==null || headerValue ==null)
                throw new ParseException
                ("The header name or value is null",0);            
            Class parserClass =
            (Class) parserTable.get(headerName.toLowerCase());
            
            if (parserClass != null) { 
                    
                try {
                    
                    HeaderParser retval = 
                        (HeaderParser) parserClass.newInstance();
                    retval.setHeaderToParse(line);
                    return retval;

                   
                    
                } catch (Exception ex) {
		    InternalErrorHandler.handleException(ex);
		    return null; // to placate the compiler.
                }
                       
            } else {
                // Just generate a generic Header. We define
                // parsers only for the above.
                return new HeaderParser(line);
            }
            
    }



}
