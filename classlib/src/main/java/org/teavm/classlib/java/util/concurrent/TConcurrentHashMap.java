/*
 *  Copyright 2020 Joerg Hohwiller.
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
package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.util.THashMap;
import org.teavm.classlib.java.util.TMap;

// currently behaves like regular map, concurrency has to be added
// https://github.com/konsoletyper/teavm/issues/445
public class TConcurrentHashMap<K, V> extends THashMap<K, V> implements TConcurrentMap<K, V> {

  public TConcurrentHashMap() {

    super();
  }

  public TConcurrentHashMap(int capacity, float loadFactor, int concurrencyLevel) {

    super(capacity, loadFactor);
  }

  public TConcurrentHashMap(int capacity, float loadFactor) {

    super(capacity, loadFactor);
  }

  public TConcurrentHashMap(int capacity) {

    super(capacity);
  }

  public TConcurrentHashMap(TMap<? extends K, ? extends V> map) {

    super(map);
  }

}
