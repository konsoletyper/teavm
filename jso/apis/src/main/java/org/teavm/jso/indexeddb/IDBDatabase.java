/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.jso.indexeddb;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventTarget;

public interface IDBDatabase extends EventTarget {
    String TRANSACTION_READONLY = "readonly";

    String TRANSACTION_READWRITE = "readwrite";

    String TRANSACTION_VERSIONCHANGE = "versionchange";

    @JSProperty
    String getName();

    @JSProperty
    int getVersion();

    @JSProperty
    String[] getObjectStoreNames();

    IDBObjectStore createObjectStore(String name, IDBObjectStoreParameters optionalParameters);

    IDBObjectStore createObjectStore(String name);

    void deleteObjectStore(String name);

    IDBTransaction transaction(String storeName, String transactionMode);

    IDBTransaction transaction(String storeName);

    IDBTransaction transaction(String[] storeNames, String transactionMode);

    IDBTransaction transaction(String[] storeNames);

    void close();

    @JSProperty("onabort")
    void setOnAbort(EventHandler handler);

    @JSProperty("onerror")
    void setOnError(EventHandler handler);

    @JSProperty("onversionchange")
    void setOnVersionChange(EventHandler handler);
}
