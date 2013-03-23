/*
 * SipConnectionNotifierImpl.java
 * 
 * Created on Jan 29, 2004
 *
 */
package sip4me.gov.nist.microedition.sip;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Vector;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.ObjectInUseException;
import sip4me.gov.nist.siplite.SipProvider;
import sip4me.gov.nist.siplite.SipStack;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.stack.Dialog;
import sip4me.gov.nist.siplite.stack.ServerTransaction;
import sip4me.nist.javax.microedition.sip.SipConnectionNotifier;
import sip4me.nist.javax.microedition.sip.SipDialog;
import sip4me.nist.javax.microedition.sip.SipException;
import sip4me.nist.javax.microedition.sip.SipServerConnection;
import sip4me.nist.javax.microedition.sip.SipServerConnectionListener;


/**
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SipConnectionNotifierImpl 
			implements SipConnectionNotifier{//, Runnable {
	/**
	 * Listener interface for incoming SIP requests.
	 */
	private SipServerConnectionListener sipServerConnectionListener=null;			
	/**
	 * Messages received held in this vector
	 */
	private Vector messageQueue=null;	
	/**
	 * flag to know the state of the connection (open or close)
	 */
	private boolean connectionOpen;
	/**
	 * listen address
	 */
	private String localAddress=null;
	/**
	 * port number
	 */
	private final int portNumber;
	/**
	 * The Sip Provider for this connection Notifier
	 */
	private SipProvider sipProvider=null;		

	/**
	 * 
	 */	
	private StackConnector stackConnector=null;
	
	/**
	 * Constructor called by the Connector.open() method
	 * @param portNumber - the port number on which the listener will wait for 
	 * incoming messages
	 * @param secure - flag to check if the listener should accept connections
	 * only over a secure transport layer
	 * @throws IOException - 
	 */
	protected SipConnectionNotifierImpl(
							SipProvider sipProvider,
							String localAddress,
							int portNumber) {
		this.sipProvider=sipProvider;
		this.localAddress=localAddress;
		this.portNumber=portNumber;
		//Setting default value to attributes
		connectionOpen=true;		
		messageQueue=new Vector();	
		try{
			stackConnector=StackConnector.getInstance();			
		}
		catch(IOException ioe){
			ioe.printStackTrace();				
		}
	}


	/** (non-Javadoc)
	 * @see javax.microedition.nist.sip.SipConnectionNotifier#acceptAndOpen()
	 */
	public SipServerConnection acceptAndOpen()
							   throws IOException, InterruptedIOException, SipException {
		if(!connectionOpen)
			throw new InterruptedIOException("Connection was closed!");		
		//TODO : handle the two others exceptions
								   		
		//create the sipServerConnection when
		//a request is received through the processRequest method
		//the processRequest method will add the Request in the Queue and notify()		 				 							   		
		if(messageQueue==null || messageQueue.size()<1){
			synchronized(this){
				try{
					wait();
				}
				catch(InterruptedException ie){
					ie.printStackTrace();
				}
				catch(IllegalMonitorStateException imse){
					imse.printStackTrace();
				}
			}
		}		
		//Get the request received from the message queue
		Request request = (Request) messageQueue.firstElement();
		// Set up the dialog
		SipDialog sipDialog = null;
		ServerTransaction serverTransaction = (ServerTransaction) request
				.getTransaction();
		// Get the nist-siplite dialog
		Dialog dialog = serverTransaction.getDialog();
		//If the method is an INVITE or a SUBSCRIBE, we create a new dialog
		//and add it to the list of dialog we have
		if(request.getMethod().equals(Request.INVITE) ||
		   request.getMethod().equals(Request.SUBSCRIBE) ){		   	
			sipDialog=new SipDialogImpl(
				dialog,
				this,
				request.getFromHeader().getAddress().getURI());
	        //((SipDialogImpl)sipDialog).dialog.addRoute (request);
			stackConnector.sipDialogList.addElement(sipDialog);
		}
		else if (dialog != null) {			
			sipDialog = stackConnector.findDialog(dialog.getDialogId());
		} else {
			// MESSAGE, CANCEL, OPTIONS, UPDATE...
			sipDialog = null;
		}
		
		//TODO : check security access before returning the connection
		SipServerConnection sipServerConnection=
			new SipServerConnectionImpl(
				request,
				sipDialog,
				this);
		//We remove the request from the queue				
		messageQueue.removeElement(request);
		return sipServerConnection;
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnectionNotifier#setListener(javax.microedition.nist.sip.SipServerConnectionListener)
	 */
	public void setListener(SipServerConnectionListener sscl)
		throws IOException {			
			if(!connectionOpen)
				throw new IOException("Connection was closed!");			
			this.sipServerConnectionListener=sscl;		

	}

	/**
	 * @see javax.microedition.nist.sip.SipConnectionNotifier#getLocalAddress()
	 */
	public String getLocalAddress() throws IOException {
		if(!connectionOpen)
			throw new IOException("Connection was closed!");		
		return localAddress;
	}
	
	/**
	 * Needed on Nokia phones that always return "127.0.0.1", thus breaking
	 * "Contact" and "Via" headers. You should get the real IP of your phone by
	 * other means, for instance opening a SocketConnection against your
	 * outbound proxy and then calling getLocalAddress() on that.
	 * 
	 * @deprecated The preferred method is now to set the StackConnector property
	 *             "javax.sip.IP_ADDRESS" before creating the SCN.
	 * @author Arnau Vazquez (Genaker)
	 */
	public void setLocalAddress(String newLocalAddress) throws IOException {
		if(!connectionOpen)
			throw new IOException("Connection was closed!");		
		this.localAddress = newLocalAddress;
	}

	/**
	 * @see javax.microedition.nist.sip.SipConnectionNotifier#getLocalPort()
	 */
	public int getLocalPort() throws IOException {
		if(!connectionOpen)
			throw new IOException("Connection was closed!");		
		return this.portNumber;
	}

	/**
	 * @see javax.microedition.io.Connection#close()
	 */
	public void close() throws IOException {
		if (LogWriter.needsLogging)
			LogWriter.logMessage("Closing SipConnectionNotifier at " + localAddress + ":" + portNumber);
		
		// Removing the connection from the connection list held by the
		// stackConnector.
		StackConnector.getInstance().connectionNotifiersList
				.removeElement(this);
		
		// stop the listening points and sipProvider
		SipStack sipStack = sipProvider.getSipStack();
		try {
			sipStack.deleteSipProvider(sipProvider);
		} catch (ObjectInUseException oiue) {
			throw new IOException(oiue.getMessage());
		}
		// Notification that the connection has been closed
		connectionOpen = false;

	}	
	
	/**
	 * 
	 */
	/*public void run(){
		while(connectionOpen){
			try{
				listeningThread.sleep(1);
			}
			catch(InterruptedException ie){
				ie.printStackTrace();
			}
		}
	}*/
	
	/**
	 * The stack connector notifies this class when it receive a new request
	 * @param request - the new received request
	 */
	protected void notifyRequestReceived(Request request){
		messageQueue.addElement(request);		
		//We notify the listener that a request has been received
		if(this.sipServerConnectionListener!=null)
			sipServerConnectionListener.notifyRequest(this);
		synchronized(this){
			try{
				notify();
			}
			catch(IllegalMonitorStateException imse){
				imse.printStackTrace();
			}
		}
	}
	
	/**
	 * Gets the sip provider
	 * @return the sip provider
	 */
	protected SipProvider getSipProvider(){
		return sipProvider;
	}
	
	/**
	 * Tells whether this SipConnectionNotifier
	 * was open in shared mode or dedicated mode.
	 * @return true if shared, false if dedicated
	 */
	public boolean isSharedMode() {
		return false;
	}
	
}
