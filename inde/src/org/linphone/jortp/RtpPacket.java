package org.linphone.jortp;

public interface RtpPacket {
	public byte [] getBytes();;
	public int getRealLength();
	public void setRealLength(int len) throws RtpException;
	public int getPhysicalSize();
	public int getDataOffset();
	public void setMarkerBit(boolean bit);
	public boolean getMarkerBit();
	public int getTimestamp();
	public int getPayloadType();
	public int getSeqNumber();
	public int getSSRC();
	public void setSocketAddress(SocketAddress sockaddr);
	SocketAddress getSocketAddress();
	public String toString();
	public void recycle();
};
