package org.linphone.jortp;

class RtpPacketImpl extends Node implements RtpPacket {
	private byte [] mBuffer;
	private int mPhysSize;
	private int mRealLength;
	private int mTimestamp;
	private int mSeqNumber;
	private int mPayloadType;
	private int mSSRC;
	private boolean mMarker;
	private SocketAddress mSocketAddress;
	static public int sRtpHeaderSize=12;
	
	public RtpPacketImpl(int dataSize){
		mPhysSize=sRtpHeaderSize+dataSize;
		mBuffer=new byte[mPhysSize];
		mRealLength=mPhysSize;
		mTimestamp=0;
		mSeqNumber=0;
		mPayloadType=0;
		mSSRC=0;
	}
	public byte[] getBytes() {
		return mBuffer;
	}
	public int getDataOffset(){
		return sRtpHeaderSize;
	}
	public int getDataLength() {
		int len=mRealLength-sRtpHeaderSize;
		if (len>0) return len;
		return 0;
	}
	public void setRealLength(int reallen) throws RtpException{
		if (reallen>mPhysSize){
			throw new RtpException("new size exceeds physical storage of packet !");
		}
		mRealLength=reallen;
	}
	public int getPhysicalSize() {
		return mPhysSize;
	}
	public int getRealLength() {
		return mRealLength;
	}
	public void setTimestamp(int ts){
		mTimestamp=ts;
	}
	public int getTimestamp(){
		return mTimestamp;
	}
	public void setSeqNumber(int seq){
		mSeqNumber=seq;
	}
	public int getSeqNumber(){
		return mSeqNumber;
	}
	public void setPayloadType(int pt){
		mPayloadType=pt;
	}
	public int getPayloadType(){
		return mPayloadType;
	}
	public void setSSRC(int ssrc){
		mSSRC=ssrc;
	}
	public int getSSRC(){
		return mSSRC;
	}
	public void setMarkerBit(boolean bit){
		mMarker=bit;
	}
	public boolean getMarkerBit(){
		return mMarker;
	}
	private void writeUint32(byte[] buf, int pos, int val){
		buf[pos+0]=(byte)((val >>> 24) & 0xff);
		buf[pos+1]=(byte)((val >>> 16) & 0xff);
		buf[pos+2]=(byte)((val >>> 8) & 0xff);
		buf[pos+3]=(byte)(val & 0xff);
	}
	private int readUint32(byte[] buf, int pos){
		int b0,b1,b2,b3;
		b0=((int)buf[pos])<<24;
		b1=( ((int)buf[pos+1])<<16) & 0xffffff;
		b2=( ((int)buf[pos+2])<<8) & 0xffff;
		b3=((int)buf[pos+3]) & 0xff;
		long ret=b0|b1|b2|b3;
		return (int)ret;
	}
	public void writeHeader(){
		mBuffer[0]=(byte)(2<<6);
		mBuffer[1]=(byte)( ((mMarker? 1: 0) << 7 | mPayloadType));
		mBuffer[2]=(byte)(mSeqNumber>>>8);
		mBuffer[3]=(byte)(mSeqNumber & 0xff);
		writeUint32(mBuffer,4,mTimestamp);
		writeUint32(mBuffer,8,mSSRC);
	}
	public void readHeader() throws RtpException{
		int ver;
		if (mRealLength<12)
			throw new RtpException("Too short to be a RTP packet");
		ver=(mBuffer[0]>>>6)& 0x3;
		if ( ver!=2){
			throw new RtpException("Not a RTP packet, bad version number "+ver);
		}
		if ( ((mBuffer[1]>>>7) & 0x1) == 1) mMarker=true;
		else mMarker=false;
		mPayloadType=mBuffer[1] & 0x7f;
		mSeqNumber=((((int)mBuffer[2])<<8) & 0xffff) | (((int)mBuffer[3]) & 0xff);
		mTimestamp=readUint32(mBuffer,4);
		mSSRC=readUint32(mBuffer,8);
	}
	public void recycle(){
	}
	public SocketAddress getSocketAddress() {
		return mSocketAddress;
	}
	public void setSocketAddress(SocketAddress sockaddr) {
		mSocketAddress=sockaddr;
	}
	public String toString(){
		return "Seq="+mSeqNumber+" Ts="+mTimestamp;
	}
}
