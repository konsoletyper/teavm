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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.jps.model.TeaVMJpsRemoteConfiguration;

@State(name = "teavm", storages = @Storage(id = "other", file = "$PROJECT_FILE$"))
public class TeaVMRemoteConfigurationStorage implements PersistentStateComponent<TeaVMJpsRemoteConfiguration> {
    private TeaVMJpsRemoteConfiguration state = new TeaVMJpsRemoteConfiguration();

    @NotNull
    @Override
    public TeaVMJpsRemoteConfiguration getState() {
        return state.createCopy();
    }

    @Override
    public void loadState(TeaVMJpsRemoteConfiguration state) {
        this.state.applyChanges(state);
    }
}
