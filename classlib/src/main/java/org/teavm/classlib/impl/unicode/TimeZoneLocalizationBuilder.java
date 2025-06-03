/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl.unicode;

import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.builders.ObjectResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;
import org.teavm.platform.metadata.builders.StringResourceBuilder;

public class TimeZoneLocalizationBuilder extends ObjectResourceBuilder {
    public final ResourceMapBuilder<ResourceMapBuilder<StringResourceBuilder>> timeZones = new ResourceMapBuilder<>();

    @Override
    public Object getValue(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException();
        }
        return timeZones;
    }

    @Override
    public String[] fieldNames() {
        return new String[] { "timeZones" };
    }

    @Override
    public Class<? extends Resource> getOutputClass() {
        return TimeZoneLocalization.class;
    }
}
