/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.cache;

import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectByteMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;

public final class AnnotationAwareCacheStatus implements CacheStatus {
    private static final byte UNKNOWN = 0;
    private static final byte STALE = 1;
    private static final byte UNDECIDED = 2;
    private static final byte FRESH = 3;

    private CacheStatus underlyingStatus;
    private IncrementalDependencyProvider dependencyProvider;
    private List<Predicate<String>> synthesizedClasses = new ArrayList<>();
    private ObjectByteMap<String> classStatusCache = new ObjectByteHashMap<>();
    private ClassReaderSource classSource;

    public AnnotationAwareCacheStatus(CacheStatus underlyingStatus, IncrementalDependencyProvider dependencyProvider,
            ClassReaderSource classSource) {
        this.underlyingStatus = underlyingStatus;
        this.dependencyProvider = dependencyProvider;
        this.classSource = classSource;
    }

    public void addSynthesizedClasses(Predicate<String> synthesizedClasses) {
        this.synthesizedClasses.add(synthesizedClasses);
    }

    @Override
    public boolean isStaleClass(String className) {
        return getClassStatus(className) == STALE;
    }

    private byte getClassStatus(String className) {
        byte status = classStatusCache.getOrDefault(className, UNKNOWN);
        if (status == UNKNOWN) {
            classStatusCache.put(className, UNDECIDED);
            status = computeClassStatus(className);
            classStatusCache.put(className, status);
        }
        return status;
    }

    private byte computeClassStatus(String className) {
        if (!isSynthesizedClass(className)) {
            if (underlyingStatus.isStaleClass(className)) {
                return STALE;
            }
        }

        if (dependencyProvider.isNoCache(className)) {
            return STALE;
        }

        if (hasStaleDependencies(dependencyProvider.getDependencies(className))) {
            return STALE;
        }

        ClassReader cls = classSource.get(className);
        if (cls != null) {
            if (cls.getParent() != null && getClassStatus(cls.getParent()) == STALE) {
                return STALE;
            }
            for (String itf : cls.getInterfaces()) {
                if (getClassStatus(cls.getParent()) == STALE) {
                    return STALE;
                }
            }
        }

        return FRESH;
    }

    @Override
    public boolean isStaleMethod(MethodReference method) {
        return isStaleClass(method.getClassName()) || dependencyProvider.isNoCache(method)
                || hasStaleDependencies(dependencyProvider.getDependencies(method));
    }

    private boolean hasStaleDependencies(String[] dependencies) {
        for (String dependency : dependencies) {
            byte dependencyStatus = getClassStatus(dependency);
            if (dependencyStatus == STALE) {
                return true;
            }
        }

        return false;
    }

    private boolean isSynthesizedClass(String className) {
        return synthesizedClasses.stream().anyMatch(p -> p.test(className));
    }
}
