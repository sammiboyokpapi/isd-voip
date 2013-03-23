/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;

import java.util.Vector;

public class SDPObjectList extends Vector {


	public Object clone() {
		SDPObjectList retval = new SDPObjectList();
		for (int i = 0; i < this.size(); i++) {
		     Object obj = 
			   ((SDPObject) this.elementAt(i)).clone();
		     retval.addElement(obj);
		}
		return retval;
	}
    /**
     * Add an sdp object to this list.
     */
   public void add (Object s) {  this.addElement(s);   }


	/**
	 * Get the input text of the sdp object (from which the object was
	 * generated).
	 */

	public SDPObjectList() {
		super();
	}

	public String encode() {
		StringBuffer retval = new StringBuffer();
		SDPObject sdpObject;
		for (int i = 0; i < this.size(); i++) {
			sdpObject = (SDPObject) this.elementAt(i);
			retval.append(sdpObject.encode());
		}
		return retval.toString();
	}

	public String toString() {
		return this.encode();
	}

}
