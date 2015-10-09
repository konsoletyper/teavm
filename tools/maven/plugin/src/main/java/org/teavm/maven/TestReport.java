package org.teavm.maven;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
public class TestReport {
    private List<TestResult> results = new ArrayList<>();

    public List<TestResult> getResults() {
        return results;
    }
}
