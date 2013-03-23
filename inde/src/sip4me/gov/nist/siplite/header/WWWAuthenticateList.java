/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.siplite.header;


/**
* WWWAuthenticate Header (of which there can be several?)
*
*@version  JAIN-SIP-1.1
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/
public class WWWAuthenticateList extends HeaderList {
     
        /**
         * constructor.
         */
    public WWWAuthenticateList () {
        super(  WWW_AUTHENTICATE);
    }
        
}

