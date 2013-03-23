package org.linphone.jortp;

public interface RtpStatistics {
	long getPacketCount();
	long getLostPacketCount();
	long getLatePacketCount();
	long getPlayedPacketCount();
	String toString();
}
