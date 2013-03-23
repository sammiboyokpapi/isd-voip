package sip4me.gov.nist.siplite;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.siplite.stack.MessageProcessor;

/** Implementation of the ListeningPoint interface
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class ListeningPoint  {
    
    protected String host;
    
    protected String transport;
    
    /** My port. (same thing as in the message processor) */

    int port;
   
    /** pointer to the embedded mesage processor.
     */
    protected MessageProcessor messageProcessor;
    
    /** Provider back pointer
     */    
    protected SipProvider sipProviderImpl;
   
    /** Our stack
     */    
    protected SipStack sipStack;
    
   
    
    /** Construct a key to refer to this structure from the SIP stack
     * @param host host string
     * @param port port
     * @param transport transport
     * @return a string that is used as a key
     */    
    public static String makeKey(String host, int port, String transport) {
	if (Utils.equalsIgnoreCase(transport,"tcp")) {
		// can support only one outgoing TCP connection.
		return new StringBuffer(host).append(":/").append(transport).
			toString().toLowerCase();
	} else return new StringBuffer(host).
                    append(":").
                    append(port).
                    append("/").
                    append(transport).
                    toString().
                    toLowerCase();
    }
    
    /** Get the key for this strucut
     * @return  get the host
     */    
    protected String getKey() {
        return makeKey(host,port,transport);
    }
    
    
    /** set the sip provider for this structure.
     * @param sipProviderImpl provider to set
     */    
    protected void setSipProvider(SipProvider sipProviderImpl) {
        this.sipProviderImpl = sipProviderImpl;
    }
    
    /** remove the sip provider from this listening point.
     */    
    protected void removeSipProvider() { 
        this.sipProviderImpl = null;
    }
    
    /**
     * Constructor
     * @param sipStack Our sip stack
     */
	protected ListeningPoint(SipStack sipStack, int port, String transport) {
		this.sipStack = (SipStack) sipStack;
		this.host = sipStack.getIPAddress();
		this.port = port;
		this.transport = transport;
	}
    
    
  
    
    
    /** Gets host name of this ListeningPoint
     *
     * @return host of ListeningPoint
     */
    public String getHost() {
        return this.sipStack.getHostAddress();
    }
    
    /** Gets the port of the ListeningPoint. The default port of a ListeningPoint
     * is dependent on the scheme and transport.  For example:
     * <ul>
     * <li>The default port is 5060 if the transport UDP the scheme is <i>sip:</i>.
     * <li>The default port is 5060 if the transport is TCP the scheme is <i>sip:</i>.
     * <li>The default port is 5060 if the transport is SCTP the scheme is <i>sip:</i>.
     * <li>The default port is 5061 if the transport is TLS over TCP the scheme is <i>sip:</i>.
     * <li>The default port is 5061 if the transport is TCP the scheme is <i>sips:</i>.
     * </ul>
     *
     * @return port of ListeningPoint
     */
    public int getPort() {
        return messageProcessor.getPort();
    }
    
    /** Gets transport of the ListeningPoint.
     *
     * @return transport of ListeningPoint
     */
    public String getTransport() {
        return messageProcessor.getTransport();
    }

   /** Get the provider.
    *
    *@return the provider.
    */
    public SipProvider getProvider() {
		return this.sipProviderImpl;
    }
    
    
}
