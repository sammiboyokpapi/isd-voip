package sip4me.gov.nist.siplite.address;
import java.util.Enumeration;

import sip4me.gov.nist.siplite.SipStack;
import sip4me.gov.nist.siplite.message.Request;


public interface Router {
	/** Return a linked list of addresses corresponding to a requestURI.
	* This is called for sending out outbound messages for which we do
	* not directly have the request URI. The implementaion function 
	* is expected to return a linked list of addresses to which the
	* request is forwarded. The implementation may use this method
	* to perform location searches etc.
	*
	*@param sipRequest is the message to route.
	*/
	public Enumeration getNextHops(Request sipRequest);

        
        
        /** Set the outbound proxy.
	*/
	public void setOutboundProxy(String outboundProxy);

        
        /** Set the sip stack.
 	*/
	public void setSipStack( SipStack sipStack);
      
        /**
        * Get the outbound proxy.
	*/
	public Hop getOutboundProxy();
        
        
}
	
