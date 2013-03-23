/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
 *******************************************************************************/

package sip4me.gov.nist.siplite;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;
import sip4me.gov.nist.siplite.stack.MessageChannel;
import sip4me.gov.nist.siplite.stack.SIPServerRequestInterface;
import sip4me.gov.nist.siplite.stack.SIPServerResponseInterface;
import sip4me.gov.nist.siplite.stack.SIPStackMessageFactory;
import sip4me.gov.nist.siplite.stack.SIPTransactionStack;
import sip4me.gov.nist.siplite.stack.Transaction;

/**
 * Implements all the support classes that are necessary for the nist-sip
 * stack on which the jain-sip stack has been based.
 * This is a mapping class to map from the NIST-SIP abstractions to
 * the JAIN abstractions. (i.e. It is the glue code that ties
 * the NIST-SIP event model and the JAIN-SIP event model together.
 * When a SIP Request or SIP Response is read from the corresponding
 * messageChannel, the NIST-SIP stack calls the SIPStackMessageFactory
 * implementation that has been registered with it to process the request.)
 *
 *@version  JAIN-SIP-1.1
 *
 *@author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */

public class NistSipMessageFactoryImpl
implements SIPStackMessageFactory {
    
    
    /**
     *Construct a new SIP Server Request.
     *@param sipRequest is the Request from which the SIPServerRequest
     * is to be constructed.
     *@param messageChannel is the MessageChannel abstraction for this
     * 	SIPServerRequest.
     */
    public SIPServerRequestInterface
    newSIPServerRequest
    ( Request sipRequest, MessageChannel messageChannel )
    throws IllegalArgumentException{
        
        if (messageChannel == null || sipRequest == null )  {
            throw new IllegalArgumentException("Null Arg!");
        }
        
        NistSipMessageHandlerImpl retval = new NistSipMessageHandlerImpl();
        if (messageChannel instanceof Transaction) {
            // If the transaction has already been created
            // then set the transaction channel.
            retval.transactionChannel = (Transaction)messageChannel;
        }
        SIPTransactionStack theStack =
        (SIPTransactionStack) messageChannel.getSIPStack();
        
        retval.listeningPoint =
        messageChannel.getMessageProcessor().getListeningPoint();
        
        if (LogWriter.needsLogging)
            LogWriter.logMessage("Returning request interface for " +
            sipRequest.getFirstLine() + " " + retval +
            " messageChannel = " + messageChannel );
        
        return  retval;
    }
    
    /**
     * Generate a new server response for the stack.
     *@param sipResponse is the Response from which the SIPServerResponse
     * is to be constructed.
     *@param messageChannel is the MessageChannel abstraction for this
     * 	SIPServerResponse
     */
    public SIPServerResponseInterface
    newSIPServerResponse(Response sipResponse,
    MessageChannel messageChannel) {
        try {
            NistSipMessageHandlerImpl retval = new NistSipMessageHandlerImpl();
            SIPTransactionStack theStack = (SIPTransactionStack)
            messageChannel.getSIPStack();
			// Tr is null if a transaction is not mapped.
			Transaction tr = (theStack).findTransaction(sipResponse, false);

            retval.transactionChannel = tr;
            if (LogWriter.needsLogging)  {
                LogWriter.logMessage("Found Transaction " + tr + " for " +
                sipResponse);
                LogWriter.logMessage("MessageProcessor = " +
                messageChannel.getMessageProcessor() + "/"+
                messageChannel.getMessageProcessor().
                getListeningPoint());
            }
            
            ListeningPoint lp =
            messageChannel.getMessageProcessor().getListeningPoint();
            retval.listeningPoint = lp;
            return  retval;
            
        } catch (RuntimeException ex) {
            System.out.println("runtime exception caught!");
            ex.printStackTrace();
            return null;
        }
        
    }
    
    
    public NistSipMessageFactoryImpl() {
    }
    
    
}
