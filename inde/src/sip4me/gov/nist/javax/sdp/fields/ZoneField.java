/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package sip4me.gov.nist.javax.sdp.fields;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import sip4me.gov.nist.core.Separators;
import sip4me.gov.nist.javax.sdp.SdpException;
import sip4me.gov.nist.javax.sdp.SdpParseException;


/**
* Z= SDP field.
*
*@version  JSR141-PUBLIC-REVIEW (subject to change).
*
*@author Olivier Deruelle <deruelle@antd.nist.gov>
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*
*<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
*
*/

public  class ZoneField  extends SDPField  {

	protected SDPObjectList zoneAdjustments;


	public Object clone() {
		ZoneField retval = new ZoneField();
		retval.zoneAdjustments = 
		   (SDPObjectList) this.zoneAdjustments.clone();
		return retval;
	}

	/**
	* Constructor.
	*/
	public ZoneField() {
		super(ZONE_FIELD);
		zoneAdjustments = new SDPObjectList();
	}

	/**
	* Add an element to the zone adjustment list.
	*@param za zone adjustment to add.
	*/
	public void addZoneAdjustment( ZoneAdjustment za ) {
		zoneAdjustments.addElement(za);
	}


	/**
	* Get the zone adjustment list.
	*@return the list of zone adjustments.
	*/

	public SDPObjectList getZoneAdjustments() {
		return zoneAdjustments;
	}


	/**
	* Encode this structure into a canonical form.
	*/
	public String encode() {
		StringBuffer retval = new StringBuffer(ZONE_FIELD);
		for(int i = 0; i < zoneAdjustments.size(); i++) {
		   ZoneAdjustment za = (ZoneAdjustment)
				zoneAdjustments.elementAt(i);
		   if (i > 0) retval.append(Separators.SP);
		   retval.append( za.encode() );
		}
		retval.append( Separators.NEWLINE );
		return retval.toString();
	}

    /** Returns a Hashtable of adjustment times, where:
     *        key = Date. This is the equivalent of the decimal NTP time value.
     *        value = Int Adjustment. This is a relative time value in seconds.
     * @param create to set
     * @throws SdpParseException
     * @return create - when true, an empty Hashtable is created, if it is null.
     */    
    public Hashtable getZoneAdjustments(boolean create)
    throws SdpParseException {
        Hashtable result=new Hashtable();
        SDPObjectList zoneAdjustments=getZoneAdjustments();
        ZoneAdjustment zone;
        if (zoneAdjustments==null )
            if (create) return new Hashtable();
            else return null;
        else {
            for ( int i = 0 ; i < zoneAdjustments.size(); i++ ) {
		zone = (ZoneAdjustment) zoneAdjustments.elementAt(i);
                Long l=new Long( zone.getTime() );
                Integer time= new Integer( (int) l.longValue() );
                Date date=new Date(zone.getTime());
                result.put(date,time);
            }
            return result;
        }
    }
    
    /** Sets the Hashtable of adjustment times, where:
     *          key = Date. This is the equivalent of the decimal NTP time value.
     *          value = Int Adjustment. This is a relative time value in seconds.
     * @param map Hashtable to set
     * @throws SdpException if the parameter is null
     */    
    public void setZoneAdjustments(Hashtable map)
    throws SdpException {
        if (map ==null ) throw new SdpException("The map is null");
        else {
            SDPObjectList zoneAdjustments=getZoneAdjustments();
            for ( Enumeration e=map.keys() ; e.hasMoreElements() ;) {
               Object o=e.nextElement();
               if (o instanceof Date) {
                   Date date=(Date)o;
                   ZoneAdjustment zone=new ZoneAdjustment();
                   zone.setTime(date.getTime());
                   addZoneAdjustment(zone);
               }
               else throw new SdpException("The map is not well-formated ");
            }
        }
    }
    
    /** Sets whether the field will be output as a typed time or a integer value.
     *
     *     Typed time is formatted as an integer followed by a unit character. 
     * The unit indicates an appropriate multiplier for
     *     the integer.
     *
     *     The following unit types are allowed.
     *          d - days (86400 seconds)
     *          h - hours (3600 seconds)
     *          m - minutes (60 seconds)
     *          s - seconds ( 1 seconds)
     * @param typedTime typedTime - if set true, the start and stop times will be
     * output in an optimal typed time format; if false, the
     *          times will be output as integers.
     */    
    public void setTypedTime(boolean typedTime) {
		// Dummy -- feature not implemented.
    }
    
    /** Returns whether the field will be output as a typed time or a integer value.
     *
     *     Typed time is formatted as an integer followed by a unit character.
     * The unit indicates an appropriate multiplier for
     *     the integer.
     *
     *     The following unit types are allowed.
     *          d - days (86400 seconds)
     *          h - hours (3600 seconds)
     *          m - minutes (60 seconds)
     *          s - seconds ( 1 seconds)
     * @return true, if the field will be output as a typed time; false, if as an integer value.
     */    
    public boolean getTypedTime() {
        return false;
    }
	


}

