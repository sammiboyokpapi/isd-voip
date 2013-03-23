package sip4me.gov.nist.siplite.stack;


import java.util.Enumeration;
import java.util.Vector;

import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.siplite.SipStack;
import sip4me.gov.nist.siplite.address.Address;
import sip4me.gov.nist.siplite.address.Hop;
import sip4me.gov.nist.siplite.address.Router;
import sip4me.gov.nist.siplite.address.SipURI;
import sip4me.gov.nist.siplite.address.URI;
import sip4me.gov.nist.siplite.header.RequestLine;
import sip4me.gov.nist.siplite.header.RouteHeader;
import sip4me.gov.nist.siplite.header.RouteList;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.message.Request;


/** This is the default router. When the implementation wants to forward
* a request and  had run out of options, then it calls this method
* to figure out where to send the request. The default router implements
* a simple "default routing algorithm" which just forwards to the configured
* proxy address.
*/

public class DefaultRouter implements Router {
        protected Hop defaultRoute;
        
        protected SipStack sipStack;

	/**
	* Constructor.
	*/
        public DefaultRouter() {
	}

        /**
         *Set the next hop address.
         *@param hopString is a string which is interpreted
         *  by us in the following fashion :
         *   host:port/TRANSPORT determines the next hop.
         */
        public void setNextHop(String hopString) 
            throws IllegalArgumentException {
            defaultRoute = new Hop(hopString);
	    defaultRoute.setDefaultRouteFlag();
        }
        
        /**
         * Return  addresses for default proxy to forward the request to. 
         * The list is organized in the following priority.
         * If the requestURI refers directly to a host, the host and port
         * information are extracted from it and made the first hop on the
         * list. The second element in the list is the default route, if
         * such a route is specified in the configuration of the stack.
         *@param method is the method of the request.
         *@param requestURI is the request URI of the request.
         */
        public Hop getNextHop(Request sipRequest) 
        throws IllegalArgumentException {


        	RequestLine requestLine = sipRequest.getRequestLine();
        	if (requestLine == null) {
        		return defaultRoute;
        	}
        	URI requestURI = requestLine.getUri();
        	if (requestURI == null)
        		throw new IllegalArgumentException("Bad message: Null requestURI");

        	RouteList routes = sipRequest.getRouteHeaders();

        	/*
        	 * In case the topmost Route header contains no 'lr' parameter (which
        	 * means the next hop is a strict router), the implementation will
        	 * perform 'Route Information Postprocessing' as described in RFC3261
        	 * section 16.6 step 6 (also known as "Route header popping"). That is,
        	 * the following modifications will be made to the request:
        	 * 
        	 * The implementation places the Request-URI into the Route header field
        	 * as the last value.
        	 * 
        	 * The implementation then places the first Route header field value
        	 * into the Request-URI and removes that value from the Route header
        	 * field.
        	 * 
        	 * Subsequently, the request URI will be used as next hop target
        	 */

        	if (routes != null) {

        		// to send the request through a specified hop the application is
        		// supposed to prepend the appropriate Route header.
        		RouteHeader route = (RouteHeader) routes.getFirst();
        		URI uri = route.getAddress().getURI();
        		if (uri.isSipURI()) {
        			SipURI sipUri = (SipURI) uri;
        			if (!sipUri.hasLrParam()) {

        				fixStrictRouting(sipRequest);
        				if (LogWriter.needsLogging) 
        					LogWriter.logMessage
        					("Route post processing fixed strict routing");
        			}

        			Hop hop = createHop(sipUri,sipRequest);
        			if (LogWriter.needsLogging) 
        				LogWriter.logMessage
        				("NextHop based on Route:" + hop);

        			return hop;
        		} else {
        			throw new IllegalArgumentException("First Route not a SIP URI");
        		}

        	} else if (requestURI.isSipURI()
        			&& ((SipURI) requestURI).getMAddrParam() != null) {
        		Hop hop = createHop((SipURI) requestURI,sipRequest);

        		if (LogWriter.needsLogging) 
        			LogWriter.logMessage
        			("Using request URI maddr to route the request = "
        					+ hop.toString());

        		((SipURI) requestURI).removeParameter("maddr");

        		return hop;

        	} else if (defaultRoute != null) {

        		if (LogWriter.needsLogging) 
        			LogWriter.logMessage
        			("Using outbound proxy to route the request = "
        					+ defaultRoute.toString());
        		return defaultRoute;
        	} else if (requestURI.isSipURI()) {
        		Hop hop = createHop((SipURI) requestURI,sipRequest);
        		if (hop != null && LogWriter.needsLogging)
        			LogWriter.logMessage("Used request-URI for nextHop = "
        					+ hop.toString());
        		else if (LogWriter.needsLogging) {
        			LogWriter.logMessage("returning null hop -- loop detected");
        		}
        		return hop;

        	} else {
        		// The internal router should never be consulted for non-sip URIs.
        		throw new IllegalArgumentException("Unexpected non-sip URI");
        	}


        }

	
	public Enumeration  getNextHops(Request sipRequest) 
	throws IllegalArgumentException { 
		Vector nextHops = new Vector();
		nextHops.addElement(getNextHop(sipRequest));
		return nextHops.elements();
	}
	
	
	/** Get the default hop.
	*@return defaultRoute is the default route.
	*/
	public Hop getOutboundProxy() { return this.defaultRoute; }


    public void setOutboundProxy(String outboundProxy) {
		this.defaultRoute = new Hop(outboundProxy);
	}

	public void setSipStack(SipStack sipStack) {
		this.sipStack = sipStack;
	}
	
	/**
	 * Performs strict router fix according to RFC3261 section 16.6 step 6
	 * 
	 * pre: top route header in request has no 'lr' parameter in URI post:
	 * request-URI added as last route header, new req-URI = top-route-URI
	 */
	public void fixStrictRouting(Request req) {

		RouteList routes = req.getRouteHeaders();
		RouteHeader first = (RouteHeader) routes.getFirst();
		SipURI firstUri = (SipURI) first.getAddress().getURI();
		routes.removeFirst();

		// Add request-URI as last Route entry
		Address addr = new Address();
		addr.setURI(req.getRequestURI()); // don't clone it
		RouteHeader route = new RouteHeader(addr);

		routes.add(route); // as last one
		req.setRequestURI(firstUri);
		
		if (LogWriter.needsLogging) 
			LogWriter.logMessage
			("post: fixStrictRouting" + req);
		
	}
	
	
	/**
	 * Utility method to create a hop from a SIP URI
	 * 
	 * @param sipUri
	 * @return
	 */
	private final Hop createHop(SipURI sipUri, Request request) {
		// always use TLS when secure
		String transport = sipUri.isSecure() ? "tls" : sipUri
				.getTransportParam();
		if (transport == null) {
			ViaHeader via = (ViaHeader) request.getHeader(ViaHeader.NAME);
			transport = via.getTransport();
		} 
		
		// sipUri.removeParameter("transport");

		int port;
		if (sipUri.getPort() != -1) {
			port = sipUri.getPort();
		} else {
			if (transport.equalsIgnoreCase("tls"))
				port = 5061;
			else
				port = 5060; // TCP or UDP
		}
		String host = sipUri.getMAddrParam() != null ? sipUri.getMAddrParam()
				: sipUri.getHost();
		return new Hop(host, port, transport);

	}


        
}
		
