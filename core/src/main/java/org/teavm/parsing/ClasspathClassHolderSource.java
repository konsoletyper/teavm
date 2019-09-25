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
package org.teavm.parsing;

import java.util.Date;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.resource.ClasspathResourceReader;
import org.teavm.parsing.resource.MapperClassHolderSource;
import org.teavm.parsing.resource.ResourceClassHolderMapper;

public class ClasspathClassHolderSource implements ClassHolderSource, ClassDateProvider {
    private MapperClassHolderSource innerClassSource;
    private ClasspathResourceMapper classPathMapper;

    public ClasspathClassHolderSource(ClassLoader classLoader, ReferenceCache referenceCache) {
        ClasspathResourceReader reader = new ClasspathResourceReader(classLoader);
        ResourceClassHolderMapper rawMapper = new ResourceClassHolderMapper(reader, referenceCache);
        classPathMapper = new ClasspathResourceMapper(classLoader, referenceCache, rawMapper);
        innerClassSource = new MapperClassHolderSource(classPathMapper);
    }

    public ClasspathClassHolderSource(ReferenceCache referenceCache) {
        this(ClasspathClassHolderSource.class.getClassLoader(), referenceCache);
    }

    @Override
    public ClassHolder get(String name) {
        return innerClassSource.get(name);
    }

    @Override
    public Date getModificationDate(String className) {
        return classPathMapper.getModificationDate(className);
    }
}
