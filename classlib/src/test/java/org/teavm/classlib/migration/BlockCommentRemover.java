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

/**
 * {@link LineMigrator} that removes all block comments.
 */
public class BlockCommentRemover implements LineMigrator {

    private static final String JAVADOC_END = "*/";

    private static final String JAVADOC_START = "/*";

    private boolean inJavaDoc;

    @Override
    public String migrate(String line) {

        if (this.inJavaDoc) {
            int end = line.indexOf(JAVADOC_END);
            if (end >= 0) {
                String result = line.substring(end + 2);
                this.inJavaDoc = false;
                result = migrate(result);
            }
            return null;
        } else {
            int start = line.indexOf(JAVADOC_START);
            if (start >= 0) {
                String result = line.substring(0, start);
                int end = line.indexOf(JAVADOC_END, start + 2);
                if (end > start) {
                    result = result + line.substring(end + 2);
                    result = migrate(result);
                } else {
                    this.inJavaDoc = true;
                }
                if (result.trim().isEmpty()) {
                    return null;
                }
                return result;
            }
            return line;
        }
    }

}
