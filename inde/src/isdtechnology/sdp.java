package isdtechnology;

import java.io.IOException;
import java.util.Vector;

import sip4me.gov.nist.javax.sdp.MediaDescription;
import sip4me.gov.nist.javax.sdp.SdpException;
import sip4me.gov.nist.javax.sdp.SdpFactory;
import sip4me.gov.nist.javax.sdp.SdpParseException;
import sip4me.gov.nist.javax.sdp.SessionDescription;
import sip4me.gov.nist.javax.sdp.fields.MediaField;
import sip4me.gov.nist.javax.sdp.fields.URIField;

public class sdp extends Thread {
	
	protected String sdpread;
	protected dialogsam _Dest = null;
	String sdpData[];
	SessionDescription sessionDescription;
	MediaDescription m;
	private String medias;
	private int intmedias;
	public sdp(String URL,dialogsam screen) {
		super();
		_Dest = screen;
		sdpread = URL;
	
	}
	
	public void run(){
		
		try{
			String sdpData[] = { sdpread };
			String sdps[]= {};
			for (int i=0; i < sdpData.length; i++) {
				//System.out.println("---------START OF MEDIA-------------");
				
				sessionDescription =
				SdpFactory.getInstance().createSessionDescription(sdpData[i]);
				
				
				Vector mediaDescriptions =
				sessionDescription.getMediaDescriptions(false);
				
				_Dest.sdpin("sessionDescription:\n" + sessionDescription);
				
				//System.out.println("      <<MEDIA DESCRIPTIONS>>        ");
				//
				for (int j = 0; j < mediaDescriptions.size(); j++) {
				m = (MediaDescription) mediaDescriptions.elementAt(j);
				 //sdps[1] = "m = " + m.toString();
				//_Dest.sdpin("\n" +m.toString());
				MediaField media = m.getMedia();
				Vector formats = media.getMediaFormats(false);
								
				//sdps[2] = "formats = " + formats;
				}
				
				//sdps[2] = "---------END OF MEDIA-------------\n\n";
				
				
			}
		}
		catch (Exception e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	public String portnum(String sc){
		
		try {
			sessionDescription =
			SdpFactory.getInstance().createSessionDescription(sc);
			Vector mediaDescriptions =sessionDescription.getMediaDescriptions(false);
			for (int j = 0; j < mediaDescriptions.size(); j++) {
				m = (MediaDescription) mediaDescriptions.elementAt(j);
				//_Dest.sdpin("\n" +m.toString());
				MediaField media = m.getMedia();
				Vector formats = media.getMediaFormats(false);
				int port = media.getMediaPort();		
				medias = String.valueOf(port);
				//sdps[2] = "formats = " + formats;
				}
		
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return medias;
	}
	
	/**
	 * returns the remote callers ip4 address.
	 * @param string sc
	 * @returns the remote callers ip4 address from the connection attribute
	 */
	public String remoteaddress(String sc){
		
		String medias = null;
		try {
			sessionDescription =
				SdpFactory.getInstance().createSessionDescription(sc);
			String md = sessionDescription.getConnection().getAddress().toString();
			medias = md.toString();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	return medias ;
}
	
	/**
	 * returns the pcm u-law attribute.
	 * 
	 * @returns the pcm u-law attribute is available else provides a boolean false resp.
	 */
	public String pcm_ulaw(String sc){
	// provides the pcm u-law attributes if provided else provides a false response
		try {
			sessionDescription =
			SdpFactory.getInstance().createSessionDescription(sc);
			Vector mediaDescriptions =sessionDescription.getMediaDescriptions(false);
			for (int j = 0; j < mediaDescriptions.size(); j++) {
				m = (MediaDescription) mediaDescriptions.elementAt(j);
				//_Dest.sdpin("\n" +m.toString());
				MediaField media = m.getMedia();
				medias = m.getAttribute("rtpmap");
				boolean compare = medias.startsWith("0");
				if(compare == false){
					medias = "false";	
					
				}
				//sdps[2] = "formats = " + formats;
				}
		
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	return medias ;
}
	
}
