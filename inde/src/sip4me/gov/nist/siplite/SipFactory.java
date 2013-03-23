package sip4me.gov.nist.siplite;

import sip4me.gov.nist.siplite.address.AddressFactory;
import sip4me.gov.nist.siplite.header.HeaderFactory;
import sip4me.gov.nist.siplite.message.MessageFactory;

public class SipFactory {
	private static SipFactory myFactory;

	private static AddressFactory addressFactory;

	private static MessageFactory msgFactory;

	private static HeaderFactory headerFactory;

	private SipFactory() {
		// Dont let outsiders call me!
	}

	public static SipFactory getInstance() {
		if (myFactory == null)
			myFactory = new SipFactory();
		return myFactory;
	}

	public SipStack createSipStack(ConfigurationProperties properties)
			throws IllegalArgumentException {
		return new SipStack(properties);
	}

	public MessageFactory createMessageFactory() {
		if (msgFactory != null)
			return msgFactory;
		msgFactory = new MessageFactory();
		return msgFactory;
	}

	public HeaderFactory createHeaderFactory() {
		if (headerFactory != null)
			return headerFactory;
		headerFactory = new HeaderFactory();
		return headerFactory;
	}

	public AddressFactory createAddressFactory() {
		if (addressFactory != null)
			return addressFactory;
		addressFactory = new AddressFactory();
		return addressFactory;
	}

}
