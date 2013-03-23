package sip4me.gov.nist.javax.sdp.parser;

import java.util.Vector;

import sip4me.gov.nist.core.ParseException;
import sip4me.gov.nist.core.ParserCore;
import sip4me.gov.nist.javax.sdp.SessionDescription;
import sip4me.gov.nist.javax.sdp.fields.SDPField;

/**
 *
 */
public class SDPAnnounceParser extends ParserCore {

	protected Lexer lexer;
	protected Vector sdpMessage;

	/**
	 * Creates new SDPAnnounceParser
	 * 
	 * @param sdpMessage
	 *            Vector of messages to parse.
	 */
	public SDPAnnounceParser(Vector sdpMessage) {
		this.sdpMessage = sdpMessage;

	}

	/**
	 * Create a new SDPAnnounceParser.
	 * 
	 * @param message
	 *            message containing the sdp announce message.
	 */
	public SDPAnnounceParser(String message) {
		if (message == null)
			return;
		int start = 0;
		sdpMessage = new Vector();
		// Strip off leading and trailing junk.
		String sdpAnnounce = message.trim() + "\r\n";

		// Bug fix by Andreas Bystrom.
		while (start < sdpAnnounce.length()) {
			int add = 0;
			int index = sdpAnnounce.indexOf("\n", start);
			if (index == -1)
				break;
			if (sdpAnnounce.charAt(index - 1) == '\r') {
				index = index - 1;
				add = 1;
			}
			String line = sdpAnnounce.substring(start, index);
			start = index + 1 + add;
			sdpMessage.addElement(line);
		}

		/**
		 * while (start < sdpAnnounce.length() ) { int index =
		 * sdpAnnounce.indexOf("\r\n",start); if (index == -1) break; String
		 * line = sdpAnnounce.substring(start,index); start = index + 2;
		 * sdpMessage.addElement(line); }
		 **/

	}

	public SessionDescription parse() throws ParseException {
		SessionDescription retval = new SessionDescription();
		for (int i = 0; i < sdpMessage.size(); i++) {
			String field = (String) sdpMessage.elementAt(i);
			SDPParser sdpParser = ParserFactory.createParser(field);
			SDPField sdpField = sdpParser.parse();
			retval.addField(sdpField);
		}
		return retval;

	}

}
