/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Separators;

/**
* Event SIP Header.
*
*@version  JAIN-SIP-1.1
*
*@author ArnauVP <arnau.vazquez@genaker.net>  <br/>
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class SupportedHeader extends ParametersHeader {
    
    protected String supportedType;

    public final static String ID = "id";

    public final static String NAME = Header.SUPPORTED;

    public static Class clazz;


    static {
	clazz = new SupportedHeader().getClass();
    }
    
    /** Creates a new instance of Supported Header */
    public SupportedHeader() {
        super(SUPPORTED);
    }
    
      /**
     * Sets the supportedType to the newly supplied supportedType string.
     *
     * @param supportedType - the  new string defining the supportedType supported
     * in this SupportedHeader
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the supportedType value.
     */
    public void setSupportedType(String supportedType) throws ParseException {
		if (supportedType == null)
			throw new NullPointerException("the supportedType is null");
		this.supportedType = supportedType;
	}

    /**
     * Gets the supportedType of the SupportedHeader. 
     *
     * @return the string object identifing the supportedType of SupportedHeader.
     */
    public String getsupportedType() {
        return supportedType;
    }
    
    
    /**
     * Encode in canonical form.
     * @return String
     */
    public String encodeBody() {
            StringBuffer retval = new StringBuffer();
           
            if (supportedType!=null) retval.append(supportedType);
            
            if (!parameters.isEmpty()) retval.append(Separators.SEMICOLON + 
                        this.parameters.encode());
            return retval.toString();
    }

    /** Return true if the given event header matches the supplied one.
     *
     * @param matchTarget -- event header to match against.
     */
     public boolean match(SupportedHeader matchTarget) {
	  if (matchTarget.supportedType == null && this.supportedType != null)
		return false;
	  else if (matchTarget.supportedType != null && this.supportedType == null)
		return false;
	  else if (this.supportedType == null && matchTarget.supportedType == null)
		return false;
	  return equalsIgnoreCase(matchTarget.supportedType,this.supportedType);
     }

     public Object getValue() {
		return this.supportedType;
     }
	
    
}
