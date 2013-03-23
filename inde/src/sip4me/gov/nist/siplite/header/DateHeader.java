
package sip4me.gov.nist.siplite.header;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import sip4me.gov.nist.core.NameValueList;



/** Date sip header.
*@author M. Ranganathan.
*/

public class DateHeader extends Header {


	private Calendar date;
	public static final String NAME = Header.DATE;
	protected static Class clazz;

	static {
		clazz = new DateHeader().getClass();
	}


	public DateHeader() {
		super (Header.DATE);
		date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	}

	/** Set the expiry date.
	*@param date is the date to set.
	*/
	public void setDate(Date date) {
		this.date.setTime(date);
	}

	/** Set the date.
	*/
	public void setDate(Calendar date) {
		this.date = date;

	}


	/** Get the expiry date.
	*@return get the expiry date.
	*/
	public Date getDate() {
		return this.date.getTime();
	}

	/** Get the calendar date.
	*/
	public Object getValue() { 
		return this.date; 
	}


	/** Encode into canonical form.
	*/
	public String encodeBody() {
	    StringBuffer sbuf  = new StringBuffer();
	    sbuf.append(encodeCalendar(date));
            return sbuf.toString();
	}


	/** Get the parameters for the header.
	*/
	public NameValueList getParameters() {
		return null;
	}

	public Object clone() {
		DateHeader retval = new DateHeader();
		retval.setDate(this.getDate());
		return retval;
	}


}
