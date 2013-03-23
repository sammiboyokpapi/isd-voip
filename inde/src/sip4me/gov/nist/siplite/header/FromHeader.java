/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.siplite.address.Address;

/**
* FromHeader SIP Header
* @author M. Ranganathan and Olivier Deruelle
*<a "href=${docRoot}/uncopyright.html">This code is in the public domain.</a>
*/

public final class FromHeader extends AddressParametersHeader  {

    
    public static final String TAG = "tag";

    public static final String NAME = Header.FROM;

    public static Class clazz;

    static {
	clazz = new FromHeader().getClass();
     }
   
   
    /** Default constructor
     */   
   public FromHeader() {
       super(FROM);
   }
    /** Generate a FROM header from a TO header
    */
   public FromHeader(ToHeader to) {
	super(FROM);
	this.address = (Address) to.address.clone();
	this.parameters = (NameValueList) to.parameters.clone();
   }
   
   /**
    * Compare two from headers for equality.
    * @param otherHeader Object to set
    * @return true if the two headers are the same, false otherwise.
    */
   public boolean equals(Object otherHeader) {
       if (otherHeader==null || address ==null) return false;
       if (!otherHeader.getClass().equals(this.getClass())){
           return false;
       }
       
       return super.equals(otherHeader);
   }
   

   
   /**
    * Get the address field.
    * @return Address
    */
   public Address getAddress()  {
       return address;
   }
   
  
    
   
    /**
     * Get the display name from the address.
     * @return String
     */
   public String getDisplayName() {
       return address.getDisplayName();
   }
   
   /**
    * Get the tag parameter from the address parm list.
    * @return String
    */
   public String getTag() {
     return super.getParameter(FromHeader.TAG);
   }
   
   
   
    
   
    /** Boolean function
     * @return true if this header has a Tag, false otherwise.
     */   
     public boolean hasTag() {
         return super.hasParameter(TAG);
        
    }
   
  
    
    /** remove the Tag field.
     */   
    public void removeTag() {
        super.removeParameter(TAG);
         
    }
   
   
   
   
   
   /**
    * Set the tag member
    * @param tag String to set.
    */
   public void setTag(String tag) {
       super.setParameter(TAG,tag);
       
   }

   
   
   
}
