/**************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).    *
***************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;


public class TypedTime extends SDPObject {
	protected String unit;
	protected int time;

	public String encode() {
		String retval = "";
		retval += new Integer(time).toString();
		if (unit != null) retval += unit;
		return retval;
	}
	
	public void setTime( int t ) {
		time = t;
	}

	public int getTime() {
		return time;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String u) {
		unit = u;
	}

	public Object clone() {
		TypedTime retval = new TypedTime();
		retval.unit = this.unit;
		retval.time = this.time;
		return retval;
	}

}
