/*
 * DigestClientAuthentication.java
 *
 * Created on January 7, 2003, 10:45 AM
 */

package sip4me.gov.nist.siplite.stack.authentication;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.siplite.AuthenticationHelper;
import sip4me.gov.nist.siplite.SIPUtils;
import sip4me.gov.nist.siplite.SipStack;
import sip4me.gov.nist.siplite.header.AuthorizationHeader;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.ProxyAuthenticateHeader;
import sip4me.gov.nist.siplite.header.ProxyAuthorizationHeader;
import sip4me.gov.nist.siplite.header.WWWAuthenticateHeader;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;



/**
 *
 * @author olivier deruelle
 * @author Jean Deruelle
 */
public class DigestClientAuthentication implements AuthenticationHelper{
    
    private String realm;
    private String uri;
    private String nonce;
    private String method;
    private String algorithm;
    private String qop;
    private String opaque;
    
	private int requestCounter; // nc in digest headers
	private Random nonceGenerator;
	private String cnonce;
   
    private Vector credentials;
  
    public DigestClientAuthentication(Vector credentials) {
    	this.credentials=credentials;
    	requestCounter = 0x01; 
    	nonceGenerator = new Random(System.currentTimeMillis());
    	
        /*try{
            rs=RecordStore.openRecordStore("pass",true);   
        }
        catch(Exception e) {
            if (LogWriter.needsLogging)
                  LogWriter.logMessage("DigestClientAuthentication, exception raised:",e.getMessage());
        }*/        
    }
    
     /**
     * Creates a new ProxyAuthorizationHeader based on the newly supplied 
     * scheme value.
     *
     * @param scheme - the new string value of the scheme.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the scheme value.
     * @return the newly created ProxyAuthorizationHeader object.
     */
    public ProxyAuthorizationHeader createProxyAuthorizationHeader
			(String scheme) throws ParseException {
		if (scheme == null) 
			throw new NullPointerException("bad scheme arg");
        ProxyAuthorizationHeader p=new ProxyAuthorizationHeader();
        p.setScheme(scheme);
        
        return p;
    }
    
       /**
     * Creates a new AuthorizationHeader based on the newly supplied 
     * scheme value.
     *
     * @param scheme - the new string value of the scheme.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the scheme value.
     * @return the newly created AuthorizationHeader object.
     */
    public AuthorizationHeader createAuthorizationHeader(String scheme)
                                    throws ParseException {
            if ( scheme==null)
            	throw new NullPointerException ("null arg scheme "); 
             AuthorizationHeader auth=new AuthorizationHeader();
             auth.setScheme(scheme);
             
             return auth;
    }
    
    
     /**
     * to hex converter
     */
    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6',
    '7', '8', '9', 'a', 'b', 'c', 'd',
    'e', 'f' };
    
    /**
     * convert an array of bytes to an hexadecimal string
     * @return a string
     * @param b bytes array to convert to a hexadecimal
     * string
     */
    
    public static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length*2];
        for (int i=0; i< b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }
    
    
    public Request createNewRequest(SipStack sipStack, Request originalRequest,
			Response response) {

		if (LogWriter.needsLogging)
			LogWriter
					.logMessage("Creating new authenticated request with credentials");

		try {

			Request newRequest = (Request) originalRequest.clone();
			CSeqHeader cseqHeader = newRequest.getCSeqHeader();
			cseqHeader.setSequenceNumber(cseqHeader.getSequenceNumber() + 1);

			// Create new from tag, we want this request to generate 
			// a new dialog and therefore the dialogID has to be different
			newRequest.getFromHeader().setTag(SIPUtils.generateTag());
			
			// Proxy-Authenticate header:
			ProxyAuthenticateHeader proxyAuthHeader = (ProxyAuthenticateHeader) response
					.getHeader(ProxyAuthenticateHeader.NAME);

			// WWWAuthenticate header:
			WWWAuthenticateHeader wwwAuthenticateHeader = (WWWAuthenticateHeader) response
					.getHeader(WWWAuthenticateHeader.NAME);

			// Cseq header:
			cseqHeader = response.getCSeqHeader();
			method = cseqHeader.getMethod();

			uri = originalRequest.getRequestURI().encode();

			if (proxyAuthHeader == null) {
				if (wwwAuthenticateHeader == null) {
					if (LogWriter.needsLogging)
						LogWriter
								.logMessage("DigestClientAuthentication, "
										+ " ERROR: No ProxyAuthenticate header or WWWAuthenticateHeader in the response!");
					return null;
				}

				algorithm = wwwAuthenticateHeader.getAlgorithm();
				if (algorithm == null) {
					// RFC 2617 mandates MD5 by default
					algorithm = "MD5";
				}
				nonce = wwwAuthenticateHeader.getNonce();
				realm = wwwAuthenticateHeader.getRealm();
				if (realm == null) {
					if (LogWriter.needsLogging)
						LogWriter
								.logMessage("DigestClientAuthentication, "
										+ "ERROR: 'realm' not found on the 401 response!");
					return null;
				}
				opaque = wwwAuthenticateHeader.getOpaque();
				qop = wwwAuthenticateHeader.getParameter("qop");
			} else {

				algorithm = proxyAuthHeader.getAlgorithm();
				if (algorithm == null) {
					// RFC 2617 mandates MD5 by default
					algorithm = "MD5";
				}

				nonce = proxyAuthHeader.getNonce();
				realm = proxyAuthHeader.getRealm();
				if (realm == null) {
					if (LogWriter.needsLogging)
						LogWriter
								.logMessage("DigestClientAuthentication, "
										+ "ERROR: 'realm' not found on the 407 response!");
					return null;
				}
				opaque = proxyAuthHeader.getOpaque();
				qop = proxyAuthHeader.getParameter("qop");
			}

			Credentials credentials = getCredentials(realm);
			if (credentials == null) {
				if (LogWriter.needsLogging)
					LogWriter
							.logMessage("DigestClientAuthentication, "
									+ " ERROR: Credentials not properly set for Digest authentication!");
				return null;
			}

			if (proxyAuthHeader == null) {
				AuthorizationHeader header = createAuthorizationHeader("Digest");
				header.setParameter("username", credentials.getUserName());
				header.setParameter("realm", realm);
				header.setParameter("uri", uri);
				header.setParameter("algorithm", algorithm);
				if (opaque != null)
					header.setParameter("opaque", opaque);
				header.setParameter("nonce", nonce);

				// cnonce and nc are only mandatory if qop is present
				if (qop != null) {
					cnonce = Utils.toHexString(nonceGenerator.nextDouble())
							+ Utils.toHexString(nonceGenerator.nextDouble());
					header.setParameter("cnonce", cnonce);
					header.setParameter("qop", qop);
					header
							.setParameter("nc", Utils
									.toHexString(requestCounter));
				} else {
					cnonce = null;
					requestCounter = 0x00;
				}

				String digestResponse = generateResponse(credentials
						.getUserName(), credentials.getPassword());
				if (digestResponse == null) {
					if (LogWriter.needsLogging)
						LogWriter
								.logMessage(LogWriter.TRACE_MESSAGES, "DigestClientAuthentication, "
										+ "the digest response is null for the Authorization header!");
					return null;
				}
				requestCounter++;
				header.setParameter("response", digestResponse);
				newRequest.setHeader(header);
				return newRequest;
			} else {
				ProxyAuthorizationHeader header = createProxyAuthorizationHeader("Digest");
				header.setParameter("username", credentials.getUserName());
				header.setParameter("realm", realm);
				header.setParameter("uri", uri);
				header.setParameter("algorithm", algorithm);
				if (opaque != null)
					header.setParameter("opaque", opaque);
				header.setParameter("nonce", nonce);

				// cnonce and nc are only mandatory if qop is present
				if (qop != null) {
					cnonce = Utils.toHexString(nonceGenerator.nextDouble())
							+ Utils.toHexString(nonceGenerator.nextDouble());
					header.setParameter("cnonce", cnonce);
					header.setParameter("qop", qop);
					header
							.setParameter("nc", Utils
									.toHexString(requestCounter));
				} else {
					cnonce = null;
					requestCounter = 0x00;
				}

				String digestResponse = generateResponse(credentials
						.getUserName(), credentials.getPassword());
				if (digestResponse == null) {
					if (LogWriter.needsLogging)
						LogWriter
								.logMessage(LogWriter.TRACE_MESSAGES,"DigestClientAuthentication, "
										+ "the digest response is null for the ProxyAuthorization header!");
					return null;
				}
				requestCounter++;
				header.setParameter("response", digestResponse);
				newRequest.setHeader(header);
				return newRequest;
			}
		} catch (Exception ex) {
			if (LogWriter.needsLogging) {
				LogWriter.logMessage(LogWriter.TRACE_EXCEPTION,
						"DigestClientAuthentication, createNewRequest()"
								+ " exception raised:");
				LogWriter.logException(ex);
			}
			return null;
		}
	}
    
     /** 
      * generate the response
      */
    public String generateResponse(String userName,String password) {
        if (userName == null) {
            if (LogWriter.needsLogging)
                LogWriter.logMessage("DigestClientAuthentication, generateResponse(): "+
                "ERROR: no userName parameter");
            return null;
        }
        if (realm == null) {
            if (LogWriter.needsLogging)
                LogWriter.logMessage("DigestClientAuthentication, generateResponse(): "+
                "ERROR: no realm parameter");
            return  null;
        }
        
        if (LogWriter.needsLogging)
            LogWriter.logMessage("DigestClientAuthentication, generateResponse(): "+
             "Trying to generate a response for the user: "+userName+" , with "+
             "the realm: "+ realm);
         
         if (password == null)  {
             if (LogWriter.needsLogging)
                 LogWriter.logMessage("DigestClientAuthentication, generateResponse(): "+
                 "ERROR: no password parameter");
             return null;
         }
         if (method == null)  {
             if (LogWriter.needsLogging)
                 LogWriter.logMessage("DigestClientAuthentication, generateResponse(): "+
                 "ERROR: no method parameter");
             return null;
         }
         if (uri== null)  {
             if (LogWriter.needsLogging)
                 LogWriter.logMessage("DigestClientAuthentication, generateResponse(): "+
                 "ERROR: no uri parameter");
             return null;
         }
         if (nonce== null)  {
             if (LogWriter.needsLogging)
                 LogWriter.logMessage("DigestClientAuthentication, generateResponse(): "+
                 "ERROR: no nonce parameter");
             return null;
         }
        
         
         /*******    GENERATE RESPONSE      ************************************/
         if (LogWriter.needsLogging) {
             LogWriter.logMessage("DigestClientAuthentication, generateResponse(), userName:"+userName+"!");
             LogWriter.logMessage("DigestClientAuthentication, generateResponse(), realm:"+realm+"!");
             LogWriter.logMessage("DigestClientAuthentication, generateResponse(), password:"+password+"!");
             LogWriter.logMessage("DigestClientAuthentication, generateResponse(), uri:"+uri+"!");
             LogWriter.logMessage("DigestClientAuthentication, generateResponse(), nonce:"+nonce+"!");
             LogWriter.logMessage("DigestClientAuthentication, generateResponse(), method:"+method+"!");
         }
         
         
 		String A1 = userName + ":" + realm + ":" +  password ;
        String A2 = method.toUpperCase() + ":" + uri ;
//        System.out.println("A1: " + A1);
//        System.out.println("A2: " + A2);
        byte mdbytes[] = Utils.digest(A1.getBytes());
        String HA1 = toHexString(mdbytes);
//        System.out.println("HA1: " + HA1);

        mdbytes = Utils.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);
//        System.out.println("HA2: " + HA2);

        String KD = HA1 + ":" + nonce;
        if (qop != null) {
        	KD += ":" + Utils.toHexString(requestCounter);
            KD += ":" + cnonce;
            KD += ":" + qop;
        }
        KD += ":" + HA2;
//        System.out.println("KD: " + KD);
        mdbytes = Utils.digest(KD.getBytes());
        String response = toHexString(mdbytes);
//        System.out.println("RESPONSE: " + response);
         
        
         if (LogWriter.needsLogging)
             LogWriter.logMessage("DigestClientAuthentication, generateResponse():"+
             " response generated: "+response);
         
         return response;
    }
    
    
    public Credentials getCredentials(String realm) {
    	Enumeration e=credentials.elements();
    	while(e.hasMoreElements()){
    		Credentials credentials=(Credentials)e.nextElement();
    		if(credentials.getRealm().equals(realm))
    			return credentials;	
    	}
    	return null;
    }
    
    /**
     * digest auth test
     * @param args
     */
    /*public static void main(String args[]) {
    	
//    	http://en.wikipedia.org/wiki/Digest_authentication#Example_with_explanation
    	String method = "GET"; 
    	String nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
    	int nonceCount = 0x01;
    	String cnonce = "0a4f113b";
    	String password = "Circle Of Life";
        String userName = "Mufasa";
        String theRealm = "testrealm@host.com";
        String uri = "/dir/index.html";
        String theQop = "auth";

		String A1 = userName + ":" + theRealm+ ":" +  password ;
        String A2 = method.toUpperCase() + ":" + uri ;
        byte mdbytes[] = Utils.digest(A1.getBytes());
        String HA1 = toHexString(mdbytes);
        System.out.println("HA1: " + HA1);

        mdbytes = Utils.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);
        System.out.println("HA2: " + HA2);

        String KD = HA1 + ":" + nonce;
        if (theQop != null) {
        	KD += ":" + Utils.toHexString(nonceCount);
            KD += ":" + cnonce;
            KD += ":" + theQop;
        }
        KD += ":" + HA2;
        System.out.println("KD: " + KD);
        mdbytes = Utils.digest(KD.getBytes());
        String response = toHexString(mdbytes);
        System.out.println("RESPONSE: " + response);
    }*/
    
    
    
}
