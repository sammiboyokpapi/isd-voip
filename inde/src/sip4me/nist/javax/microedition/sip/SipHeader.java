/*
 * Created on Jan 28, 2004
 */
package sip4me.nist.javax.microedition.sip;

import java.util.Vector;

import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.microedition.sip.StackConnector;
import sip4me.gov.nist.siplite.header.Header;



/**
 * SipHeader provides generic SIP header parser helper. This class can be used 
 * to parse bare String header values that are read from SIP message using e.g. 
 * SipConnection.getHeader() method. It should be noticed that SipHeader 
 * is separate helper class and not mandatory to use for creating SIP connections. 
 * Correspondingly, SIP headers can be constructed with this class.
 * SipHeader uses generic format to parse the header value and parameters 
 * following the syntax given in RFC 3261[1] p.31
 * •field-name: field-value *(;parameter-name=parameter-value)
 * SipHeader also supports parsing for the following authorization and 
 * authentication headers: WWW-Authenticate, Proxy-Authenticate, 
 * Proxy-Authorization, Authorization. 
 * For those a slightly different syntax is applied, where comma is used as the 
 * parameter separator instead of semicolon. 
 * Authentication parameters are accessible through get/set/removeParameter() methods 
 * in the same way as generic header parameters. 
 * •auth-header-name: auth-scheme LWS auth-param *(COMMA auth-param)
 * The ABNF for the SipHeader parser is derived from SIP ABNF as follows:
 *      header         =  header-name “:” header-value *(“;” generic-param) /                        
 * 														WWW-Authenticate / 
 * 														Proxy-Authenticate /
 * 								                        Proxy-Authorization / 
 * 														Authorization      
 * 		header-name    =  token      
 * 		generic-param  =  token [ EQUAL gen-value ]      
 * 		gen-value      =  token / host / quoted-string      
 * 		header-value   =  1*(chars) name-addr      
 * 		chars          =  %x20-3A / “=” %x3F-7E                             
 * 						 ; any visible character except “;” “<” “>” 
 * Reference, SIP 3261 [1] p.159 Header Fields and p.219 SIP BNF for terminals 
 * not defined in this BNF.
 * Example headers: 
 * 	Call-ID: a84b4c76e66710 
 * 		Call-ID header with no parameters 
 *  From: Bob <sip:bob@biloxi.com>;tag=a6c85cf 
 * 		From header with parameter ’tag’ 
 *  Contact: <sip:alice@pc33.atlanta.com> 
 * 		Contact header with no parameters
 *  Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bKhjhs8ass877 
 * 		Via header with parameter ’branch’ 
 *  Contact: “Mr. Watson” <sip:watson@worcester.bell-telephone.com>;q=0.7;expires=3600 
 * 		Contact header with parameters ’q’ and ’expires’ 
 *  WWW-Authenticate: Digest realm=“atlanta.com”, domain=“sip:boxesbybob.com”, qop=“auth”, nonce=“f84f1cec41e6cbe5aea9c8e88d359”, opaque=“”, stale=FALSE, algorithm=MD5 
 * 		WWW-Authenticate header with digest authentication scheme.
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SipHeader {
	/** constant ERROR_INFO field.
		 */
	public static final String ERROR_INFO = "Error-Info"; 
    
		 /** constant MIME_VERSION field.
		 */
	public static final String MIME_VERSION="Mime-Version";
    
		 /** constant IN_REPLY_TO field.
		 */
	public static final String IN_REPLY_TO="In-Reply-To";
    
		 /** constant ALLOW field.
		 */
	public static final String ALLOW="Allow";
    
		 /** constant CONTENT_LANGUAGE field.
		 */
	public static final String CONTENT_LANGUAGE="Content-Language";
    
		 /** constant CALL_INFO field.
		 */
	public static final String CALL_INFO="Call-Info";
    
		 /** constant CSEQ field.
		 */
	public static final String CSEQ="CSeq";
    
		 /** constant ALERT_INFO field.
		 */
	public static final String ALERT_INFO="Alert-Info";
    
		 /** constant ACCEPT_ENCODING field.
		 */
	public static final String ACCEPT_ENCODING="Accept-Encoding";
    
		 /** constant ACCEPT field.
		 */
	public static final String ACCEPT="Accept";
    
		 /** constant ENCRYPTION field.
		 */
	public static final String ENCRYPTION="Encryption";
    
		 /** constant ACCEPT_LANGUAGE field.
		 */
	public static final String ACCEPT_LANGUAGE="Accept-Language";
    
		 /** constant RECORD_ROUTE field.
		 */
	public static final String RECORD_ROUTE="Record-Route";
    
		 /** constant TIMESTAMP field.
		 */
	public static final String TIMESTAMP="Timestamp";
    
		 /** constant TO field.
		 */
	public static final String TO="To";
    
		 /** constant VIA field.
		 */
	public static final String VIA="Via";
    
		 /** constant  FROM field.
		 */
	public static final String FROM="From";
    
		 /** constant CALL_ID field.
		 */
	public static final String CALL_ID="Call-Id";
    
		 /** constant AUTHORIZATION field.
		 */
	public static final String AUTHORIZATION="Authorization";
    
		 /** constant PROXY_AUTHENTICATE field.
		 */
	public static final String PROXY_AUTHENTICATE="Proxy-Authenticate";
    
		 /** constant SERVER field.
		 */
	public static final String SERVER="Server";
    
		 /** constant UNSUPPORTED field.
		 */
	public static final String UNSUPPORTED="Unsupported";
    
		 /** constant RETRY_AFTER field.
		 */
	public static final String RETRY_AFTER="Retry-After";
    
		 /** constant CONTENT_TYP field.
		 */
	public static final String CONTENT_TYPE="Content-Type";

		 /** constant CONTENT_ENCODING field.
		 */
		public static final String CONTENT_ENCODING="Content-Encoding";
    
		/** constant CONTENT_LENGTH field.
		 */
	public static final String CONTENT_LENGTH="Content-Length";
    
		/** constant  HIDE field.
		 */
	public static final String HIDE="Hide";

		/** constant ROUTE field.
		 */
		public static final String ROUTE="Route";

		/** constant CONTACT field.
		 */
		public static final String CONTACT="Contact";
    
		/** constant WWW_AUTHENTICATE field.
		 */
	public static final String WWW_AUTHENTICATE="WWW-Authenticate";
    
		/** constant MAX_FORWARDS field.
		 */
	public static final String MAX_FORWARDS="Max-Forwards";
    
		/** constant ORGANIZATION field.
		 */
	public static final String ORGANIZATION="Organization";
    
		/** constant PROXY_AUTHORIZATION field.
		 */
	public static final String PROXY_AUTHORIZATION="Proxy-Authorization";
    
		/** constant PROXY_REQUIRE field.
		 */
	public static final String PROXY_REQUIRE="Proxy-Require";
    
		/** constant REQUIRE  field.
		 */
	public static final String REQUIRE="Require";
    
		/** constant CONTENT_DISPOSITION field.
		 */
	public static final String CONTENT_DISPOSITION="Content-Disposition";
    
		 /** constant SUBJECT field.
		 */
	public static final String SUBJECT="Subject";
    
		/** constant USER_AGENT field.
		 */
	public static final String USER_AGENT="User-Agent";
    
		/** constant WARNING field.
		 */
	public static final String WARNING="Warning";
    
		/** constant PRIORITY field.
		 */
	public static final String PRIORITY="Priority";
    
		/** constant DATE field.
		 */
	public static final String DATE="Date";
    
		/** constant EXPIRES field.
		 */
	public static final String EXPIRES="Expires";
    
		/** constant RESPONSE_KEY field.
		 */
	public static final String RESPONSE_KEY="Response-Key";
    
		/** constant WARN_AGENT field.
		 */
	public static final String WARN_AGENT="Warn-Agent";
    
		/** constant SUPPORTED field.
		 */
	public static final String SUPPORTED = "Supported";

	/**
	 * Constant EVENT field
	 */
	public static final String EVENT = "Event";
	
	/** constant P_ASSOCIATED_URI field.
     */
	public static final String P_ASSOCIATED_URI = "P-Associated-URI";
	
	/** constant P_PREFERRED_IDENTITY field.
     */
	public static final String P_PREFERRED_IDENTITY = "P-Preferred-Identity";

	
	/**
	 * the nist-siplite corresponding header
	 */
	private Header header=null;
	
	/**
	 * Constructs a SipHeader from name value pair. For example:
	 * name = Contact
	 * value = <sip:UserB@192.168.200.201>;expires=3600
	 * @param name - name of the header (Contact, Call-ID, ...)
	 * @param value - full header value as String
	 * @throws IllegalArgumentException - if the header value or name are invalid
	 */	 
	public SipHeader(String name, String value) 
		   				throws IllegalArgumentException {
		try{
			StackConnector.headerFactory.createHeader(name,value);
		}
		catch(ParseException pe){
			throw new IllegalArgumentException(pe.getMessage());
		}
	}
	
	/**
	 * Sets the header name, for example Contact
	 * @param name - Header name
	 * @throws IllegalArgumentException - if the name is invalid
	 */
	public void setName(java.lang.String name)
			    throws IllegalArgumentException {
		header.setHeaderName(name);				    	
	}
	
	/**
	 * Returns the name of this header
	 * @return the name of this header as String
	 */
	public java.lang.String getName() {
		return header.getName();
	}
	
	/**
	 * Returns the full header value including parameters. 
	 * For example “Alice <sip:alice@atlanta.com>;tag=1928301774”
	 * @return full header value including parameters
	 */
	public java.lang.String getValue(){
		return header.getValue().toString();
	}
	
	/**
	 * Returns the header value without header parameters. 
	 * For example for header <sip:UserB@192.168.200.201>;expires=3600 method 
	 * returns <sip:UserB@192.168.200.201>
	 * In the case of an authorization or authentication header getValue() 
	 * returns only the authentication scheme e.g. “Digest”.
	 * @return header value without header parameters	 
	 */
	public java.lang.String getHeaderValue(){
		return header.getHeaderValue();
	}
	
	/**
	 * Sets the header value as String without parameters. 
	 * For example “<sip:UserB@192.168.200.201>”. 
	 * The existing (if any) header parameter values are not modified. 
	 * For the authorization and authentication header this method sets 
	 * the authentication scheme e.g. “Digest”.
	 * @param value - the header value
	 * @throws IllegalArgumentException - if the value is invalid or there is 
	 * parameters included.
	 */
	public void setValue(java.lang.String value)
			    		throws IllegalArgumentException {
		header.setHeaderValue(value);	    	
	}
	
	/**
	 * Returns the value of one header parameter. 
	 * For example, from value “<sip:UserB@192.168.200.201>;expires=3600” 
	 * the method call getParameter(“expires”) will return “3600”.
	 * @param name - name of the header parameter
	 * @return value of header parameter. returns empty string for a parameter 
	 * without value and null if the parameter does not exist.
	 */
	public java.lang.String getParameter(java.lang.String name){
		NameValueList parameterList=header.getParameters();
		return parameterList.getParameter(name);	
	}
	
	/**
	 * Returns the names of header parameters. Returns null if there are no 
	 * header parameters.
	 * @return names of the header parameters. Returns null if there are no parameters.
	 */
	public java.lang.String[] getParameterNames(){
		NameValueList parameterList=header.getParameters();		
		Vector parameterNameList=parameterList.getNames();		
		String parameterNames[]=new String[parameterNameList.size()];
		for(int i=0;i<parameterList.size();i++)
			parameterNames[i]=(String)parameterNameList.elementAt(i);
		return parameterNames;
	}
	
	/**
	 * Sets value of header parameter. If parameter does not exist it will be added. 
	 * For example, for header value “<sip:UserB@192.168.200.201>” calling 
	 * setParameter(“expires”, “3600”) will construct header value 
	 * “<sip:UserB@192.168.200.201>;expires=3600”.
	 * If the value is null, the parameter is interpreted as a parameter 
	 * without value.
	 * @param name - name of the header parameter
	 * @param value - value of the parameter
	 * @throws IllegalArgumentException - if the parameter name or value are invalid
	 */
	public void setParameter(java.lang.String name, java.lang.String value)
				throws IllegalArgumentException {
		NameValueList parameterList=header.getParameters();
		parameterList.add(name,value);		
	}
	
	/**
	 * Removes the header parameter, if it is found in this header.
	 * @param name - name of the header parameter
	 */
	public void removeParameter(java.lang.String name){
		NameValueList parameterList=header.getParameters();		
		parameterList.delete(name);
	}
	
	/**
	 * Returns the String representation of the header according to header type. 
	 * For example: 
	 * 		•From: Alice <sip:alice@atlanta.com>;tag=1928301774
	 * 		•WWW-Authenticate: Digest realm=“atlanta.com”, domain=“sip:boxesbybob.com”, qop=“auth”, nonce=“f84f1cec41e6cbe5aea9c8e88d359”, opaque=“”, stale=FALSE, algorithm=MD5
	 */
	public java.lang.String toString(){
		return header.toString();
	}
}