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
package org.teavm.parsing.substitution;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import org.teavm.extension.spi.substitution.ClassSubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;
import org.teavm.model.ClassReaderSource;

public class DefaultSubstituteClassNameMapping implements SubstituteClassNameMapping {
    private List<? extends ClassNameMappingRule> rules;

    private DefaultSubstituteClassNameMapping(List<? extends ClassNameMappingRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public String originalToSubstitute(ClassReaderSource classes, String original) {
        var dontFallback = false;
        for (var rule : rules) {
            if (rule.predicate.test(original)) {
                dontFallback |= rule.dontFallback;
            }
        }
        for (var rule : rules) {
            if (rule.predicate.test(original)) {
                var lastDot = original.lastIndexOf('.');
                var packageName = lastDot >= 0 ? original.substring(0, lastDot) : "";
                var simpleName = original.substring(lastDot + 1);
                var newName = constructName(packageName, simpleName, rule);
                if (!newName.equals(original)) {
                    var newCls = classes.get(newName);
                    if (newCls != null) {
                        return newName;
                    }
                }
            }
        }
        if (dontFallback && classes.get(original) == null) {
            return null;
        }
        return original;
    }

    private String constructName(String packageName, String simpleName, ClassNameMappingRule rule) {
        var sb = new StringBuilder();
        if (rule.packagePrefix != null) {
            sb.append(rule.packagePrefix);
        }
        var newPackageName = packageName;
        if (rule.packageRemove != null) {
            if (newPackageName.equals(rule.packageRemove)) {
                newPackageName = "";
            } else if (newPackageName.startsWith(rule.packageRemove)
                    && newPackageName.charAt(rule.packageRemove.length()) == '.') {
                newPackageName = newPackageName.substring(rule.packageRemove.length() + 1);
            }
        }
        if (!newPackageName.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append('.');
            }
            sb.append(newPackageName);
        }
        if (rule.packageSuffix != null) {
            if (!sb.isEmpty()) {
                sb.append('.');
            }
            sb.append(rule.packageSuffix);
        }
        if (!sb.isEmpty()) {
            sb.append('.');
        }
        if (rule.simpleNamePrefix != null) {
            sb.append(rule.simpleNamePrefix);
        }
        sb.append(simpleName);
        if (rule.simpleNameSuffix != null) {
            sb.append(rule.simpleNameSuffix);
        }

        return sb.toString();
    }

    @Override
    public String substituteToOriginal(String substitute) {
        var lastDot = substitute.lastIndexOf('.');
        var packageName = lastDot >= 0 ? substitute.substring(0, lastDot) : "";
        var simpleName = substitute.substring(lastDot + 1);
        for (var rule : rules) {
            var newSimpleName = simpleName;
            var newPackageName = packageName;

            if (rule.simpleNamePrefix != null) {
                if (newSimpleName.startsWith(rule.simpleNamePrefix)) {
                    newSimpleName = newSimpleName.substring(rule.simpleNamePrefix.length());
                } else {
                    continue;
                }
            }

            if (rule.simpleNameSuffix != null) {
                if (newSimpleName.endsWith(rule.simpleNameSuffix)) {
                    newSimpleName = newSimpleName.substring(0, newSimpleName.length()
                            - rule.simpleNameSuffix.length());
                } else {
                    continue;
                }
            }

            if (rule.packagePrefix != null) {
                newPackageName = removePackagePrefix(newPackageName, rule.packagePrefix);
                if (newPackageName == null) {
                    continue;
                }
            }

            if (rule.packageSuffix != null) {
                newPackageName = removePackageSuffix(newPackageName, rule.packageSuffix);
                if (newPackageName == null) {
                    continue;
                }
            }

            if (rule.packageRemove != null) {
                if (newPackageName.isEmpty()) {
                    newPackageName = rule.packageRemove;
                } else {
                    newPackageName = rule.packageRemove + "." + newPackageName;
                }
            }

            var newName = newPackageName.isEmpty() ? newSimpleName : newPackageName + '.' + newSimpleName;
            if (rule.predicate.test(newName)) {
                return newName;
            }
        }
        return substitute;
    }

    private String removePackagePrefix(String packageName, String packagePrefix) {
        if (packageName.equals(packagePrefix)) {
            return "";
        } else if (packageName.startsWith(packagePrefix) && packageName.charAt(packagePrefix.length()) == '.') {
            return packageName.substring(packagePrefix.length() + 1);
        } else {
            return null;
        }
    }

    private String removePackageSuffix(String packageName, String packageSuffix) {
        if (packageName.equals(packageSuffix)) {
            return "";
        } else if (packageName.endsWith(packageSuffix)
                && packageName.charAt(packageName.length() - packageSuffix.length() - 1) == '.') {
            return packageName.substring(0, packageName.length() - packageSuffix.length() - 1);
        } else {
            return null;
        }
    }

    public static DefaultSubstituteClassNameMapping createWithPolicies(
            Iterable<? extends SubstitutionPolicy> policies) {
        var rules = new ArrayList<ClassNameMappingRule>();
        var sink = new SubstitutionSink() {
            @Override
            public ClassSubstitutionPolicy selectClasses(Predicate<String> predicate) {
                var rule = new ClassNameMappingRule(predicate);
                rules.add(rule);
                return new ClassSubstitutionPolicy() {
                    @Override
                    public ClassSubstitutionPolicy simpleNamePrefix(String name) {
                        if (name == null || name.isEmpty()) {
                            rule.simpleNamePrefix = null;
                        } else {
                            rule.simpleNamePrefix = name;
                        }
                        return this;
                    }

                    @Override
                    public ClassSubstitutionPolicy simpleNameSuffix(String name) {
                        if (name == null || name.isEmpty()) {
                            rule.simpleNameSuffix = null;
                        } else {
                            rule.simpleNameSuffix = name;
                        }
                        return this;
                    }

                    @Override
                    public ClassSubstitutionPolicy packagePrefix(String name) {
                        if (name == null || name.isEmpty()) {
                            rule.packagePrefix = null;
                        } else {
                            if (name.endsWith(".")) {
                                name = name.substring(0, name.length() - 1);
                            }
                            rule.packagePrefix = name;
                        }
                        return this;
                    }

                    @Override
                    public ClassSubstitutionPolicy packageSuffix(String name) {
                        if (name == null || name.isEmpty()) {
                            rule.packageSuffix = null;
                        } else {
                            if (name.startsWith(".")) {
                                name = name.substring(1);
                            }
                            rule.packageSuffix = name;
                        }
                        return this;
                    }

                    @Override
                    public ClassSubstitutionPolicy replacePackage(String from, String to) {
                        if (from.endsWith(".")) {
                            from = from.substring(0, from.length() - 1);
                        }
                        rule.packageRemove = from;
                        return packagePrefix(to);
                    }

                    @Override
                    public ClassSubstitutionPolicy dontFallbackWhenNoSubstitution() {
                        rule.dontFallback = true;
                        return null;
                    }
                };
            }
        };
        for (var policy : policies) {
            policy.contribute(sink);
        }

        return new DefaultSubstituteClassNameMapping(rules);
    }

    public static DefaultSubstituteClassNameMapping createWithSPI(ClassLoader classLoader) {
        var policies = ServiceLoader.load(SubstitutionPolicy.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        return createWithPolicies(policies);
    }
}
