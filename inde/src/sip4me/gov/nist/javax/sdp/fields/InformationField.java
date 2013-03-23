/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;
import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.javax.sdp.SdpException;
import sip4me.gov.nist.javax.sdp.SdpParseException;
 
/** Information field implementation 
*@version  JSR141-PUBLIC-REVIEW (subject to change)
*
*@author Oliver Deruelle <deruelle@antd.nist.gov> 
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*/

public class InformationField extends SDPField    {
	protected String information;


	public InformationField() 
		{ super(INFORMATION_FIELD); }

	public String getInformation() 
		{ return information; }

	public void setInformation( String info ) 
		{ information = info; } 

    public Object clone() {
	InformationField retval = new InformationField();
	retval.information = this.information;
	return retval;
    }

    /**
     *  Get the string encoded version of this object
     * @since v1.0
     */
    public String encode() {
	return INFORMATION_FIELD + information + Separators.NEWLINE;
    }

     /** Returns the value.
      * @throws SdpParseException
      * @return the value
      */
    public String getValue() throws SdpParseException {
        return information;
    }
    
    /** Set the value.
     * @param value to set
     * @throws SdpException if the value is null
     */
    public void setValue(String value) throws SdpException {
        if (value==null) throw new SdpException("The value is null");
        else {
            setInformation(value);
        }
    }

}

