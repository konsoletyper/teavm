/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.teavm.classlib.java.io;

import org.teavm.classlib.java.lang.TString;

/**
 *
 * @author bora
 */
public class TFileNotFoundException extends TIOException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TFileNotFoundException() {
        super();
    }

    public TFileNotFoundException(TString s) {
        super(s);
    }
}