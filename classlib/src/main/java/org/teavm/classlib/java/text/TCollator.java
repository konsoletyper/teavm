/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.teavm.classlib.java.text;

import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.jso.JSBody;

public class TCollator implements TComparator {
   private static final TCollator instance = new TCollator(); 
    
   public static TCollator getInstance() {
      return instance;
   }
   
   public static TCollator getInstance(TLocale locale) {
      return instance;
   }

   @JSBody(params = { "source", "target" }, script = "return source.localeCompare(target);")
   public native int compare( String source, String target );
   
   @Override
   public int compare(Object o1, Object o2) {
     return compare((String)o1, (String)o2);
   }
}