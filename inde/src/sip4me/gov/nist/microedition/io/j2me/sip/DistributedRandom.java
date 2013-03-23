/*
 * DistributedRandom.java
 * 
 * Created on Feb 9, 2004
 *
 */
package sip4me.gov.nist.microedition.io.j2me.sip;

import java.util.Random;

/**
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class DistributedRandom extends Random {
	
	/**
	 * Method copied from the jdk src 1.4.2_03
	 */
	public int nextInt(int n) throws IllegalArgumentException{
		if(n<=0)
			throw new IllegalArgumentException("n must be positive");
		if((n & -n) ==n)//i.e. n is a power of 2
			return (int)((n * (long)next(31)) >> 31);
		
		int bits,val;
		do{
			bits = next(31);
			val= bits%n;			
		}while(bits - val +(n-1)<0);
		return val;	
	}
}
