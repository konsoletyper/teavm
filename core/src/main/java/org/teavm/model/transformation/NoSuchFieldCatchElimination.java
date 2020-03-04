/*
 *  Copyright 2020 Alexey Andreev.
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
package org.teavm.model.transformation;

import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;

public class NoSuchFieldCatchElimination {
    private UnreachableBasicBlockEliminator blockEliminator = new UnreachableBasicBlockEliminator();

    public void apply(Program program) {
        boolean modified = false;
        for (BasicBlock block : program.getBasicBlocks()) {
            for (int i = 0; i < block.getTryCatchBlocks().size(); ++i) {
                TryCatchBlock tryCatch = block.getTryCatchBlocks().get(i);
                if (tryCatch.getExceptionType() != null
                        && tryCatch.getExceptionType().equals(NoSuchFieldError.class.getName())) {
                    updateTryCatchHandler(tryCatch);
                    block.getTryCatchBlocks().remove(i--);
                    modified = true;
                }
            }
        }
        if (modified) {
            blockEliminator.optimize(program);
        }
    }

    private void updateTryCatchHandler(TryCatchBlock tryCatch) {
        for (Phi phi : tryCatch.getHandler().getPhis()) {
            for (int i = 0; i < phi.getIncomings().size(); ++i) {
                Incoming incoming = phi.getIncomings().get(i);
                if (incoming.getSource() == tryCatch.getProtectedBlock()) {
                    phi.getIncomings().remove(i--);
                }
            }
        }
    }
}
