/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 ******************************************************************************/
package sip4me.gov.nist.siplite.header;

/**
*  Keeps a list and a hashtable of via header functions.
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public final class ViaList extends HeaderList {
    
         /**
          * Default Constructor.
          */
    public ViaList() {
        super( VIA);
    }
    
    
        /**
         * make a clone of this header list. This supercedes the parent
         * function of the same signature.
         * @return clone of this Header.
         */
    public Object clone() {
        ViaList vlist = new ViaList();

	for (int i = 0 ; i < this.sipHeaderVector.size(); i++) {
		ViaHeader v = (ViaHeader)
		((ViaHeader) (this.sipHeaderVector.elementAt(i))).clone();
                vlist.add(v);
        }
        return (Object) vlist;
    }
    
    
}
