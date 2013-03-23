/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;
import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.javax.sdp.SdpException;
import sip4me.gov.nist.javax.sdp.SdpParseException;

public class SessionNameField  extends SDPField  {
	protected String sessionName;

	public Object clone() {
		SessionNameField snf = new SessionNameField();
		snf.sessionName = this.sessionName;
		return snf;
	}

	public SessionNameField() {
		super(SDPFieldNames.SESSION_NAME_FIELD);
	}
	public String getSessionName() { return sessionName; }
	/**
	* Set the sessionName member  
	*/
	public	 void setSessionName(String s) 
 	 	{ sessionName = s ; } 

    /** Returns the value.
     * @throws SdpParseException
     * @return  the value
     */    
    public String getValue()
    throws SdpParseException {
        return getSessionName();
    }
    
    
    /** Sets the value
     * @param value the - new information.
     * @throws SdpException if the value is null
     */    
    public void setValue(String value)
    throws SdpException {
        if (value==null) throw new SdpException("The value is null");
        else {
            setSessionName(value);
        }
    }

    /**
     *  Get the string encoded version of this object
     * @since v1.0
     */
    public String encode() {
	return SESSION_NAME_FIELD + sessionName + Separators.NEWLINE;
    }
	
}
