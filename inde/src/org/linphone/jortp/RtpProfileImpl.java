package org.linphone.jortp;

public class RtpProfileImpl implements RtpProfile {
	private PayloadType[] mPayloads = new PayloadType[127];
	
	public PayloadType findPayloadType(String mime, int clockrate,
			int[] position) {
		int i;
		for (i=0;i<127;++i){
			PayloadType p=mPayloads[i];
			if (p!=null){
				if (mime.equalsIgnoreCase(p.getMimeType())){
					if (clockrate!=-1 && clockrate==p.getClockRate()){
						position[0] =i;
						return p;
					}
				}
			}
		}
		return null;
	}
	public PayloadType getPayloadType(int pos) {
		return mPayloads[pos];
	}
	public void setPayloadType(PayloadType pt, int pos) {
		mPayloads[pos]=pt;
	}
	public static RtpProfile createAVProfile(){
		RtpProfile p=new RtpProfileImpl();
		p.setPayloadType(PayloadTypeImpl.createAudio("pcmu",8000), 0);
		p.setPayloadType(PayloadTypeImpl.createAudio("gsm",8000), 3);
		p.setPayloadType(PayloadTypeImpl.createAudio("g723",8000), 4);
		p.setPayloadType(PayloadTypeImpl.createAudio("pcma",8000), 8);
		return p;
	}
}
