/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.text;

import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.jso.JSBody;

/**
 *
 * @author Bora Ertung
 */
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
