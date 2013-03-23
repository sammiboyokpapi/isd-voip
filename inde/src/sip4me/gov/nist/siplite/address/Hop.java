/*
 * Hop.java
 *
 * Created on July 15, 2001, 2:28 PM
 */

package sip4me.gov.nist.siplite.address;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.StringTokenizer;
import sip4me.gov.nist.core.Utils;

/**
 *
 * @author  M. Ranganathan
 * @version
 */

/**
 * Routing algorithms return a list of hops to which the request is
 * routed.
 */
public class Hop extends Object {
    protected String host;
    protected int port;
    protected String transport;
    protected boolean explicitRoute; // this is generated from a ROUTE header.
    protected boolean defaultRoute; // This is generated from the proxy addr
    protected boolean uriRoute; // This is extracted from the requestURI.
    
    public String toString() {
		return host+":"+port+"/"+transport;
    }
    
    public boolean equals(Object other) {
        if (other.getClass().equals(this.getClass())) {
            Hop otherhop = (Hop) other;
            return (otherhop.host.equals(this.host) &&
            otherhop.port == this.port);
        } else return false;
    }
    
    
    /** Create new hop given host, port and transport.
     *@param hostName hostname
     *@param portNumber port
     *@param trans transport
     */
    public Hop(String hostName, int portNumber, String trans) {
    	if (LogWriter.needsLogging) {
    		LogWriter.logMessage("create hop for " + hostName + ":" + portNumber + "/" + trans);
    	}
        host = hostName;
        port = portNumber;
        if (trans == null) transport = "UDP";
        else if (trans == "") transport = "UDP";
        else transport = trans;
    }
    
    /** Creates new Hop
     *@param hop is a hop string in the form of host:port/Transport
     *@throws IllegalArgument exception if string is not properly formatted or
     * null.
     */
    public Hop(String hop) throws IllegalArgumentException {
	if (LogWriter.needsLogging) {
		LogWriter.logMessage("create hop for " + hop);
	}
        if (hop == null) throw new IllegalArgumentException("Null arg!");
        try {
            StringTokenizer stringTokenizer = new StringTokenizer(hop + "/");
            String hostPort = stringTokenizer.getNextToken('/');
            // Skip over the slash.
            stringTokenizer.getNextChar();
            // get the transport string.
            transport = stringTokenizer.getNextToken('/').trim();
            if (transport == null) transport = "UDP";
            else if (transport == "") transport = "UDP";
            if ( Utils.compareToIgnoreCase(transport,"UDP") != 0 &&
            Utils.compareToIgnoreCase(transport,"TCP") != 0
            ) {
                System.out.println("Bad transport string " + transport);
                throw new IllegalArgumentException(hop);
            }
            stringTokenizer = new StringTokenizer(hostPort+":");
            host = stringTokenizer.getNextToken(':');
            if (host == null || host.equals( "") )
                throw new IllegalArgumentException("no host!");
            stringTokenizer.consume(1);
            String portString = null;
            portString = stringTokenizer.getNextToken(':');
            
            if (portString == null || portString.equals("")) {
                port = 5060;
            } else {
                try {
                    port = Integer.parseInt(portString);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Bad port spec");
                }
            }
            defaultRoute = true;
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Bad hop");
        }
        
    }
    
    /**
     *Retruns the host string.
     *@return host String
     */
    public String getHost() {
        return host;
    }
    
    /**
     *Returns the port.
     *@return port integer.
     */
    public int getPort() {
        return port;
    }
    
    /** returns the transport string.
     */
    public String getTransport() {
        return transport;
    }
    
    /** Return true if this is an explicit route (extacted from a ROUTE
     * Header)
     */
    public boolean isExplicitRoute() {
        return explicitRoute;
    }
    
    /** Return true if this is a default route (next hop proxy address).
     */
    public boolean isDefaultRoute() {
        return defaultRoute;
    }
    
    /** Return true if this is uriRoute
     */
    public boolean isURIRoute() { return uriRoute; }
    
    /** Set the URIRoute flag.
     */
    public void setURIRouteFlag() { uriRoute = true; }
    
    
    /** Set the defaultRouteFlag.
     */
    public void setDefaultRouteFlag() { defaultRoute = true; }
    
    /** Set the explicitRoute flag.
     */
    public void setExplicitRouteFlag() { explicitRoute = true; }
    
    
}
