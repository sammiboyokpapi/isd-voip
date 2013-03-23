package isdtechnology;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.SocketConnection;
import javax.microedition.lcdui.StringItem;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

import org.linphone.jortp.JOrtpFactory;
//import org.linphone.jortp.RtpBBTransport;
import org.linphone.jortp.RtpException;
import org.linphone.jortp.RtpTransport;
import org.linphone.jortp.SocketAddress;

import sip4me.gov.nist.core.Debug;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.javax.sdp.SdpFactory;
import sip4me.gov.nist.microedition.sip.SipConnector;
import sip4me.gov.nist.microedition.sip.StackConnector;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.stack.ServerLog;
import sip4me.nist.javax.microedition.sip.SipClientConnection;
import sip4me.nist.javax.microedition.sip.SipClientConnectionListener;
import sip4me.nist.javax.microedition.sip.SipConnectionNotifier;
import sip4me.nist.javax.microedition.sip.SipDialog;
import sip4me.nist.javax.microedition.sip.SipException;
import sip4me.nist.javax.microedition.sip.SipRefreshListener;
import sip4me.nist.javax.microedition.sip.SipServerConnection;
import sip4me.nist.javax.microedition.sip.SipServerConnectionListener;
import net.rim.device.api.command.*;
import net.rim.device.api.media.control.AudioPathControl;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.decor.Background;
import net.rim.device.api.ui.decor.BackgroundFactory;
import net.rim.device.api.ui.decor.Border;
import net.rim.device.api.ui.decor.BorderFactory;
import net.rim.device.api.util.StringProvider;

public class dialogsam extends MainScreen implements FieldChangeListener, SipServerConnectionListener, SipRefreshListener, SipClientConnectionListener{

	private VerticalFieldManager _managers;	
	protected RichTextField _Output2 = new RichTextField(Field.FIELD_BOTTOM | Field.NON_FOCUSABLE);
	protected RichTextField _Output3 = new RichTextField(Field.FIELD_BOTTOM);
	protected RichTextField _Output5 = new RichTextField(Field.FIELD_BOTTOM);
	private MenuItem _fetchMenuItem;
	private MenuItem _fetchMenuItem1;
	private MenuItem _fetchMenuItem2;
	protected LabelField status = new LabelField("",Field.FIELD_HCENTER);
	
	EditField dial = new EditField(EditField.FILTER_EMAIL | EditField.FIELD_HCENTER);
	String lock = System.getProperty("microedition.hostname");
	
	//button fields
	ButtonField call = new ButtonField("Call",ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER);
	ButtonField decline = new ButtonField("decline",ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER);
	
	//status
	private SipDialog dialog;
	private final int OK_STATUS = 200;
    
	//States
    private final short S_OFFLINE = 0;
    private final short S_CALLING = 1;
    private final short S_RINGING = 2;
    private final short S_ONLINE = 3;
    
	private short state = S_OFFLINE;
	
	
	protected dialogsam _Dest = null;
	private SipConnectionNotifier connectionNotifier;
	protected SipClientConnection resultscc;
	protected SipServerConnection resultssc;
	private SipServerConnection origSSC = null;
	private int registerRefreshID;
	
	final String uri_ = "sip:1001@10.0.0.7";
	//final String uri2_ = "sip:1003@10.0.0.7";
	final String username_ = "1001";
	final String credUsername_ = "1001";
	final String password_ = "1001";
	final String realm_ = "asterisk";
	//to
	final String to = "1003@10.0.0.7";
	final String auth_ ="1001";
	
	
	
	//sdp message
	 private String sdp = "v=0\no=sippy 2890844730 2890844732 IN IP4 10.0.0.1\ns=isd mobile voip\nc=IN IP4 10.0.0.1\nt=0 0\nm=audio 54344 RTP/AVP\na=rtpmap:0 PCMU/8000";
	SdpFactory sds;
	 private String str;
	
	//commands 
	public String answer="null"; 
	public String bye="null";
	public String cancel ="null";
	public String invitecancel ="null";
	
	//ringtone player
	Player mRingPlayer;
	private int localport;
	private String localAdd;
	private int localports;
	
public dialogsam(){
	
	_managers = (VerticalFieldManager)getMainManager();
	
	Background bg = BackgroundFactory.createSolidBackground(Color.WHITE);
	_managers.setBackground(bg);
	XYEdges edges =new XYEdges(10, 10, 10, 10);
	Border border = BorderFactory.createBevelBorder(edges);
	_managers.setBorder(border);

	setTitle("ISD Mobile Express Talk");
	
	RichTextField username = new RichTextField(Field.NON_FOCUSABLE);
	RichTextField usernames = new RichTextField(Field.NON_FOCUSABLE);
	
	username.setLabel("Welcome to ISD mobile Express Talk");
    Background backgrounds = BackgroundFactory.createSolidTransparentBackground(Color.STEELBLUE, 200);
    username.setBackground(backgrounds);
    usernames.setBackground(backgrounds);
    add(username);
    add(usernames);
	add(new SeparatorField());


	 
	 
	_Output2.setLabel("");
	add(new SeparatorField());
	
	this.setStatus(_Output2);
	add(new SeparatorField());
	_Output3.setLabel("state");
	_Output3.setText(":0 :offline ");
	_Output5.setLabel("papa: ");
	add(_Output5);
	add(_Output3);

	add(new LabelField("Enter Number", Field.FIELD_HCENTER ));
	dial.setBorder(BorderFactory.createRoundedBorder(new XYEdges(2, 2, 2, 2), 0x35586c, Border.STYLE_SOLID));
	   Background backgroundss = BackgroundFactory.createSolidTransparentBackground(Color.GRAY, 10);
	   dial.setBackground(backgroundss);
	   
	add(dial);
	butt();
	butt1();
	add(new LabelField());
	add(new LabelField("call state", Field.FIELD_HCENTER ));
	add(status);
	
	
	runww();


	_fetchMenuItem = new MenuItem(new StringProvider("Settings") , 0x230010, 0);
	_fetchMenuItem.setCommand(new Command(new CommandHandler() 
	{
	    /**
	     * @see net.rim.device.api.command.CommandHandler#execute(ReadOnlyCommandMetadata, Object)
	     */
	    public void execute(ReadOnlyCommandMetadata metadata, Object context) 
	    {
	    	//UiApplication.getUiApplication().pushScreen(new Settings());	
	   storm();		  
	    }
	}));
	
	_fetchMenuItem1 = new MenuItem(new StringProvider("Option") , 0x230010, 0);
	_fetchMenuItem1.setCommand(new Command(new CommandHandler() 
	{
	    /**
	     * @see net.rim.device.api.command.CommandHandler#execute(ReadOnlyCommandMetadata, Object)
	     */
	    public void execute(ReadOnlyCommandMetadata metadata, Object context) 
	    {
	        // Don't execute on a blank url.
	   storm();
	    		  
	    }
	    
	}));

	_fetchMenuItem2 = new MenuItem(new StringProvider("Refresh Connection") , 0x230010, 0);
	_fetchMenuItem2.setCommand(new Command(new CommandHandler() 
	{
	    /**
	     * @see net.rim.device.api.command.CommandHandler#execute(ReadOnlyCommandMetadata, Object)
	     */
	    public void execute(ReadOnlyCommandMetadata metadata, Object context) 
	    {
	        // Don't execute on a blank url.
	

}
	}));

}



public void butt(){
	 call.setChangeListener(this);
	 add(call);
	
}
public void butt1(){
     decline.setChangeListener(this);
	 add(decline);
	
}
public void fieldChanged(Field arg0, int arg1) {
	// TODO Auto-generated method stub
	
	 if(arg0 == call)
     {
			if(answer.equals("null")){	
            invites();
			}
			else if(answer.equals("ok")){
				
				answer();
			}
     }
	 
	 if(arg0 == decline)
     {
		 if(bye.equals("null")){			 
	      
		  }
		 
	     else if(bye.equals("ok")){
		    
	    	 if(state == S_RINGING) {
	    		 if(invitecancel.equals("sent")){
	    			 sendcancel();
	    			 invitecancel = "null";
	    			 
	    		 }
	    		 else{
			 try{
             resultssc.initResponse(486); // Busy here
             resultssc.send();
              //state is offline
			 }
			 catch(Exception e){
				 
			 }
	    		 }
		 }
	    	 
		 else if(state == S_ONLINE) {
			 if(invitecancel.equals("sent")){
    		sendbye2();
    			 invitecancel = "null";	 
    		 }
    		 else{
    			 sendbye();
    		 }
		    }
	    	 
		 else if(state == S_CALLING){
    		 if(invitecancel.equals("sent")){
    			 sendcancel();
    			 invitecancel = "null";	 
    		 }
    		 else{}
		 }
	    	 
		 state = S_OFFLINE;
		 try {
			mRingPlayer.stop();
		} catch (MediaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  bye = "null";
          answer="null";
		 updateDestination3(":0 :offline ");
		 }
		 

     }
	
}


private void sendbye2() {
	 try {
	 resultscc.initAck();
		resultscc.send();
		mRingPlayer.stop();
		}
	 
catch (Exception e) {
		// TODO Auto-generated catch block
	 updateDestination2("EXCEPTION" + e.getMessage());
	}
	  
	// TODO Auto-generated method stub
	  /* if(dialog != null) {
	        try {
	        	resultscc = dialog.getNewClientConnection("BYE");
	        	resultscc.setHeader("Accept-Contact",
	                             "*;type=\"" + "application/sdp"+"\"");
	                resultscc.setRequestURI("sip:" + to);
	                resultscc.send();
	             

	                boolean gotit = resultscc.receive(10000);
	                if(gotit) {
	                        if(resultscc.getStatusCode() == OK_STATUS) {
	                                state = S_OFFLINE;
	                        }
	                        else
	                        	updateDestination2("Error: "+resultscc.getReasonPhrase());
	                }
	                
	                resultscc.close();
	        } catch(IOException iox) {
	        	updateDestination2("Exception: "+iox.getMessage());
	        }
	} else {
		updateDestination2("No dialog information!");
	} */
	
	
}



private void sendcancel() {
	// TODO Auto-generated method stub
    if(resultscc != null) {
        try {
                SipClientConnection cancel = resultscc.initCancel();
                cancel.send();
             
                if(cancel.receive(30000)) {
                        state = S_OFFLINE;
                        mRingPlayer.stop();
                } else {
                	updateDestination2("error"+ cancel.getReasonPhrase());
                }
        } catch(Exception ex) {
             
                updateDestination2("EXCEPTION" + ex.getMessage());
        }
}
	
	
	
}



private void sendbye() {
	// TODO Auto-generated method stub
    if(dialog != null) {
        try {
                SipClientConnection sc = dialog.getNewClientConnection("BYE");
                sc.setHeader("Accept-Contact",
                             "*;type=\"" + "application/sdp"+"\"");
                sc.send();
                

                boolean gotit = sc.receive(10000);
                if(gotit) {
                        if(sc.getStatusCode() == OK_STATUS) {
                                state = S_OFFLINE;
                                mRingPlayer.stop();
                        }
                        else
                        	updateDestination2("Error: "+sc.getReasonPhrase());
                }
                
                sc.close();
        } catch(IOException iox) {
        	updateDestination2("Exception: "+iox.getMessage());
        } catch (MediaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
} else {
	updateDestination2("No dialog information!");
}
}



private void invites() {
	// TODO Auto-generated method stub
	String too = dial.getText();
	try {
		//openSipConnectionNotifier("sip:;transport=udp");
		 state = S_CALLING;
		 updateDestination3(":1 : Calling");
		 bye = "ok";
		 invitecancel = "sent";
		resultscc = (SipClientConnection) SipConnector.open(uri_);
						
		resultscc.initRequest(Request.INVITE, connectionNotifier);
		resultscc.setHeader(Header.FROM, uri_);
		//registerScc.setHeader(Header.TO, uri2_);
		resultscc.setHeader(Header.USER_AGENT, "ISD Mobile Express Talk version 1.0.0");
		resultscc.setHeader(Header.EXPIRES, "100");
		resultscc.setHeader(Header.CONTENT_TYPE, "application/sdp");
		String contactHdr = "SIP:"+username_ + "@"
				+ connectionNotifier.getLocalAddress() + ":"
				+ connectionNotifier.getLocalPort();
		resultscc.setHeader(Header.CONTACT, contactHdr);
		
		resultscc.setRequestURI("sip:" + too);
		//credentials
		resultscc.setCredentials(credUsername_, password_,
				realm_);
		registerRefreshID = resultscc.enableRefresh(dialogsam.this);
		
		// Finally, send register
		resultscc.send();
		int statuss = resultscc.getStatusCode();
		updateDestination2("Invite sent: " + statuss);
		
		resultscc.setListener(dialogsam.this);
		
	} catch (Exception e) {
		updateDestination2("Exception sending Invite " + e.getMessage());
		state = S_OFFLINE;
		updateDestination3(":0 :Offline ");
	}
}



protected void makeMenu(Menu menu, int instance)
{
menu.add(_fetchMenuItem);
menu.add(_fetchMenuItem1);
menu.add(_fetchMenuItem2);
super.makeMenu(menu, instance);
}


public void updateDestination2(final String text) {
	UiApplication.getUiApplication().invokeLater(new Runnable() {
		public void run()
		{
			_Output2.setText(text);
			
			}
	});

 
}

public void updateDestination3(final String text) {
	UiApplication.getUiApplication().invokeLater(new Runnable() {
		public void run()
		{
			_Output3.setText(text);
			
			}
	});

 
}

public void updateDestination4(final String text) {
	UiApplication.getUiApplication().invokeLater(new Runnable() {
		public void run()
		{
			status.setText(text);
			
			}
	});

 
}
public void updateDestination5(final String text) {
	// TODO Auto-generated method stub
	UiApplication.getUiApplication().invokeLater(new Runnable() {
		public void run()
		{
			_Output5.setText(text);
			
			}
	});

}
public void sdpin(final String text) {
	UiApplication.getUiApplication().invokeLater(new Runnable() {
		public void run()
		{
			_Output5.setText(text);
			
			}
	});

 
}

public void runww(){

new Thread() {

	public void run() {
	try {
		
		Thread.sleep(3000);
	initializeStack();
	Thread.sleep(4000);
		}
		catch (Exception e) {
		}
		try {
			// if no transport is specified, both TCP and UDP 
			// listening points will be created
			
			openSipConnectionNotifier("sip:;transport=udp;deviceside=true");
			Thread.sleep(4000);
			
		} catch (IOException e) {
			updateDestination2("Error opening SCN!" +e);
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Register svcc = new Register((dialogsam)UiApplication.getUiApplication().getActiveScreen(),connectionNotifier);
		svcc.start();
		
	  }
}.start();


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
    StackConnector.properties.setProperty("sip4me.gov.nist.javax.sip.NETWORK_LAYER", "sip4me.gov.nist.core.net.BBNetworkLayer");		
       
	// Get our real IP address, and set it on the Stack properties
	// We have to do this because inbound sockets don't return real IP 
	// (they may return 'localhost' or '127.0.0.1')
    // This address will also be set on SipConnectionNotifiers
    
    /*
	String dummyConnString = "socket://"+"10.0.0.1"+":"+"5060";
	SocketConnection dummyCon = (SocketConnection) Connector.open(dummyConnString);
	String localAdd = dummyCon.getLocalAddress();//getLocalAddress();
	dummyCon.close();
    */
    
  /*  
	RtpTransport mLastRecv = JOrtpFactory.instance().createDefaultTransport();
	SocketAddress sock = JOrtpFactory.instance().createSocketAddress("10.0.0.1",5060);
	try {
		mLastRecv.init(sock);
	} catch (RtpException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	String localAdd = mLastRecv.getipaddress();
	mLastRecv.close();
*/
	
    SocketAddress sock = JOrtpFactory.instance().createSocketAddress("10.0.0.7",5060);
    localAdd = sock.getHost();
    localports = sock.getPort();

	StackConnector.properties.setProperty ("javax.sip.IP_ADDRESS", localAdd);  
	updateDestination2("Stack initialized with IP: " + localAdd);
}


public void openSipConnectionNotifier(String connectorString) throws IOException {
  
    connectionNotifier = (SipConnectionNotifier) SipConnector.open(connectorString);
	connectionNotifier.setListener(this);
	updateDestination2("SipConnectionNotifier opened at: "
			+ connectionNotifier.getLocalAddress() + ":"
			+ connectionNotifier.getLocalPort());
//	String hh = connectionNotifier.toString();
//	_Dest.updateDestination2(hh);
    }

public void storm(){
		//UiApplication.getUiApplication().pushScreen(new EyelidFieldDemoScreen());			
		}

/*
public boolean onClose(){
	return true;	
}
*/


public void refreshEvent(int refreshID, int statusCode, String reasonPhrase) {
	// TODO Auto-generated method stub
}


private void answer() {
	// TODO Auto-generated method stub
	try{
	 resultssc.initResponse(OK_STATUS); // OK
	 resultssc.send();
	 //stops ring tone
	 mRingPlayer.stop();
	 //sends your sdp message
	 resultssc.setHeader("Content-Length", ""+sdp.length());
	 resultssc.setHeader(Header.CONTENT_TYPE, "application/sdp");
	 OutputStream os = resultssc.openContentOutputStream();
     os.write(sdp.getBytes());
     os.close(); 
	//dialog = resultssc.getDialog();
	//updateDestination3("Dialog state: " + dialog.getState()+"\n");
} catch(Exception ex) {
    ex.printStackTrace();
   // form.append(EXCEPTION + ex.getMessage());
}


}

//receiving calls and data

public void notifyRequest(SipConnectionNotifier ssc) {
	// TODO Auto-generated method stub
	String tmp =null;
	try {
		resultssc = ssc.acceptAndOpen();
		if(resultssc.getMethod().equals(Request.INVITE)) {
			origSSC = resultssc;
			 state = S_CALLING;
			 updateDestination3(":1 :calling ");
			 String contentType = resultssc.getHeader(Header.CONTACT);
	         String contentLength = resultssc.getHeader("Content-Length");
	         
	         updateDestination4("incoming call from :" + contentType);
	       
             //resultssc.initResponse(200);

             int length = Integer.parseInt(contentLength);
			 InputStream is = resultssc.openContentInputStream();
             byte content[] = new byte[length];
             is.read(content);
             String sc = new String(content);
             
            sdp sdpread = new sdp(sc,(dialogsam)UiApplication.getUiApplication().getActiveScreen());
 	   		sdpread.start();
 	   		//String d = sdpread.remoteaddress(sc);
 
 	   //	updateDestination5(d);
 	   	
             resultssc.initResponse(180); // OK
             resultssc.send(); 
             state = S_RINGING;
             
             try {
         		mRingPlayer = Manager.createPlayer(getClass().getResourceAsStream("/oldphone.wav"),"audio/wav");
     			mRingPlayer.realize();
     			mRingPlayer.prefetch();
     			mRingPlayer.setLoopCount(Integer.MAX_VALUE);
     			AudioPathControl  lPathCtr = (AudioPathControl) mRingPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
     			lPathCtr.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
     			mRingPlayer.start();
     			
     		}catch (Throwable e) {
     			Dialog.alert("cannot play ringback tone"+ e);
     			mRingPlayer.close();
     			return;
     		}
			dialog = resultssc.getDialog();
			answer = "ok";
			bye = "ok";	
			//updateDestination3("Dialog state: " + dialog.getState()+"\n");
			updateDestination3(":2 :Ringing ");
		      
		//	updateDestination5("SDP DATA:\n " + sc);
				updateDestination3(":2 :Ringing ");
				
			return;
	         
		}
		else if(resultssc.getMethod().equals("ACK")) {
			updateDestination4("Session established: "+resultssc.getHeader("CONTACT"));
            state = S_ONLINE;
            dialog = resultssc.getDialog();
            updateDestination3(":3 :Online ");
            //updateDestination3("Dialog state: " + dialog.getState()+"\n");
            resultssc.initResponse(200);
            resultssc.send();
            // Wait for otherside to send BYE
          //  form.append("Waiting for BYE...");
    }
		else if(resultssc.getMethod().equals("BYE")) {
			resultssc.initResponse(OK_STATUS);
			resultssc.send();
        state = S_OFFLINE;
        updateDestination3(":0 :Offline ");
        
        updateDestination4("Call Ended");
       // updateDestination2(str);
        //updateDestination3("Dialog state: " + dialog.getState());
        
        answer = "null";
        bye = "null";
        
        resultssc.close();
		}
		else if(resultssc.getMethod().equals("CANCEL")) {
			resultssc.initResponse(OK_STATUS);
			resultssc.send();
			origSSC.initResponse(487);//request terminated status
			origSSC.send();
            state = S_OFFLINE;
        	try {
				mRingPlayer.stop();
			} catch (MediaException e) {
				// TODO Auto-generated catch block
				
			}
            updateDestination3(":0 :Offline "); 
            updateDestination4("Call Cancelled");
            answer = "null";
            bye = "null";
    }	
	}catch(Exception e) {
	      // ex.getMessage();
		updateDestination2("Exception thrown value is "+ e.getMessage());
		   
	}	
}




public void notifyResponse(SipClientConnection scc) {
	try {
		scc.receive(0);
		int status = scc.getStatusCode();
		//if(scc.getMethod().equals("INVITE")){
		updateDestination2("Response to invite: " + status);
		Thread.sleep(3000);
		if((status >= 0) && (status < 100)){
			updateDestination2("no response to invite");
		}
		else if (status == 100){
			//ringing response
			updateDestination4("Trying");
			// provisional response
		}
		else if (status == 180){
			//ringing response
			dialog = scc.getDialog();
                  state = S_RINGING;
         		 updateDestination3(":2 : Ringing");
         		 
              updateDestination4("ringing");
                
			
			// provisional response
		} else if ((status >= 200) && (status < 300)) {
		
			if(status == 200){
				 String contenttype = scc.getHeader(Header.CONTENT_TYPE);
				 if(contenttype.equals("application/sdp")) {
					  state = S_ONLINE;
					  updateDestination3(":3 : online");
				  scc.initAck();
					  scc.send();
				  dialog = resultscc.getDialog();
				  updateDestination4("call established "+dialog.getState());
				 }
			}
			
		} else if (status == 401 || status == 407){
			// sip-for-me already handles authentication
			return;
		} else {
			updateDestination4("No response from contact:" +status);
			state = S_OFFLINE;
			try {
				mRingPlayer.stop();
			} catch (MediaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			updateDestination3(":0 : Offline"); 
		       }
		//}
		
	}catch (Exception e) {
		// TODO Auto-generated catch block
		updateDestination2("Exception thrown value is "+ e.getMessage());
		state = S_OFFLINE;
		updateDestination3(":0 :Offline ");
	}
}


public void close()
{
    // Display a farewell message before closing the application
    Dialog.alert("Goodbye!");     
    super.close();
}  

}
