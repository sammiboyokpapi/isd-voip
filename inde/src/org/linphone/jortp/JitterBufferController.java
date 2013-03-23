package org.linphone.jortp;

abstract class JitterBufferController {
	private JitterBufferParams mParams;
	private boolean mStarted;
	private double mDiffMean;
	private long mDiffCurrent;
	private double mJitter;
	private double mCurrentSize;
	private int mTsJump=0;
	private int mFirstSeq;
	private int mLastSeq;
	static private double sSmoothFactor=(double) 0.01; 
	private static Logger sLogger=JOrtpFactory.instance().createLogger("jortp");
	
	JitterBufferController(){
		mParams=new JitterBufferParamsImpl();
		mStarted=false;
	}
	public void setParams(JitterBufferParams params){
		mParams=params;
	}
	public void resync(){
		mStarted=false;
	}
	public boolean isStarted(){
		return mStarted;
	}
	abstract public void onTimestampJump();
	public void newIncomingPacket(RtpPacket p, int user_recvts, int clockrate){
		long ldiff=p.getTimestamp()-user_recvts;
		double diff=ldiff;
		
		if (mStarted){
			if (mParams.isAdaptive()){
				double diff2;
				double adapt;
				
				
				mDiffMean=(sSmoothFactor*diff)+((1-sSmoothFactor)*mDiffMean);
				diff2=diff-mDiffMean;
				mJitter=(sSmoothFactor*diff2)+((1-sSmoothFactor)*mJitter);
				adapt=mDiffMean-mDiffCurrent;
				if (adapt>=160){
					mDiffCurrent+=160;
					if (sLogger.isLevelEnabled(Logger.Info)) sLogger.info("mDiffCurrent="+mDiffCurrent);
				}else if (adapt<=-160){
					mDiffCurrent-=160;
					if (sLogger.isLevelEnabled(Logger.Info)) sLogger.info("mDiffCurrent="+mDiffCurrent);
				}
			}
			//check for timestamp jumps
			if (Math.abs(mDiffCurrent-ldiff)>mTsJump){
				onTimestampJump();
				mStarted=false;
			}
		}
		
		if (mStarted==false){
			mCurrentSize=(mParams.getNominalSize()*clockrate)/1000;
			mTsJump=clockrate*5; //5 seconds difference between timestamps
			mDiffMean=diff;
			mDiffCurrent=ldiff;
			mFirstSeq=p.getSeqNumber();
			mStarted=true;
			if (sLogger.isLevelEnabled(Logger.Info)) sLogger.info("JitterBufferControl started, mCurrentSize="+mCurrentSize
					+ " mDiffCurrent="+mDiffCurrent);
		}
		mLastSeq=p.getSeqNumber();
	}
	public int convertTimestamp(int user_ts){
		return (int)((long)user_ts+mDiffCurrent-(int)mCurrentSize);
	}
	public int getCurrentSize(){
		return (int)mCurrentSize;
	}
	public JitterBufferParams getParams() {
		return mParams;
	}
	public int getRelativeSeqNumber(){
		return mLastSeq-mFirstSeq;
	}
}
