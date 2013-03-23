/***************************************************
 *                                                 *
 *  Mobicents: The Open Source JSLEE Platform      *
 *                                                 *
 *  Distributable under LGPL license.              *
 *  See terms of license at gnu.org.               *
 *                                                 *
 ***************************************************/
/*
 * ConnectionBaseInterface.java
 * 
 * Created on Jul 3, 2005
 *
 */
package sip4me.gov.nist.microedition.sip;

/**
 * Mimick the ConnectionBaseInterface coming with CLDC RI.
 * (it seems that this interface is not implemented by nokia phones)
 * 
 * Fix brought to us by Bill Bramwell <a href="mailto:bbramwell@ubiquitysoftware.com">bbramwell@ubiquitysoftware.com</a>
 * 
 * @author DERUELLE Jean <a href="mailto:jean.deruelle@gmail.com">jean.deruelle@gmail.com</a>
 *
 */
import java.io.IOException;

import javax.microedition.io.Connection;

public interface ConnectionBaseInterface
{

    public abstract Connection openPrim(String s, int i, boolean flag)
        throws IOException;
}