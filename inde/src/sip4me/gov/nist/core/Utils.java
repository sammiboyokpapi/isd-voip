package sip4me.gov.nist.core;
import java.util.Random;

import org.crypto.digests.MD5Digest;

/**
 * A set of utilities that compensate for things that are missing in CLDC 1.0
 * @author  M. Ranganathan and Olivier Deruelle
 * @version 1.0
 */
public class Utils  {
    private   static MD5Digest  messageDigest;
    static {
        messageDigest = new MD5Digest();
    }
    /** Do an MD5 Digest.
    */
    public static byte[] digest(byte[] digestBytes) {
	messageDigest.update(digestBytes,0,digestBytes.length);
	byte retval[] = new byte[messageDigest.getDigestSize()];
        messageDigest.doFinal(retval,0);
        
	return retval;
    }

    /** Generate a random alphanumeric tag for a FROM header or TO header.
     * @return a string that can be used as a tag parameter.
     */
    public static String generateTag() {
    	return randomAlphanumericString(9);
    }

    /**
     * to hex converter
     */
    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6',
    '7', '8', '9', 'a', 'b', 'c', 'd',
    'e', 'f' };
    
    /** Compares two strings lexicographically, ignoring case considerations.
     * @param s1 string to compare
     * @param s2 string to compare.
     * @return 1,-1,0 as in compare To
     */
    public static int compareToIgnoreCase(String s1, String s2) {
        // System.out.println(s1+" "+s2);
        String su1=s1.toUpperCase();
        String su2=s2.toUpperCase();
        return su1.compareTo(su2);
    }
    public static  boolean equalsIgnoreCase(String s1, String s2) {
		return s1.toLowerCase().equals(s2.toLowerCase());
    }
    
    /**
     * convert an array of bytes to an hexadecimal string
     * @return a string
     * @param b bytes array to convert to a hexadecimal
     * string
     */
    public static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length*2];
        for (int i=0; i< b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }
  
    
    /**
     * Turns a long into a byte array, then applies
     * toHexString(byte[]).
     * @param aLong
     * @return
     */
    public static String toHexString(long aLong) {	
    	
		byte[] bytes = new byte[8];
    	bytes[7] = (byte)( (aLong >>> 0) & 0xFF);
    	bytes[6] = (byte)( (aLong >>> 8) & 0xFF);
		bytes[5] = (byte)( (aLong >>> 16) & 0xFF);
		bytes[4] = (byte)( (aLong >>> 24) & 0xFF);
		bytes[3] = (byte)( (aLong >>> 32) & 0xFF);
		bytes[2] = (byte)( (aLong >>> 40) & 0xFF);
		bytes[1] = (byte)( (aLong >>> 48) & 0xFF);
		bytes[0] = (byte)( (aLong >>> 56) & 0xFF );

    	return toHexString(bytes);
    		
    }
    
    /**
     * Turns a double into a byte array, then applies
     * toHexString(byte[]).
     * @param aLong
     * @return
     */
    public static String toHexString(double aDouble) {	
    	
    	return toHexString(Double.doubleToLongBits(aDouble));
  	
    }
    
    /**
     * Turns an int into a byte array, then applies
     * toHexString(byte[]).
     * @param anInt
     * @return
     */
    public static String toHexString(int anInt) {	
		byte[] b = new byte[4];
		b[3] = (byte)(anInt & 0xFF);
		b[2] = (byte)((anInt & 0xFF00) >> 8);
		b[1] = (byte)((anInt & 0xFF0000) >> 16);
		b[0] = (byte)((anInt & 0xFF000000) >> 24);
		return toHexString(b);
    }
    
    /**
     * Put quotes around a string and return it.
     * @return a quoted string
     * @param str string to be quoted
     */
    public static String getQuotedString( String str) {
        return  '"' + str + '"';
    }
    
    
    /**
     * Squeeze out white space from a string and return the reduced string.
     * @param input input string to sqeeze.
     * @return String a reduced string.
     */
    public static String reduceString( String input) {
        String newString = input.toLowerCase();
        int len = newString.length();
        String retval = "";
        for (int i = 0; i < len ; i++ ) {
            if ( newString.charAt(i) == ' '
            || newString.charAt(i) == '\t' )
                continue;
            else retval += newString.charAt(i);
        }
        return retval;
    }
    
    
    // Random strings
    //-----------------------------------------------------------------------
    private static final Random rnd = new Random(System.currentTimeMillis());
    private static final byte[] allowed_chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".getBytes(); 
    public static String randomAlphanumericString(int length) {
    	byte[] array = new byte[length];
    	
    	for (int i = 0; i < length; i++) {
    		array[i] = allowed_chars[rnd.nextInt(allowed_chars.length)];
    	}
    	
    	return new String(array);
    }

    
}
