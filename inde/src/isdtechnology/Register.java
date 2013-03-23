package isdtechnology;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.microedition.io.Connector;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.SocketConnection;
import javax.microedition.lcdui.StringItem;


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
import sip4me.gov.nist.siplite.stack.ServerLog;
import sip4me.nist.javax.microedition.sip.*;

public class Register extends Thread implements SipRefreshListener{

	protected dialogsam _Dest = null;
	private SipConnectionNotifier connectionNotifier;
	protected SipClientConnection registerScc;
	protected SipServerConnection resultssc;
	private int registerRefreshID;
	

	public Register(dialogsam screen, SipConnectionNotifier connectionNotifiersss){
		super();
		_Dest = screen;	
		connectionNotifier = connectionNotifiersss;
			 
		    

	}
	
	public void run(){
		final String uri_ = "sip:1001@10.0.0.7";
		final String username_ = "1001";
		final String credUsername_ = "1001";
		final String password_ = "1001";
		final String realm_ = "asterisk";
		final String domain_ = "10.0.0.1";
		final String auth_ ="1001";
		try { 
	
		if (connectionNotifier != null) {
			register(domain_, uri_,
					username_, auth_,
			password_, realm_);
			Thread.sleep(4000);
			//unregister();
		}
		
		
	} catch (Exception e) {
		_Dest.updateDestination2("Error registering!");
		e.printStackTrace();
	}
		
	}

	
	public void register(String domain, String uri, String username,
			String credUsername, String password, String realm) {
		_Dest.updateDestination2("Registering to outbound proxy with domain " + domain);
		
		final String uri_ = uri;
		final String username_ = username;
		final String credUsername_ = credUsername;
		final String password_ = password;
		final String realm_ = realm;
		final String domain_ = domain;
		
		new Thread() {

			public void run() {
				try {
					registerScc = (SipClientConnection) SipConnector.open(uri_);
									
					registerScc.initRequest(Request.REGISTER, connectionNotifier);
					registerScc.setHeader(Header.FROM, uri_);
					registerScc.setHeader(Header.USER_AGENT, "ISD Mobile Express Talk version 1.0.0");
					registerScc.setHeader(Header.EXPIRES, "100");
					registerScc.setHeader(Header.ALLOW, "INVITE,ACK,OPTIONS,BYE,CANCEL,SUBSCRIBE,NOTIFY,REFER,MESSAGE,INFO,PING");

					String contactHdr = "SIP:"+username_ + "@"
							+ connectionNotifier.getLocalAddress() + ":"
							+ connectionNotifier.getLocalPort();
					registerScc.setHeader(Header.CONTACT, contactHdr);
					
					registerScc.setRequestURI("sip:" + domain_);
					
					if (credUsername_ != null && password_ != null
							&& realm_ != null)
						registerScc.setCredentials(credUsername_, password_,
								realm_);
					
					registerRefreshID = registerScc.enableRefresh(Register.this);
					
					// Finally, send register
					registerScc.send();
					int statuss = registerScc.getStatusCode();
					_Dest.updateDestination2("REGISTER sent: " + statuss);
					
					registerr();
					
				} catch (Exception e) {
					_Dest.updateDestination2("Exception sending register " + e.getMessage());
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void registerr() throws IOException{
	
		registerScc.setListener(new SipClientConnectionListener() {
			public void notifyResponse(SipClientConnection scc) {
				try {
					scc.receive(0);
					int status = scc.getStatusCode();
					_Dest.updateDestination2("Response to register: " + status);
					Thread.sleep(3000);
					if((status >= 0) && (status < 100)){
						_Dest.updateDestination2("no response to register");
					}
					else if ((status >= 100) && (status < 200)) {
						// provisional response
					} else if ((status >= 200) && (status < 300)) {
					
						onRegisterSuccess();
						
					} else if (status == 401 || status == 407){
						// sip-for-me already handles authentication
						return;
					} else {
						onRegisterFailed("Negative response received");
					}
				} catch (Exception e){
					_Dest.updateDestination2("Exception thrown while receiving REGISTER response " + String.valueOf(e));
					e.printStackTrace();
					onRegisterFailed("Exception thrown while receiving");
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
	
	
	
	public void onRegisterSuccess() {
		_Dest.updateDestination4("Registered");
	}
	
	public void onRegisterFailed(String reason) {
		_Dest.updateDestination4("Registration failed! Reason: " + reason);
	};
	
	
	
	public void refreshEvent(int refreshID, int statusCode, String reasonPhrase) {
		_Dest.updateDestination2("Online");
	}

	
}