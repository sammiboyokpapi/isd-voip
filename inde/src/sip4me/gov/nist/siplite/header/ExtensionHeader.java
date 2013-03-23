package sip4me.gov.nist.siplite.header;

import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.core.Separators;

/** Use this class when there is no parser for parsing the given header. */
public class ExtensionHeader extends ParametersHeader {

	public static Class clazz;

	protected String headerValue;

	public ExtensionHeader() {}

	static {
		clazz = new ExtensionHeader().getClass();
	}


	/** A generic header.
	*/
	public ExtensionHeader(String headerName, String headerValue) {
		super(headerName);
		this.headerValue = headerValue;
	}

	/** set the value for a generic header.
	*/
	public void setValue( String value) {
		this.headerValue = value;
	}

	public  void setName(String name) {
		this.headerName = name;
	}

	public NameValueList getParameters() {
		return parameters;
	}


	public String encodeBody() { 
                if (parameters!=null && ! this.parameters.isEmpty()) 
                    return this.headerValue + Separators.SEMICOLON + 
				this.parameters.encode(); 
                else
                    return this.headerValue ;
            
		
	}

	/** Get the value of the header.
	*/
	public Object getValue() { 
		return this.headerValue;
	}



}
