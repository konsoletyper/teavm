/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.restructurization;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.HashSet;
import java.util.Set;
import org.teavm.common.CommonIndentPrinter;

public class BlockPrinter {
    private CommonIndentPrinter sb = new CommonIndentPrinter();
    private ObjectIntMap<LabeledBlock> identifiers = new ObjectIntHashMap<>();
    private Set<Block> visitingBlocks = new HashSet<>();
    private int identifierGen;

    public String print(Block block) {
        visitMany(block);
        var result = sb.toString();
        sb.clear();
        identifierGen = 0;
        return result;
    }

    private void start(LabeledBlock block) {
        identifiers.put(block, identifierGen++);
    }

    private void end(LabeledBlock block) {
        identifiers.remove(block);
    }

    private void visitMany(Block block) {
        if (block != null) {
            block = block.first;
        }
        if (visitingBlocks.contains(block)) {
            sb.append("error: recursion").newLine();
            return;
        }
        while (block != null) {
            visitingBlocks.add(block);
            block.acceptVisitor(visitor);
            visitingBlocks.remove(block);
            block = block.getNext();
        }
    }

    private BlockVisitor visitor = new BlockVisitor() {
        @Override
        public void visit(SimpleBlock block) {
            sb.append("simple ").append(block.getBasicBlock().getIndex()).newLine();
        }

        @Override
        public void visit(BreakBlock block) {
            sb.append("break ").append(getIdentifier(block.getTarget())).newLine();
        }

        @Override
        public void visit(ContinueBlock block) {
            sb.append("continue ").append(getIdentifier(block.getTarget())).newLine();
        }

        @Override
        public void visit(ReturnBlock block) {
            sb.append("return");
            if (block.getValue() != null) {
                sb.append(" ").append(block.getValue().getIndex());
            }
            sb.newLine();
        }

        @Override
        public void visit(ThrowBlock block) {
            sb.append("throw ").append(block.getException().getIndex()).newLine();
        }

        @Override
        public void visit(SimpleLabeledBlock block) {
            start(block);
            sb.append(getIdentifier(block)).append(": {").newLine().indent();
            visitMany(block.getBody());
            sb.outdent().append("}").newLine();
            end(block);
        }

        @Override
        public void visit(SwitchBlock block) {
            start(block);
            sb.append(getIdentifier(block)).append(": switch").append(block.getCondition().getIndex())
                    .append(" {").newLine().indent();
            for (var entry : block.getEntries()) {
                for (var value : entry.getMatchValues()) {
                    sb.append("case ").append(value).newLine();
                }
                sb.indent();
                visitMany(entry.getBody());
                sb.outdent();
            }
            if (block.getDefaultBody() != null) {
                sb.append("default:").newLine().indent();
                visitMany(block.getDefaultBody());
                sb.outdent();
            }
            sb.outdent().append("}").newLine();
            end(block);
        }

        @Override
        public void visit(IfBlock block) {
            start(block);
            sb.append(getIdentifier(block)).append(": if");
            if (block.inverted) {
                sb.append("!");
            }
            sb.append(" {").newLine().indent();
            visitMany(block.getThenBody());
            sb.outdent().append("}");
            if (block.getElseBody() != null) {
                sb.append(" else {").indent().newLine();
                visitMany(block.getElseBody());
                sb.outdent().append("}");
            }
            sb.newLine();
            end(block);
        }

        @Override
        public void visit(LoopBlock block) {
            start(block);
            sb.append(getIdentifier(block)).append(": loop {").newLine().indent();
            visitMany(block.getBody());
            sb.outdent().append("}").newLine();
            end(block);
        }

        @Override
        public void visit(TryBlock block) {
            sb.append("try {").newLine().indent();
            visitMany(block.getTryBlock());
            sb.outdent().append("} catch (").append(block.getExceptionType()).append(" ")
                    .append(block.getException().getIndex()).append(") {").indent().newLine();
            visitMany(block.getCatchBlock());
            sb.outdent().append("}").newLine();
        }
    };

    private int getIdentifier(LabeledBlock block) {
        return identifiers.getOrDefault(block, -1);
    }
}
