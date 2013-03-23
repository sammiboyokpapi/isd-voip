package org.linphone.jortp;

public class SocketAddressImpl implements SocketAddress {
	private String mHost;
	int mPort;
	int mRawAddress;
	boolean mRawAddressOk;
	
	public SocketAddressImpl(String host, int port){
		mHost=host;
		mPort=port;
	}
	
	public Family getFamily() {
		return Family.Inet;
	}

	public String getHost() {
		return mHost;
	}

	public int getPort() {
		return mPort;
	}
	public String toString() {
		return getHost()+":"+getPort();
	}

	public int getRawAddress() throws RtpException {
		if (!mRawAddressOk){
			long addr=0;
			int i;
			int begin=0;
			int bytecount=0;
			for(i=0;i<mHost.length();++i){
				if (mHost.charAt(i)=='.'){
					String tmp=mHost.substring(begin,i);
					begin=i+1;
					long num=Integer.parseInt(tmp);
					addr|=num<<(24-(8*bytecount));
					bytecount++;
				}
			}
			{
				String tmp=mHost.substring(begin,i);
				long num=Integer.parseInt(tmp);
				addr|=num;
				bytecount++;
			}
			if (bytecount!=4){
				throw new RtpException("Not an IP address");
			}
			addr&=0xffffffff;
			mRawAddress=(int)addr;
		}
		return mRawAddress;
	}
}
