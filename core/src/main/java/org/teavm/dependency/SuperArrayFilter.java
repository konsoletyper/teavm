/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.dependency;

import org.teavm.model.ValueType;

class SuperArrayFilter implements DependencyTypeFilter {
    private DependencyAnalyzer analyzer;
    private DependencyTypeFilter itemTypeFilter;

    SuperArrayFilter(DependencyAnalyzer analyzer, DependencyTypeFilter itemTypeFilter) {
        this.analyzer = analyzer;
        this.itemTypeFilter = itemTypeFilter;
    }

    @Override
    public boolean match(DependencyType type) {
        if (!type.getName().startsWith("[")) {
            return false;
        }

        String typeName = type.getName().substring(1);
        ValueType valueType = ValueType.parseIfPossible(typeName);
        if (valueType == null || valueType instanceof ValueType.Primitive) {
            return false;
        }
        if (valueType instanceof ValueType.Object) {
            typeName = ((ValueType.Object) valueType).getClassName();
        }
        return itemTypeFilter.match(analyzer.getType(typeName));
    }
}
