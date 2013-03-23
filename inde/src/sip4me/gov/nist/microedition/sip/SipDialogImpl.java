/*
 * SipDialogImpl.java
 * 
 * Created on Jan 29, 2004
 *
 */
package sip4me.gov.nist.microedition.sip;


import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.address.URI;
import sip4me.gov.nist.siplite.header.AuthorizationHeader;
import sip4me.gov.nist.siplite.header.ProxyAuthorizationHeader;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.stack.Dialog;
import sip4me.nist.javax.microedition.sip.SipClientConnection;
import sip4me.nist.javax.microedition.sip.SipClientConnectionListener;
import sip4me.nist.javax.microedition.sip.SipConnection;
import sip4me.nist.javax.microedition.sip.SipConnectionNotifier;
import sip4me.nist.javax.microedition.sip.SipDialog;
import sip4me.nist.javax.microedition.sip.SipException;

/**
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SipDialogImpl implements SipDialog {	
	/**
	 * current state of the dialog
	 */
	private byte state;
	/**	 
	 * dialogID (Call-ID + remote tag + local tag).
	 */
	private String dialogID=null;
	/**
	 * This implementation of dialog is linked to the Nist-Siplite dialog
	 */	
	protected Dialog dialog=null;	
	/**
	 * 
	 */
	private SipConnectionNotifier sipConnectionNotifier=null;
	/**
	 * 
	 */
	private URI requestURI=null;
    
    
	private SipClientConnectionListener sipClientConnectionListener=null;
	protected ProxyAuthorizationHeader proxyAuthorizationHeader=null;
	protected AuthorizationHeader authorizationHeader=null;
	/**
	 * Constructs this dialog based upon the Nist-Siplite dialog
	 * @param dialog - Nist-Siplite dialog
	 */
	protected SipDialogImpl(Dialog dialog,
							SipConnectionNotifier sipConnectionNotifier,							
							URI requestURI) {		
		state=EARLY;
		if(dialog!=null)
			dialogID=dialog.getDialogId();
		this.dialog=dialog;
		this.sipConnectionNotifier=sipConnectionNotifier;
		this.requestURI=requestURI;		
	}

	/**
	 * @see javax.microedition.nist.sip.SipDialog#getNewClientConnection(java.lang.String)
	 */
	public SipClientConnection getNewClientConnection(String method)
								throws IllegalArgumentException, SipException {
		if(state!=SipDialog.CONFIRMED)
			throw new SipException(
				"the client connection can not be initialized, because of wrong state.",
				SipException.INVALID_STATE);
		
		//Create the new sip client connection
		//and init the request		
		SipClientConnection scc = new SipClientConnectionImpl(requestURI,this);
		if(method.trim().equalsIgnoreCase(Request.ACK))
			scc.initAck();
		else if(method.trim().equalsIgnoreCase(Request.CANCEL))
			scc.initCancel();
		else
			scc.initRequest(method,sipConnectionNotifier);
		
		return scc;
			
	}

	/**
	 * @see javax.microedition.nist.sip.SipDialog#isSameDialog(javax.microedition.nist.sip.SipConnection)
	 */
	public boolean isSameDialog(SipConnection sc) {
		if(state==SipDialog.TERMINATED)
			return false;
		if(sc.getDialog().getDialogID().equals(dialogID))
			return true;
		return false;
	}

	/**
	 * @see javax.microedition.nist.sip.SipDialog#getState()
	 */
	public byte getState() {		
		return state;
	}

	/**
	 * @see javax.microedition.nist.sip.SipDialog#getDialogID()
	 */
	public String getDialogID() {		
		if(state==TERMINATED)
			return null;		
		return dialogID;
	}
	
	/**
	 * 
	 * @param dialogID
	 */
	protected void setDialogID(String dialogID) {		
		this.dialogID=dialogID;
	}
	
	/**
	 * 
	 * @param dialog
	 */
	protected void setDialog(Dialog dialog) {		
		this.dialog=dialog;
		this.setDialogID(dialog.getDialogId());
	}
	
	/**
	 * 
	 * @param dialog
	 */
	public Dialog getDialog() {		
		return dialog;
	}


	/**
	 * 
	 * @param sipClientConnectionListener
	 */
	protected void setSipClientConnectionListener(
						SipClientConnectionListener sipClientConnectionListener){
		this.sipClientConnectionListener=sipClientConnectionListener;
	}
	
	/**
	 * 
	 * @return
	 */
	protected SipClientConnectionListener getSipClientConnectionListener(){
		return sipClientConnectionListener;
	}

	/**
	 * Changes the state of this dialog
	 * @param state - the new state of this dialog
	 */
	protected void setState(byte state){
		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_MESSAGES, "setting state of SipDialog to " + state);
		this.state=state;
	}
}
