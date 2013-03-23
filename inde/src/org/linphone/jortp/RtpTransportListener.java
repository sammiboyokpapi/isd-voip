package org.linphone.jortp;

public interface RtpTransportListener {
	void onPacketReceived(RtpPacket p);
}
