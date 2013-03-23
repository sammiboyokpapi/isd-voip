/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.stack;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;

/**
* An interface for generating new requests and responses. This is implemented
* by the application and called by the stack for processing requests
* and responses. When a Request comes in off the wire, the stack calls
* newSIPServerRequest which is then responsible for processing the request.
* When a response comes off the wire, the stack calls newSIPServerResponse
* to process the response. 
*/

public interface SIPStackMessageFactory {
	/**
	* Make a new SIPServerResponse given a Request and a message 
	* channel. This is invoked by the stack on an new incoming request.
	*@param sipRequest is the incoming SIP request.
	*@param msgChan is the message channel on which the incoming 
	* 	sipRequest was received.
	*/
	public SIPServerRequestInterface
		newSIPServerRequest(Request sipRequest, 
				    MessageChannel msgChan);

	/**
	* Generate a new server response for the stack. This is invoked
	* by the stack on a new incoming server response.
	*@param sipResponse is the incoming response.
	*@param msgChan is the message channel on which the incoming response
	* is received.
	*/
	public SIPServerResponseInterface
         newSIPServerResponse 
		(Response sipResponse, 
		  MessageChannel msgChan);

}
