/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;
import sip4me.gov.nist.core.Separators;

/**
* Zone adjustment class.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class ZoneAdjustment extends SDPObject {
	protected long time;
	protected String sign;
	protected TypedTime offset;

	/**
	* Set the time.
	*@param t time to set.
	*/
	public void setTime(long t) {
		time = t;
	}

	/**
	* Get the time.
	*/
	public long getTime() {
		return time;
	}

	/**
	* get the offset.
	*/
	public TypedTime getOffset() {
		return offset;
	}

	/**
	* Set the offset.
	*@param off typed time offset to set.
	*/
	public void setOffset(TypedTime off) {
		offset = off;
	}

	/**
	* Set the sign.
	*@param s sign for the offset.
	*/
	public void setSign(String s) {
		sign = s;
	}
		

	/**
	* Encode this structure into canonical form.
	*@return encoded form of the header.
	*/

	public String encode() {
		String retval = new Long(time).toString();
		retval += Separators.SP;
		if (sign != null) retval += sign;
		retval += offset.encode();
		return retval;
	}

	public Object clone() {
		ZoneAdjustment retval = new ZoneAdjustment();
		retval.sign = this.sign;
		retval.offset  = (TypedTime) this.offset.clone();
		retval.time  = this.time;
		return retval;
	}
	


}
