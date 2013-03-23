package sip4me.gov.nist.siplite;
import java.util.Hashtable;

public class ConfigurationProperties extends Hashtable {

	public ConfigurationProperties() { super() ; }

	public String getProperty(String name) {
		return (String)super.get(name);
	}

	public void setProperty(String name, String value) {
		super.put(name,value);
	}

}

