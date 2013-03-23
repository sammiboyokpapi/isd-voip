package org.linphone.jortp;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.UDPDatagramConnection;

import net.rim.device.api.io.transport.TransportInfo;
import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.system.RadioInfo;

public class RtpBBTransport implements RtpTransport, Runnable {
	private DatagramConnection mConnection;
	private final Thread mThread;
	private ListHead mRecvQueue = new ListHead();
	private SocketAddress mLastRecv;
	boolean mRunning;
	private static Logger sLogger=JOrtpFactory.instance().createLogger("RtpBBTransport");
	private Datagram mReceiveDataGram;
	private RtpTransportListener mListener;

	
	public RtpBBTransport(){
		mThread=new Thread(this,"BB RtpTransport thread");
		mThread.setPriority(Thread.MAX_PRIORITY);
		mRunning=false;
		
	}
	
	
	public void close() {
		mRunning=false;
		closeConnection();
	}

	public void init(SocketAddress local) throws RtpException {

		try {
			mConnection=createDatagramCnx(local.getPort());
			mReceiveDataGram = mConnection.newDatagram(mConnection.getMaximumLength());
		} catch (IOException e) {
			sLogger.error("Could not create connection for port " + local.getPort(), e);
			throw new RtpException(e);
		}
		mRunning=true;
		
		mThread.start();
	}
	
	protected DatagramConnection createDatagramCnx(int port) throws IOException {
		String url="datagram://:"+port;
		if ((CoverageInfo.getCoverageStatus(RadioInfo.WAF_WLAN, true) & CoverageInfo.COVERAGE_DIRECT )== CoverageInfo.COVERAGE_DIRECT){
			url+="/ ;interface=wifi";
		}
		return (DatagramConnection)Connector.open(url);
	}

	public RtpPacket recvfrom() {
		RtpPacket rp=null;
		
		synchronized(mRecvQueue){
			rp=(RtpPacket)mRecvQueue.popFront();
		}
		return rp;
	}

	public synchronized void sendto(RtpPacket p) {
		if (mConnection == null ) {
			sLogger.info("cannot send message because connection is null");
			return;
		}
		StringBuffer sb=new StringBuffer();
		sb.append("datagram://");
		sb.append(p.getSocketAddress().getHost());
		sb.append(':');
		sb.append(p.getSocketAddress().getPort());
		Datagram dg;
		try {
			dg = mConnection.newDatagram(p.getBytes(),p.getRealLength(),sb.toString());
		} catch (IOException e) {
			sLogger.error("Could not create new Datagram for sending", e);
			return;
		}
		try {
			mConnection.send(dg);
		} catch (IOException e) {
			sLogger.error("Could not send datagram", e);
		}
	}

	private SocketAddress getSocketAddress(String url){
		//should be fixed, bad parsing
		int index="//".length();
		int colon=url.indexOf(':', index);
		String host=url.substring(index,colon);
		String portstr=url.substring(colon+1,url.indexOf(";"));
		int port=Integer.parseInt(portstr);
		if (mLastRecv!=null){
			if (mLastRecv.getHost().equals(host) && mLastRecv.getPort()==port){
				return mLastRecv;
			}
		}
		mLastRecv=JOrtpFactory.instance().createSocketAddress(host, port);
		return mLastRecv;
	}
	public void run() {
		try {
			while(mRunning){
				RtpPacket rp=null;
				try {
					mConnection.receive(mReceiveDataGram);
					rp=JOrtpFactory.instance().createRtpPacket(mReceiveDataGram.getLength());
				} catch (IOException e) {
					if (mRunning) {
					sLogger.error("Could not receive datagram", e);
					continue;
					} else {
						sLogger.info("rtp session closed");
						break;

					}
				}
				
				//rp.setSocketAddress(getSocketAddress(mReceiveDataGram.getAddress()));
				try {
					System.arraycopy(mReceiveDataGram.getData(),mReceiveDataGram.getOffset(), rp.getBytes(), 0, mReceiveDataGram.getLength());
					rp.setRealLength(mReceiveDataGram.getLength());
				} catch (RtpException e) {
					sLogger.error("Could not setRealLength()",e);
					continue;
				}
				if (mListener==null){
					synchronized (mRecvQueue){
						if (sLogger.isLevelEnabled(Logger.Debug)) sLogger.debug("new rtp packet received at ["+System.currentTimeMillis()+"]");
						mRecvQueue.pushBack((Node)rp);
					}
				}else{
					mListener.onPacketReceived(rp);
				}
			}
		} catch (Throwable e) {
			sLogger.error("Exiting ["+this+"]",e);
		}
		finally  {
			closeConnection();
		}
	}
	private synchronized void closeConnection() {
		if (mConnection != null)
			try {
				sLogger.info("Closing rtp connection["+this+"]");
				mConnection.close();
				mConnection = null;
			} catch (IOException e) {
				sLogger.warn("cannot close ["+this+"]" );
			}		
		
	}
	public String toString() {
		try {
			return (mConnection!=null && ((UDPDatagramConnection) mConnection).getLocalAddress()!=null)?((UDPDatagramConnection) mConnection).getLocalAddress().toString()+":"+String.valueOf(((UDPDatagramConnection) mConnection).getLocalPort()):"not set";
		} catch (Throwable e) {
			return "not set"+e.getMessage();
		}
	}
	
	public String getipaddress() {
		try {
			return ((UDPDatagramConnection) mConnection).getLocalAddress().toString();	
		} catch (Throwable e) {
			return "not set";
		}
	}


	public void setListener(RtpTransportListener l) {
		mListener=l;
	}
}
