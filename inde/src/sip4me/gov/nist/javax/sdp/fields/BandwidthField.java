/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;
import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.javax.sdp.SdpException;
import sip4me.gov.nist.javax.sdp.SdpParseException;
/**
* Bandwidth field of a SDP header.
*
*@version  JSR141-PUBLIC-REVIEW (Subject to change)
*
*@author Olivier Deruelle <deruelle@antd.nist.gov>
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public class BandwidthField extends SDPField {
	protected String bwtype;
	protected int 	bandwidth;

	public Object clone() {
		BandwidthField bf  = new BandwidthField();
		bf.bwtype = bwtype;
		bf.bandwidth = bandwidth;
		return bf;
	}
	public BandwidthField() {
		super(SDPFieldNames.BANDWIDTH_FIELD);
	}
	public	 String getBwtype() 
 	 	{ return bwtype ; } 
	public	 int getBandwidth() 
 	 	{ return bandwidth ; } 
	/**
	* Set the bwtype member  
	*/
	public	 void setBwtype(String b) 
 	 	{ bwtype = b ; } 
	/**
	* Set the bandwidth member  
	*/
	public	 void setBandwidth(int b) 
 	 	{ bandwidth = b ; } 

	/**
	*  Get the string encoded version of this object
	* @since v1.0
	*/
	public String encode() {
	   String encoded_string = BANDWIDTH_FIELD;

	   if (bwtype != null) encoded_string += bwtype + Separators.COLON;
	   return encoded_string + bandwidth + Separators.NEWLINE; 
	}

    /** Returns the bandwidth type.
     * @throws SdpParseException 
     * @return type
     */
    public String getType() throws SdpParseException {
        return  getBwtype();
    }
    
    /** Sets the bandwidth type.
     * @param type to set
     * @throws SdpException if the type is null
     */
    public void setType(String type) throws SdpException {
        if (type==null) throw new SdpException("The type is null");
        else setBwtype(type);
    }
    
    /** Returns the bandwidth value measured in kilobits per second.
     * @throws SdpParseException 
     * @return the bandwidth value
     */
    public int getValue() throws SdpParseException {
        return getBandwidth();
    }
    
    /** Sets the bandwidth value.
     * @param value to set
     * @throws SdpException 
     */
    public void setValue(int value) throws SdpException {
        setBandwidth(value);
    }
	
    

}
