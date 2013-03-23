
package sip4me.gov.nist.core;

public abstract class GenericObject {


	public abstract Object clone();
        public abstract String encode();
	public static  boolean equalsIgnoreCase(String s1, String s2) {
		return s1.toLowerCase().equals(s2.toLowerCase());
	}

	public static int compareToIgnoreCase(String s1, String s2) {
		return s1.toLowerCase().compareTo(s2.toLowerCase());
	}
}
