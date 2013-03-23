package sip4me.gov.nist.siplite.address;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.siplite.parser.StringMsgParser;
import sip4me.gov.nist.siplite.parser.URLParser;

/** Implementation of the JAIN-SIP address factory.
 *@version  JAIN-SIP-1.1
 *
 *@author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * IPv6 Support added by Emil Ivov (emil_ivov@yahoo.com)<br/>
 * Network Research Team (http://www-r2.u-strasbg.fr))<br/>
 * Louis Pasteur University - Strasbourg - France<br/>
 *
 */
public class AddressFactory  {

    /** Creates a new instance ofAddressFactoryImpl
     */
    public AddressFactory() {
    }


    /**
     * Creates anAddress with the new display name and URI attribute
     * values.
     *
     * @param displayName - the new string value of the display name of the
     * address. A <code>null</code> value does not set the display name.
     * @param uri - the new URI value of the address.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the displayName value.
     */
    public Address
            createAddress(String displayName, URI uri) {
        if (uri == null)
            throw new NullPointerException("null  URI");
       Address addressImpl = new Address();
        if (displayName != null) addressImpl.setDisplayName(displayName);
        addressImpl.setURI(uri);
        return addressImpl;

    }

    /** create a sip uri.
     *
     *@param uri -- the uri to parse.
     */
    public SipURI createSipURI(String uri)
          //  throws java.netURISyntaxException {
    throws ParseException {
        if (uri == null)
            throw new NullPointerException("null URI");
        try {
            StringMsgParser smp = new StringMsgParser();
            SipURI sipUri = smp.parseSIPUrl(uri);
            return (SipURI) sipUri;
        } catch (ParseException ex) {
          //  throw new java.netURISyntaxException(uri, ex.getMessage());
             throw new ParseException(ex.getMessage(),0);
        }

    }


    /** Create aSipURI
     *
     *@param user -- the user
     *@param host -- the host.
     */
    public SipURI createSipURI(String user, String host)
     throws ParseException {
        if (host == null) throw new NullPointerException("null host");

        StringBuffer uriString = new StringBuffer("sip:");
        if (user != null) {
            uriString.append(user);
            uriString.append("@");
        }

        //if host is an IPv6 string we should enclose it in sq brackets
        if(   host.indexOf(':') != host.lastIndexOf(':')
           && host.trim().charAt(0) != '[')
            host = '[' + host + ']';

        uriString.append(host);

        StringMsgParser smp = new StringMsgParser();
        try {

            SipURI sipUri = smp.parseSIPUrl(uriString.toString());
            return sipUri;
        } catch (ParseException ex) {
            throw new ParseException(ex.getMessage(),0);
        }
    }

    /**
     * Creates a TelURL based on given URI string (e.g. "tel:+12345")
     *
     * @param uri - the new string value of the phoneNumber.
     * @throws URISyntaxException if the URI string is malformed.
     */
    public  TelURL createTelURL(String uri)
    throws ParseException {
        if (uri == null) throw new NullPointerException("null url");
        if (uri.startsWith("<"))
        	uri = uri.substring(1);
        if (uri.endsWith(">"))
        	uri = uri.substring(0,uri.length()-1);
        try {
            StringMsgParser smp = new StringMsgParser();
            TelURL timp = (TelURL) smp.parseUrl(uri);
            return (TelURL) timp;
        } catch (ParseException ex) {
            throw new ParseException(   ex.getMessage(),0);
        }
    }


    public Address createAddress
            (URI uri) {
        if (uri == null)
            throw new NullPointerException("null address");
       Address addressImpl = new Address();
        addressImpl.setURI(uri);
        return addressImpl;
    }

    /**
     * Creates anAddress with the new address string value. The address
     * string is parsed in order to create the new Address instance. Create
     * with a String value of "*" creates a wildcard address. The wildcard
     * can be determined if
     * <code>(SipURIAddress.getURI).getUser() == *;</code>.
     *
     * @param address - the new string value of the address.
     * @throws ParseException which signals that an error has been reached
     * unexpectedly while parsing the address value.
     */
    public Address createAddress(String address)
            throws ParseException {
        if (address == null)
            throw new NullPointerException("null address");

        if (address.equals("*")) {
           Address addressImpl = new Address();
            addressImpl.setAddressType(Address.WILD_CARD);
            return addressImpl;
        } else {
            StringMsgParser smp = new StringMsgParser();
            return smp.parseAddress(address);
        }
    }

    /**
     * Creates a URI based on given URI string. The URI string is parsed in
     * order to create the new URI instance. Depending on the scheme the
     * returned may or may not be a SipURI or TelURL cast as a URI.
     *
     * @param uri - the new string value of the URI.
     * @throws URISyntaxException if the URI string is malformed.
     */

    public URI createURI(String uri)
     throws ParseException {
        if (uri == null)
            throw new NullPointerException("null arg");
        try {
            URLParser urlParser = new URLParser(uri);
            String scheme = urlParser.peekScheme();
            if (scheme == null)
                throw new ParseException("bad scheme",0);
            if (Utils.equalsIgnoreCase(scheme,"sip")) {
                return (URI) urlParser.sipURL();
            } else if (Utils.equalsIgnoreCase(scheme,"sips")) {
                return (URI) urlParser.sipURL();
            } else if (Utils.equalsIgnoreCase(scheme,"tel")) {
                return (URI) urlParser.telURL();
            }
        } catch (ParseException ex) {
            throw new ParseException(ex.getMessage(),0);
        }
        return new URI(uri);
    }

}
