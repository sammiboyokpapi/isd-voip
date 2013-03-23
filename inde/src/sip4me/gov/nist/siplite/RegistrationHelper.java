
package sip4me.gov.nist.siplite;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.HeaderFactory;
import sip4me.gov.nist.siplite.header.MaxForwardsHeader;
import sip4me.gov.nist.siplite.message.MessageFactory;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.ClientTransaction;


/** A helper class that runs a registration call flow.
*/

public class RegistrationHelper implements SipListener , Runnable{
	  private SipStack sipStack;  
	  private SipProvider sipProvider;
	  private MessageFactory messageFactory;
	  private HeaderFactory headerFactory;
	  private String userName;
	  private String userAddress;
	  private ListeningPoint lp;
	  private Thread myThread;
	  protected boolean successfulRegistration;
	  private AuthenticationHelper authenticationHelper;

	  public RegistrationHelper(SipStack myStack, 
			String userName,  String userAddress, 
			ListeningPoint lp ) {
		this.sipStack = myStack;
		this.userName = userName;
		this.userAddress   = userAddress;
		this.messageFactory  = new MessageFactory();
		this.headerFactory = new HeaderFactory();
		this.lp = lp;
		myThread = new Thread(this);
        //authenticationHelper=new DigestClientAuthentication();
	  }
	
	  public void doRegister() {
		myThread.start();
	       // Wait to register ourselves so we can receive messages.
	       synchronized(this) {
		try {
                    System.out.println("WAIT"); 
	           this.wait();
                   System.out.println("WAKE UP"); 
		} catch (InterruptedException ex) {
		   return;
		}
	       }
	  }

	  public void run() {
	      try {
		if (LogWriter.needsLogging)
		 	LogWriter.logMessage
			("starting registration thread");
		sipStack.stackInitialized = false;
		Hop hop = sipStack.getRouter().getOutboundProxy();

		if (LogWriter.needsLogging)
		 	LogWriter.logMessage
			("got listening point");
		sipProvider = lp.getProvider();
		StringBuffer requestLine = 
		new StringBuffer("REGISTER sip:"  )
		    .append(sipStack.getNextHop().getHost() )
		    .append( ":" ) 
		    .append( sipStack.getNextHop().getPort() )
		    .append(";transport="+hop.getTransport())
		    .append( " SIP/2.0\r\n");
		StringBuffer from  = 
			new StringBuffer( "From: <sip:")
			.append( userName )
			.append( "@" )
			.append( userAddress)
			.append( ">;tag=1234\r\n");
		StringBuffer to = 
			new StringBuffer("To: <sip:" )
			.append(userName)
			.append ("@" )
			.append( userAddress)
			.append( ">\r\n");
		String via = 
		lp.messageProcessor.getViaHeader().toString();
		int port = lp.getPort();

		StringBuffer contact = new 
			StringBuffer( 
			"Contact: <sip:"+userName+"@"+sipStack.getIPAddress()
			+":"+port+";transport="+hop.getTransport() +">\r\n") ;

		CallIdHeader callId = sipProvider.getNewCallId();

		CSeqHeader cseq = new CSeqHeader();
		cseq.setMethod(Request.REGISTER);
		cseq.setSequenceNumber(1);

		MaxForwardsHeader maxForwards = new MaxForwardsHeader();
		maxForwards.setMaxForwards(1);

		String registerRequest = 
			new StringBuffer().append(requestLine)
			.append(via).append(callId.toString())
			.append(cseq.toString())
			.append(maxForwards.toString())
			.append(from).append(to).append(contact).
			toString();

		System.out.println(registerRequest);

		Request request = messageFactory.createRequest
				(registerRequest);
		ClientTransaction ct = 
			sipProvider.getNewClientTransaction(request);
		if (LogWriter.needsLogging) 
		    LogWriter.logMessage("Got client Transaction " + ct);
                System.out.println("SENDING REGISTER TO THE PROXY");
		ct.sendRequest();
	     } catch (Exception ex) {
		if (LogWriter.needsLogging)
			LogWriter.logException(ex);
		synchronized (this) {
                   System.out.println("NOTIFY"); 
		   this.notify();
		}
	     }
	  }


	  public void processRequest (RequestEvent requestEvent) {
		System.out.println("Ignoring request");

	  }
		
	  public void processResponse (ResponseEvent responseEvent) {
		Response response = responseEvent.getResponse();
		if (response.getStatusCode() == Response.OK) {
		    this.successfulRegistration = true;
	           if (LogWriter.needsLogging) {
		      LogWriter.logMessage
			("Registration listener : sending notify!");
		   }
		   synchronized  (this) {
                       System.out.println("NOTIFY"); 
		 	this.notify(); // Authentication done!
		  } 
		}
                else {
                    // Need to call out here to the Authentication listener.
                    // check if 401 or 407
                    if ( response.getStatusCode()==Response.
                    PROXY_AUTHENTICATION_REQUIRED ||
                    response.getStatusCode()==Response.UNAUTHORIZED) {
                        try{
                            ClientTransaction clientTransac=responseEvent.
                            getClientTransaction();
                            
                            Request newRequest =
                            authenticationHelper.createNewRequest(sipStack,
                            clientTransac.getRequest(), response);
                            
                            if (newRequest==null) {
                                if (LogWriter.needsLogging)
                                    LogWriter.logMessage("Authentication failed...");
                                return;
                            }
                            
                            ClientTransaction ct =
                            sipProvider.getNewClientTransaction(newRequest);
                            if (LogWriter.needsLogging)
                                LogWriter.logMessage("Got client Transaction " + ct);
                            
                            ct.sendRequest();
                            if (LogWriter.needsLogging)
                                LogWriter.logMessage("RegistrationHelper: request sent:\n"+
                                newRequest);
                            
                        } catch (Exception ex) {
                            if (LogWriter.needsLogging)
                                LogWriter.logMessage("RegistrationHelper: processResponse(),"+
                                " exception raised: "+ex.getMessage());
                        }
                    }
                }
          }

	  public void processTimeout 
		( TimeoutEvent timeoutEvent ) {
		synchronized  (this) {
                    System.out.println("NOTIFY"); 
		 	this.notify(); // Authentication done!
		} 

	  }

    }
