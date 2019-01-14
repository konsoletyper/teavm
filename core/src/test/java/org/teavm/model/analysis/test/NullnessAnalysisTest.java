/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.model.analysis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectByteMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.teavm.model.ListingParseUtils;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.analysis.NullnessInformation;
import org.teavm.model.text.ListingBuilder;
import org.teavm.model.util.ProgramUtils;

public class NullnessAnalysisTest {
    @Rule
    public TestName name = new TestName();

    private static final String NOT_NULL_DIRECTIVE = "// NOT_NULL ";
    private static final String NULLABLE_DIRECTIVE = "// NULLABLE ";
    private static final String NULL_DIRECTIVE = "// NULL ";

    @Test
    public void simple() {
        test();
    }

    @Test
    public void phiJoin() {
        test();
    }

    @Test
    public void branch() {
        test();
    }

    @Test
    public void phiPropagation() {
        test();
    }

    @Test
    public void tryCatchJoint() {
        test();
    }

    @Test
    public void loop() {
        test();
    }

    @Test
    public void nonDominatedBranch() {
        test();
    }

    @Test
    public void exception() {
        test();
    }

    @Test
    public void nullAndNull() {
        test();
    }

    @Test
    public void irreduciblePhiLoop() {
        test();
    }

    private void test() {
        String baseName = "model/analysis/nullness/" + name.getMethodName();
        String originalResourceName = baseName + ".original.txt";
        String extendedResourceName = baseName + ".extended.txt";
        Program originalProgram = ListingParseUtils.parseFromResource(originalResourceName);
        Program extendedProgram = ListingParseUtils.parseFromResource(extendedResourceName);

        ListingBuilder listingBuilder = new ListingBuilder();
        String listingBeforeExtension = listingBuilder.buildListing(originalProgram, "");

        NullnessInformation information = NullnessInformation.build(originalProgram,
                new MethodDescriptor("foo", ValueType.VOID));

        ProgramUtils.makeUniqueLabels(originalProgram);
        String actualListing = listingBuilder.buildListing(originalProgram, "");
        String expectedListing = listingBuilder.buildListing(extendedProgram, "");

        assertEquals(expectedListing, actualListing);

        ObjectByteMap<String> expectedNullness = extractExpectedNullness(extendedResourceName);
        Map<String, Variable> variablesByLabel = variablesByLabel(originalProgram);
        for (ObjectCursor<String> varNameCursor : expectedNullness.keys()) {
            String varName = varNameCursor.value;
            Variable var = variablesByLabel.get(varName);
            assertNotNull("Variable " + varName + " is missing", var);
            byte nullness = expectedNullness.get(varName);
            switch (nullness) {
                case 0:
                    assertTrue("Variable " + varName + " must be null",  information.isNull(var));
                    break;
                case 1:
                    assertTrue("Variable " + varName + " must be non-null",  information.isNotNull(var));
                    break;
                case 2:
                    assertFalse("Variable " + varName + " must not be null",  information.isNull(var));
                    assertFalse("Variable " + varName + " must not be non-null", information.isNotNull(var));
                    break;
            }

        }

        information.dispose();
        String listingAfterDispose = listingBuilder.buildListing(originalProgram, "");
        assertEquals(listingBeforeExtension, listingAfterDispose);
    }

    private ObjectByteMap<String> extractExpectedNullness(String name) {
        ClassLoader classLoader = NullnessAnalysisTest.class.getClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(name);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            ObjectByteMap<String> result = new ObjectByteHashMap<>();

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                int index = line.indexOf(NULL_DIRECTIVE);
                if (index >= 0) {
                    String variable = line.substring(index + NULL_DIRECTIVE.length()).trim();
                    result.put(variable, (byte) 0);
                }

                index = line.indexOf(NOT_NULL_DIRECTIVE);
                if (index >= 0) {
                    String variable = line.substring(index + NOT_NULL_DIRECTIVE.length()).trim();
                    result.put(variable, (byte) 1);
                }

                index = line.indexOf(NULLABLE_DIRECTIVE);
                if (index >= 0) {
                    String variable = line.substring(index + NULLABLE_DIRECTIVE.length()).trim();
                    result.put(variable, (byte) 2);
                }
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Variable> variablesByLabel(Program program) {
        Map<String, Variable> result = new HashMap<>();
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = program.variableAt(i);
            if (var.getLabel() != null) {
                result.put(var.getLabel(), var);
            }
        }
        return result;
    }
}
