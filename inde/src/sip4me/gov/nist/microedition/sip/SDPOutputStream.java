/*
 * SDPOutputStream.java
 * 
 * Created on Feb 20, 2004
 *
 */
package sip4me.gov.nist.microedition.sip;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import sip4me.gov.nist.siplite.message.Request;
import sip4me.nist.javax.microedition.sip.SipConnection;


/**
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SDPOutputStream extends ByteArrayOutputStream{
	private SipConnection connection=null;

	/**
	 * 
	 * @param connection - 
	 */
	public SDPOutputStream(SipConnection connection) {
		this.connection=connection;					
	}
	
	/**
	 * Close the SDPOutputStream and send the message held by the sip connection
	 */
	public void close(){
		if(connection instanceof SipClientConnectionImpl){			
			SipClientConnectionImpl sipClientConnection=
				(SipClientConnectionImpl)connection;
			//If the client connection is in a STREAM_OPEN state and 
			//the request is an ACK
			//The connection goes into the COMPLETED state
			if(sipClientConnection.state==SipClientConnectionImpl.STREAM_OPEN){
				if(sipClientConnection.getMethod().equals(Request.ACK)){					
					sipClientConnection.state=SipClientConnectionImpl.COMPLETED;
					try{
						super.close();
					}
					catch(IOException ioe){
						ioe.printStackTrace();
					}
					return;
				}							
			}
		}
		try{
			connection.send();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		try{
			super.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public byte[] toByteArray() {
		return super.toByteArray();
	}
	
}
