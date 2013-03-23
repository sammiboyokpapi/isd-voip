package sip4me.gov.nist.siplite;
import java.util.Date;
import java.util.Random;

import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.core.Utils;


public class SIPUtils extends  Utils {
    /** Generate the branch identifiers.
     *
     *@return a branch identifier.
     */
     public static String generateBranchId() {
          String b =  new Long(System.currentTimeMillis()).toString() +
			new Random().nextLong();
          byte bid[] = digest(b.getBytes());
          // cryptographically random string.
          // prepend with a magic cookie to indicate we
          // are bis09 compatible.
          return    SIPConstants.BRANCH_MAGIC_COOKIE +
                        Utils.toHexString(bid);
     }


	/** Generate a call identifier for the stack.
	*
	*@return a call id.
	*/
    public static String  generateCallIdentifier(String stackAddr) {
        String date = (new Date()).toString() + new Random().nextLong() ;
	byte[] cidbytes =  Utils.digest(date.getBytes());
	String cidString = Utils.toHexString(cidbytes);
        return cidString + Separators.AT + stackAddr;
    }

}
