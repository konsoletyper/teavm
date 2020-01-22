package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.util.THashMap;
import org.teavm.classlib.java.util.TMap;

public class TConcurrentHashMap<K, V> extends THashMap<K, V> implements TConcurrentMap<K, V> {

  public TConcurrentHashMap() {

    super();
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
