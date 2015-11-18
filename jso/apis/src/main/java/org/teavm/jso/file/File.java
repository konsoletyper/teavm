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
package org.teavm.jso.file;

import org.teavm.jso.JSProperty;
import org.teavm.jso.blob.Blob;
import org.teavm.jso.core.JSDate;

/**
*
* @author Jan-Felix Wittmann
*/
public abstract class File extends Blob {

    @JSProperty
    public abstract JSDate getLastModifiedDate();

    @JSProperty
    public abstract String getName();

}
