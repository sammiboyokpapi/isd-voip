/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.siplite.address.Address;

/**
* ToHeader SIP Header.
*@author M. Ranganathan and Olivier Deruelle
*<a "href=${docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public final class ToHeader extends AddressParametersHeader  {


   public static final String NAME = Header.TO;

   public static final String TAG = "tag";

   public static Class clazz;


   static {
	clazz = new ToHeader().getClass();
    }

        /** default Constructor.
         */
    public ToHeader() {
        super(TO);
    }
	/** Generate a TO header from a FROM header
	*/
     public ToHeader (FromHeader from) {
	super(TO);
	address = (Address)from.address.clone();
	parameters = (NameValueList)from.parameters.clone();
     }
    
    /**
     * Compare two ToHeader headers for equality.
     * @param otherHeader Object to set
     * @return true if the two headers are the same.
     */
    public boolean equals(Object otherHeader) {
	try {
          if (!otherHeader.getClass().equals(this.getClass())){
	      return false;
           }
	   return super.equals(otherHeader);
        
	} finally {
	    // System.out.println("equals " + retval + exitpoint);
	}
    }
    

   /**
    * Encode the header content into a String.
    * @return String
    */
    public String encodeBody() {
	String retval = "";
        if (address.getAddressType() != Address.NAME_ADDR) {
            retval += Separators.LESS_THAN;
        }
        retval += address.encode();
        if (address.getAddressType() != Address.NAME_ADDR) {
            retval += Separators.GREATER_THAN;
        }
        if (!parameters.isEmpty() ) {
            retval += Separators.SEMICOLON + parameters.encode();
        }
        return retval;
    }
    
   
    
   

   
    
    
   /**
    * Get the tag parameter from the address parm list.
    * @return tag field
    */
    public String getTag() {
       return 	getParameter(TAG);
    }
    
  
    
    
    /** Boolean function
     * @return true if the Tag exist
     */
    public boolean hasTag() {
       return hasParameter(TAG);
    }
  
   
   /**
    * Set the tag member
    * @param t String to set
    */
    public void setTag(String t) {
      setParameter(TAG,t);
    }  
    
    public void removeTag() {
        removeParameter(TAG);
    }
         

}
