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
package org.teavm.platform.metadata;

/**
 * <p>Marks a valid <b>resource interface</b>. Resource interface is an interface, that has get* and set* methods,
 * according the default convention for JavaBeans. Each property must have both getter and setter.
 * Also each property's must be either primitive value (except for <code>long</code>) or a valid resource.</p>
 *
 * @see MetadataGenerator
 * @see ResourceArray
 * @see ResourceMap
 *
 * @author Alexey Andreev
 */
public interface Resource {
}
