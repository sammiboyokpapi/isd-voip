package org.linphone.jortp;

class PayloadTypeImpl implements PayloadType {
	private String mMimeType;
	private int mClockRate;
	private int mChannels;
	private MediaType mType;
	private int mNumber;
	private String mRecvFmtp;
	private String mSendFmtp;
	
	PayloadTypeImpl(MediaType type, String mime, int clockrate, int chans){
		mType=type;
		mMimeType=mime;
		mClockRate=clockrate;
		mChannels=chans;
	}
	
	public int getClockRate() {
		return mClockRate;
	}

	public String getMimeType() {
		return mMimeType;
	}

	public int getNumChannels() {
		return mChannels;
	}

	public void setClockRate(int rate) {
		mClockRate=rate;
	}

	public void setMimeType(String mime) {
		mMimeType=mime;
	}

	public void setNumChannels(int nchans) {
		mChannels=nchans;
	}

	public void setType(MediaType mt) {
		mType=mt;
	}
	
	public MediaType getType(){
		return mType;
	}
	
	public static PayloadType createAudio(String mime, int clockrate){
		return new PayloadTypeImpl(MediaType.Audio,mime,clockrate,1);
	}

	public int getNumber() {
		return mNumber;
	}

	public void setNumber(int number) {
		mNumber=number;
	}

	public void appendRecvFmtp(String value) {
		if (mRecvFmtp==null) mRecvFmtp=value;
		else if (value!=null){
			mRecvFmtp=mRecvFmtp+value;
		}
	}

	public void appendSendFmtp(String value) {
		if (mSendFmtp==null) mSendFmtp=value;
		else if (value!=null){
			mSendFmtp=mSendFmtp+value;
		}
	}

	public String getRecvFmtp() {
		return mRecvFmtp;
	}

	public String getSendFmtp() {
		return mSendFmtp;
	}

	public void setRecvFmtp(String value) {
		mRecvFmtp=value;
	}

	public void setSendFmtp(String value) {
		mSendFmtp=value;
	}

	public PayloadType clone() {
		PayloadTypeImpl obj=new PayloadTypeImpl(mType,mMimeType,mClockRate,mChannels);
		obj.mRecvFmtp=mRecvFmtp;
		obj.mSendFmtp=mSendFmtp;
		return obj;
	}

	public boolean equals(PayloadType pt) {
		return mClockRate==pt.getClockRate() &&
			mChannels==pt.getNumChannels() &&
			mType==pt.getType() &&
			mNumber==pt.getNumber() &&
			mMimeType.equals(pt.getMimeType());
	}

}
