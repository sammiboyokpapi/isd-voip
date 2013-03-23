package org.linphone.jortp;

public class RtpStatisticsImpl implements RtpStatistics {
	protected long mCount;
	protected long mLost;
	protected long mLate;
	protected long mPlayed;
	
	public RtpStatisticsImpl(){
		mCount=0;
		mLost=0;
		mLate=0;
	}
	
	public long getLatePacketCount() {
		return mLate;
	}

	public long getLostPacketCount() {
		return mLost;
	}

	public long getPacketCount() {
		return mCount;
	}
	public String toString(){
		return "Count=" + mCount + "\nLost=" + mLost + "\nLate=" +mLate
			+"\nPlayed=" + mPlayed;
	}

	public long getPlayedPacketCount() {
		return mPlayed;
	}
}
