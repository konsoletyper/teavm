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
package org.teavm.idea;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;
import org.teavm.idea.ui.TeaVMFacetEditorTab;

public class TeaVMFacetConfiguration implements FacetConfiguration, PersistentStateComponent<TeaVMJpsConfiguration> {
    private TeaVMJpsConfiguration state = new TeaVMJpsConfiguration();

    @Override
    public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext,
            FacetValidatorsManager validatorsManager) {
        return new FacetEditorTab[] { new TeaVMFacetEditorTab(editorContext.getModule(), state) };
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
    }

    @Nullable
    @Override
    public TeaVMJpsConfiguration getState() {
        return state;
    }

    @Override
    public void loadState(TeaVMJpsConfiguration state) {
        this.state = state;
    }
}
