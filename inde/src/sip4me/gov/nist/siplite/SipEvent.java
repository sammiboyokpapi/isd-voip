
package sip4me.gov.nist.siplite;

public class SipEvent {
    public SipEvent (Object source) {
		this.source = source;
    }
    public Object getSource() { 
	return this.source; 
    }
    private Object  source;

}
