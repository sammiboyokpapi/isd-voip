/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;
import sip4me.gov.nist.core.GenericObject;
import sip4me.gov.nist.core.Separators;

/**
*   Media Range 
* @see Accept
* @since 0.9
* @version 1.0
* <pre>
* Revisions:
*
* Version 1.0
*    1. Added encode method.
*
* media-range    = ( "STAR/STAR"
*                        | ( type "/" STAR )
*                        | ( type "/" subtype )
*                        ) *( ";" parameter )       
* 
* HTTP RFC 2616 Section 14.1
* </pre>
*/
public class MediaRange  extends GenericObject  {
    
        /** type field
         */    
	protected String  type;
        
        /** subtype field
         */        
	protected String  subtype;

	public Object clone() {
	    MediaRange retval = new MediaRange();
	    if (type != null) 
	       retval.type = new String(this.type);
	    if (subtype != null) 
		retval.subtype = new String(this.subtype);
	    return retval;
	}
	


        /** Default constructor
         */        
	public  MediaRange() {
	}

        /** get type field
         * @return String
         */        
	public String getType() {
            return type ;
        }
            
        /** get the subType field.
         * @return String
         */                
	public String getSubtype() {
            return subtype ;
        } 
   
        
	/**
         * Set the type member
         * @param t String to set
         */
	public void setType(String t) {
            type = t ;
        }
        
	/**
         * Set the subtype member
         * @param s String to set
         */
	public void setSubtype(String s) {
            subtype = s ;
        }
        

	/**
         * Encode the object.
         * @return String
         */
	public String encode() {
		String encoding = type + Separators.SLASH + subtype;
		return encoding;
	}
       
       
        
}
