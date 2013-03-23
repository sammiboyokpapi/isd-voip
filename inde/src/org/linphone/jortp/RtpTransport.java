package org.linphone.jortp;


public interface RtpTransport {
	void init(SocketAddress local) throws RtpException;
	void setListener(RtpTransportListener l);
	RtpPacket recvfrom();
	void sendto(RtpPacket p);
	void close();
	String getipaddress();
}
