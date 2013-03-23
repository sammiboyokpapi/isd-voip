/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;
import sip4me.gov.nist.core.GenericObject;


/**
* Root class for everything in this package.
*/

public abstract class SDPObject 
	extends GenericObject implements SDPFieldNames  {


	public abstract String encode();


	public abstract Object clone();


}

