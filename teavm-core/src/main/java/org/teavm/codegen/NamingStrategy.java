/*
 *  Copyright 2011 Alexey Andreev.
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
package org.teavm.codegen;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public interface NamingStrategy {
    String getNameFor(String cls) throws NamingException;

    String getNameFor(MethodReference method) throws NamingException;

    String getNameForAsync(MethodReference method) throws NamingException;

    String getNameForInit(MethodReference method) throws NamingException;

    String getFullNameFor(MethodReference method) throws NamingException;

    String getFullNameForAsync(MethodReference method) throws NamingException;

    String getNameFor(FieldReference field) throws NamingException;
}
