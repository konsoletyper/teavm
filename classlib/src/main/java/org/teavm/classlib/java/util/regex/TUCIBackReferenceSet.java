/*
 *  Copyright 2017 Alexey Andreev.
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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Nikolay A. Kuznetsov
 */
package org.teavm.classlib.java.util.regex;

/**
 * Unicode case insensitive back reference (i.e. \1-9) node.
 *
 * @author Nikolay A. Kuznetsov
 */
class TUCIBackReferenceSet extends TCIBackReferenceSet {

    int groupIndex;

    public TUCIBackReferenceSet(int groupIndex, int consCounter) {
        super(groupIndex, consCounter);
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        String group = getString(matchResult);

        if (group == null || (stringIndex + group.length()) > matchResult.getRightBound()) {
            return -1;
        }

        for (int i = 0; i < group.length(); i++) {
            if (Character.toLowerCase(Character.toUpperCase(group.charAt(i))) != Character.toLowerCase(Character
                    .toUpperCase(testString.charAt(stringIndex + i)))) {
                return -1;
            }
        }
        matchResult.setConsumed(consCounter, group.length());
        return next.matches(stringIndex + group.length(), testString, matchResult);
    }

    @Override
    public String getName() {
        return "UCI back reference: " + this.groupIndex; //$NON-NLS-1$
    }
}
