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
import java.util.HashMap;
import java.util.Map;
import org.teavm.common.CachedFunction;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.resource.ResourceProvider;
import org.teavm.parsing.substitution.SubstituteClassNameMapping;

public class RenamingClassHolderSource implements ClassHolderSource, ClassDateProvider {
    private static final Date VOID_DATE = new Date(0);
    private ClassHolderSource innerSource;
    private ClassRefsRenamer renamer;
    private ResourceProvider resourceProvider;
    private Map<String, Date> modificationDates = new HashMap<>();
    private SubstituteClassNameMapping classNameMapping;
    private Map<String, String> substituteToOriginalCache = new HashMap<>();
    private Map<String, CacheEntry> cache = new HashMap<>();

    public RenamingClassHolderSource(ResourceProvider resourceProvider, ReferenceCache referenceCache,
            ClassHolderSource innerSource, SubstituteClassNameMapping classNameMapping) {
        this.innerSource = innerSource;
        this.classNameMapping = classNameMapping;
        renamer = new ClassRefsRenamer(referenceCache, new CachedFunction<>(this::substituteToOriginal));
        this.resourceProvider = resourceProvider;
    }

    @Override
    public ClassHolder get(String name) {
        return cache.computeIfAbsent(name, n -> new CacheEntry(acquire(n))).value;
    }

    private ClassHolder acquire(String name) {
        var originalName = name;
        name = classNameMapping.originalToSubstitute(innerSource, name);
        if (name == null) {
            return null;
        }
        var cls = innerSource.get(name);
        if (cls != null && !cls.getName().equals(originalName)) {
            cls = renamer.rename(innerSource.get(name));
        }
        return cls;
    }

    @Override
    public Date getModificationDate(String className) {
        Date mdate = modificationDates.get(className);
        if (mdate == null) {
            mdate = getOriginalModificationDate(substituteToOriginal(className));
            modificationDates.put(className, mdate);
        }
        return mdate == VOID_DATE ? null : mdate;
    }

    private Date getOriginalModificationDate(String className) {
        if (resourceProvider == null) {
            return null;
        }
        var res = resourceProvider.getResource(className.replace('.', '/') + ".class");
        return res == null ? null : res.getModificationDate();
    }

    private String substituteToOriginal(String name) {
        return substituteToOriginalCache.computeIfAbsent(name, classNameMapping::substituteToOriginal);
    }

    private static class CacheEntry {
        ClassHolder value;

        CacheEntry(ClassHolder value) {
            this.value = value;
        }
    }
}