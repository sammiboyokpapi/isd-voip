package org.linphone.jortp;

public class JitterBufferStatistics {
	public float mSize=0;
	public float mDelay=0;
	/**
	 * Returns the current size of the jitter buffer in seconds.
	 * It represents the number of seconds of audio or video present in the jitter buffer.
	 * @return
	 */
	public float getCurrentSize(){
		return mSize;
	}
	/**
	 * Returns
	 * @return
	 */
	public float getDelay(){
		return mDelay;
	}
	public String toString(){
		return "current size= "+mSize+" seconds\ndelay= "+mDelay+" seconds";
	}
}
