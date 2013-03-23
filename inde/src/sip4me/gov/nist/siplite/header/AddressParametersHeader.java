package sip4me.gov.nist.siplite.header;
import sip4me.gov.nist.core.HostPort;
import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.NameValueList;
import sip4me.gov.nist.siplite.address.Address;

/** An abstract class for headers that take an address and parameters. 
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public abstract class AddressParametersHeader extends ParametersHeader {

	protected Address address;

        /** get the Address field
         * @return the imbedded  Address
         */        
        public Address getAddress() {
		return address;
	}
        
        /** set the Address field
         * @param address Address to set
         */        
	public void setAddress(Address address) {
		this.address = (Address) address;
	}

	/** Constructor given the name of the header.
	*/
	protected AddressParametersHeader(String name) {
		super(name);
	}

	public Object getValue() {
		return address;

	}

	public String getDisplayName() { 
		return address.getDisplayName(); 
	}

        public String getUserAtHostPort() {
		return address.getUserAtHostPort();
        }

	public  HostPort getHostPort() {
		return address.getHostPort();
	}
	
       public boolean equals( Object other ) {
	   if (! other.getClass().equals(this.getClass())) return false;
       	   Address otherAddress=((AddressParametersHeader) other).getAddress();
       	   if (otherAddress==null) return false;
       	   if (! otherAddress.equals(address)) {
           	return false;
           }
           if (! parameters.equals
		(((AddressParametersHeader)other).parameters) ) {
	      return false;
	   } else return true;
	}

     /**
      * Encode the header content into a String.
      * @return String
      */
    public String encodeBody() {
       if (address==null) {
		throw new RuntimeException("No body!");
       }
       StringBuffer retval = new StringBuffer();
       if (address.getAddressType() != Address.NAME_ADDR) {
           retval .append( "<" );
       }
       retval .append(address.encode());
       if (address.getAddressType() != Address.NAME_ADDR) {
           retval .append( ">") ;
       }
       if (!parameters.isEmpty() ) {
           retval .append(";") .append( parameters.encode());
       }
       return retval.toString();
   }


   public Object clone() {
       try {
	AddressParametersHeader retval = 
		(AddressParametersHeader) this.getClass().newInstance(); 
	if (this.address != null) 
		retval.address = (Address) this.address.clone();
	if (this.parameters != null) 
		retval.parameters = (NameValueList) this.parameters.clone();
	return retval;
       } catch (Exception ex) {
           InternalErrorHandler.handleException(ex);
           return null;
       } 
   }
	

}

