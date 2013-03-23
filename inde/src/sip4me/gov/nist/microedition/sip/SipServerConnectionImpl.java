/*
 * SipServerConnectionImpl.java
 * 
 * Created on Jan 29, 2004
 *
 */
package sip4me.gov.nist.microedition.sip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Enumeration;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.siplite.SipProvider;
import sip4me.gov.nist.siplite.TransactionAlreadyExistsException;
import sip4me.gov.nist.siplite.TransactionUnavailableException;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.header.ContactHeader;
import sip4me.gov.nist.siplite.header.ContactList;
import sip4me.gov.nist.siplite.header.ExpiresHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.ToHeader;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.ServerTransaction;
import sip4me.nist.javax.microedition.sip.SipDialog;
import sip4me.nist.javax.microedition.sip.SipException;
import sip4me.nist.javax.microedition.sip.SipServerConnection;


  
/**
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SipServerConnectionImpl implements SipServerConnection {
	//Server Transaction States
	/**
	 * Request Received, SipServerConnection returned from SipConnectionNotifier or 
 	 * provisional response(s) (1xx) sent. 
	 */
	protected static final int REQUEST_RECEIVED=1;
	/**
	 * Initialized, response initialized calling initResponse()
	 */
	protected static final int INITIALIZED=2;
	/**
	 * Stream Open, OutputStream opened with openContentOutputStream(). 
	 * Opening InputStream for received request does not trigger state transition.
	 */
	protected static final int STREAM_OPEN=3;
	/**
	 * Completed, transaction completed with sending final response (2xx, 3xx, 4xx, 5xx, 6xx)
	 */
	protected static final int COMPLETED=4;
	/**
	 * Terminated, the final state, in which the SIP connection has been terminated by error or closed
	 */
	protected static final int TERMINATED=5;
	/**
	 * Attribute keeping the actual state of this server transaction
	 */
	private int state;
	/**
	 * the sip dialog this client transaction belongs to
	 */
	private SipDialog sipDialog=null;
	/**
	 * the request for this server transaction	 
	 */
	private Request request=null;
	/**	 
	 * the response to the actual request
	 */
	private Response response=null;	
	/**
	 * content of the response body
	 */
	private OutputStream contentOutputStream=null;
	/**
	 * content from the request body
	 */
	private InputStream contentInputStream=null;	
	
	/**
	 * Receiver of incoming messages
	 */
	private SipConnectionNotifierImpl sipConnectionNotifierImpl; 
	
	/**
	 * Constructor
	 * @param request - 
	 */
	protected SipServerConnectionImpl(
				Request request,
				SipDialog sipDialog,
				SipConnectionNotifierImpl sipConnectionNotifierImpl) {
		
		state=REQUEST_RECEIVED;
		this.request=request;		
		this.sipDialog=sipDialog;
		this.sipConnectionNotifierImpl=sipConnectionNotifierImpl;
	}

	/**
	 * @see javax.microedition.nist.sip.SipServerConnection#initResponse(int)
	 */
	public void initResponse(int code)
				throws IllegalArgumentException, SipException {
		//Check if the code is not out of range					
		if(code<100 || code>699)
			throw new IllegalArgumentException("the response code is out of range.");
		//Check if we are in a good state to init the response	
		if(state!=REQUEST_RECEIVED)
			throw new SipException(
						"the response can not be initialized, because of wrong state.",
						SipException.INVALID_STATE);
		//Generating the response to the request
		try{
			response = StackConnector.messageFactory.createResponse(code,request);				
		}
		catch(ParseException pe){
			pe.printStackTrace();
		}					
		//Set the toTag in the ToHeader if not already present
		ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
		if(toHeader.getTag()==null)
			toHeader.setTag(StackConnector.generateTag()); 
		//if we don't have any contact headers we add one
		ContactList contactList=response.getContactHeaders();
		if(contactList==null || contactList.isEmpty()){
			ContactHeader contactHeader=null;
			try{
				Address address=StackConnector.addressFactory.createAddress(
					"<sip:"+sipConnectionNotifierImpl.getLocalAddress()
					+":"+sipConnectionNotifierImpl.getLocalPort()+">");
				contactHeader=StackConnector.headerFactory.createContactHeader(address);
				response.addHeader(contactHeader);
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}
			catch(ParseException pe){
				pe.printStackTrace();
			}
		}		
		state=INITIALIZED;	
		//System.out.println("The following response has been initialized:\n"+
		//					response.toString());						
	}

	/**
	 * @see javax.microedition.nist.sip.SipServerConnection#setReasonPhrase(java.lang.String)
	 */
	public void setReasonPhrase(String phrase)
				throws SipException, IllegalArgumentException {
		if(state!=INITIALIZED)
			throw new SipException(
					  "the Reason Phrase can not be removed, because of wrong state.",
					  SipException.INVALID_STATE);
		if(phrase==null)
			throw new IllegalArgumentException("The reason phrase can not be null");
		response.setReasonPhrase(phrase);
	}

	/* (non-Javadoc)
	 * @see javax.microedition.nist.sip.SipConnection#send()
	 */
	public void send()
				throws IOException, InterruptedIOException, SipException {
		if(state!=STREAM_OPEN && state!=INITIALIZED)
			throw new SipException(
						"can not enable the refresh, because of wrong state.",
						SipException.INVALID_STATE);
		ServerTransaction serverTransaction = (ServerTransaction) request
				.getTransaction();
		// TODO : check in CASE of a trying if it has already been sent by the stack
		// if yes the response is ignored
		if(response.getStatusCode()/100!=1){
			state=COMPLETED;						
		}
		else{
			state=REQUEST_RECEIVED;
		}								
		//Set the sdp body of the message	
		if(contentOutputStream!=null){
			response.setContent(contentOutputStream.toString());
			contentOutputStream=null;						
		}
		//send the response				
		if (serverTransaction == null) {
			try {
				SipProvider sipProvider = sipConnectionNotifierImpl
						.getSipProvider();
				serverTransaction = sipProvider
						.getNewServerTransaction(request);
			} catch (TransactionAlreadyExistsException taee) {
				taee.printStackTrace();
				// return;
			} catch (TransactionUnavailableException tue) {
				tue.printStackTrace();
				// return;
			}
		}	
		//Set the application data so that when the request comes in, 
		//it will retrieve this SipConnectionNotifier
		serverTransaction.setApplicationData(sipConnectionNotifierImpl);	
		try{
			serverTransaction.sendResponse(response);	
		}
		catch(sip4me.gov.nist.siplite.SipException se){			
			se.printStackTrace();
		} catch (Exception e) {
			System.err.println("Couldn't send the response");
			e.printStackTrace();
		}
		
		if (serverTransaction.getDialog() == null) { // No dialog created
			return;
		}
				
		// Fix by ArnauVP (Genaker): NPE on responses to NOTIFYs
		if (sipDialog == null) {
			sipDialog = new SipDialogImpl(serverTransaction.getDialog(), 
					sipConnectionNotifierImpl, request.getRequestURI());
		} else if (StackConnector.getInstance().sipStack.isDialogCreated(request.getMethod())) {
			((SipDialogImpl)sipDialog).setDialog(serverTransaction.getDialog());
		}
		
		//Change the dialog state
		if(response.isSuccessfulResponse()) {
			if(!response.getCSeqHeader().getMethod().equals(Request.BYE)) {		
				((SipDialogImpl)sipDialog).setState(SipDialog.CONFIRMED);	
			}
			else {
				((SipDialogImpl)sipDialog).setState(SipDialog.TERMINATED);
			}
			
		} else if(((SipDialogImpl)sipDialog).getDialog().getState() == SipDialog.EARLY &&
				response.getStatusCode()/100!=1) {
			serverTransaction.getDialog().setState(SipDialog.TERMINATED);
		}
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String name, String value)
				throws SipException, IllegalArgumentException {		
		if(state!=INITIALIZED)
			throw new SipException(
					  "the Header can not be set, because of wrong state.",
					  SipException.INVALID_STATE);
		if(name==null)
			throw new IllegalArgumentException("The header name can not be null");
		if(value==null)
			throw new IllegalArgumentException("The header value can not be null");
		Header header = null; 							
		try{	
			header=StackConnector.headerFactory.createHeader(name,value);
		}
		catch(ParseException pe){
			throw new IllegalArgumentException(pe.getMessage());
		}
		if (header == null)
			throw new IllegalArgumentException("null header!");
					
		response.attachHeader(header,true,true);

	}

	/* (non-Javadoc)
	 * @see javax.microedition.nist.sip.SipConnection#addHeader(java.lang.String, java.lang.String)
	 */
	public void addHeader(String name, String value)
				throws SipException, IllegalArgumentException {		
		if(state!=INITIALIZED)
			throw new SipException(
					  "the Header can not be add, because of wrong state.",
					  SipException.INVALID_STATE);	
		if(name==null)
			throw new IllegalArgumentException("The header name can not be null");
		if(value==null)
			throw new IllegalArgumentException("The header value can not be null");
		Header header=null;
		try{		
			header=StackConnector.headerFactory.createHeader(name,value);
		}
		catch(ParseException pe){
			throw new IllegalArgumentException("The header can not be created," +				" check if it is correct");
		}
		response.addHeader(header);
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#removeHeader(java.lang.String)
	 */
	public void removeHeader(String name) 
				throws SipException, IllegalArgumentException {				
		if(state!=INITIALIZED)
			throw new SipException(
						"the Header can not be removed, because of wrong state.",
						SipException.INVALID_STATE);					
		if(name==null)
			throw new IllegalArgumentException("The header name can not be null");
									
		response.removeHeader(name,true);
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getHeaders(java.lang.String)
	 */
	public String[] getHeaders(String name) {		
		Enumeration e=request.getHeaders(name);
		//Check the size of the enumeration
		int size=0;
		while(e.hasMoreElements()){
			e.nextElement();
			size++;
		}
		//If there is no elements in the enumeration we return null
		if(size<1)	
			return null;
		//Create the array of headers		
		String[] headers=new String[size];
		e=request.getHeaders(name);
		int count=0;
		while(e.hasMoreElements())
			headers[count++]=((Header)e.nextElement()).getHeaderValue();		
		return headers;			
	}

	/** 
	 * @see javax.microedition.nist.sip.SipConnection#getHeader(java.lang.String)
	 */
	public String getHeader(String name) {	
		// Fix by Arnau Vazquez (Genaker)
		Header hdr = request.getHeader(name);
		if (hdr != null) {
			return hdr.getHeaderValue();
		} else {
			return null;
		}
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getMethod()
	 */
	public String getMethod(){
		return request.getMethod();		
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getRequestURI()
	 */
	public String getRequestURI() {		
		return request.getRequestURI().toString();
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getStatusCode()
	 */
	public int getStatusCode() {		
		return response.getStatusCode();
	}

	/** 
	 * @see javax.microedition.nist.sip.SipConnection#getReasonPhrase()
	 */
	public String getReasonPhrase() {		
		return response.getReasonPhrase();
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#getDialog()
	 */
	public SipDialog getDialog() {		
		return sipDialog;
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#openContentInputStream()
	 */
	public InputStream openContentInputStream()
					   throws IOException, SipException {
		if(state!=REQUEST_RECEIVED)
			throw new SipException(
				"the content input strean can not be open, because of wrong state.",
				SipException.INVALID_STATE);
		byte[] buf=request.getRawContent();
		if (buf == null)
			return null;
		contentInputStream = new ByteArrayInputStream(buf);				
		return contentInputStream;
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnection#openContentOutputStream()
	 */
	public OutputStream openContentOutputStream()
						throws IOException, SipException {
		if(state!=INITIALIZED)
			throw new SipException(
				"the content output strean can not be open, because of wrong state.",
				SipException.INVALID_STATE);
		if(response.getHeader(Header.CONTENT_TYPE)==null)
			throw new SipException(
				"Content-Type unknown, set the content-type header first",
				SipException.UNKNOWN_TYPE
			);		
		if(response.getHeader(Header.CONTENT_LENGTH)==null)
			throw new SipException(
				"Content-Length unknown, set the content-length header first",
				SipException.UNKNOWN_LENGTH
			);
		contentOutputStream=new SDPOutputStream(this);						
		state=STREAM_OPEN;
		return contentOutputStream;
	}

	/**
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {				
		state=TERMINATED;
	}	
	
	/**
	 * Change the state of the dialog due to an incoming response
	 * @param response
	 */
	private void changeDialogState(){
		//Change the dialog state					
		if(response.getCSeqHeader().getMethod().equals(Request.REGISTER))
			return;
		if(!response.isSuccessfulResponse()){
			//handle the un-Subscribe state
		  	if(response.getCSeqHeader().getMethod().equals(Request.SUBSCRIBE)){
				ExpiresHeader expiresHeader=
				  	(ExpiresHeader)request.getHeader(ExpiresHeader.NAME);
			  	if(expiresHeader.getExpires()==0){
					((SipDialogImpl)sipDialog).setState(SipDialog.TERMINATED);
			  	}
		  	}
		  	else if(!response.getCSeqHeader().getMethod().equals(Request.BYE)){
				//If it's a REGISTER the dialog can be null
				if(sipDialog!=null){
					((SipDialogImpl)sipDialog).setState(SipDialog.CONFIRMED);
					((SipDialogImpl)sipDialog).setDialogID(response.getDialogId(false));		  	
				}							
			}
			else 
				((SipDialogImpl)sipDialog).setState(SipDialog.TERMINATED);
		}	  	
		else if(sipDialog.getState()==SipDialog.EARLY && 
					response.getStatusCode()/100!=1)			
				((SipDialogImpl)sipDialog).setState(SipDialog.TERMINATED);			
	}
}
