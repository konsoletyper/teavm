package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TLinkedList;

public class TLinkedBlockingDeque<T> extends TLinkedList<T> {

	public TLinkedBlockingDeque() {
		super();
	}

	public TLinkedBlockingDeque(TCollection<T> coll) {
		super(coll);
	}
	
}
