package org.linphone.jortp;

abstract class JitterBuffer extends ListHead {
	private int mCount;
	private int mLastReturnedTs;
	private int mTimestampDiff,mDelayTs;
	private boolean mPermissive,mStarted;
	private static Logger sLogger=JOrtpFactory.instance().createLogger("jortp");
	public JitterBuffer(){
		mCount=0;
		mPermissive=false;
		mStarted=false;
		mTimestampDiff=0;
		mDelayTs=0;
	}
	public void setPermissive(boolean val){
		mPermissive=val;
	}
	public int getTimestampDiff(){
		return mTimestampDiff;
	}
	public int getDelayTs(){
		return mDelayTs;
	}
	public void put(RtpPacket p){
		Node it;
		
		if (sLogger.isLevelEnabled(Logger.Debug)) sLogger.debug("Packet is entering jitter buffer: "+p);
		
		if (mStarted && !mPermissive && (mLastReturnedTs-p.getTimestamp()>0)){
			//if the packet is strictly older than the last returned, discard it
			onDrop(p);
			p.recycle();
			if (sLogger.isLevelEnabled(Logger.Debug)) sLogger.debug("Discarding incoming packet too old: mLastReturned="+mLastReturnedTs+"timestamp="+p.getTimestamp());
			return;
		}
		//insert by decreasing sequence number (usually on top)
		if (empty()){
			end().insertBefore((Node)p);
			mCount++;
			return;
		}
		for(it=begin();it!=end();it=it.getNext()){
			RtpPacketImpl itp=(RtpPacketImpl)it;
			if (((RtpPacket)p).getSeqNumber() > itp.getSeqNumber()){
				itp.insertBefore((RtpPacketImpl)p);
				mCount++;
				return;
			}
			if (((RtpPacket)p).getSeqNumber() == itp.getSeqNumber()){
				p.recycle();
				break;
			}
		}
	}
	public RtpPacket get(int ts){
		Node it;
		RtpPacket itp=null;
		RtpPacket newer=null;
		
		if (!empty()){
			RtpPacket first,last;
			first=(RtpPacket)begin();
			last=(RtpPacket)rend();
			mDelayTs=first.getTimestamp()-ts;
			mTimestampDiff=first.getTimestamp()-last.getTimestamp();
		}
		
		// find a timestamp older or equal than user's supplied
		for (it=rend();it!=rbegin();it=it.getPrev()){
			itp=(RtpPacket)it;
			
			if (mPermissive){
				newer=itp;
				//return the oldest packet immediately
				break;
			}
			if (timestampNewerOrEqual(ts,itp.getTimestamp())){
				/* this packet is older than the queried timestamp, this a candidate
				 * but we must continue to iterate: we need to return the most recent
				 * of the packets older than the queried timestamp. 
				 */
				
				if (newer!=null){
					/*
					 * 'newer' was the last candidate but we found a best one.
					 * Drop it.
					 */
					((Node)newer).remove();
					onDrop(newer);
					mCount--;
					newer.recycle();
				}
				newer=itp;
			}else{
				/*
				 * we are seeing packets that are more recent than the queried timestamp. 
				 * Stop.
				 */
				break;
			}
		}
		if (newer!=null){
			mStarted=true;
			((Node)newer).remove();
			mLastReturnedTs=newer.getTimestamp();
			mCount--;
		}
		if (newer!=null){
			if (sLogger.isLevelEnabled(Logger.Debug)) sLogger.debug("Looking for "+ts+" returning "+newer.getTimestamp());
		}else{
			if (sLogger.isLevelEnabled(Logger.Debug)) sLogger.debug("Looking for "+ts+" returning nothing");
		}
		return newer;
	}
	public int getCount(){
		return mCount;
	}
	public void purge(){
		Node it;
		while ((it=popFront()) != null) {
			((RtpPacket)it).recycle();
		}
		mCount=0;
		mStarted=false;
	}
	public abstract void onDrop(RtpPacket p);
	private boolean timestampNewerOrEqual(int ts1, int ts2){
		return (ts1-ts2)>=0;
	}
}
