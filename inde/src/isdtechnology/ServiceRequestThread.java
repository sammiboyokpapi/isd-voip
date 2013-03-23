package isdtechnology;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.StreamConnection;



public class ServiceRequestThread extends Thread {
	protected String _URL;
	protected MyScreen _Dest = null;

	public ServiceRequestThread(String URL,MyScreen screen) {
		super();
		_Dest = screen;
		_URL = URL;
	}

public void run(){
	
	try {
		StreamConnection s = (StreamConnection)Connector.open(_URL);
		HttpConnection conn = (HttpConnection)s ;
		conn.setRequestMethod(HttpConnection.POST);
		conn.setRequestProperty("User-Agent", "BlackBerry/3.2.1");
		String lang = conn.getRequestProperty("Content-Language");
		InputStream data = conn.openInputStream();
		OutputStream dataout = conn.openOutputStream();
		int responseCode = conn.getResponseCode();
		if (responseCode == HttpConnection.HTTP_OK) {
			
			StringBuffer raw = new StringBuffer();
			byte[] buf = new byte[4096];
			int nRead = data.read(buf);
			while (nRead > 0) {
				raw.append(new String(buf,0,nRead));
				nRead = data.read(buf);
			}
			_Dest.updateDestination(raw.toString());
		}
		else
		{
			_Dest.updateDestination("responseCode="+Integer.toString(responseCode));
		}
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		_Dest.updateDestination("Exception sammi:"+e.toString());
	}
	
}
}
