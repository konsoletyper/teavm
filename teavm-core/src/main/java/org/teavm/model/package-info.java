/*
 *  Copyright 2013 Alexey Andreev.
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
/**
 * Represents a class model that is alternative to {@code java.lang.reflection} package.
 * Model is suitable for representing classes that are not in class path. Also
 * it allows to disassemble method bodies into three-address code that is very
 * close to JVM bytecode (see {@code org.teavm.model.instructions}.
 *
 * <p>The entry point is some implementation of {@link org.teavm.model.ClassHolderSource} interface.
 *
 */
package org.teavm.model;