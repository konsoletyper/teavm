/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.teavm.classlib.java.io;

/**
 *
 * @author bora
 */
public interface TFilenameFilter {
    boolean accept(Object dir, String name);
}
 