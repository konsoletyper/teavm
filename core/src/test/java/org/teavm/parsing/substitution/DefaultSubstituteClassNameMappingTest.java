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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.List;
import org.junit.Test;
import org.teavm.extension.spi.substitution.SimpleSubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;

public class DefaultSubstituteClassNameMappingTest {
    private ClassHolderSource classes;

    public DefaultSubstituteClassNameMappingTest() {
        classes = new ClassHolderSource() {
            @Override
            public ClassHolder get(String name) {
                return new ClassHolder(name);
            }
        };
    }

    @Test
    public void simpleNamePrefix() {
        var mapping = DefaultSubstituteClassNameMapping.createWithPolicies(List.of(
                new SimpleSubstitutionPolicy() {
                    @Override
                    public void contribute(SubstitutionSink sink) {
                        sink.selectClasses(inPackage("org.teavm"))
                                .simpleNamePrefix("Pre_");
                    }
                }
        ));
        assertEquals("org.teavm.Pre_Simple", mapping.originalToSubstitute(classes, "org.teavm.Simple"));
        assertEquals("org.teavm.parsing.Simple", mapping.originalToSubstitute(classes, "org.teavm.parsing.Simple"));

        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("org.teavm.Pre_Simple"));
        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("org.teavm.Simple"));
        assertEquals("org.teavm.parsing.Pre_Simple", mapping.substituteToOriginal("org.teavm.parsing.Pre_Simple"));
    }

    @Test
    public void simpleNameSuffix() {
        var mapping = DefaultSubstituteClassNameMapping.createWithPolicies(List.of(
                new SimpleSubstitutionPolicy() {
                    @Override
                    public void contribute(SubstitutionSink sink) {
                        sink.selectClasses(inPackage("org.teavm"))
                                .simpleNameSuffix("_Impl");
                    }
                }
        ));
        assertEquals("org.teavm.Simple_Impl", mapping.originalToSubstitute(classes, "org.teavm.Simple"));
        assertEquals("org.teavm.parsing.Simple", mapping.originalToSubstitute(classes, "org.teavm.parsing.Simple"));

        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("org.teavm.Simple_Impl"));
        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("org.teavm.Simple"));
        assertEquals("org.teavm.parsing.Simple_Impl", mapping.substituteToOriginal("org.teavm.parsing.Simple_Impl"));
    }

    @Test
    public void packagePrefix() {
        var mapping = DefaultSubstituteClassNameMapping.createWithPolicies(List.of(
                new SimpleSubstitutionPolicy() {
                    @Override
                    public void contribute(SubstitutionSink sink) {
                        sink.selectClasses(inPackage("org.teavm"))
                                .packagePrefix("impl");
                    }
                }
        ));
        assertEquals("impl.org.teavm.Simple", mapping.originalToSubstitute(classes, "org.teavm.Simple"));
        assertEquals("org.teavm.parsing.Simple", mapping.originalToSubstitute(classes, "org.teavm.parsing.Simple"));

        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("impl.org.teavm.Simple"));
        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("org.teavm.Simple"));
    }

    @Test
    public void packageSuffix() {
        var mapping = DefaultSubstituteClassNameMapping.createWithPolicies(List.of(
                new SimpleSubstitutionPolicy() {
                    @Override
                    public void contribute(SubstitutionSink sink) {
                        sink.selectClasses(inPackage("org.teavm"))
                                .packageSuffix("impl");
                    }
                }
        ));
        assertEquals("org.teavm.impl.Simple", mapping.originalToSubstitute(classes, "org.teavm.Simple"));
        assertEquals("org.teavm.parsing.Simple", mapping.originalToSubstitute(classes, "org.teavm.parsing.Simple"));

        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("org.teavm.impl.Simple"));
        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("org.teavm.Simple"));
    }

    @Test
    public void replacePackage() {
        var mapping = DefaultSubstituteClassNameMapping.createWithPolicies(List.of(
                new SimpleSubstitutionPolicy() {
                    @Override
                    public void contribute(SubstitutionSink sink) {
                        sink.selectClasses(inPackage("org.teavm", true))
                                .replacePackage("org.teavm", "org.replaced");
                    }
                }
        ));
        assertEquals("org.replaced.Simple", mapping.originalToSubstitute(classes, "org.teavm.Simple"));
        assertEquals("org.replaced.parsing.Simple", mapping.originalToSubstitute(classes, "org.teavm.parsing.Simple"));
        assertEquals("other.Simple", mapping.originalToSubstitute(classes, "other.Simple"));

        assertEquals("org.teavm.Simple", mapping.substituteToOriginal("org.replaced.Simple"));
        assertEquals("org.teavm.parsing.Simple", mapping.substituteToOriginal("org.replaced.parsing.Simple"));
        assertEquals("other.Simple", mapping.substituteToOriginal("other.Simple"));
    }

    @Test
    public void dontFallbackWhenNoSubstitution() {
        var limited = new ClassHolderSource() {
            @Override
            public ClassHolder get(String name) {
                return name.equals("org.teavm.Pre_Existing") ? new ClassHolder(name) : null;
            }
        };
        var mapping = DefaultSubstituteClassNameMapping.createWithPolicies(List.of(
                new SimpleSubstitutionPolicy() {
                    @Override
                    public void contribute(SubstitutionSink sink) {
                        sink.selectClasses(inPackage("org.teavm"))
                                .simpleNamePrefix("Pre_")
                                .dontFallbackWhenNoSubstitution();
                    }
                }
        ));
        assertEquals("org.teavm.Pre_Existing", mapping.originalToSubstitute(limited, "org.teavm.Existing"));
        assertNull(mapping.originalToSubstitute(limited, "org.teavm.Missing"));
        assertEquals("org.teavm.parsing.Simple", mapping.originalToSubstitute(limited, "org.teavm.parsing.Simple"));
    }

    @Test
    public void fallbackWhenNoSubstitution() {
        var empty = new ClassHolderSource() {
            @Override
            public ClassHolder get(String name) {
                return null;
            }
        };
        var mapping = DefaultSubstituteClassNameMapping.createWithPolicies(List.of(
                new SimpleSubstitutionPolicy() {
                    @Override
                    public void contribute(SubstitutionSink sink) {
                        sink.selectClasses(inPackage("org.teavm"))
                                .simpleNamePrefix("Pre_");
                    }
                }
        ));
        assertEquals("org.teavm.Simple", mapping.originalToSubstitute(empty, "org.teavm.Simple"));
    }
}
