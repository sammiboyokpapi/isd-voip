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

public class RegisterLayer extends Thread implements SipServerConnectionListener, SipRefreshListener{

	protected dialogsam _Dest = null;
	private SipConnectionNotifier connectionNotifier;
	protected SipClientConnection registerScc;
	protected SipServerConnection resultssc;
	private int registerRefreshID;
	
	private static Random _random;  // For generating random numbers for dates and strings.
	private static final int MIN_STRING_LENGTH = 4000;   // Minimum length for random string.
	private static final int MAX_STRING_LENGTH = 10000;
	
	private SipDialog dialog;
	
    //Protocol constants - status
    private final int OK_STATUS = 200;
    private final int BUSY_STATUS = 486;
    private final int REQUEST_TERMINATED_STATUS = 487;
    private final int RING_STATUS = 180;
	
    private String sdp = "v=0\no=sippy 2890844730 2890844732 IN IP4 10.0.0.1\ns=isd mobile voip\nc=IN IP4 10.0.0.1\nt=0 0\nm=aurdio 54344 RTP/AVP\na=rtpmap:0 PCMU/8000";
    
    
    //contacts
    private final String SDP_MIME_TYPE = "application/sdp";
    private final String ACCEPT_CONTACT = "*;type=\"" + SDP_MIME_TYPE + "\"";
    private String contact = null;
    
    //states
    private final short S_OFFLINE = 0;
    private final short S_CALLING = 1;
    private final short S_RINGING = 2;
    private final short S_ONLINE = 3;

    private short state = S_OFFLINE;
    
	public RegisterLayer(dialogsam screen){
		super();
		_Dest = screen;	
		
		
	}
	
	public void run(){

		startsip();
		
	}
	
	public void startsip(){
		
		final String uri_ = "sip:1001@10.0.0.7";
		final String username_ = "1001";
		final String credUsername_ = "1001";
		final String password_ = "1001";
		final String realm_ = "asterisk";
		final String domain_ = "10.0.0.1";
		final String auth_ ="1001";
		
		// begin almighty application
		try {
			initializeStack();
			Thread.sleep(4000);
		} catch (IOException e) {
			_Dest.updateDestination2("Error initializing stack! Aborting!");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			// if no transport is specified, both TCP and UDP 
			// listening points will be created
			
			openSipConnectionNotifier("sip:;transport=udp");
			Thread.sleep(4000);
			
		} catch (IOException e) {
			_Dest.updateDestination2("Error opening SCN!" +e);
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

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

	
	public void initializeStack() throws IOException {
		// Configure logging of the stack
        Debug.enableDebug(false);
		LogWriter.needsLogging = true;
		LogWriter.setTraceLevel(LogWriter.TRACE_EXCEPTION);
		ServerLog.setTraceLevel(ServerLog.TRACE_NONE);
		
		
		// If we set the outbound proxy on TCP, all the outgoing requests
		// would be on TCP
        StackConnector.properties.setProperty("javax.sip.OUTBOUND_PROXY","10.0.0.1" + ":" + "5060" + "/UDP");
        StackConnector.properties.setProperty ("javax.sip.RETRANSMISSION_FILTER", "on");
        
		// Get our real IP address, and set it on the Stack properties
		// We have to do this because inbound sockets don't return real IP 
		// (they may return 'localhost' or '127.0.0.1')
        // This address will also be set on SipConnectionNotifiers
		String dummyConnString = "socket://" + "10.0.0.1" + ":" + "5060";
		SocketConnection dummyCon = (SocketConnection) Connector.open(dummyConnString);
		String localAdd = dummyCon.getLocalAddress();
		dummyCon.close();
		StackConnector.properties.setProperty ("javax.sip.IP_ADDRESS", localAdd);  
		_Dest.updateDestination2("Stack initialized with IP: " + localAdd);
	}
	
	public void openSipConnectionNotifier(String connectorString) throws IOException {
	  
        connectionNotifier = (SipConnectionNotifier) SipConnector.open(connectorString);
		connectionNotifier.setListener(this);
		_Dest.updateDestination2("SipConnectionNotifier opened at: "
				+ connectionNotifier.getLocalAddress() + ":"
				+ connectionNotifier.getLocalPort());
	//	String hh = connectionNotifier.toString();
	//	_Dest.updateDestination2(hh);
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
					
					registerRefreshID = registerScc.enableRefresh(RegisterLayer.this);
					
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
	
	
	public void Invite() {

		final String uri_ = "sip:1001@10.0.0.7";
		final String username_ = "1001";
		final String credUsername_ = "1001";
		final String password_ = "1001";
		final String realm_ = "asterisk";
		final String domain_ = "10.0.0.1";
		final String auth_ ="1001";
		_Dest.updateDestination2("Registering to outbound proxy with domain " + domain_);		
		new Thread() {

			public void run() {
				try {
					//openSipConnectionNotifier("sip:;transport=udp");
					
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
					
					registerRefreshID = registerScc.enableRefresh(RegisterLayer.this);
					
					// Finally, send register
					registerScc.send();
					int statuss = registerScc.getStatusCode();
					_Dest.updateDestination2("REGISTER sent: " + statuss);
					
					Inviter();
					
				} catch (Exception e) {
					_Dest.updateDestination2("Exception sending Invite " + e.getMessage());
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
					_Dest.updateDestination2("Exception thrown while sending invite request " + String.valueOf(e));
					e.printStackTrace();
					onRegisterFailed("Exception thrown while inviting");
				}
			}			
		});

		
	}	 
	
	public void onRegisterSuccess() {
		_Dest.updateDestination4("Registered");
	}
	
	public void onRegisterFailed(String reason) {
		_Dest.updateDestination4("Registration failed! Reason: " + reason);
	};
	
	
	public void notifyRequest(SipConnectionNotifier ssc) {
		// TODO Auto-generated method stub
		String tmp =null;
		try {
			resultssc = ssc.acceptAndOpen();
			if(resultssc.getMethod().equals(Request.INVITE)) {
		         String contentType = resultssc.getHeader("Content-Type");
		         String contentLength = resultssc.getHeader("Content-Length");
		         int length = Integer.parseInt(contentLength);
		         if((contentType != null) && contentType.equals("application/sdp")) {
		            InputStream is = resultssc.openContentInputStream();
		            int i=0;
		            byte testBuffer[] = new byte[length];
		            i = is.read(testBuffer);
		             tmp = new String(testBuffer, 0, i); 
		               // the message received
					//_Dest.updateDestination2("msg received:" + tmp);
		         }
		         _Dest.updateDestination3("msg received:" + contentType +" : "+ tmp);
		       
	             //resultssc.initResponse(200);
	             resultssc.initResponse(OK_STATUS); // OK
	             resultssc.setHeader("Content-Length", ""+sdp.length());
				 resultssc.setHeader(Header.CONTENT_TYPE, "application/sdp");
				 OutputStream os = resultssc.openContentOutputStream();
                 os.write(sdp.getBytes());
                 os.close(); 
				dialog = resultssc.getDialog();
				_Dest.updateDestination3("Dialog state: " + dialog.getState()+"\n");
			}
			
		}catch(IOException ex) {
		      // ex.getMessage();
		   }
		
		
	}
	
	
	public void refreshEvent(int refreshID, int statusCode, String reasonPhrase) {
		_Dest.updateDestination2("Online");
	}

	
}