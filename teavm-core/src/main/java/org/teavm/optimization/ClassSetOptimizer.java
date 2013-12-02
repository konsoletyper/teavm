package org.teavm.optimization;

import java.util.Arrays;
import java.util.List;
import org.teavm.model.ClassHolder;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.MethodHolder;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClassSetOptimizer {
    private List<MethodOptimization> optimizations = Arrays.<MethodOptimization>asList(
            new UnusedVariableElimination());

    public void optimizeAll(ListableClassHolderSource classSource) {
        for (String className : classSource.getClassNames()) {
            ClassHolder cls = classSource.getClassHolder(className);
            for (MethodHolder method : cls.getMethods()) {
                for (MethodOptimization optimization : optimizations) {
                    optimization.optimize(method);
                }
            }
        }
    }
}
