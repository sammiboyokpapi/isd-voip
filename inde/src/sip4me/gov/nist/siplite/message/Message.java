/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD)        *
 ******************************************************************************/
package sip4me.gov.nist.siplite.message;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import sip4me.gov.nist.core.GenericObject;
import sip4me.gov.nist.core.InternalErrorHandler;
import sip4me.gov.nist.core.LogWriter;
import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.core.Utils;
import sip4me.gov.nist.microedition.sip.SDPOutputStream;
import sip4me.gov.nist.siplite.SIPConstants;
import sip4me.gov.nist.siplite.header.CSeqHeader;
import sip4me.gov.nist.siplite.header.CallIdHeader;
import sip4me.gov.nist.siplite.header.ContactList;
import sip4me.gov.nist.siplite.header.ContentLengthHeader;
import sip4me.gov.nist.siplite.header.ContentTypeHeader;
import sip4me.gov.nist.siplite.header.FromHeader;
import sip4me.gov.nist.siplite.header.Header;
import sip4me.gov.nist.siplite.header.HeaderList;
import sip4me.gov.nist.siplite.header.RecordRouteList;
import sip4me.gov.nist.siplite.header.RequestLine;
import sip4me.gov.nist.siplite.header.RouteList;
import sip4me.gov.nist.siplite.header.StatusLine;
import sip4me.gov.nist.siplite.header.ToHeader;
import sip4me.gov.nist.siplite.header.ViaHeader;
import sip4me.gov.nist.siplite.header.ViaList;
import sip4me.gov.nist.siplite.parser.HeaderParser;
import sip4me.gov.nist.siplite.parser.ParserFactory;
import sip4me.gov.nist.siplite.parser.PipelinedMsgParser;
import sip4me.gov.nist.siplite.parser.StringMsgParser;

/**
 * This is the main SIP Message structure.
 * 
 * @see StringMsgParser
 * @see PipelinedMsgParser
 * 
 * 
 *@version JAIN-SIP-1.1
 * 
 *@author M. Ranganathan <mranga@nist.gov> <br/>
 * 
 *        <a href="{@docRoot} /uncopyright.html">This code is in the public
 *         domain.</a>
 * 
 */

public abstract class Message extends GenericObject {

	private static Class sipHeaderListClass;

	protected static final String contentEncodingCharset = MessageFactory
			.getDefaultContentEncodingCharset();

	/**
	 * unparsed headers
	 */
	protected Vector unrecognizedHeaders;
	/**
	 * List of parsed headers (in the order they were added)
	 */
	protected Vector headers;

	/** Direct accessors for frequently accessed headers **/
	protected FromHeader fromHeader;
	protected ToHeader toHeader;
	protected CSeqHeader cSeqHeader;
	protected CallIdHeader callIdHeader;
	protected ContentLengthHeader contentLengthHeader;
	// protected MaxForwards maxForwardsHeader;

	// Payload
	protected String messageContent;
	protected byte[] messageContentBytes;
	protected Object messageContentObject;

	static {
		try {
			sipHeaderListClass = Class
					.forName("sip4me.gov.nist.siplite.header.HeaderList");
		} catch (ClassNotFoundException ex) {
			InternalErrorHandler.handleException(ex);
		}
	}

	// Table of headers indexed by name.
	private Hashtable nameTable;

	/**
	 * Return true if the header belongs only in a Request.
	 * 
	 *@param sipHeader
	 *            is the header to test.
	 */
	public static boolean isRequestHeader(Header sipHeader) {
		return sipHeader.getHeaderName().equals(Header.ALERT_INFO)
				|| sipHeader.getHeaderName().equals(Header.IN_REPLY_TO)
				|| sipHeader.getHeaderName().equals(Header.AUTHORIZATION)
				|| sipHeader.getHeaderName().equals(Header.MAX_FORWARDS)
				|| sipHeader.getHeaderName().equals(Header.PRIORITY)
				|| sipHeader.getHeaderName().equals(Header.PROXY_AUTHORIZATION)
				|| sipHeader.getHeaderName().equals(Header.PROXY_REQUIRE)
				|| sipHeader.getHeaderName().equals(Header.ROUTE)
				|| sipHeader.getHeaderName().equals(Header.SUBJECT);

	}

	/**
	 * Return true if the header belongs only in a response.
	 * 
	 *@param sipHeader
	 *            is the header to test.
	 */
	public static boolean isResponseHeader(Header sipHeader) {
		return sipHeader.getHeaderName().equals(Header.ERROR_INFO)
				|| sipHeader.getHeaderName().equals(Header.PROXY_AUTHENTICATE)
				|| sipHeader.getHeaderName().equals(Header.SERVER)
				|| sipHeader.getHeaderName().equals(Header.UNSUPPORTED)
				|| sipHeader.getHeaderName().equals(Header.RETRY_AFTER)
				|| sipHeader.getHeaderName().equals(Header.WARNING)
				|| sipHeader.getHeaderName().equals(Header.WWW_AUTHENTICATE);

	}

	/**
	 * Get A dialog identifier constructed from this messsage. This is an id
	 * that can be used to identify dialogs.
	 * 
	 * @param isServerTransaction
	 *            is a flag that indicates whether this is a server transaction.
	 */
	public abstract String getDialogId(boolean isServerTransaction);

	/**
	 * Encode this message as a string. This is more efficient when the payload
	 * is a string (rather than a binary array of bytes). If the payload cannot
	 * be encoded as a UTF-8 string then it is simply ignored (will not appear
	 * in the encoded message).
	 * 
	 * @return The Canonical String representation of the message (including the
	 *         canonical string representation of the SDP payload if it exists).
	 */
	public String encode() {
		StringBuffer encoding = new StringBuffer();
		// Synchronization added because of concurrent modification exception
		// noticed by Lamine Brahimi.
		synchronized (this.headers) {
			Enumeration it = this.headers.elements();

			while (it.hasMoreElements()) {
				Header siphdr = (Header) it.nextElement();
				if (!(siphdr instanceof ContentLengthHeader))
					encoding.append(siphdr.encode());
			}
		}

		// Add the content-length header
		if (contentLengthHeader != null)
			encoding.append(contentLengthHeader.encode()).append(
					Separators.NEWLINE);

		if (this.messageContentObject != null) {
			String mbody = this.getContent().toString();
			encoding.append(mbody);
		} else if (this.messageContent != null
				|| this.messageContentBytes != null) {
			String content = null;
			try {
				if (messageContent != null)
					content = messageContent;
				else
					content = new String(messageContentBytes,
							contentEncodingCharset);
			} catch (Exception ex) {
				content = "";
			}
			encoding.append(content);
		}
		return encoding.toString();
	}

	/**
	 * Encode the message as a byte array. Use this when the message payload is
	 * a binary byte array.
	 * 
	 * @return The Canonical byte array representation of the message (including
	 *         the canonical byte array representation of the SDP payload if it
	 *         exists all in one contiguous byte array).
	 * 
	 */
	public byte[] encodeAsBytes() {
		StringBuffer encoding = new StringBuffer();
		Enumeration it = this.headers.elements();

		while (it.hasMoreElements()) {
			Header siphdr = (Header) it.nextElement();
			if (!(siphdr instanceof ContentLengthHeader)) {
				encoding.append(siphdr.encode());
			}

		}

		byte[] retval = null;
		byte[] content = this.getRawContent();
		if (content != null) {
			if (this.contentLengthHeader == null) {
				encoding.append(Header.CONTENT_LENGTH + Separators.COLON
						+ Separators.SP + content.length + Separators.NEWLINE);
			} else {
				encoding.append(contentLengthHeader.encode());
			}
			encoding.append(Separators.NEWLINE);

			// Append the content
			byte[] msgarray = null;
			try {
				msgarray = encoding.toString().getBytes("UTF-8");
			} catch (Exception ex) {
				InternalErrorHandler.handleException(ex);
			}

			retval = new byte[msgarray.length + content.length];
			System.arraycopy(msgarray, 0, retval, 0, msgarray.length);
			System.arraycopy(content, 0, retval, msgarray.length,
					content.length);
		} else {
			// Message content does not exist.
			encoding.append(Header.CONTENT_LENGTH + Separators.COLON
					+ Separators.SP + '0' + Separators.NEWLINE);
			encoding.append(Separators.NEWLINE);
			try {
				retval = encoding.toString().getBytes("UTF-8");
			} catch (Exception ex) {
				InternalErrorHandler.handleException(ex);
			}
		}
		return retval;
	}

	/**
	 * clone this message (create a new deep physical copy). All headers in the
	 * message are cloned. You can modify the cloned copy without affecting the
	 * original.
	 * 
	 * @return A cloned copy of this object.
	 */
	public Object clone() {
		Message retval = null;
		try {
			retval = (Message) this.getClass().newInstance();
		} catch (IllegalAccessException ex) {
			InternalErrorHandler.handleException(ex);
		} catch (InstantiationException ex) {
			InternalErrorHandler.handleException(ex);
		}
		Enumeration li = headers.elements();
		while (li.hasMoreElements()) {
			try {
				Header sipHeader = (Header) ((Header) li.nextElement()).clone();
				retval.attachHeader(sipHeader);
			} catch (ParseException ex) {
			}
		}
		if (retval instanceof Request) {
			Request thisRequest = (Request) this;
			RequestLine rl = (RequestLine) (thisRequest.getRequestLine())
					.clone();
			((Request) retval).setRequestLine(rl);
		} else {
			Response thisResponse = (Response) this;
			StatusLine sl = (StatusLine) (thisResponse.getStatusLine()).clone();
			((Response) retval).setStatusLine(sl);
		}

		if (this.getContent() != null) {
			try {
				retval.setContent(this.getContent(), this
						.getContentTypeHeaderHeader());
			} catch (ParseException ex) {
				/** Ignore **/
			}
		}

		return retval;
	}

	/**
	 * 
	 * Constructor: Initializes lists and list headers. All the headers for
	 * which there can be multiple occurances in a message are derived from the
	 * HeaderListClass. All singleton headers are derived from Header class.
	 * 
	 */
	public Message() {
		this.unrecognizedHeaders = new Vector();
		this.headers = new Vector();
		nameTable = new Hashtable();
	}

	/**
	 * Attach a header and die if you get a duplicate header exception.
	 * 
	 * @param h
	 *            Header to attach.
	 */
	private void attachHeader(Header h) throws ParseException,
			IllegalArgumentException {
		if (h == null)
			throw new IllegalArgumentException("null header!");
		if (h instanceof HeaderList) {
			HeaderList hl = (HeaderList) h;
			if (hl.isEmpty()) {
				// System.out.println("Attaching an empty header: " +
				// h.getClass().getName());
				return;
			}
		}
		attachHeader(h, false, false);
	}

	/**
	 * Attach a header (replacing the original header).
	 * 
	 * @param header
	 *            Header that replaces a header of the same type.
	 */
	public void setHeader(Header sipHeader) throws IllegalArgumentException {
		Header header = (Header) sipHeader;
		if (header == null)
			throw new IllegalArgumentException("null header!");
		if (header instanceof HeaderList) {
			HeaderList hl = (HeaderList) header;
			// Ignore empty lists.
			if (hl.isEmpty())
				return;
		}
		this.removeHeader(header.getHeaderName());

		attachHeader(header, true, false);

	}

	/**
	 * Set a header from a linked list of headers.
	 * 
	 *@param headers
	 *            -- a list of headers to set.
	 */

	public void setHeaders(Vector headers) {
		Enumeration elements = headers.elements();
		while (elements.hasMoreElements()) {
			Header sipHeader = (Header) elements.nextElement();
			this.attachHeader(sipHeader, false);
		}
	}

	/**
	 * Attach a header to the end of the existing headers in this Message
	 * structure. This is equivalent to the
	 * attachHeader(Header,replaceflag,false); which is the normal way in which
	 * headers are attached. This was added in support of JAIN-SIP.
	 * 
	 * @since 1.0 (made this public)
	 * @param h
	 *            header to attach.
	 * @param replaceflag
	 *            if true then replace a header if it exists.
	 */
	public void attachHeader(Header h, boolean replaceflag) {

		this.attachHeader(h, replaceflag, false);

	}

	/**
	 * Attach the header to the SIP Message structure at a specified position in
	 * its list of headers.
	 * 
	 * @param newHeader
	 *            Header to attach.
	 * @param replaceFlag
	 *            If true then replace the existing header.
	 * @param top
	 *            Whether to add the new header to the top
	 */
	
	public void attachHeader(Header newHeader, boolean replaceFlag, boolean top) {
		if (newHeader == null) {
			throw new NullPointerException("null header");
		}
	
		Header headerToAttach;
	
		if (ListMap.hasList(newHeader)
				&& !sipHeaderListClass.isAssignableFrom(newHeader.getClass())) {
			HeaderList hdrList = ListMap.getList(newHeader);
			hdrList.add(newHeader);
			headerToAttach = hdrList;
		} else {
			headerToAttach = newHeader;
		}
	
	
		if (replaceFlag) {
			removeHeader(newHeader.getHeaderName());
		} else if (nameTable.containsKey(newHeader.getHeaderName()
				.toLowerCase())
				&& !(headerToAttach instanceof HeaderList)) {
			if (headerToAttach instanceof ContentLengthHeader) {
				try {
					ContentLengthHeader cl = (ContentLengthHeader) headerToAttach;
					contentLengthHeader.setContentLength(cl.getContentLength());
				} catch (IllegalArgumentException e) {
				}
			}
			// We don't want to replace it and the
			// header is already there. Abort.
			return;
		} else {
			// HeaderList already there, don't replace
		}
	

	
		if (!nameTable.containsKey(newHeader.getHeaderName().toLowerCase())) {
			nameTable.put(newHeader.getHeaderName().toLowerCase(),
					headerToAttach);
			headers.addElement(headerToAttach);
		} else {
			if (headerToAttach instanceof HeaderList) {
				HeaderList existingHdrList = (HeaderList) nameTable
						.get(newHeader.getHeaderName().toLowerCase());
				if (existingHdrList != null)
					existingHdrList.concatenate((HeaderList) headerToAttach,
							top);
				else
					nameTable.put(headerToAttach.getHeaderName().toLowerCase(),
							headerToAttach);
			} else {
				nameTable.put(headerToAttach.getHeaderName().toLowerCase(),
						headerToAttach);
			}
		}
	
		// Direct accessor fields for frequently accessed headers.
		if (headerToAttach instanceof FromHeader) {
			this.fromHeader = (FromHeader) headerToAttach;
		} else if (headerToAttach instanceof ContentLengthHeader) {
			this.contentLengthHeader = (ContentLengthHeader) headerToAttach;
		} else if (headerToAttach instanceof ToHeader) {
			this.toHeader = (ToHeader) headerToAttach;
		} else if (headerToAttach instanceof CSeqHeader) {
			this.cSeqHeader = (CSeqHeader) headerToAttach;
		} else if (headerToAttach instanceof CallIdHeader) {
			this.callIdHeader = (CallIdHeader) headerToAttach;
		}
	
	}

	/**
	 * Remove a header given its name. If multiple headers of a given name are
	 * present then the top flag determines which end to remove headers from.
	 * 
	 *@param headerName
	 *            is the name of the header to remove.
	 *@param top
	 *            -- flag that indicates which end of header list to process.
	 */
	public void removeHeader(String headerName, boolean top) {
		// System.out.println("removeHeader " + headerName);
		Header toRemove = (Header) nameTable.get(headerName.toLowerCase());
		// nothing to do then we are done.
		if (toRemove == null)
			return;
		if (toRemove instanceof HeaderList) {
			HeaderList hdrList = (HeaderList) toRemove;
			if (top)
				hdrList.removeFirst();
			else
				hdrList.removeLast();
			// Clean up empty list
			if (hdrList.isEmpty()) {
				Enumeration li = this.headers.elements();
				int index = -1;
				while (li.hasMoreElements()) {
					Header sipHeader = (Header) li.nextElement();
					index++;
					if (Utils.equalsIgnoreCase(sipHeader.getName(), headerName))
						break;
				}
				if (index != -1 && index < this.headers.size())
					headers.removeElementAt(index);
			}
		} else {
			this.nameTable.remove(headerName.toLowerCase());
			if (toRemove instanceof FromHeader) {
				this.fromHeader = null;
			} else if (toRemove instanceof ToHeader) {
				this.toHeader = null;
			} else if (toRemove instanceof CSeqHeader) {
				this.cSeqHeader = null;
			} else if (toRemove instanceof CallIdHeader) {
				this.callIdHeader = null;
			} else if (toRemove instanceof ContentLengthHeader) {
				this.contentLengthHeader = null;
			}
			Enumeration li = this.headers.elements();
			int index = -1;
			while (li.hasMoreElements()) {
				Header sipHeader = (Header) li.nextElement();
				index++;
				if (Utils.equalsIgnoreCase(sipHeader.getName(), headerName))
					break;
			}
			if (index != -1 && index < this.headers.size())
				this.headers.removeElementAt(index);
		}

	}

	/**
	 * Remove all headers given its name.
	 * 
	 *@param headerName
	 *            is the name of the header to remove.
	 */
	public void removeHeader(String headerName) {

		if (headerName == null)
			throw new NullPointerException("null arg");
		Header toRemove = (Header) nameTable.get(headerName.toLowerCase());
		// nothing to do then we are done.
		if (toRemove == null)
			return;
		nameTable.remove(headerName.toLowerCase());
		// Remove the fast accessor fields.
		if (toRemove instanceof FromHeader) {
			this.fromHeader = null;
		} else if (toRemove instanceof ToHeader) {
			this.toHeader = null;
		} else if (toRemove instanceof CSeqHeader) {
			this.cSeqHeader = null;
		} else if (toRemove instanceof CallIdHeader) {
			this.callIdHeader = null;
		} else if (toRemove instanceof ContentLengthHeader) {
			this.contentLengthHeader = null;
		}

		Enumeration li = this.headers.elements();
		int index = -1;
		while (li.hasMoreElements()) {
			Header sipHeader = (Header) li.nextElement();
			index++;
			if (Utils.equalsIgnoreCase(sipHeader.getName(), headerName))
				break;

		}
		if (index != -1 && index < headers.size())
			headers.removeElementAt(index);
	}

	/**
	 * Generate (compute) a transaction ID for this SIP message.
	 * 
	 * @return A string containing the concatenation of various portions of the
	 *         FromHeader,To,Via and RequestURI portions of this message as
	 *         specified in RFC 2543: All responses to a request contain the
	 *         same values in the Call-ID, CSeqHeader, To, and FromHeader fields
	 *         (with the possible addition of a tag in the To field (section
	 *         10.43)). This allows responses to be matched with requests.
	 *         Incorporates a bug fix for a bug sent in by Gordon Ledgard of
	 *         IPera for generating transactionIDs when no port is present in
	 *         the via header. Incorporates a bug fix for a bug report sent in
	 *         by Chris Mills of Nortel Networks (converts to lower case when
	 *         returning the transaction identifier).
	 * 
	 *@return a string that can be used as a transaction identifier for this
	 *         message. This can be used for matching responses and requests
	 *         (i.e. an outgoing request and its matching response have the same
	 *         computed transaction identifier).
	 */
	public String getTransactionId() {
		ViaHeader topVia = null;
		if (!this.getViaHeaders().isEmpty()) {
			topVia = (ViaHeader) this.getViaHeaders().first();
		}
		// Have specified a branch Identifier so we can use it to identify
		// the transaction.
		if (topVia.getBranch() != null
				&& topVia.getBranch().startsWith(
						SIPConstants.BRANCH_MAGIC_COOKIE)) {
			// Bis 09 compatible branch assignment algorithm.
			// implies that the branch id can be used as a transaction
			// identifier.
			return topVia.getBranch().toLowerCase();
		} else {
			// Old style client so construct the transaction identifier
			// from various fields of the request.
			StringBuffer retval = new StringBuffer();
			FromHeader from = (FromHeader) this.getFromHeader();
			ToHeader to = (ToHeader) this.getTo();
			String hpFromHeader = from.getUserAtHostPort();
			retval.append(hpFromHeader).append(":");
			if (from.hasTag())
				retval.append(from.getTag()).append(":");
			String hpTo = to.getUserAtHostPort();
			retval.append(hpTo).append(":");
			String cid = this.callIdHeader.getCallId();
			retval.append(cid).append(":");
			retval.append(this.cSeqHeader.getSequenceNumber()).append(":")
					.append(this.cSeqHeader.getMethod());
			if (topVia != null) {
				retval.append(":").append(topVia.getSentBy().encode());
				if (!topVia.getSentBy().hasPort()) {
					retval.append(":").append(5060);
				}
			}
			String hc = Utils.toHexString(retval.toString().toLowerCase()
					.getBytes());
			if (hc.length() < 32)
				return hc;
			else
				return hc.substring(hc.length() - 32, hc.length() - 1);
		}
		// Convert to lower case -- bug fix as a result of a bug report
		// from Chris Mills of Nortel Networks.
	}

	/**
	 * Return true if this message has a body.
	 */
	public boolean hasContent() {
		return messageContent != null || messageContentBytes != null;
	}

	/**
	 * Return an iterator for the list of headers in this message.
	 * 
	 * @return an Iterator for the headers of this message.
	 */
	public Enumeration getHeaders() {
		return headers.elements();
	}

	/**
	 * Get the first header of the given name.
	 * 
	 *@return header -- the first header of the given name.
	 */
	public Header getHeader(String headerName) {
		if (headerName == null)
			throw new NullPointerException("null header name");
		Header sipHeader = (Header) this.nameTable
				.get(headerName.toLowerCase());
		if (sipHeader instanceof HeaderList)
			return (Header) ((HeaderList) sipHeader).getFirst();
		else
			return (Header) sipHeader;
	}

	/**
	 * Get the contentType header (null if one does not exist).
	 * 
	 * @return contentType header
	 */
	public ContentTypeHeader getContentTypeHeaderHeader() {
		return (ContentTypeHeader) this.getHeader(Header.CONTENT_TYPE);
	}

	/**
	 * Get the from header.
	 * 
	 * @return -- the from header.
	 */
	public FromHeader getFromHeader() {
		return (FromHeader) fromHeader;
	}

	/**
	 * 
	 * /** Get the Contact list of headers (null if one does not exist).
	 * 
	 * @return List containing Contact headers.
	 */
	public ContactList getContactHeaders() {
		return (ContactList) this.getHeaderList(Header.CONTACT);
	}

	/**
	 * Get the Via list of headers (null if one does not exist).
	 * 
	 * @return List containing Via headers.
	 */
	public ViaList getViaHeaders() {
		return (ViaList) getHeaderList(Header.VIA);
	}

	/**
	 * Get an iterator to the list of vial headers.
	 * 
	 * @return a list iterator to the list of via headers. public ListIterator
	 *         getVia() { return this.viaHeaders.listIterator(); }
	 */

	/**
	 * Set A list of via headers.
	 * 
	 * @param - a list of via headers to add.
	 */
	public void setVia(ViaList viaList) {
		this.setHeader(viaList);
	}

	/**
	 * Set a list of via headers.
	 */
	public void setVia(Vector viaList) {
		this.removeHeader(ViaHeader.NAME);
		for (int i = 0; i < viaList.size(); i++) {
			ViaHeader via = (ViaHeader) viaList.elementAt(i);
			this.addHeader(via);
		}
	}

	/**
	 * Set the header given a list of headers.
	 * 
	 *@param headerList
	 *            a headerList to set
	 */

	public void setHeader(HeaderList sipHeaderList) {
		this.setHeader((Header) sipHeaderList);
	}

	/**
	 * Get the topmost via header.
	 * 
	 * @return the top most via header if one exists or null if none exists.
	 */
	public ViaHeader getTopmostVia() {
		if (this.getViaHeaders() == null)
			return null;
		else
			return (ViaHeader) (getViaHeaders().getFirst());
	}

	/**
	 * Get the CSeqHeader list of header (null if one does not exist).
	 * 
	 * @return CSeqHeader header
	 */
	public CSeqHeader getCSeqHeader() {
		return cSeqHeader;
	}

	/**
	 * Get the sequence number.
	 * 
	 * @return the sequence number.
	 */
	public int getCSeqHeaderNumber() {
		return cSeqHeader.getSequenceNumber();
	}

	/**
	 * Get the Route List of headers (null if one does not exist).
	 * 
	 * @return List containing Route headers
	 */
	public RouteList getRouteHeaders() {
		return (RouteList) getHeaderList(Header.ROUTE);
	}

	/**
	 * Get the CallIdHeader header (null if one does not exist)
	 * 
	 * @return Call-ID header .
	 */
	public CallIdHeader getCallId() {
		return callIdHeader;
	}

	/**
	 * Set the call id header.
	 * 
	 *@param callid
	 *            -- call idHeader (what else could it be?)
	 */
	public void setCallId(CallIdHeader callId) {
		this.setHeader(callId);
	}

	/**
	 * Get the CallIdHeader header (null if one does not exist)
	 * 
	 *@param callId
	 *            -- the call identifier to be assigned to the call id header
	 */
	public void setCallId(String callId) throws ParseException {
		if (callIdHeader == null) {
			this.setHeader(new CallIdHeader());
		}
		callIdHeader.setCallId(callId);
	}

	/**
	 * Get the call ID string. A conveniance function that returns the stuff
	 * following the header name for the call id header.
	 * 
	 *@return the call identifier.
	 * 
	 */
	public String getCallIdentifier() {
		return callIdHeader.getCallId();
	}

	/**
	 * Get the RecordRoute header list (null if one does not exist).
	 * 
	 * @return Record-Route header
	 */
	public RecordRouteList getRecordRouteHeaders() {
		return (RecordRouteList) this.getHeaderList(Header.RECORD_ROUTE);
	}

	/**
	 * Get the To header (null if one does not exist).
	 * 
	 * @return To header
	 */
	public ToHeader getTo() {
		return (ToHeader) toHeader;
	}

	public void setTo(ToHeader to) {
		this.setHeader(to);
	}

	public void setFromHeader(FromHeader from) {
		this.setHeader(from);

	}

	/**
	 * Get the ContentLengthHeader header (null if one does not exist).
	 * 
	 * @return content-length header.
	 */
	public ContentLengthHeader getContentLengthHeader() {
		return contentLengthHeader;
	}

	/**
	 * Get the message body as a string. If the message contains a content type
	 * header with a specified charset, and if the payload has been read as a
	 * byte array, then it is returned encoded into this charset.
	 * 
	 * @return Message body (as a string)
	 * 
	 */
	public String getMessageContent() throws UnsupportedEncodingException {
		if (this.messageContent == null && this.messageContentBytes == null)
			return null;
		else if (this.messageContent == null) {
			ContentTypeHeader contentTypeHeader = (ContentTypeHeader) this.nameTable
					.get(Header.CONTENT_TYPE.toLowerCase());
			if (contentTypeHeader != null) {
				String charset = contentTypeHeader.getCharset();
				if (charset != null) {
					this.messageContent = new String(messageContentBytes,
							charset);
				} else {
					this.messageContent = new String(messageContentBytes,
							contentEncodingCharset);
				}
			} else
				this.messageContent = new String(messageContentBytes,
						contentEncodingCharset);
		}
		return this.messageContent;
	}

	/**
	 * Get the message content as an array of bytes. If the payload has been
	 * read as a String then it is decoded using the charset specified in the
	 * content type header if it exists. Otherwise, it is encoded using the
	 * default encoding which is UTF-8.
	 * 
	 *@return an array of bytes that is the message payload.
	 * 
	 */
	public byte[] getRawContent() {
		try {
			if (this.messageContent == null && this.messageContentBytes == null
					&& this.messageContentObject == null) {
				return null;
			} else if (this.messageContentObject != null) {
				String messageContent = String.valueOf(messageContentObject);
				byte[] messageContentBytes;
				ContentTypeHeader contentTypeHeader = (ContentTypeHeader) this.nameTable
						.get(Header.CONTENT_TYPE.toLowerCase());
				if (contentTypeHeader != null) {
					String charset = contentTypeHeader.getCharset();
					if (charset != null) {
						messageContentBytes = messageContent.getBytes(charset);
					} else {
						messageContentBytes = messageContent
								.getBytes(contentEncodingCharset);
					}
				} else
					messageContentBytes = messageContent
							.getBytes(contentEncodingCharset);
				return messageContentBytes;
			} else if (this.messageContent != null) {
				byte[] messageContentBytes;
				ContentTypeHeader contentTypeHeader = (ContentTypeHeader) this.nameTable
						.get(Header.CONTENT_TYPE.toLowerCase());
				if (contentTypeHeader != null) {
					String charset = contentTypeHeader.getCharset();
					if (charset != null) {
						messageContentBytes = this.messageContent
								.getBytes(charset);
					} else {
						messageContentBytes = this.messageContent
								.getBytes(contentEncodingCharset);
					}
				} else
					messageContentBytes = this.messageContent
							.getBytes(contentEncodingCharset);
				return messageContentBytes;
			} else {
				return messageContentBytes;
			}
		} catch (UnsupportedEncodingException ex) {
			InternalErrorHandler.handleException(ex);
			return null;
		}
	}

	/**
	 * Set the message content given type and subtype.
	 * 
	 *@param type
	 *            is the message type (eg. application)
	 *@param subType
	 *            is the message sybtype (eg. sdp)
	 *@param messageContent
	 *            is the messge content as a string.
	 */

	public void setMessageContent(String type, String subType,
			String messageContent) throws IllegalArgumentException {
		if (messageContent == null)
			throw new IllegalArgumentException("messageContent is null");
		ContentTypeHeader ct = new ContentTypeHeader(type, subType);
		this.setHeader(ct);
		this.messageContent = messageContent;
		this.messageContentBytes = null;
		this.messageContentObject = null;

		if (this.getContentLengthHeader() != null) {
			this.getContentLengthHeader().setContentLength(
					messageContent.length());
		}

	}

	/**
	 * Set the message content after converting the given object to a String.
	 * 
	 *@param content
	 *            -- content to set.
	 *@param contentTypeHeader
	 *            -- content type header corresponding to content.
	 */
	public void setContent(Object content, ContentTypeHeader contentTypeHeader)
			throws ParseException {
		if (content == null)
			throw new NullPointerException("null content");

		this.messageContent = null;
		this.messageContentBytes = null;
		this.messageContentObject = null;
		this.setHeader(contentTypeHeader);

		if (content instanceof String) {
			this.messageContent = (String) content;
		} else if (content instanceof SDPOutputStream) {
			this.messageContentBytes = ((SDPOutputStream) content)
					.toByteArray();
		} else if (content instanceof byte[]) {
			this.messageContentBytes = (byte[]) content;
		} else
			this.messageContentObject = content;

		computeContentLength(content);

	}

	public void setContent(Object content) {
		if (content == null)
			throw new NullPointerException("null content");
		String contentString = String.valueOf(content);
		this.setMessageContent(contentString);
		this.removeContent();
		if (content instanceof String) {
			this.messageContent = (String) content;
		} else if (content instanceof SDPOutputStream) {
			this.messageContentBytes = ((SDPOutputStream) content)
					.toByteArray();
		} else if (content instanceof byte[]) {
			this.messageContentBytes = (byte[]) content;
		} else
			this.messageContentObject = content;

		computeContentLength(content);
	}

	/**
	 * Get the content of the header.
	 * 
	 *@return the content of the sip message.
	 */
	public Object getContent() {
		if (this.messageContentObject != null)
			return messageContentObject;
		else if (this.messageContentBytes != null)
			return this.messageContentBytes;
		else if (this.messageContent != null)
			return this.messageContent;
		else
			return null;
	}

	/**
	 * Set the message content for a given type and subtype.
	 * 
	 *@param type
	 *            is the messge type.
	 *@param subType
	 *            is the message subType.
	 *@param messageContent
	 *            is the message content as a byte array.
	 */
	public void setMessageContent(String type, String subType,
			byte[] messageContent) {
		ContentTypeHeader ct = new ContentTypeHeader(type, subType);
		this.setHeader(ct);
		this.setMessageContent(messageContent);

		if (this.getContentLengthHeader() != null) {
			this.getContentLengthHeader().setContentLength(
					messageContent.length);
		}

	}

	/**
	 * Set the message content for this message.
	 * 
	 * @param content
	 *            Message body as a string.
	 */
	public void setMessageContent(String content) {
		int clength = (content == null ? 0 : content.length());

		if (this.getContentLengthHeader() != null) {
			this.getContentLengthHeader().setContentLength(clength);
		}

		messageContent = content;
		messageContentBytes = null;
		messageContentObject = null;
	}

	/**
	 * Set the message content as an array of bytes.
	 * 
	 *@param content
	 *            is the content of the message as an array of bytes.
	 */
	public void setMessageContent(byte[] content) {
		if (LogWriter.needsLogging)
			LogWriter.logMessage(LogWriter.TRACE_DEBUG,
					"Setting payload to SIP Message:\n" + new String(content));

		if (this.getContentLengthHeader() != null) {
			this.getContentLengthHeader().setContentLength(content.length);
		}

		messageContentBytes = content;
		messageContent = null;
		messageContentObject = null;
	}

	/**
	 * Remove the message content if it exists.
	 * 
	 */
	public void removeContent() {
		messageContent = null;
		messageContentBytes = null;
		messageContentObject = null;
	}

	/**
	 * Compute and set the Content-length header based on the given content
	 * object.
	 * 
	 * @param content
	 *            is the content, as String, array of bytes, or other object.
	 */
	private void computeContentLength(Object content) {
		int length = 0;
		if (content != null) {
			if (content instanceof String) {
				String charset = null;
				ContentTypeHeader contentTypeHeader = (ContentTypeHeader) this.nameTable
						.get(Header.CONTENT_TYPE.toLowerCase());
				if (contentTypeHeader != null) {
					charset = contentTypeHeader.getCharset();
				}

				// Solves NullPointerException pointed out by
				// Pulkit Bhardwaj, from Tata Consultancy Services
				if (charset == null) {
					charset = contentEncodingCharset;
				}
				try {
					// This line seems to be not there on JAIN SIP 1.2,
					// but we keep it unless it makes any harm.
					// as we already checked in setContent() methods that
					// content is instanceof string, messageContent cannot
					// be null at this point.
					messageContentBytes = messageContent.getBytes(charset);
					length = ((String) content).getBytes(charset).length;
				} catch (UnsupportedEncodingException ex) {
					InternalErrorHandler.handleException(ex);
				}
			} else if (content instanceof byte[]) {
				length = ((byte[]) content).length;
			} else {
				length = String.valueOf(content).length();
			}
		}
		contentLengthHeader.setContentLength(length);
	}

	/**
	 * Get a SIP header or Header list given its name.
	 * 
	 * @param headerName
	 *            is the name of the header to get.
	 *@return a header or header list that contians the retrieved header.
	 */
	public Enumeration getHeaders(String headerName) {
		if (headerName == null)
			throw new NullPointerException("null headerName");
		Header sipHeader = (Header) nameTable.get(headerName.toLowerCase());
		// empty iterator
		if (sipHeader == null)
			return new Vector().elements();
		if (sipHeader instanceof HeaderList) {
			return ((HeaderList) sipHeader).getElements();
		} else {
			Vector v = new Vector();
			v.addElement(sipHeader);
			return v.elements();
		}
	}

	private HeaderList getHeaderList(String headerName) {
		return (HeaderList) nameTable.get(headerName.toLowerCase());
	}

	/**
	 * Return true if the Message has a header of the given name.
	 * 
	 *@param headerName
	 *            is the header name for which we are testing.
	 *@return true if the header is present in the message
	 */

	public boolean hasHeader(String headerName) {
		return nameTable.containsKey(headerName.toLowerCase());
	}

	/**
	 * Return true if the message has a FromHeader header tag.
	 * 
	 *@return true if the message has a from header and that header has a tag.
	 */
	public boolean hasFromHeaderTag() {
		return fromHeader != null && fromHeader.getTag() != null;
	}

	/**
	 * Return true if the message has a To header tag.
	 * 
	 *@return true if the message has a to header and that header has a tag.
	 */
	public boolean hasToTag() {
		return toHeader != null && toHeader.getTag() != null;
	}

	/**
	 * Return the from tag.
	 * 
	 *@return the tag from the from header.
	 * 
	 */
	public String getFromHeaderTag() {
		return fromHeader == null ? null : fromHeader.getTag();
	}

	/**
	 * Set the FromHeader Tag.
	 * 
	 *@param tag
	 *            -- tag to set in the from header.
	 */
	public void setFromHeaderTag(String tag) {

		fromHeader.setTag(tag);

	}

	/**
	 * Set the to tag.
	 * 
	 *@param tag
	 *            -- tag to set.
	 */
	public void setToTag(String tag) {

		toHeader.setTag(tag);

	}

	/**
	 * Return the to tag.
	 */
	public String getToTag() {
		return toHeader == null ? null : toHeader.getTag();
	}

	/**
	 * Return the encoded first line.
	 */
	public abstract String getFirstLine();

	/**
	 * Add a SIP header.
	 * 
	 * @param sipHeader
	 *            -- sip header to add.
	 */
	public void addHeader(Header sipHeader) {
		Header sh = (Header) sipHeader;
		if (sipHeader instanceof ViaHeader) {
			attachHeader(sh, false, true);
		} else {
			attachHeader(sh, false, false);
		}
	}

	/**
	 * Add a header to the unparsed list of headers.
	 * 
	 *@param unparsed
	 *            -- unparsed header to add to the list.
	 */
	public void addUnparsed(String unparsed) {
		this.unrecognizedHeaders.addElement(unparsed);
	}

	/**
	 * Add a SIP header.
	 * 
	 * @param sipHeader
	 *            -- string version of SIP header to add.
	 */

	public void addHeader(String sipHeader) {
		String hdrString = sipHeader.trim() + "\n";
		try {
			HeaderParser parser = ParserFactory.createParser(sipHeader);
			Header sh = parser.parse();
			this.attachHeader(sh, false);
		} catch (ParseException ex) {
			this.unrecognizedHeaders.addElement(hdrString);
		}
	}

	/**
	 * Get a list containing the unrecognized headers.
	 * 
	 * @return a linked list containing unrecongnized headers.
	 */
	public Enumeration getUnrecognizedHeaders() {
		return this.unrecognizedHeaders.elements();
	}

	/**
	 * Get the header names.
	 * 
	 *@return a list iterator to a list of header names. These are ordered in
	 *         the same order as are present in the message.
	 */
	public Enumeration getHeaderNames() {
		Enumeration li = this.headers.elements();
		Vector retval = new Vector();
		while (li.hasMoreElements()) {
			Header sipHeader = (Header) li.nextElement();
			String name = sipHeader.getName();
			retval.addElement(name);
		}
		return retval.elements();
	}

	/**
	 * Compare for equality.
	 * 
	 *@param other
	 *            -- the other object to compare with.
	 * 
	 */

	public boolean equals(Object other) {
		if (!other.getClass().equals(this.getClass()))
			return false;
		Message otherMessage = (Message) other;
		Enumeration values = this.nameTable.elements();
		if (otherMessage.nameTable.size() != nameTable.size())
			return false;
		while (values.hasMoreElements()) {
			Header mine = (Header) values.nextElement();
			Header his = (Header) nameTable.get(mine.getHeaderName()
					.toLowerCase());
			if (his == null)
				return false;
			else if (!his.equals(mine))
				return false;
		}
		return true;
	}

	/**
	 * Set the content length header.
	 * 
	 *@param contentLength
	 *            -- content length header.
	 */
	public void setContentLength(ContentLengthHeader contentLength) {
		this.setHeader(contentLength);
	}

	/**
	 * Set the CSeqHeader header.
	 * 
	 *@param cseqHeader
	 *            -- CSeqHeader Header.
	 */

	public void setCSeqHeader(CSeqHeader cseqHeader) {
		this.setHeader(cseqHeader);
	}

	public abstract void setSIPVersion(String sipVersion) throws ParseException;

	public abstract String getSIPVersion();
}
