/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD)         *
*******************************************************************************/
package sip4me.gov.nist.siplite.message;

import java.util.Hashtable;

import sip4me.gov.nist.siplite.header.ContactHeader;
import sip4me.gov.nist.siplite.header.ContactList;
import sip4me.gov.nist.siplite.header.ExtensionHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.HeaderList;
import sip4me.gov.nist.siplite.header.ProxyAuthenticateHeader;
import sip4me.gov.nist.siplite.header.ProxyAuthenticateList;
import sip4me.gov.nist.siplite.header.RecordRouteHeader;
import sip4me.gov.nist.siplite.header.RecordRouteList;
import sip4me.gov.nist.siplite.header.RouteHeader;
import sip4me.gov.nist.siplite.header.RouteList;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.header.ViaList;
import sip4me.gov.nist.siplite.header.WWWAuthenticateHeader;
import sip4me.gov.nist.siplite.header.WWWAuthenticateList;


/**
* A map of which of the standard headers may appear as a list 
*/

class ListMap   {
       // A table that indicates whether a header has a list representation or
       // not (to catch adding of the non-list form when a list exists.)
       // Entries in this table allow you to look up the list form of a header
       // (provided it has a list form).
       private static Hashtable		headerListTable;
       static { initializeListMap();  }


    /**
    * Build a table mapping between objects that have a list form
    * and the class of such objects.
    */
       static  private void initializeListMap() {
	    headerListTable = new Hashtable();
	    headerListTable.put (ExtensionHeader.clazz,
		new HeaderList().getClass());

	    headerListTable.put( ContactHeader.clazz, 
			new ContactList().getClass());


	    headerListTable.put( ViaHeader.clazz,
			new ViaList().getClass());
	
	    headerListTable.put( WWWAuthenticateHeader.clazz, 
			new WWWAuthenticateList().getClass());

	    headerListTable.put(  RouteHeader.clazz, 
			new RouteList().getClass());

	    headerListTable.put( ProxyAuthenticateHeader.clazz, 
				new ProxyAuthenticateList().getClass());

	    headerListTable.put( RecordRouteHeader.clazz, 
			new RecordRouteList().getClass());


	}

	/**
	* return true if this has an associated list object.
	*/
	static  protected boolean hasList(Header sipHeader) {
		if (sipHeader instanceof HeaderList) return false;
	        else {
			Class headerClass = sipHeader.getClass();
			return headerListTable.get(headerClass) != null;
		}
	}

	/**
	* Return true if this has an associated list object.
	*/
	static  protected boolean hasList(Class sipHdrClass) {
		return headerListTable.get(sipHdrClass) != null;
	}

	/**
	* Get the associated list class.
	*/
	static protected Class getListClass(Class sipHdrClass) {
		return (Class) headerListTable.get(sipHdrClass);
	}

	/**
	* Return a list object for this header if it has an associated
	* list object.
	*/
	static protected 
		HeaderList getList(Header sipHeader) {
		try {
		   Class headerClass = sipHeader.getClass();
		   Class listClass = (Class) headerListTable.get(headerClass);
		   HeaderList shl =  (HeaderList) listClass.newInstance();
		   shl.setHeaderName(sipHeader.getHeaderName());
		   return shl;
		} catch (InstantiationException ex) {
		    ex.printStackTrace();
		} catch (IllegalAccessException ex)  {
		    ex.printStackTrace();
		}
		return  null;
	}

}
