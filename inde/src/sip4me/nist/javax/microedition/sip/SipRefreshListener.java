/*
 * Created on Jan 28, 2004
 */
package sip4me.nist.javax.microedition.sip;

/**
 * Listener interface for RefreshHelper events. This interface defines an event 
 * that contains a refreshID to identify the corresponding refresh task, 
 * statusCode that represent the result of this refresh 
 * (0 = cancelled, 200 = successful, else = failure). The statusCode corresponds 
 * to the response received for the original request sent by the SipRefreshHelper. 
 * The reasonPhrase gives a textual message about either success or failure of 
 * this refresh task.
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public interface SipRefreshListener {

	/**
	 * Called when a refresh task is successfully started, refreshed, cancelled or failed.
	 * @param refreshID - refresh ID
	 * @param statusCode - the status code of the refresh task. If a response 
	 * message was received the code corresponds to the response status code
	 * @param reasonPhrase - additional textual message
	 */
	public void refreshEvent(
							 int refreshID, 
							 int statusCode, 
							 java.lang.String reasonPhrase);
}
