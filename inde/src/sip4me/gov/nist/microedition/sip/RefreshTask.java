/*
 * RefreshTask.java
 * 
 * Created on Apr 8, 2004
 *
 */
package sip4me.gov.nist.microedition.sip;


import java.io.IOException;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.header.AuthorizationHeader;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.stack.SIPStackTimerTask;
import sip4me.nist.javax.microedition.sip.SipClientConnection;
import sip4me.nist.javax.microedition.sip.SipConnectionNotifier;
import sip4me.nist.javax.microedition.sip.SipException;
import sip4me.nist.javax.microedition.sip.SipRefreshListener;


/**
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class RefreshTask extends SIPStackTimerTask {
	private Request request = null;
	private SipClientConnection sipClientConnection = null;
	private SipConnectionNotifier sipConnectionNotifier = null;
	private final SipRefreshListener sipRefreshListener;
	
	/**
	 * Creates a new instance of RefreshTask
	 * 
	 * @param request
	 *            - the request to resend
	 * @param sipConnectionNotifier
	 *            - the connection used to send the request
	 * @param sipRefreshListener
	 *            - the callback interface used listening for refresh event on
	 *            this task
	 */
	public RefreshTask(Request request,
			SipConnectionNotifier sipConnectionNotifier,
			SipRefreshListener sipRefreshListener,
			SipClientConnection sipClientConnection) {
		this.request = request;
		this.sipConnectionNotifier = sipConnectionNotifier;
		this.sipRefreshListener = sipRefreshListener;
		this.sipClientConnection = sipClientConnection;
	}

	/**
	 * @see java.lang.Runnable#run()
	 * improvements by ArnauVP (refreshes inside dialogs)
	 */
	public void runTask() {
		Request clonedRequest = null;
		
		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "Running refresh task " + this);
		
		// Refreshes outside of a dialog
		if (request.getMethod().equals(Request.REGISTER)) {
			clonedRequest = (Request) request.clone();
		} 
		// Refreshes inside a dialog
		else if (request.getMethod().equals(Request.SUBSCRIBE)) {
			try {
				clonedRequest = ((SipDialogImpl) sipClientConnection.getDialog()).dialog
						.createRequest(Request.SUBSCRIBE);
			} catch (sip4me.gov.nist.siplite.SipException e) {
				clonedRequest = (Request) request.clone();
			}
		}

		if (clonedRequest.getHeader(AuthorizationHeader.NAME) != null) {
			clonedRequest.removeHeader(AuthorizationHeader.NAME);
			clonedRequest = ((SipClientConnectionImpl) sipClientConnection).authenticateRequest(clonedRequest);
		}
		
		// This will have been already done in authenticateRequest 
		if (clonedRequest.getCSeqHeaderNumber() == request.getCSeqHeaderNumber()) {
			CSeqHeader cseq = request.getCSeqHeader();
			cseq.setSequenceNumber(cseq.getSequenceNumber()+1);
			clonedRequest.setCSeqHeader(cseq);
		}
		
		request = null;
		updateRequest(clonedRequest);
		try{
			sipClientConnection.send();
		}
		catch (SipException se) {
			se.printStackTrace();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Return the callback interface listening for events on this task
	 * @return the callback interface listening for events on this task
	 */
	public SipRefreshListener getSipRefreshListener(){
		return sipRefreshListener;
	}

	/**
	 * Return the callback interface listening for events on this task
	 * @return the callback interface listening for events on this task
	 */
	public SipConnectionNotifier getSipConnectionNotifier(){
		return sipConnectionNotifier;
	}

	/**
	 * Return the sipClientconnection on which is enabled the listener
	 * @return the sipClientconnection on which is enabled the listener
	 */
	public SipClientConnection getSipClientConnection(){
		return sipClientConnection;
	}

	/**
	 * Update the request in the sipClientConnection and the state of the connection 
	 * @param request - the updated request
	 */
	public void updateRequest(Request request){
		this.request = request;
		((SipClientConnectionImpl) sipClientConnection).
			updateRequestFromRefresh(request);
	}

	public CallIdHeader getNewCallId(){		
		return ((SipConnectionNotifierImpl)sipConnectionNotifier).
					getSipProvider().getNewCallId();
	}

	/**
	 * Return the request to refresh
	 * @return the request to refresh
	 */
	public Request getRequest(){
		return request;
	}
	
	
}
