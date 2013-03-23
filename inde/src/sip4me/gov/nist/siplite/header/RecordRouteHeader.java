/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

import sip4me.gov.nist.siplite.address.Address;


/** The Request-Route header is added to a request by any proxy that insists on
* being in the path of subsequent requests for the same call leg.
*
*  Record-Route  =  "Record-Route" HCOLON 1#
*                        ( name-addr *( SEMI rr-param ))
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class RecordRouteHeader extends AddressParametersHeader {

 	public static Class clazz;
	public static final String NAME = Header.RECORD_ROUTE;

	static {
		clazz = new RecordRouteHeader().getClass();
	}

    
        /**  constructor
         * @param addr address to set
         */        
	public RecordRouteHeader(Address address ) {
		super(RECORD_ROUTE);
		this.address = address;
                
	}
        
	 /** default constructor
         */        
       public RecordRouteHeader() {
		super(RECORD_ROUTE);
              
	}



      
	
        
}
