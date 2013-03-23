package org.linphone.jortp;

public interface RtpProfile {
	public void setPayloadType(PayloadType pt, int pos);
	public PayloadType getPayloadType(int pos);
	public PayloadType findPayloadType(String mime, int clockrate, int[] position);
}
