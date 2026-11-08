/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.extension.bridge.guest;

import java.util.function.Predicate;
import org.teavm.extension.bridge.BridgeSubstitutionRule;
import org.teavm.extension.spi.substitution.ClassSubstitutionPolicy;

/**
 * Records calls to {@link ClassSubstitutionPolicy}, so that host can replay them later.
 */
class GuestSubstitutionRule implements BridgeSubstitutionRule, ClassSubstitutionPolicy {
    private final Predicate<String> predicate;
    private String simpleNamePrefix;
    private String simpleNameSuffix;
    private String packagePrefix;
    private String packageSuffix;
    private String replacedPackage;
    private boolean dontFallback;

    GuestSubstitutionRule(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean matches(String className) {
        return predicate.test(className);
    }

    @Override
    public String simpleNamePrefix() {
        return simpleNamePrefix;
    }

    @Override
    public String simpleNameSuffix() {
        return simpleNameSuffix;
    }

    @Override
    public String packagePrefix() {
        return packagePrefix;
    }

    @Override
    public String packageSuffix() {
        return packageSuffix;
    }

    @Override
    public String replacedPackage() {
        return replacedPackage;
    }

    @Override
    public boolean dontFallback() {
        return dontFallback;
    }

    @Override
    public ClassSubstitutionPolicy simpleNamePrefix(String name) {
        simpleNamePrefix = name;
        return this;
    }

    @Override
    public ClassSubstitutionPolicy simpleNameSuffix(String name) {
        simpleNameSuffix = name;
        return this;
    }

    @Override
    public ClassSubstitutionPolicy packagePrefix(String name) {
        packagePrefix = name;
        return this;
    }

    @Override
    public ClassSubstitutionPolicy packageSuffix(String name) {
        packageSuffix = name;
        return this;
    }

    @Override
    public ClassSubstitutionPolicy replacePackage(String from, String to) {
        replacedPackage = from;
        packagePrefix = to;
        return this;
    }

    @Override
    public ClassSubstitutionPolicy dontFallbackWhenNoSubstitution() {
        dontFallback = true;
        return this;
    }
}
