/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;

/**
* List of contact headers.ContactLists are also maintained in a hashtable
* for quick lookup.
* @author M. Ranganathan and Olivier Deruelle
*/
public class ContactList extends HeaderList {
       /**
	* Constructor. 
	*/
        public ContactList() {
            super(CONTACT);
	}

}
