## Rationale

The development of Wasm backend has started before Wasm was released and was based on old Wasm specifications, prior
to migration to stack machine approach. Relying on expr-based approach makes it possible to generate Wasm code, but
it does not allow to utilize all the features of modern Wasm.

Currently `org.teavm.backend.wasm.model` has `expression` subpackage, which reflects old expression-based spec.
The idea is to introduce `instruction` subpackage, where modern stack machine instructions will be represented.

* base `WasmInstruction` class
* instructions must be organized as a linked list, much like `Instruction` class from main IR.
* instead of having separate field `WasmInstructionList`, all instruction containers must inherit from base instruction list class.
* instructions must be mutable
* instructions must implement visitor pattern
* sources of inspiration:
  - `org.teavm.backend.wasm.model.expression`
  - `org.teavm.backend.wasm.parser.CodeListener`
* no documentation or tests needed


# Migration plan

## Step 1. Introduce `WasmInstruction` class

1. Introduce `org.teavm.backend.wasm.model.instruction.WasmInstruction` class and its subclasses
2. Introduce `WasmExpressionVisitor` implementation that takes expr-based representation and converts in into instruction-based representation
3. Introduce `WasmBinaryInstructionRenderingVisitor`, which must be similar to `org.teavm.backend.wasm.render.WasmBinaryRenderingVisitor`, but take intructions as input.
4. Update `org.teavm.backend.wasm.render.WasmBinaryRenderer.renderFunction` to first convert expression tree into instructions tree and the use `WasmBinaryInstructionRenderingVisitor` to render instructions into binary format.

All DWARF-related routines can be temporarily avoided.

As the intermediate result, at least smoke end-to-end test should pass.