/*
 * Created on Jan 28, 2004
 */
package sip4me.nist.javax.microedition.sip;

import java.io.IOException;
import java.io.OutputStream;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.microedition.sip.RefreshManager;
import sip4me.gov.nist.microedition.sip.RefreshTask;
import sip4me.gov.nist.microedition.sip.SipClientConnectionImpl;
import sip4me.gov.nist.microedition.sip.SipDialogImpl;
import sip4me.gov.nist.microedition.sip.StackConnector;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.header.AuthorizationHeader;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.ContactHeader;
import sip4me.gov.nist.siplite.header.ContentLengthHeader;
import sip4me.gov.nist.siplite.header.ExpiresHeader;
import sip4me.gov.nist.siplite.message.Request;



/**
 * This class implements the functionality that facilitates the handling of 
 * refreshing requests on behalf of the application. Some SIP requests 
 * (REGISTER, SUBSCRIBE, ...) need to be timely refreshed 
 * (binding between end point and server, see RFC 3261 - chapter 10.2.1 page 58). 
 * For example the REGISTER request (RFC 3261, chapter 10 page 56) needs to be 
 * re-sent to ensure that the originating end point is still well and alive. 
 * The request�s validity is proposed by the end point in the request and 
 * confirmed in the response by the registrar/notifier for example in expires 
 * header (RFC 3261, chapter 2 page 5). The handling of such binding would 
 * significantly increase application complexity and size. As a consequence 
 * the SipRefreshHelper can be used to facilitate such operations. 
 * When the application wants to send a refreshable request it: 
 * 		�implements SipRefreshListener callback interface. 
 * 		�creates a new SipClientConnection and sets it up. 
 * 		�calls the method enableRefresh(SipRefreshListener). 
 * 		 A refresh ID is returned. If the request is not refreshable the method returns 0. 
 * 		�if the refresh task fails a failure event is sent to the SipRefreshListener 
 * 		 A reference to the SipRefreshHelper object is obtained by calling the 
 * 		 static method SipRefreshHelper.getInstance() (singleton pattern).
 * Finally, using the refresh ID returned from enableRefresh(SipRefreshListener) 
 * the application can: 
 * 		�stop() a refresh. The possible binding between end point and server is cancelled 
 * 		 (RFC3261, chapter 10.2.2 page 61). 
 * 		�update(...) the refreshed request with new parameters 
 * 		 (Contact info and new payload). Note that this functionality is limited 
 * 		 to the most typical case. A more complex case would require to stop 
 * 		 the refresh and to create a new request with the needed updates. 
 * When all refresh tasks belonging to one refresh listener are stopped, 
 * the listener reference will be removed from the SipRefreshHelper.
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SipRefreshHelper {
	/**
	 * The unique instance of this class
	 */
	private static SipRefreshHelper instance = null;	
	
	private static RefreshManager refreshManager= null;
	/**
	 * Returns the instance of SipRefreshHelper
	 * @return the instance of SipRefreshHelper singleton
	 */
	public static sip4me.nist.javax.microedition.sip.SipRefreshHelper getInstance(){
		if(instance==null){
			instance=new SipRefreshHelper();
			refreshManager=RefreshManager.getInstance();
		}
					
		return instance;
	}
	
	/**
	 * Stop refreshing a specific request related to refeshID. The possible 
	 * binding between end point and registrar/notifier is cancelled 
	 * (RFC3261, chapter 10.2.2 page 61). An event will be sent to the listeners 
	 * with refreshID and statusCode = 0 and reasonPhrase = �refresh stopped�.
	 * @param refreshID - the ID of the refresh to be stopped. If the ID does not 
	 * match any refresh task the method does nothing.
	 */
	public void stop(int refreshID){
		String taskId;
		try{
			taskId=String.valueOf(refreshID);		
		}
		catch(NumberFormatException nfe){
			return;
		}
		RefreshTask refreshTask=refreshManager.getTask(taskId);
		if(refreshTask==null)
			return;
		
		refreshTask.cancel();
		refreshTask.getSipRefreshListener().refreshEvent(
														refreshID,
														0,
														"refresh stopped for method " + refreshTask.getRequest().getMethod());
		
		SipDialog dialog = refreshTask.getSipClientConnection().getDialog();
		
		// The possible binding between end point and 
		// registrar/notifier is cancelled: i.e. send a last 
		// update UNREGISTERING / UNSUBSCRIBING.
		// Don't do it if the initial request failed or wasn't answered,
		// so the dialog would be already terminated or not established.
		// TODO: do something similar with a failed REGISTER
		// Fix by ArnauVP (Genaker)
		if (!(refreshTask.getRequest().getMethod() == Request.SUBSCRIBE && 
				(dialog.getState() == SipDialog.EARLY ||
						dialog.getState() == SipDialog.TERMINATED))) {
				this.update(refreshID, null, null, 0, 0);
		} else {
			if (LogWriter.needsLogging)
				LogWriter
						.logMessage(LogWriter.TRACE_DEBUG,
								"Not sending UNSUBSCRIBE because the dialog is terminated or not established");
		}
		refreshManager.removeTask(taskId);

	}
	
	/**
	 * Updates one refreshed request with new values.
	 * 		�new Contact header values. existing values are kept if this is null.
	 * 		�Expires header value. expires = 0 has the same effect as calling stop
	 * 		 (refreshID). expires = -1 leaves the Expires header out.
	 * 		�new payload: Content-Type and Content-Length. 
	 * 		 The message is sent when the returned OutputStream is closed. 
	 * 		 If no content is set the message will be sent automatically and 
	 * 		 the method returns null.
	 * @param refreshID - ID returned from enableRefresh(...). If the ID does not 
	 * match any refresh task the method just returns without doing anything.
	 * @param contact - new Contact headers as String array. Replaces all old values. 
	 * Multiple Contact header values are applicable only for REGISTER method. 
	 * If contact param is null or empty the system will set the Contact header.
	 * @param type - value of Content-Type (null or empty, no content)
	 * @param length - value of Content-Length (<=0, no content)
	 * @param expires - value of Expires (-1, no Expires header), (0, stop the refresh)
	 * @return Returns the OutputStream to fill the content. If the update does 
	 * not have new content (type = null and/or length = 0) method returns null 
	 * and the message is sent automatically.
	 * @throws java.lang.IllegalArgumentException - if some input parameter is invalid
	 */
	public java.io.OutputStream update(
										int refreshID,
										java.lang.String[] contact,
										java.lang.String type, 
										int length, 
										int expires) 
								throws java.lang.IllegalArgumentException {
		String taskId;
		try{
			taskId=String.valueOf(refreshID);		
		}
		catch(NumberFormatException nfe){
			return null;
		}
		
		RefreshTask refreshTask=refreshManager.getTask(taskId);
		if(refreshTask==null)
			return null;			
		
		SipClientConnection sipClientConnection=
			refreshTask.getSipClientConnection();
		
		Request requestNotCloned=refreshTask.getRequest();			
		Request request = null;
		
		// Refreshes outside of a dialog
		if (requestNotCloned.getMethod().equals(Request.REGISTER)) {
			request = (Request) requestNotCloned.clone();
		} 
		// Refreshes inside a dialog
		else if (requestNotCloned.getMethod().equals(Request.SUBSCRIBE)) {
			try {

				request = ((SipDialogImpl) refreshTask.getSipClientConnection()
						.getDialog()).getDialog().createRequest(
						Request.SUBSCRIBE);
			} catch (sip4me.gov.nist.siplite.SipException e) {
				request = (Request) requestNotCloned.clone();
			}
		}
		
		if (request.getHeader(AuthorizationHeader.NAME) != null) {
			request.removeHeader(AuthorizationHeader.NAME);
			request = ((SipClientConnectionImpl) sipClientConnection).authenticateRequest(request);
		}
		
		// This will have been already done in authenticateRequest
		if (request.getCSeqHeaderNumber() == requestNotCloned.getCSeqHeaderNumber()) {
			CSeqHeader cseq = request.getCSeqHeader();
			cseq.setSequenceNumber(cseq.getSequenceNumber()+1);
			request.setCSeqHeader(cseq);
		}
		

		requestNotCloned=null;
		//Contacts
		if(contact!=null){
			if(contact.length>1 && request.getMethod().equals(Request.SUBSCRIBE))
			request.removeHeader(ContactHeader.NAME);
			for(int i=0;i<contact.length;i++){
				String contactURI=contact[i];
				Address address=null;
				try{
					address=StackConnector.addressFactory.createAddress(contactURI);
				}
				catch(ParseException pe){
					throw new IllegalArgumentException("one of the contact " +						"addresses is not valid");
				}
				ContactHeader contactHeader=
					StackConnector.headerFactory.createContactHeader(address);
				request.addHeader(contactHeader);							
			}
		}
		//Expires
		if(expires==-1){
			request.removeHeader(ExpiresHeader.NAME);
		}
		else if(expires >= 0){
			((ExpiresHeader)request.getHeader(ExpiresHeader.NAME)).setExpires(expires);
		}
		else{
			throw new IllegalArgumentException("the expires value is not correct");
		}
		ContentLengthHeader contentLengthHeader=request.getContentLengthHeader();
		if(contentLengthHeader==null)
			request.addHeader(
				StackConnector.headerFactory.createContentLengthHeader(0));
		//Content Length					
		if(length>0)	
			request.getContentLengthHeader().setContentLength(length);
		//Update the request of the sipClientConnection
		refreshTask.updateRequest(request);

		// Fix by ArnauVP (Genaker): refreshes always
		// need the same CallID value as original request! (RFC 3261)
	
		//Content Type
		if(type==null){		
			//send the message
			try{
				if (LogWriter.needsLogging)
		            LogWriter.logMessage("Before sending, request line is: " + request.getRequestLine().encode());
				sipClientConnection.send();
			}
			catch(SipException sipex){
				sipex.printStackTrace();
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}
			return null;
		}
		else{
			OutputStream contentOutputStream=null;
			try{
				//set the content type
				contentOutputStream=sipClientConnection.openContentOutputStream();
			}
			catch(IOException ioe){
				ioe.printStackTrace();
				return null;
			}
			try{
				contentOutputStream.write(type.getBytes());
			}
			catch(IOException ioe){
				ioe.printStackTrace();
				return null;
			}
			return contentOutputStream;
		}
			
											
	}
		
}
