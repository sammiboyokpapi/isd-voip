/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;
import sip4me.gov.nist.core.Separators;


/** Implementation of URI field.
*
*@author Olivier Deruelle <deruelle@antd.nist.gov>
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*/

public class URIField extends SDPField  {
	protected String urlString ;

	public URIField() { super(URI_FIELD) ; }
        
        
	public String getURI() { return urlString; }

	public void setURI(String uri)  {
		this.urlString = uri;
	}

	public String get() {
		return urlString;
	}

	public Object clone() {
	   URIField retval = new URIField();
	   retval.urlString = this.urlString;
	   return retval;
	}

    /**
     *  Get the string encoded version of this object
     * @since v1.0
     */
    public String encode() {
	if (urlString != null) {
	   return URI_FIELD + urlString + Separators.NEWLINE;
	} else return "";
    }

	
}
