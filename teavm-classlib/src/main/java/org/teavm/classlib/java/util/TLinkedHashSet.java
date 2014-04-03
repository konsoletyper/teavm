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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;

public class TLinkedHashSet<E> extends THashSet<E> implements TSet<E>, TCloneable, TSerializable {
    public TLinkedHashSet() {
        super(new TLinkedHashMap<E, THashSet<E>>());
    }

    public TLinkedHashSet(int capacity) {
        super(new TLinkedHashMap<E, THashSet<E>>(capacity));
    }

    public TLinkedHashSet(int capacity, float loadFactor) {
        super(new TLinkedHashMap<E, THashSet<E>>(capacity, loadFactor));
    }

    public TLinkedHashSet(TCollection<? extends E> collection) {
        super(new TLinkedHashMap<E, THashSet<E>>(collection.size() < 6 ? 11 : collection.size() * 2));
        for (TIterator<? extends E> iter = collection.iterator(); iter.hasNext();) {
            add(iter.next());
        }
    }

    /* overrides method in HashMap */
    @Override
    THashMap<E, THashSet<E>> createBackingMap(int capacity, float loadFactor) {
        return new TLinkedHashMap<>(capacity, loadFactor);
    }
}
