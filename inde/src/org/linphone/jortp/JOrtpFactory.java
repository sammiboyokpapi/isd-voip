package org.linphone.jortp;

public class JOrtpFactory {
	private static JOrtpFactory sInstance=null;
	private ListHead mRecycledPackets;
	private static Logger sLogger=Logger.getLogger("jortp");
	public static JOrtpFactory instance(){
		if (sInstance==null)
			sInstance=new JOrtpFactory();
		return sInstance;
	}
	public Logger createLogger(String domain){
		return Logger.getLogger(domain);
	}
	public RtpPacket createRtpPacket(int dataSize){
		RtpPacket p;
		synchronized(mRecycledPackets){
			if (!mRecycledPackets.empty()){
				RtpPacket c=(RtpPacket)mRecycledPackets.popFront();
				if (c.getPhysicalSize()>=dataSize+RtpPacketImpl.sRtpHeaderSize){
					byte b[]=c.getBytes();
					for(int  i=0;i<b.length;++i){
						b[i]=0;
					}
					try {
						c.setRealLength(dataSize+RtpPacketImpl.sRtpHeaderSize);
						return c;
					} catch (RtpException e) {
						sLogger.warn("Cannot recycle rtp packet, creating new",e);
					}
					
				}
			}
		}
		p=new RtpPacketImpl(dataSize){
			public void recycle(){
				synchronized(mRecycledPackets){
					mRecycledPackets.pushBack(this);
				}
			}
		};
		return p;
	}
	public PayloadType createPayloadType(){
		return new PayloadTypeImpl(null, null, 0, 0);
	}
	public RtpProfile createRtpProfile(){
		return new RtpProfileImpl();
	}
	public RtpProfile createRfc3551Profile(){
		return RtpProfileImpl.createAVProfile();
	}
	public RtpSession createRtpSession(){
		return new RtpSessionImpl();
	}
	public SocketAddress createSocketAddress(String host, int port){
		return new SocketAddressImpl(host, port);
	}
	private JOrtpFactory(){
		mRecycledPackets=new ListHead();
	}
	public RtpTransport createDefaultTransport() {
		try {
			Class clasz = Class.forName("org.linphone.jortp.RtpJ2seTransport");
			return (RtpTransport) clasz.newInstance();
		} catch (Exception e) {
			try{
				Class clazz= Class.forName("org.linphone.jortp.RtpBBTransport");
				return (RtpTransport) clazz.newInstance();
			}catch (Exception e2){
				throw new RuntimeException("Cannot find rtp transport ");
			}
		}
		
	}
}
