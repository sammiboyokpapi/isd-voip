/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 ******************************************************************************/
package sip4me.gov.nist.siplite.header;
import sip4me.gov.nist.core.GenericObject;
import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.siplite.SIPConstants;

/**
* Status Line (for SIPReply) messages.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*/

public final class StatusLine  extends GenericObject {
    

    
    /** sipVersion field
     */
    protected String sipVersion;
    
    /** status code field
     */
    protected int statusCode;
    
    /** reasonPhrase field
     */
    protected String  reasonPhrase;
    
    /** Default Constructor
     */
    public  StatusLine() {
        reasonPhrase = null;
        sipVersion = SIPConstants.SIP_VERSION_STRING;
    }
     
    public  static Class clazz;

    static {
	clazz = new StatusLine().getClass();
    }
    
        /**
         * Encode into a canonical form.
         * @return String
         */
    public String encode() {
        String encoding = SIPConstants.SIP_VERSION_STRING + Separators.SP +
        statusCode ;
        if (reasonPhrase != null) encoding += Separators.SP + reasonPhrase;
        encoding += Separators.NEWLINE;
        return encoding;
    }
    
    /** get the Sip Version
     * @return SipVersion
     */
    public String getSipVersion() {
        return sipVersion ;
    }
    
    /** get the Status Code
     * @return StatusCode
     */
    public int getStatusCode() {
        return statusCode ;
    }
    
    /** get the ReasonPhrase field
     * @return  ReasonPhrase field
     */
    public String getReasonPhrase() {
        return reasonPhrase ;
    }
    
        /**
         * Set the sipVersion member
         * @param s String to set
         */
    public void setSipVersion(String s) {
        sipVersion = s ;
    }
    
        /**
         * Set the statusCode member
         * @param statusCode int to set
         */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode ;
    }
    
        /**
         * Set the reasonPhrase member
         * @param reasonPhrase String to set
         */
    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase ;
    }
    
        /**
         * Get the major version number.
         *@return String major version number
         */
    public String getVersionMajor() {
        if (sipVersion == null) return null;
        String major = null;
        boolean slash = false;
        for (int i = 0; i < sipVersion.length(); i++) {
            if (sipVersion.charAt(i) == '.') slash = false;
            if (slash) {
                if (major == null)
                    major = "" + sipVersion.charAt(i) ;
                else major += sipVersion.charAt(i);
            }
            if (sipVersion.charAt(i) == '/') slash = true;
        }
        return major;
    }
    
        /**
         * Get the minor version number.
         *@return String minor version number
         */
    public String getVersionMinor() {
        if (sipVersion == null) return null;
        String minor = null;
        boolean dot = false;
        for (int i = 0; i < sipVersion.length(); i++) {
            if (dot) {
                if (minor == null)
                    minor = "" + sipVersion.charAt(i);
                else minor += sipVersion.charAt(i);
            }
            if (sipVersion.charAt(i) == '.') dot = true;
        }
        return minor;
    }

    public Object clone() {
	StatusLine retval = new StatusLine();

	if (this.sipVersion != null)
	    retval.sipVersion = new String (this.sipVersion);

	retval.statusCode = this.statusCode;

	if (this.reasonPhrase != null) 
		retval.reasonPhrase = new String(this.reasonPhrase);
	
	return retval;

    }

    public boolean equals(Object that) {
		if (that instanceof StatusLine) 
			return this.statusCode == ((StatusLine)that).statusCode;
		else return false;
    }
    
    
    
}
