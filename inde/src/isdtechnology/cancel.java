package isdtechnology;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.microedition.io.Connector;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;
import javax.microedition.lcdui.StringItem;


import net.rim.device.api.io.messaging.Headers;


import sip4me.gov.nist.core.Debug;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.javax.sdp.SdpException;
import sip4me.gov.nist.javax.sdp.SdpFactory;
import sip4me.gov.nist.javax.sdp.fields.SDPField;
import sip4me.gov.nist.microedition.sip.SipConnector;
import sip4me.gov.nist.microedition.sip.StackConnector;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.Dialog;
import sip4me.gov.nist.siplite.stack.ServerLog;
import sip4me.nist.javax.microedition.sip.*;

public class cancel extends Thread implements SipRefreshListener{

	protected dialogsam _Dest = null;
	private String dial;
	private SipConnectionNotifier connectionNotifier;
	protected SipClientConnection registerScc;
	protected SipServerConnection resultssc;
	private int registerRefreshID;
	
    private final short S_OFFLINE = 0;
    private final short S_CALLING = 1;
    private final short S_RINGING = 2;
    private final short S_ONLINE = 3;
    
    //Protocol constants - status
    private final int OK_STATUS = 200;
    private final int RING_STATUS = 180;
    
    private short state = S_OFFLINE;
    
	public cancel(dialogsam screen, SipConnectionNotifier connectionNotifiersss){
		super();
		_Dest = screen;	
		connectionNotifier = connectionNotifiersss;
			 
		    

	}
	
	public void run(){

		try { 
	
		if (connectionNotifier != null) {
		Invites();
			//unregister();
		}
		
		
	} catch (Exception e) {
		_Dest.updateDestination2("Error sending cancel!");
		e.printStackTrace();
	}
		
	}

	public void Invites() {

		final String uri_ = "sip:1001@10.0.0.7";
		//final String uri2_ = "sip:1003@10.0.0.7";
		final String username_ = "1001";
		final String credUsername_ = "1001";
		final String password_ = "1001";
		final String realm_ = "asterisk";
		//to
		final String to = dial;
		final String auth_ ="1001";
			
		new Thread() {

			public void run() {
				try {
					//openSipConnectionNotifier("sip:;transport=udp");
					
					registerScc = (SipClientConnection) SipConnector.open(uri_);
									
					registerScc.initRequest(Request.CANCEL, connectionNotifier);
					registerScc.setHeader(Header.FROM, uri_);
					//registerScc.setHeader(Header.TO, uri2_);
					registerScc.setHeader(Header.USER_AGENT, "ISD Mobile Express Talk version 1.0.0");
					registerScc.setHeader(Header.EXPIRES, "100");
					String contactHdr = "SIP:"+username_ + "@"
							+ connectionNotifier.getLocalAddress() + ":"
							+ connectionNotifier.getLocalPort();
					registerScc.setHeader(Header.CONTACT, contactHdr);
					
					registerScc.setRequestURI("sip:1003@10.0.0.1");
					//credentials
					registerScc.setCredentials(credUsername_, password_,
							realm_);
					registerRefreshID = registerScc.enableRefresh(cancel.this);
					
					// Finally, send register
					registerScc.send();
					int statuss = registerScc.getStatusCode();
					_Dest.updateDestination2("cancel sent: " + statuss);
					
					Inviter();
					
				} catch (Exception e) {
					_Dest.updateDestination2("Exception sending cancel " + e.getMessage());
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void Inviter() throws IOException{
	
		registerScc.setListener(new SipClientConnectionListener() {
			public void notifyResponse(SipClientConnection scc) {
				try {
					scc.receive(0);
					int status = scc.getStatusCode();
					_Dest.updateDestination2("Response to cancel: " + status);
					Thread.sleep(3000);
					if((status >= 0) && (status < 100)){
						_Dest.updateDestination2("no response to cancel");
					}
					else if ((status >= 100) && (status < 200)) {
						//ringing response
						  if(status == RING_STATUS){
                              state = S_RINGING;
                          _Dest.updateDestination4(".......");
                            }
						
						// provisional response
					} else if ((status >= 200) && (status < 300)) {
					
						if(status == 200){
							_Dest.updateDestination4("cancel established");
							  
							  state = S_OFFLINE;
							 }
						}
						
					 else if (status == 401 || status == 407){
						// sip-for-me already handles authentication
						return;
					} else {
						onRegisterFailed("No response from contact:" +status);
					}
				} catch (Exception e){
					_Dest.updateDestination2("Exception thrown while sending cancel request " + String.valueOf(e));
					e.printStackTrace();
					onRegisterFailed("Exception thrown while inviting");
				}
			}			
		});

		
	}	 
	
	
	//to unregister
	
	private void unregister() throws IOException {
		if (registerScc != null && registerRefreshID > 0) {
			
			registerScc.setListener(new SipClientConnectionListener() {
				public void notifyResponse(SipClientConnection scc) {
					_Dest.updateDestination2("response to unregister");
					try {				
						scc.receive(0);
						int status = scc.getStatusCode();
						_Dest.updateDestination2("Accepted response to unregister: " + status);
						 if (status == 401 || status == 407) {
							 // sip-for-me already handles authentication
							 return;
						 } 
						 // mainly for 200 OK responses
						 else {					
							// Close connection and unset variables
							 
							registerScc.close();
							registerScc = null;	
							registerRefreshID = -1;
						 }
					} catch (Exception e) {
						_Dest.updateDestination2("Problem unregistering: " + e.getMessage());
						e.printStackTrace();
					}
				}			
			});
			
			SipRefreshHelper.getInstance().stop(registerRefreshID);
		}
	}
	
	

	
	public void onRegisterFailed(String reason) {
		_Dest.updateDestination4("cancel failed! Reason: " + reason);
	};
	
	
	
	public void refreshEvent(int refreshID, int statusCode, String reasonPhrase) {
		_Dest.updateDestination2("Online");
	}

	
}