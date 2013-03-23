/*
 * Created on Jan 28, 2004
 *
 */
package sip4me.nist.javax.microedition.sip;

/**
 * SipDialog represents one SIP Dialog. 
 * The SipDialog can be retrieved from a SipConnection object, 
 * when it is available (at earliest after provisional 101-199 response). 
 * Basically, two SIP requests can open a dialog:
 * �INVITE-1xx-2xx-ACK will open a dialog. 
 * Subsequent SipClientConnection in the same dialog can be obtained by calling 
 * getNewClientConnection(String method) method. The dialog is terminated when 
 * the transaction BYE-200 OK is completed. For more information please refer to 
 * RFC 3261 [1], Chapter 12.
 * �SUBSCRIBE-200 OK(or matching NOTIFY) will open a dialog. 
 * Subsequent SipClientConnection in the same dialog can be obtained by calling 
 * getNewClientConnection(String method) method. The dialog is terminated when a
 * notifier sends a NOTIFY request with a �Subscription-State� of �terminated� 
 * and there is no other subscriptions alive with this dialog. 
 * For more information please refer to RFC 3265 [2], Chapter 3.3.4. 
 * SipDialog has following states (for both client and server side): 
 * �early (and created), provisional 101-199 response received (or sent)
 * 	Method getNewClientConnection() can not be called in this state. 
 * �confirmed, final 2xx response received (or sent)
 *  All methods available. 
 * �terminated, no response or error response (3xx-6xx) received (or sent). 
 *  Also if the dialog is terminated with BYE or un-SUBSCRIBE.
 *  Method getNewClientConnection() can not be called in this state. 
 *  The SipDialog has following state chart:
 * 
 * 
 * 
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public interface SipDialog {
	/**
	 * provisional 101-199 response received (or sent)
	 * Method getNewClientConnection() can not be called in this state.
	 */
	public static final byte EARLY=1;
	/**
	 * final 2xx response received (or sent)
	 * All methods available.
	 */
	public static final byte CONFIRMED=2;
	/**
	 * no response or error response (3xx-6xx) received (or sent). 
 	 * Also if the dialog is terminated with BYE or un-SUBSCRIBE.
	 */
	public static final byte TERMINATED=3;
	
	/**
	 * Returns new SipClientConnection in this dialog. 
	 * The SipClientConnection will be pre-initialized with the given method and 
	 * following headers will be set at least 
	 * (for details see RFC 3261 [1] 12.2.1.1 Generating the Request, p.73):
	 * <ul><li>To</li>
	 * 	   <li>From</li>
	 *     <li>CSeq</li>
	 *     <li>Call-ID</li>
	 *     <li>Max-Forwards</li>
	 *     <li>Via</li>
	 *     <li>Contact</li>
	 *     <li>Route// if the dialog route is not empty</li></ul>
	 * @param method - given method
	 * @return SipClientConnection with preset headers.
	 * @throws IllegalArgumentException - if the method is invalid 
	 * @throws SipException - INVALID_STATE if the new connection can not be 
	 * established in the current state of dialog.
	 */
	public sip4me.nist.javax.microedition.sip.SipClientConnection getNewClientConnection(java.lang.String method)
													  throws IllegalArgumentException, SipException;
													  
	/**
	 * Does the given SipConnection belong to this dialog.
	 * @param sc - SipConnection to be checked, can be either 
	 * SipClientConnection or SipServerConnection
	 * @return true if the SipConnection belongs to the this dialog. Returns false 
	 * if the connection is not part of this dialog or the dialog is terminated.
	 */
	public boolean isSameDialog(sip4me.nist.javax.microedition.sip.SipConnection sc);
	
	/**
	 * Returns the state of the SIP Dialog.
	 * @return dialog state byte number.
	 */
	public byte getState();
										
	/**
	 * Returns the ID of the SIP Dialog.
	 * @return Dialog ID (Call-ID + remote tag + local tag). 
	 * Returns null if the dialog is terminated.
	 */
	public java.lang.String getDialogID();													
}
