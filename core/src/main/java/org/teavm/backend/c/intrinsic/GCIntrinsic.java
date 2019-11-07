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
package org.teavm.backend.c.intrinsic;

import org.teavm.ast.InvocationExpr;
import org.teavm.model.MethodReference;
import org.teavm.runtime.GC;

public class GCIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(GC.class.getName())) {
            return false;
        }

        switch (method.getName()) {
            case "gcStorageAddress":
            case "gcStorageSize":
            case "heapAddress":
            case "regionsAddress":
            case "regionMaxCount":
            case "availableBytes":
            case "regionSize":
            case "minAvailableBytes":
            case "maxAvailableBytes":
            case "resizeHeap":
            case "cardTable":
            case "writeBarrier":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "resizeHeap":
                context.writer().print("teavm_gc_resizeHeap(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;

            case "writeBarrier":
                context.writer().print("teavm_gc_writeBarrier(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;

            default:
                context.includes().includePath("heaptrace.h");
                context.writer().print("teavm_gc_").print(invocation.getMethod().getName());
                break;
        }
    }
}
