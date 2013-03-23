package sip4me.gov.nist.javax.sdp.parser;
import java.util.Hashtable;

import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.ParseException;


/** Factory for creating parsers for the SDP stuff.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*/
public class ParserFactory {
	private static  Hashtable parserTable;
        private static Class[] constructorArgs;
	private static  
		final String packageName = "sip4me.gov.nist.javax.sdp.parser";

	private static Class getParser(String parserClass) {
		try {
		   return Class.forName(packageName + "." + parserClass);
		} catch (ClassNotFoundException ex) {
		   System.out.println("Could not find class");
		   ex.printStackTrace();
		   System.exit(0);
		   return null; // dummy
		}
	}

	static {
               constructorArgs = new Class[1];
		constructorArgs[0] = new String().getClass();
		parserTable = new Hashtable();
		parserTable.put("a",getParser("AttributeFieldParser"));
		parserTable.put("b",getParser("BandwidthFieldParser"));
		parserTable.put("c",getParser("ConnectionFieldParser"));
		parserTable.put("e",getParser("EmailFieldParser"));
		parserTable.put("i",getParser("InformationFieldParser"));
		parserTable.put("k",getParser("KeyFieldParser"));
		parserTable.put("m",getParser("MediaFieldParser"));
		parserTable.put("o",getParser("OriginFieldParser"));
		parserTable.put("p",getParser("PhoneFieldParser"));
		parserTable.put("v",getParser("ProtoVersionFieldParser"));
		parserTable.put("r",getParser("RepeatFieldParser"));
		parserTable.put("s",getParser("SessionNameFieldParser"));
		parserTable.put("t",getParser("TimeFieldParser"));
		parserTable.put("u",getParser("URIFieldParser"));
		parserTable.put("z",getParser("ZoneFieldParser"));
	}

	public static  SDPParser 
		createParser(String field) throws ParseException {
		String fieldName = Lexer.getFieldName(field);
		if (fieldName == null) return null;
                Class parserClass = 
			(Class) parserTable.get(fieldName.toLowerCase());
            
                if (parserClass != null) {
                   try {
                    
                    SDPParser retval = (SDPParser) parserClass.newInstance();
		    retval.setField(field);
                    return retval;
                    
                   } catch (Exception ex) {
		       InternalErrorHandler.handleException(ex);
		       return null; // to placate the compiler.
                   }
	        }  else throw new ParseException
			("Could not find parser for " + fieldName,0);
	 }
                       

}
