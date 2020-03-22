/*
 *  Copyright 2020 adam.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;
import org.teavm.parsing.substitution.java.RootPackageClass1;
import org.teavm.parsing.substitution.java.RootPackageClass2;
import org.teavm.parsing.substitution.java.RootPackageClass3;
import org.teavm.parsing.substitution.java.RootPackageClass4;
import org.teavm.parsing.substitution.java.excludedhierarchy.ExcludedPackageClass1;
import org.teavm.parsing.substitution.java.excludedhierarchy.excludedsubpackage.ExcludedSubPackageClass;
import org.teavm.parsing.substitution.java.excludedhierarchy.excludedsubpackage.IncludedSubPackageClass;
import org.teavm.parsing.substitution.java.excludedpackage.ExcludedPackageClass2;
import org.teavm.parsing.substitution.java.excludedpackage.excludedsubpackage.NotExcludedSubPackageClass;
import org.teavm.parsing.substitution.java.subpackage.ExcludedClass;
import org.teavm.parsing.substitution.java.subpackage.SubPackageClass1;
import org.teavm.parsing.substitution.java.subpackage.SubPackageClass2;
import org.teavm.parsing.substitution.java.subpackage.SubPackageClass3;
import org.teavm.parsing.substitution.java.subpackage.SubPackageClass4;
import org.teavm.parsing.substitution.java.subpackage.subsubpackage.SubSubPackageClass1;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
@SkipJVM
public class ClasspathResourceMapperTest {
    @Test
    public void classesInPackageHierarchyMappingAreSubstituted() {
        assertEquals("RootPackageClass1Substitute", new RootPackageClass1().getIdentifier());
        assertEquals("SubPackageClass1Substitute", new SubPackageClass1().getIdentifier());
    }

    @Test
    public void classesInPackageMappingAreSubstituted() {
        assertEquals("RootPackageClass2Substitute", new RootPackageClass2().getIdentifier());
    }

    @Test
    public void classesOutsidePackageMappingAreNotSubstituted() {
        assertEquals("SubPackageClass2", new SubPackageClass2().getIdentifier());
    }

    @Test
    public void specificClassMappingsAreSubstituted() {
        assertEquals("SubSubPackageClassSubstitute", new SubSubPackageClass1().getIdentifier());
    }

    @Test
    public void prefixedClassesInPackageHierarchyMappingAreSubstituted() {
        assertEquals("RootPackageClass3Substitute", new RootPackageClass3().getIdentifier());
        assertEquals("SubPackageClass3Substitute", new SubPackageClass3().getIdentifier());
    }

    @Test
    public void prefixedClassesInPackageMappingAreSubstituted() {
        assertEquals("RootPackageClass4Substitute", new RootPackageClass4().getIdentifier());
    }

    @Test
    public void prefixedClassesOutsidePackageMappingAreNotSubstituted() {
        assertEquals("SubPackageClass4", new SubPackageClass4().getIdentifier());
    }

    @Test
    public void excludedClassesInPackageHierarchyMappingAreNotSubstituted() {
        assertEquals("ExcludedPackageClass1", new ExcludedPackageClass1().getIdentifier());
        assertEquals("ExcludedSubPackageClass", new ExcludedSubPackageClass().getIdentifier());
    }

    @Test
    public void excludedClassesInPackageMappingAreNotSubstituted() {
        assertEquals("ExcludedPackageClass2", new ExcludedPackageClass2().getIdentifier());
    }

    @Test
    public void classesOutsidePackageMappingExclusionAreSubstituted() {
        assertEquals("NotExcludedSubPackageClassSubstitute", new NotExcludedSubPackageClass().getIdentifier());
    }

    @Test
    public void individualIncludedClassesInPackageHierarchyMappingExclusionAreSubstituted() {
        assertEquals("IncludedSubPackageClassSubstitute", new IncludedSubPackageClass().getIdentifier());
    }

    @Test
    public void individualExcludedClassesInPackageHierarchyMappingAreNotSubstituted() {
        assertEquals("ExcludedClass", new ExcludedClass().getIdentifier());
    }
}
