/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.message;
import java.util.Vector;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.address.URI;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.ContentTypeHeader;
import sip4me.gov.nist.siplite.header.FromHeader;
import sip4me.gov.nist.siplite.header.MaxForwardsHeader;
import sip4me.gov.nist.siplite.header.StatusLine;
import sip4me.gov.nist.siplite.header.ToHeader;
import sip4me.gov.nist.siplite.parser.StringMsgParser;

/**
*  Message Factory implementation 
* 
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  
*@author Olivier Deruelle <deruelle@nist.gov><br/>
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class MessageFactory {
	
	// Borrowed from JAIN SIP 1.2
	private static String defaultContentEncodingCharset = "UTF-8";

    
    /** Creates a new instance of MessageFactoryImpl */
    public MessageFactory() {
    }
    
        /**
     * Creates a new Request message of type specified by the method paramater,
     * containing the URI of the Request, the mandatory headers of the message
     * with a body in the form of a Java object and the body content type.
     *
     * @param requestURI - the new URI object of the requestURI value of this Message.
     * @param method - the new string of the method value of this Message.
     * @param callId - the new CallIdHeader object of the callId value of this Message.
     * @param cSeq - the new CSeqHeader object of the cSeq value of this Message.
     * @param from - the new FromHeader object of the from value of this Message.
     * @param to - the new ToHeader object of the to value of this Message.
     * @param via - the new Vector object of the ViaHeaders of this Message.
     * @param content - the new Object of the body content value of this Message.
     * @param contentType - the new ContentTypeHeader object of the content type 
     * value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the method or the body.
     */
    public Request createRequest(URI requestURI, 
		String method, CallIdHeader 
            	callId, CSeqHeader cSeq, FromHeader from, 
	        ToHeader to, Vector via,
                MaxForwardsHeader maxForwards, 
		ContentTypeHeader contentType, Object content) 
	    throws ParseException {
           if ( requestURI==null  ||
                method==null 	  ||
                callId==null 	  ||
                cSeq==null 	  ||
                from==null 	  ||
                to==null 	  ||
                via==null 	  ||
                maxForwards==null ||
                content==null 	  ||
                contentType==null) 
               throw new NullPointerException
		("Null parameters");
           
          Request sipRequest=new Request();
          sipRequest.setRequestURI(requestURI);
          sipRequest.setMethod(method);
          sipRequest.setCallId(callId);
          sipRequest.setHeader(cSeq);
          sipRequest.setHeader(from);   
          sipRequest.setHeader(to);
          sipRequest.setVia(via);
          sipRequest.setHeader(maxForwards);
          sipRequest.setContent(content,contentType);
          
          return sipRequest;
    }

    /**
     * Creates a new Request message of type specified by the method paramater,
     * containing the URI of the Request, the mandatory headers of the message
     * with a body in the form of a byte array and body content type.
     *
     * @param requestURI - the new URI object of the 
     * requestURI value of this Message.
     * @param method - the new string of the method value of this Message.
     * @param callId - the new CallIdHeader object of the callId value of this Message.
     * @param cSeq - the new CSeqHeader object of the cSeq value of this Message.
     * @param from - the new FromHeader object of the from value of this Message.
     * @param to - the new ToHeader object of the to value of this Message.
     * @param via - the new Vector object of the ViaHeaders of this Message.
     * @param content - the new byte array of the body content value of this Message.
     * @param contentType - the new ContentTypeHeader object of the content type 
     * value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the method or the body.
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader 
            callId, CSeqHeader cSeq, FromHeader from, ToHeader to, Vector via,
                MaxForwardsHeader maxForwards, byte[] content, ContentTypeHeader 
                    contentType) throws ParseException {
          if ( requestURI==null ||
                method==null ||
                callId==null ||
                cSeq==null ||
                from==null ||
                to==null ||
                via==null ||
                maxForwards==null ||
                content==null ||
                contentType==null) 
               throw new ParseException("JAIN-SIP Exception, some parameters are missing"+
               ", unable to create the request",0);
           
          Request sipRequest=new Request();
          sipRequest.setRequestURI(requestURI);
          sipRequest.setMethod(method);
          sipRequest.setCallId(callId);
          sipRequest.setHeader(cSeq);
          sipRequest.setHeader(from);   
          sipRequest.setHeader(to);
          sipRequest.setVia(via);
          sipRequest.setHeader(maxForwards);
          sipRequest.setHeader(contentType);
          sipRequest.setMessageContent(content);
          return sipRequest;               
    }

    /**
     * Creates a new Request message of type specified by the method paramater,
     * containing the URI of the Request, the mandatory headers of the message.
     * This new Request does not contain a body.
     *
     * @param requestURI - the new URI object of the requestURI value of this Message.
     * @param method - the new string of the method value of this Message.
     * @param callId - the new CallIdHeader object of the callId value of this Message.
     * @param cSeq - the new CSeqHeader object of the cSeq value of this Message.
     * @param from - the new FromHeader object of the from value of this Message.
     * @param to - the new ToHeader object of the to value of this Message.
     * @param via - the new Vector object of the ViaHeaders of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the method.
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader 
            callId, CSeqHeader cSeq, FromHeader from, ToHeader to, Vector via,
                MaxForwardsHeader maxForwards) throws ParseException {
          if ( requestURI==null ||
                method==null ||
                callId==null ||
                cSeq==null ||
                from==null ||
                to==null ||
                via==null ||
                maxForwards==null 
               ) 
               throw new ParseException("JAIN-SIP Exception, some parameters are missing"+
               ", unable to create the request",0);
           
          Request sipRequest=new Request();
          sipRequest.setRequestURI(requestURI);
          sipRequest.setMethod(method);
          sipRequest.setCallId(callId);
          sipRequest.setHeader(cSeq);
          sipRequest.setHeader(from);   
          sipRequest.setHeader(to);
          sipRequest.setVia(via);
          sipRequest.setHeader(maxForwards);
 
          return sipRequest;                          
    }
    


// Standard Response Creation methods

    /**
     * Creates a new Response message of type specified by the statusCode 
     * paramater, containing the mandatory headers of the message with a body 
     * in the form of a Java object and the body content type.
     *
     * @param statusCode - the new integer of the statusCode value of this Message.
     * @param callId - the new CallIdHeader object of the callId value of this Message.
     * @param cSeq - the new CSeqHeader object of the cSeq value of this Message.
     * @param from - the new FromHeader object of the from value of this Message.
     * @param to - the new ToHeader object of the to value of this Message.
     * @param via - the new Vector object of the ViaHeaders of this Message.
     * @param content - the new Object of the body content value of this Message.
     * @param contentType - the new ContentTypeHeader object of the content type 
     * value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, CallIdHeader callId, 
            CSeqHeader cSeq, FromHeader from, ToHeader to, Vector via, 
                MaxForwardsHeader maxForwards, Object content, 
                    ContentTypeHeader contentType) throws ParseException{
         if (   callId==null ||
                cSeq==null ||
                from==null ||
                to==null ||
                via==null ||
                maxForwards==null ||
                content==null ||
                contentType==null) 
               throw new NullPointerException(" unable to create the response");
           
          Response sipResponse=new Response();
	  StatusLine statusLine = new StatusLine();
	  statusLine.setStatusCode(statusCode);
	  String reasonPhrase = Response.getReasonPhrase(statusCode);
	  if (reasonPhrase == null) 
		throw new ParseException( statusCode + " Unkown  ", 0);
	  statusLine.setReasonPhrase(reasonPhrase);
          sipResponse.setStatusLine(statusLine);
          sipResponse.setCallId(callId);
          sipResponse.setHeader(cSeq);
          sipResponse.setHeader(from);   
          sipResponse.setHeader(to);
          sipResponse.setVia(via);
          sipResponse.setHeader(maxForwards);
          sipResponse.setContent(content,contentType);
          
          return sipResponse;                
    }

    

    /**
     * Creates a new Response message of type specified by the statusCode 
     * paramater, containing the mandatory headers of the message with a body 
     * in the form of a byte array and the body content type.
     *
     * @param statusCode - the new integer of the statusCode value of this Message.
     * @param callId - the new CallIdHeader object of the callId value of this Message.
     * @param cSeq - the new CSeqHeader object of the cSeq value of this Message.
     * @param from - the new FromHeader object of the from value of this Message.
     * @param to - the new ToHeader object of the to value of this Message.
     * @param via - the new Vector object of the ViaHeaders of this Message.
     * @param content - the new byte array of the body content value of this Message.
     * @param contentType - the new ContentTypeHeader object of the content type 
     * value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, CallIdHeader callId, 
            CSeqHeader cSeq, FromHeader from, ToHeader to, Vector via, 
                MaxForwardsHeader maxForwards, byte[] content, 
                    ContentTypeHeader contentType) throws ParseException{
           if ( callId==null ||
                cSeq==null ||
                from==null ||
                to==null ||
                via==null ||
                maxForwards==null ||
                content==null ||
                contentType==null) 
               throw new NullPointerException ( "Null params ");
           
          Response sipResponse=new Response();
          sipResponse.setStatusCode(statusCode);
          sipResponse.setCallId(callId);
          sipResponse.setHeader(cSeq);
          sipResponse.setHeader(from);   
          sipResponse.setHeader(to);
          sipResponse.setVia(via);
          sipResponse.setHeader(maxForwards);
          sipResponse.setHeader(contentType);
          sipResponse.setMessageContent(content);
          
          return sipResponse;                           
    }
    
    /**
     * Creates a new Response message of type specified by the statusCode 
     * paramater, containing the mandatory headers of the message. This new 
     * Response does not contain a body.
     *
     * @param statusCode - the new integer of the statusCode value of this Message.
     * @param callId - the new CallIdHeader object of the callId value of this Message.
     * @param cSeq - the new CSeqHeader object of the cSeq value of this Message.
     * @param from - the new FromHeader object of the from value of this Message.
     * @param to - the new ToHeader object of the to value of this Message.
     * @param via - the new Vector object of the ViaHeaders of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode.
     */
    public Response createResponse(int statusCode, CallIdHeader callId, 
            CSeqHeader cSeq, FromHeader from, ToHeader to, Vector via, 
                MaxForwardsHeader maxForwards) throws ParseException{
           if ( callId==null ||
                cSeq==null ||
                from==null ||
                to==null ||
                via==null ||
                maxForwards==null
               ) 
               throw new ParseException("JAIN-SIP Exception, some parameters are missing"+
               ", unable to create the response",0);
           
          Response sipResponse=new Response();
          sipResponse.setStatusCode(statusCode);
          sipResponse.setCallId(callId);
          sipResponse.setHeader(cSeq);
          sipResponse.setHeader(from);   
          sipResponse.setHeader(to);
          sipResponse.setVia(via);
          sipResponse.setHeader(maxForwards);
	  
    
          return sipResponse;                       
    }

    

    

// Response Creation methods based on a Request    



    /**
     * Creates a new Response message of type specified by the statusCode 
     * paramater, based on a specific Request with a new body in the form of a 
     * Java object and the body content type.
     *
     * @param statusCode - the new integer of the statusCode value of this Message.
     * @param request - the received Reqest object upon which to base the Response.
     * @param content - the new Object of the body content value of this Message.
     * @param contentType - the new ContentTypeHeader object of the content type 
     * value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse (int statusCode, Request request, 
            ContentTypeHeader contentType, Object content) 
	    throws ParseException{
           if ( request	   ==null 	||
                content	   ==null 	||
                contentType==null) 
               throw new NullPointerException("null parameters");  
           
           Request sipRequest=(Request)request;     
           Response sipResponse=sipRequest.createResponse(statusCode);
           sipResponse.setContent(content,contentType);
          
           return sipResponse;     
    }

    /**
     * Creates a new Response message of type specified by the statusCode 
     * paramater, based on a specific Request with a new body in the form of a 
     * byte array and the body content type.
     *
     * @param statusCode - the new integer of the statusCode value of this Message.
     * @param request - the received Reqest object upon which to base the Response.
     * @param content - the new byte array of the body content value of this Message.
     * @param contentType - the new ContentTypeHeader object of the content type 
     * value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, Request request, 
        ContentTypeHeader contentType, byte[] content) 
	throws ParseException {
           if ( request==null ||
                content==null ||
                contentType==null) 
               throw new NullPointerException("null Parameters");
           
           Request sipRequest=(Request)request;     
           Response sipResponse=sipRequest.createResponse(statusCode);
           sipResponse.setHeader(contentType);
           sipResponse.setMessageContent(content);
           
           return sipResponse;           
    }

    /**
     * Creates a new Response message of type specified by the statusCode 
     * paramater, based on a specific Request message. This new Response does 
     * not contain a body.
     *
     * @param statusCode - the new integer of the statusCode value of this Message.
     * @param request - the received Reqest object upon which to base the Response.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode.
     */
    public Response createResponse(int statusCode, Request request) 
                                                throws ParseException {
           if ( request==null ) 
               throw new NullPointerException("null parameters");

	   // if (LogWriter.needsLogging)
	   //	LogWriter.logMessage("createResponse " + request);

           
           Request sipRequest=(Request)request;     
           Response sipResponse=sipRequest.createResponse(statusCode);
	   // Remove the content from the message (Bug report from
	   // Antonis Karydas.
	   sipResponse.removeContent();
	   sipResponse.removeHeader(ContentTypeHeader.NAME);
          
           return sipResponse;                                                   
    }
    

    
    /** Creates a new Request message of type specified by the method paramater,
     * containing the URI of the Request, the mandatory headers of the message
     * with a body in the form of a byte array and body content type.
     *
     * @param requestURI - the new URI object of the requestURI value 
     * 		of this Message.
     * @param method - the new string of the method value of this Message.
     * @param callId - the new CallIdHeader object of the callId value 
     * 		of this Message.
     * @param cSeq - the new CSeqHeader object of the cSeq value of 
     * 		this Message.
     * @param from - the new FromHeader object of the from value of 
     *		this Message.
     * @param to - the new ToHeader object of the to value of this Message.
     * @param via - the new Vector object of the ViaHeaders of this Message.
     * @param contentType - the new ContentTypeHeader object of the content type
     * value of this Message.
     * @param content - the new byte array of the body content 
     * value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the method or the body.
     */
    public Request createRequest
	(URI requestURI, 
	String method, 
	CallIdHeader callId, 
	CSeqHeader cSeq, 
	FromHeader from, 
	ToHeader to, 
	Vector via, 
	MaxForwardsHeader maxForwards, 
	ContentTypeHeader contentType, byte[] content) throws ParseException {
           if ( requestURI==null  ||
                method==null 	  ||
                callId==null 	  ||
                cSeq==null 	  ||
                from==null 	  ||
                to==null 	  ||
                via==null 	  ||
                maxForwards==null ||
                content==null 	  ||
                contentType==null) 
               throw new NullPointerException
		("missing parameters");
           
          Request sipRequest=new Request();
          sipRequest.setRequestURI(requestURI);
          sipRequest.setMethod(method);
          sipRequest.setCallId(callId);
          sipRequest.setHeader(cSeq);
          sipRequest.setHeader(from);   
          sipRequest.setHeader(to);
          sipRequest.setVia(via);
          sipRequest.setHeader(maxForwards);
          sipRequest.setContent(content,contentType);
          return sipRequest;
    }
    
    
    
    
    /** Creates a new Response message of type specified by the statusCode
     * paramater, containing the mandatory headers of the message with a body
     * in the form of a Java object and the body content type.
     *
     * @param statusCode the new integer of the 
     * 	statusCode value of this Message.
     * @param callId the new CallIdHeader object of the 
     * 		callId value of this Message.
     * @param cSeq the new CSeqHeader object of the cSeq value of this Message.
     * @param from the new FromHeader object of the from value of this Message.
     * @param to the new ToHeader object of the to value of this Message.
     * @param via the new Vector object of the ViaHeaders of this Message.
     * @param contentType the new ContentTypeHeader object of the content type
     * value of this Message.
     * @param content the new Object of the body content value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse
	(int statusCode, 
	CallIdHeader callId, 
	CSeqHeader cSeq, 
	FromHeader from, 
	ToHeader to, 
	Vector via, 
	MaxForwardsHeader maxForwards, 
	ContentTypeHeader contentType, 
	Object content) throws ParseException {
	if ( 	callId==null 	  ||
                cSeq==null 	  ||
                from==null 	  ||
                to==null 	  ||
                via==null 	  ||
                maxForwards==null ||
                content==null 	  ||
                contentType==null) 
               throw new NullPointerException
		("missing parameters");
	Response sipResponse = new Response();
	StatusLine statusLine = new StatusLine();
	statusLine.setStatusCode(statusCode);
	String reason = Response.getReasonPhrase(statusCode);
	if (reason == null) 
		throw new ParseException(statusCode + " Unknown",0);
	statusLine.setReasonPhrase(reason);
	sipResponse.setStatusLine(statusLine);
	sipResponse.setCallId(callId);
	sipResponse.setHeader(cSeq);
	sipResponse.setHeader(from);
	sipResponse.setHeader(to);
	sipResponse.setVia(via);
        sipResponse.setContent(content,contentType);
        return sipResponse;
	
    }
    
    /** Creates a new Response message of type specified by the statusCode
     * paramater, containing the mandatory headers of the message with a body
     * in the form of a byte array and the body content type.
     *
     * @param statusCode the new integer of the statusCode value of this Message.
     * @param callId the new CallIdHeader object of the callId value of this Message.
     * @param cSeq the new CSeqHeader object of the cSeq value of this Message.
     * @param from the new FromHeader object of the from value of this Message.
     * @param to the new ToHeader object of the to value of this Message.
     * @param via the new Vector object of the ViaHeaders of this Message.
     * @param contentType the new ContentTypeHeader object of the content type
     * value of this Message.
     * @param content the new byte array of the body content value of this Message.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the statusCode or the body.
     */
    public Response createResponse(int statusCode, 
	CallIdHeader callId, 
	CSeqHeader cSeq, 
	FromHeader from, 
	ToHeader to, 
	Vector via, 
	MaxForwardsHeader maxForwards, 
	ContentTypeHeader contentType, 
	byte[] content) throws ParseException {
	if ( 	callId==null 	  ||
                cSeq==null 	  ||
                from==null 	  ||
                to==null 	  ||
                via==null 	  ||
                maxForwards==null ||
                content==null 	  ||
                contentType==null) 
               throw new NullPointerException
		("missing parameters");
	Response sipResponse = new Response();
	StatusLine statusLine = new StatusLine();
	statusLine.setStatusCode(statusCode);
	String reason = Response.getReasonPhrase(statusCode);
	if (reason == null) 
		throw new ParseException(statusCode + " : Unknown",0 );
	statusLine.setReasonPhrase(reason);
	sipResponse.setStatusLine(statusLine);
	sipResponse.setCallId(callId);
	sipResponse.setHeader(cSeq);
	sipResponse.setHeader(from);
	sipResponse.setHeader(to);
	sipResponse.setVia(via);
        sipResponse.setContent(content,contentType);
        return sipResponse;
    }
    
    /** Create a request from a string. Conveniance method for UACs
     *that want to create an outgoing request from a string. Only the
     *headers of the request should be included in the String that is
     *supplied to this method.
     *
     *@param requestString -- string from which to create the message
     *
     */
    
    public Request createRequest(String requestString) 
    throws ParseException {
        
        StringMsgParser smp = new StringMsgParser();
        
        Message sipMessage = smp.parseSIPMessage( requestString);
        
        if ( ! (sipMessage instanceof Request) ) 
            throw new ParseException(requestString,0);
        
        return (Request) sipMessage;
        
    }
    
	public static String getDefaultContentEncodingCharset() {
		return MessageFactory.defaultContentEncodingCharset;
	}
    
}
