/******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
******************************************************************************/
package sip4me.gov.nist.siplite.header;

import sip4me.gov.nist.core.NameValueList;
/**
* MaxForwards Header
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov> 
*@author Olivier Deruelle <deruelle@nist.gov>
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public  class MaxForwardsHeader extends Header {
    
        /** maxForwards field.
         */    
	protected int maxForwards;

	public static final String NAME = Header.MAX_FORWARDS;

	public final static Class clazz;

	static {
		clazz = new MaxForwardsHeader().getClass();
	}

        
        /** Default constructor.
         */        
	public MaxForwardsHeader() {
            super(Header.MAX_FORWARDS);
        }
        
        /** get the MaxForwards field.
         * @return the maxForwards member.
         */        
	public int getMaxForwards() {
            return maxForwards;
        }
	
	/**
         * Set the maxForwards member
         * @param maxForwards maxForwards parameter to set
         */
	public void setMaxForwards(int maxForwards) throws IllegalArgumentException  {
            if (maxForwards<0 || maxForwards>255) 
	    throw new IllegalArgumentException
	    ("bad max forwards value " + maxForwards);
            this.maxForwards= maxForwards ;
        }
        
	/**
         * Encode into a string.
         * @return encoded string.
         *
         */	
         public String encodeBody() {
		return new Integer(maxForwards).toString() ;
	}
          
        /** Boolean function
         * @return true if MaxForwards field reached zero.
         */        
        public boolean hasReachedZero() {
            return maxForwards==0;
        }
        
        /** decrement MaxForwards field one by one.
         */        
        public void decrementMaxForwards()  {
	    if (maxForwards >= 0) maxForwards--;
        }

        public Object getValue() {
		return new Integer(maxForwards);
	}

	public NameValueList getParameters() {
		return null;
	}
	
}
