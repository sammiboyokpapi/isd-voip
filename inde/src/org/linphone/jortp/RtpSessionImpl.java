package org.linphone.jortp;

import java.util.Random;

public class RtpSessionImpl implements RtpSession {
	private SocketAddress mLocalAddr;
	private SocketAddress mRemoteAddr;
	private RtpProfile mProfile;
	private RtpTransport mRtpTransport;
	private JitterBuffer mJitterBuffer;
	private JitterBufferController mJitterBufferController;
	private RtpStatisticsImpl mRecvStats;
	private RtpStatisticsImpl mSendStats;
	private PayloadType mRecvPtObj;
	private int mRecvPt;
	private int mSendPt;
	private int mSendSeq;
	private int mRecvSSRC;
	private int mSentSSRC;
	private int mClockrate;
	private JitterBufferStatistics mJitterBufferStats;
	private TimestampClock mTsc;
	private final static Logger mLog = JOrtpFactory.instance().createLogger("jortp");
	
	RtpSessionImpl(){
		mProfile=RtpProfileImpl.createAVProfile();
		mJitterBuffer=new JitterBuffer(){
			public void onDrop(RtpPacket p) {
				mRecvStats.mLate++;
			}
		};
		mJitterBufferStats=new JitterBufferStatistics();
		mJitterBufferController=new JitterBufferController(){
			public void onTimestampJump() {
				if (mLog.isLevelEnabled(Logger.Warn)) mLog.warn("Timestamp jump detected !");
				mJitterBuffer.purge();
				mRecvStats.mCount=0;
			}
		};
		mRecvStats=new RtpStatisticsImpl();
		mSendStats=new RtpStatisticsImpl();
		mRecvPt=0;
		mSendPt=0;
		mRecvPtObj=null;
		mSendSeq=0;
		mSentSSRC=new Random().nextInt();
		mClockrate=8000;
		mRtpTransport=JOrtpFactory.instance().createDefaultTransport();
	}
	
	public JitterBufferParams getJitterBufferParams() {
		return mJitterBufferController.getParams();
	}
	
	public void setJitterBufferParams(JitterBufferParams params) {
		mJitterBufferController.setParams(params);
	}
	
	public JitterBufferStatistics getJitterBufferStatistics(){
		mJitterBufferStats.mDelay=((float)mJitterBuffer.getDelayTs())/(float)mClockrate;
		mJitterBufferStats.mSize=((float)mJitterBuffer.getTimestampDiff())/(float)mClockrate;
		return mJitterBufferStats;
	}
	private void updateRecvPt(int pt){
		PayloadType ptobj;
		if (pt!=mRecvPt){
			ptobj=mProfile.getPayloadType(pt);
			if (ptobj!=null){
				if (mRecvPtObj!=null){
					mLog.warn("Incoming payload type changed to "+pt);
				}
				mRecvPtObj=ptobj;
				mClockrate=ptobj.getClockRate();
			}
			mRecvPt=pt;
		}
	}
	private void processIncomingPacket(RtpPacket p, int user_recvts){
		if (p.getSocketAddress() != null) {
			mRemoteAddr = p.getSocketAddress();
		}
		mRecvStats.mCount++;
		try {
			((RtpPacketImpl)p).readHeader();
		} catch (RtpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		updateRecvPt(p.getPayloadType());
		mJitterBufferController.newIncomingPacket(p, user_recvts,mClockrate);
		mJitterBuffer.put(p);
	}
	
	private void readFromTransport(int user_recvts) throws RtpException{
		RtpPacket p;
		while((p=mRtpTransport.recvfrom())!=null){
			processIncomingPacket(p,user_recvts);
		}
	}
	
	public RtpPacket recvPacket(int user_ts) throws RtpException {
		RtpPacket p;
		int stream_ts;
		if (mTsc==null) readFromTransport(user_ts);
		synchronized(mJitterBuffer){
			stream_ts=mJitterBufferController.convertTimestamp(user_ts);
			p=mJitterBuffer.get(stream_ts);
			if (p!=null){
				mRecvSSRC=p.getSSRC();
				mRecvPt=p.getPayloadType();
				mRecvStats.mPlayed++;
			}
		}
		return p;
	}

	public void sendPacket(RtpPacket p, int timeStamp) throws RtpException {
		RtpPacketImpl pi=(RtpPacketImpl)p;
		pi.setTimestamp(timeStamp);
		pi.setSeqNumber(mSendSeq);
		pi.setPayloadType(mSendPt);
		pi.setSSRC(mSentSSRC);
		pi.writeHeader();
		pi.setSocketAddress(mRemoteAddr);
		mRtpTransport.sendto(p);
		mSendSeq++;
		mSendStats.mCount++;
	}

	public void setLocalAddr(SocketAddress addr) throws RtpException {
		mLocalAddr=addr;
		mRtpTransport.init(mLocalAddr);
	}
	
	public void setRemoteAddr(SocketAddress addr) throws RtpException {
		mRemoteAddr=addr;
	}

	public void setProfile(RtpProfile prof) {
		mProfile=prof;
	}

	public void setRecvPayloadTypeNumber(int pt) {
		updateRecvPt(pt);
	}

	public void setSendPayloadTypeNumber(int pt) {
		mSendPt=pt;
	}
	
	public void setTransport(RtpTransport rtpt){
		mRtpTransport=rtpt;
		setup();
	}
	
	public void setSentSSRC(int ssrc){
		mSentSSRC=ssrc;
	}
	
	public int getSentSSRC(){
		return mSentSSRC;
	}
	
	public int getRecvSSRC(){
		return mRecvSSRC;
	}

	public RtpStatistics getRecvStats() {
		int lost=(int) (mJitterBufferController.getRelativeSeqNumber()+1 - mRecvStats.mCount);
		mRecvStats.mLost=lost;
		return mRecvStats;
	}

	public RtpStatistics getSendStats() {
		return mSendStats;
	}

	public int getRecvPayloadTypeNumber() {
		return mRecvPt;
	}

	public void close() {
		mRtpTransport.close();
	}

	public RtpProfile getProfile() {
		return mProfile;
	}

	public int getSendPayloadTypeNumber() {
		return mSendPt;
	}

	public void setTimestampClock(TimestampClock tsc) {
		mTsc=tsc;
		setup();
	}
	private void setup(){
		if (mTsc!=null){
			mRtpTransport.setListener(new RtpTransportListener(){
				public void onPacketReceived(RtpPacket p) {
					synchronized(mJitterBuffer){
						processIncomingPacket(p,mTsc.getCurrentTimestamp());
					}
					
				}
			});
		}
	}
}
