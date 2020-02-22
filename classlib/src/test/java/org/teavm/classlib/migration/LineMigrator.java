/*
 *  Copyright 2020 Joerg Hohwiller.
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
package org.teavm.classlib.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Interface for a single aspect of code-migration that will be {@link #migrate(String) called} for each line of
 * source-code.
 */
public interface LineMigrator {

    /**
     * @param line a single line of source-code to transform.
     * @return the transformed code-line or {@code null} to entirely remove the current line.
     */
    String migrate(String line);

    /**
     * @param migrators the {@link LineMigrator}s to append after this one.
     * @return a new {@link LineMigrator} that composes this one with the given {@code migrators}.
     */
    default LineMigrator append(LineMigrator... migrators) {

        Collection<LineMigrator> all = new ArrayList<>();
        collect(all);
        for (LineMigrator child : migrators) {
            if (child != null) {
                child.collect(all);
            }
        }
        return new ComposedLineMigrator(all);
    }

    /**
     * @param migrators the {@link Collection} with the {@link LineMigrator}s to collect.
     */
    default void collect(Collection<LineMigrator> migrators) {

        migrators.add(this);
    }

    /**
     * @param sourcePkg the source package to search.
     * @param targetPkg the target package to use as replacement for the source package.
     * @return a new {@link LineMigrator}.
     */
    static LineMigrator ofPackage(String sourcePkg, String targetPkg) {

        return new PatternReplaceMigrator(Pattern.compile("package[ \t]+" + sourcePkg.replace(".", "\\.")),
                "package " + targetPkg);
    }

    /**
     * @param sourceType the imported source type to search.
     * @param targetType the target type to use as replacement for the source type.
     * @return a new {@link LineMigrator}.
     */
    static LineMigrator ofImport(String sourceType, String targetType) {

        return new PatternReplaceMigrator(
                Pattern.compile("import[ \t]+(static[ \t])?[ \t]*" + sourceType.replace(".", "\\.")),
                "import $1" + targetType);
    }

    /**
     * @param match the fixed identifier to search.
     * @param replacement the identifier used to replace the given {@code match} if found.
     * @return a new {@link LineMigrator}.
     */
    static LineMigrator ofIdentifier(String match, String replacement) {

        return new IdentifierReplaceMigrator(match, replacement);
    }

    /**
     * @param migrators the {@link LineMigrator}s to compose sequentially.
     * @return a new {@link LineMigrator}.
     */
    static LineMigrator ofComposed(LineMigrator... migrators) {

        if (migrators.length == 1) {
            return migrators[0];
        }
        return new ComposedLineMigrator(Arrays.asList(migrators));
    }

    /**
     * @param types the array with the {@link Class}es of the types to migrate to T* types.
     * @return a new {@link LineMigrator}.
     */
    static LineMigrator ofJavaTypes4Classlib(Class<?>[] types) {

        String[] fqns = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            fqns[i] = types[i].getName();
        }
        return ofJavaTypes4Classlib(fqns);
    }

    /**
     * @param types the array with the {@link Class#getName() fully qualified name}s of the types to migrate to T*
     *        types.
     * @return a new {@link LineMigrator}.
     */
    static LineMigrator ofJavaTypes4Classlib(String[] types) {

        Collection<LineMigrator> result = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            String fqn = types[i];
            int lastDot = fqn.lastIndexOf('.');
            String simpleName;
            String packageName;
            if (lastDot > 0) {
                simpleName = fqn.substring(lastDot + 1);
                packageName = fqn.substring(0, lastDot);
            } else {
                simpleName = fqn;
                packageName = "";
            }
            result.add(ofImport(fqn, "org.teavm.classlib." + packageName + ".T" + simpleName));
            result.add(new IdentifierReplaceMigrator(simpleName, "T" + simpleName));
        }
        return new ComposedLineMigrator(result);
    }

}
