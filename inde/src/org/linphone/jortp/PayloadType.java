package org.linphone.jortp;

public interface PayloadType {
	public static class MediaType{
		public static MediaType Audio = new MediaType("Audio");
		public static MediaType Video = new MediaType("Video");
		public static MediaType Other = new MediaType("Other");
		private String mStringValue;
		private MediaType(String aStringValue) {
			mStringValue = aStringValue;
		}
		public String toString() {
			return mStringValue;
		}
	};
	public void setType(MediaType mt);
	public MediaType getType();
	public void setMimeType(String mime);
	public String getMimeType();
	public void setClockRate(int rate);
	public int getClockRate();
	public void setNumChannels(int nchans);
	public int getNumChannels();
	public int getNumber();
	public void setNumber(int number);
	public String getSendFmtp();
	public String getRecvFmtp();
	public void setSendFmtp(String value);
	public void appendSendFmtp(String value);
	public void setRecvFmtp(String value);
	public void appendRecvFmtp(String value);
	public PayloadType clone();
	public boolean equals(PayloadType pt);
}
