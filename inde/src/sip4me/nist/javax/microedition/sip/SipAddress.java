/*
 * Created on Jan 28, 2004
 */
package sip4me.nist.javax.microedition.sip;

import java.util.Vector;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.microedition.sip.StackConnector;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.SipURI;
import sip4me.gov.nist.siplite.address.TelURL;
import sip4me.gov.nist.siplite.address.URI;



/**
 * SipAddress provides a generic SIP address parser. 
 * This class can be used to parse either full SIP name addresses like: 
 * BigGuy <sip:UserA@atlanta.com> or SIP/SIPS URIs like: sip:+13145551111@ss1.atlanta.com;user=phone 
 * or sips:alice@atlanta.com;transport=tcp. Correspondingly, valid SIP addresses 
 * can be constructed with this class.SipAddress has following functional requirements:
 * - SipAddress does not escape address strings.
 * - SipAddress ignores headers part of SIP URI.
 * - SipAddress valid scheme format is the same as defined in SIP BNF for absolute URI.
 * - The valid Contact address “*” is accepted in SipAddress. 
 * In this case all properties will be null and port number is 0. 
 * Yet toString() method will return the value “*”. 
 * Reference, SIP 3261 [1] p.153 Example SIP and SIPS URIs and p.228 name-addr 
 * in SIP BNF specification.
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SipAddress {
	/**
	 * the nist-siplite corresponding address
	 */
	private Address address;
	/**
	 * Construct a new SipAddress from string. 
	 * The string can be either name address:Display 
	 * Name <sip:user:password@host:port;uri-parameters> or 
	 * plain SIP URI:sip:user:password@host:port;uri-parameters
	 * @param address - SIP address as String
	 * @throws IllegalArgumentException - if there was an error in parsing the address.
	 */
	public SipAddress(java.lang.String address)
		   throws IllegalArgumentException {
		try{
			this.address=StackConnector.addressFactory.createAddress(address);
		}
	 	catch(ParseException pe){
	 		throw new IllegalArgumentException(pe.getMessage());
	 	}
	}
	
	/**
	 * Construct a new SipAddress from display name and URI.
	 * @param displayName - user display name
	 * @param URI - SIP URI
	 * @throws IllegalArgumentException - if there was an error in parsing the arguments.
	 */
	public SipAddress(java.lang.String displayName, java.lang.String URI)
		   throws IllegalArgumentException {		
		try{
			URI uri=StackConnector.addressFactory.createURI(URI);
			this.address=
				StackConnector.addressFactory.createAddress(displayName,uri);
		}
		catch(ParseException pe){
			throw new IllegalArgumentException(pe.getMessage());
		}   	
	}
	
	/**
	 * Returns the display name of SIP address.
	 * @return display name or null if not available
	 */
	public java.lang.String getDisplayName(){
		return address.getDisplayName();
	}
	
	/**
	 * Sets the display name. Empty string “” removes the display name.
	 * @param name - display name
	 * @throws IllegalArgumentException - if the display name is invalid
	 */
	public void setDisplayName(java.lang.String name)
				throws IllegalArgumentException {
		address.setDisplayName(name);
	}
	
	/**
	 * Returns the scheme of SIP address.
	 * @return the scheme of this SIP address e.g. sip or sips
	 */
	public java.lang.String getScheme(){				
		return address.getURI().getScheme();
	}
	
	/**
	 * Sets the scheme of SIP address. Valid scheme format is defined in RFC 3261 [1] p.224
	 * @param scheme - the scheme of SIP address
	 * @throws IllegalArgumentException - if the scheme is invalid
	 */
	public void setScheme(java.lang.String scheme)
				throws IllegalArgumentException {
		
	}
	
	/**
	 * Returns the user part of SIP address.
	 * @return user part of SIP address. Returns null if the user part is missing.
	 */
	public java.lang.String getUser(){
		URI uri=address.getURI();
		if(uri.isSipURI())
			return ((SipURI)uri).getUser();
		else
			return ((TelURL)uri).getPhoneNumber();
	}
	
	/**
	 * Sets the user part of SIP address.
	 * @param user - the user part
	 * @throws IllegalArgumentException - if the user part is invalid 
	 */
	public void setUser(java.lang.String user)
				throws IllegalArgumentException {
		URI uri=address.getURI();
		if(uri.isSipURI())
			((SipURI)uri).setUser(user);
		else
			//TODO : check if the tel number is valid	
			((TelURL)uri).setPhoneNumber(user);
		
	}
	
	/**
	 * Returns the URI part of the address (without parameters) 
	 * i.e. scheme:user@host:port.
	 * @return the URI part of the address
	 */
	public java.lang.String getURI(){
		return address.getURI().toString();
	}
	
	/**
	 * Sets the URI part of the SIP address (without parameters) 
	 * i.e. scheme:user@host:port. Possible URI parameters are ignored.
	 * @param URI - URI part
	 * @throws IllegalArgumentException - if the URI is invalid
	 */
	public void setURI(java.lang.String URI)
				throws IllegalArgumentException {
		URI uri=null;
		try{
			uri=StackConnector.addressFactory.createURI(URI);		
		}
		catch(ParseException pe){
			throw new IllegalArgumentException(pe.getMessage());
		}   			
		if(uri==null)
			throw new IllegalArgumentException("The URI is invalid");
		address.setURI(uri);			
	}
	
	/**
	 * Returns the host part of the SIP address.
	 * @return host part of this address.
	 */
	public java.lang.String getHost(){
		return address.getHost();
	}
	
	/**
	 * Sets the host part of the SIP address.
	 * @param host - host part
	 * @throws IllegalArgumentException - if the post part is invalid
	 */
	public void setHost(java.lang.String host)
				throws IllegalArgumentException {
		URI uri=address.getURI();
		if(uri.isSipURI()){
			try{
				((SipURI)uri).setHost(host);		
			}
			catch(ParseException pe){
				pe.printStackTrace();
			}
		}
		else			
			((TelURL)uri).setPostDial(host);
	}
	
	/**
	 * Returns the port number of the SIP address. If port number is not set, return 5060. 
	 * If the address is wildcard “*” return 0.
	 * @return the port number
	 */
	public int getPort(){
		return address.getPort();
	}
	
	/**
	 * Sets the port number of the SIP address. Valid range is 0-65535, where 0 
	 * means that the port number is removed from the address URI.
	 * @param port - port number, valid range 0-65535, 0 means that port number 
	 * is removed from the address URI
	 * @throws IllegalArgumentException - if the port number is invalid
	 */
	public void setPort(int port)
				throws IllegalArgumentException {
		URI uri=address.getURI();
		if(uri.isSipURI()){			
			((SipURI)uri).setPort(port);								
		}		
		//TODO : do something for the tel URL
	}
	
	/**
	 * Returns the value associated with the named URI parameter.
	 * @param name - the name of the parameter
	 * @return the value of the named parameter, or empty string for parameters 
	 * without value and null if the parameter is not defined
	 */
	public java.lang.String getParameter(java.lang.String name) {
		URI uri=address.getURI();
		if(uri.isSipURI())
			return ((SipURI)uri).getParameter(name);
		//TODO : return something for the tel URL
		return null;
	}
	
	/**
	 * Sets the named URI parameter to the specified value. If the value is null 
	 * the parameter is interpreted as a parameter without value. 
	 * Existing parameter will be overwritten, otherwise the parameter is added.
	 * @param name - the named URI parameter
	 * @param value - the value
	 * @throws IllegalArgumentException - if the parameter is invalid RFC 3261, 
	 * chapter 19.1.1 SIP and SIPS URI Components “URI parameters” p.149
	 */
	public void setParameter(java.lang.String name, java.lang.String value)
				throws IllegalArgumentException{
		URI uri=address.getURI();
		if(uri.isSipURI()){
			try{
				((SipURI)uri).setParameter(name,value);	
			}
			catch(ParseException pe){
				pe.printStackTrace();
			}
		}
		//TODO : do something for the tel URL
	}
	
	/**
	 * Removes the named URI parameter.
	 * @param name - name of the parameter to be removed
	 */
	public void removeParameter(java.lang.String name){
		URI uri=address.getURI();
		if(uri.isSipURI())			
			((SipURI)uri).removeParameter(name);	
		//TODO : do something for the tel URL
	}
	
	/**
	 * Returns a String array of all parameter names.
	 * @return String array of parameter names. Returns null if the address does 
	 * not have any parameters.
	 */
	public java.lang.String[] getParameterNames(){
		URI uri=address.getURI();
		if(uri.isSipURI()){		
			Vector parameterNameList=((SipURI)uri).getParameterNames();
			String parameterNames[]= new String[parameterNameList.size()];
			for(int i=0;i<parameterNameList.size();i++)
				parameterNames[i]=(String)parameterNameList.elementAt(i);
			return parameterNames;
		}
		//TODO : return something for the tel URL
		return null;
	}
	
	/**
	 * Returns a fully qualified SIP address, with display name, URI and URI 
	 * parameters. If display name is not specified only a SIP URI is returned. 
	 * If the port is not explicitly set (to 5060 or other value) it will be omitted 
	 * from the address URI in returned String.
	 * @return a fully qualified SIP name address, SIP or SIPS URI
	 */
	public java.lang.String toString(){
		return address.toString();
	}
}
