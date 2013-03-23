package sip4me.gov.nist.siplite.header;

import java.util.Hashtable;

/** A mapping class that returns the Header for a given header name.
*/
public class NameMap {
	static Hashtable nameMap;
	
	static {
		initializeNameMap();		
	}

	protected static void putNameMap(String headerName, Class clazz) {
		nameMap.put(headerName.toLowerCase(),clazz);
	}

	public static Class getClassFromName(String headerName) {
		return (Class) nameMap.get(headerName.toLowerCase());		
	}


	public static boolean isHeaderSupported(String headerName) {
		return nameMap.containsKey(headerName);
        }


	private static void initializeNameMap() {
		nameMap = new Hashtable();

		putNameMap( Header.CSEQ, CSeqHeader.clazz); //1
	
		putNameMap( Header.RECORD_ROUTE, RecordRouteHeader.clazz); //2
	
		putNameMap( Header.VIA, ViaHeader.clazz); //3

		putNameMap( Header.FROM,  FromHeader.clazz); //4
		
		putNameMap( Header.CALL_ID, CallIdHeader.clazz); //5

		putNameMap( Header.MAX_FORWARDS, MaxForwardsHeader.clazz); //6


		putNameMap( Header.PROXY_AUTHENTICATE, 
				ProxyAuthenticateHeader.clazz); //7

		putNameMap( Header.CONTENT_TYPE,  ContentTypeHeader.clazz); //8

		putNameMap( Header.CONTENT_LENGTH, ContentLengthHeader.clazz); //9

		putNameMap( Header.ROUTE, RouteHeader.clazz); //10

		putNameMap( Header.CONTACT, ContactHeader.clazz); //11

		putNameMap( Header.WWW_AUTHENTICATE, 
			 WWWAuthenticateHeader.clazz); //12

		putNameMap( Header.PROXY_AUTHORIZATION,
			ProxyAuthorizationHeader.clazz); //13

		putNameMap( Header.DATE, DateHeader.clazz); //14

		putNameMap( Header.EXPIRES, ExpiresHeader.clazz); //15

		putNameMap( Header.AUTHORIZATION, AuthorizationHeader.clazz); //16
		
		// Fix by ArnauVP (Genaker)
		putNameMap( Header.EVENT, EventHeader.clazz); //17
	
		putNameMap( Header.TO,  ToHeader.clazz); //18 
		
	}

}
