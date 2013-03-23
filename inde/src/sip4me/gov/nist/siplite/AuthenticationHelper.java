package sip4me.gov.nist.siplite;
import sip4me.gov.nist.siplite.message.Request;
import sip4me.gov.nist.siplite.message.Response;

public interface AuthenticationHelper {
	
	Request createNewRequest(SipStack sipStack, Request orginalRequest,
			Response response);
	
}
