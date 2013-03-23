package org.linphone.jortp;

public interface SocketAddress {
	public static class Family{
		public static Family Inet = new Family("Inet");
		public static Family Inet6 = new Family("Inet6");
		private String mStringValue;
		private Family(String aStringValue) {
			mStringValue = aStringValue;
		}
		public String toString() {
			return mStringValue;
		}
	}
	public String getHost();
	int getRawAddress() throws RtpException;
	int getPort();
	Family getFamily();
}
