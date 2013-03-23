package org.linphone.jortp;


public interface RtpSession {
	public void setTimestampClock(TimestampClock tsc);
	public void setProfile(RtpProfile prof);
	public void setSendPayloadTypeNumber(int pt);
	public void setRecvPayloadTypeNumber(int pt);
	public int getRecvPayloadTypeNumber();
	public int getSendPayloadTypeNumber();
	public void setLocalAddr(SocketAddress addr) throws RtpException;
	public void setRemoteAddr(SocketAddress addr) throws RtpException;
	public void sendPacket(RtpPacket p, int timeStamp) throws RtpException;
	public RtpPacket recvPacket(int timeStamp) throws RtpException;
	public void setJitterBufferParams(JitterBufferParams params);
	public JitterBufferParams getJitterBufferParams();
	public void setTransport(RtpTransport rtpt);
	public RtpStatistics getRecvStats();
	public RtpStatistics getSendStats();
	public JitterBufferStatistics getJitterBufferStatistics();
	public void close();
	public RtpProfile getProfile();
}
