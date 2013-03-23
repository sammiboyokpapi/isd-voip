/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

import sip4me.gov.nist.siplite.address.Address;

/**
 * Route  Header 
 * @author M. Ranganathan and Olivier Deruelle
 * @since 1.0
 *
 * Route  =  "Route" HCOLON 1# ( name-addr
 *                  *( SEMI rr-param ))
 *
*/
public class RouteHeader extends AddressParametersHeader {

	public static Class clazz;
	public static final String NAME = Header.ROUTE;
	static {
		clazz = new RouteHeader().getClass();
	}
    
        
        /** Default constructor
         */        
	public RouteHeader() { 
            super(ROUTE);
        }

	/** Route constructor given address.
	*/
	public RouteHeader(Address address) {
		this();
		this.address = address;
	}

       
        
}
	
