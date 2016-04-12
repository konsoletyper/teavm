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
package org.teavm.metaprogramming.impl;

import java.util.List;
import org.teavm.metaprogramming.Action;
import org.teavm.model.MethodReference;

public class ActionImpl extends Fragment implements Action {
    public ActionImpl(List<CapturedValue> capturedValues, MethodReference method) {
        super(capturedValues, method);
    }

    @Override
    public void run() {
        throw new IllegalStateException("Don't call this method directly");
    }

    public static ActionImpl create(List<CapturedValue> capturedValues, MethodReference method) {
        return new ActionImpl(capturedValues, method);
    }
}
