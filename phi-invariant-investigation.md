# Exception handler phi invariant: who violates it

Investigation for branch `ir-phi-invariant-experiment`, after commit `fe0d13ad4`
("Introduce new IR invariant ... bind order of phi inputs to order of instructions").

## 1. The contract as I read it

Take a handler block `H`, a protected block `B` (`B` has a try/catch to `H`) and a phi `φ` in `H`.
List the incomings of `φ` whose source is `B`, in list order: `v0, v1, ..., vk`. Then:

1. **Entry value.** Only `v0` may be defined outside `B`'s instructions. That means a dominator of `B`,
   a phi of `B`, or the exception variable of `B`. It is the value the handler sees when the exception
   comes before any assignment.
2. **Order.** Every other `vi` is defined by an instruction of `B`, and the definitions come in strictly
   increasing instruction order. `vi` is what the handler sees for an exception thrown after `def(vi)`,
   up to and including the instruction that defines `vi+1`.
3. **No repetitions.** Each virtual edge contributes one incoming (from the comment in `PhiUpdater`).

Two consumers already rely on this positional meaning. Both ignore list order and use the
*position of the definition*:

- `RegisterAllocator.insertPhiArgumentsCopies` (JS and C). For an incoming defined by an instruction of `B`,
  it inserts the copy right after that instruction. Every other incoming gets its copy at the start of
  `B` (`addFirst`).
- `WasmCodeGenerator.processBasicBlock` / `updateHandlerPhis` (Wasm GC). For an incoming not defined by an
  instruction of `B`, it does `local.set` at block start, in list order. For incoming defined in `B`,
  the `local.set` goes right after the definition.

So there are two ways to break things:
- **(a)** put two or more incomings from `B` "outside of `B`". All of them are then assigned at block
  start, the last one wins, and the handler sees a later value too early.
- **(b)** move the definition of an incoming to a different position in `B` (or out of it).

`PhiUpdater` additionally reads **list order** of existing incomings (`sources[0]` is taken as the entry
value), so wrong list order alone is enough to make it drop incomings.

## 2. Summary

| Place | Violates? | Effect |
|---|---|---|
| `BasicBlockSplitter.fixIncomingsInExceptionHandler` | **Yes, order** | The next `PhiUpdater` run **drops the entry incoming** of every split part after the first (demonstrated) |
| ↳ `CoroutineTransformation` (C) | **Yes** (through splitter, then `PhiUpdater`) | Lost incoming; wrong value on the resume path |
| ↳ `DependencyClassSource.processInvokeDynamic` | **Yes** (through splitter) | Wrong order goes into the whole pipeline; nullness and `NullCheckInsertion` run `PhiUpdater`, which drops the incoming (demonstrated) |
| `GlobalValueNumbering` | **Yes, (a) and order** | **Miscompile** on JS/C/Wasm GC (demonstrated down to register allocation) |
| `LoopInvariantMotion` | **Yes, (a)** | **Miscompile** on JS/C/Wasm GC (demonstrated down to register allocation) |
| `JSClassProcessor.wrapJsPhis` | **Yes, (b)** | Handler sees a stale value when the exception comes before the end of the block |
| `WasmGCMethodGenerator.eliminateMultipleNullConstantUsages` | **Yes, (b)** | Same; often hidden because Wasm ref locals start as `null` |
| `NullnessInformation.dispose` | Yes, but only duplicates | Benign: `phi @p from $1, @p from $1` (demonstrated) |
| GVN copy propagation (`x1 := x`, e.g. after `RedundantNullCheckElimination`) | Duplicates | Benign |
| Inlining | No | — |
| Irreducible splitter in `CoroutineTransformation` | No | — |
| `ProgramNodeSplittingBackend` (Parser, `AsyncProgramSplitter`) | No (no phis there) | — |
| Others (see §5) | No | — |
| `CheckInstructionTransformation`, `ClassInitializerTransformer`, `EmptyBlockElimination`, `LoopInversion` | Would violate | Dead code / disabled |

## 3. Confirmed violations

All listings below come from a throwaway harness. It runs the real compiled classes from
`core/build/classes` on hand-written listings. Source and full output:
`build/phi-invariant-check/Check.java` and `build/phi-invariant-check/output.txt`
(gitignored, so they may get wiped by `clean`).

### 3.1 `BasicBlockSplitter` (wrong list order → `PhiUpdater` loses the incoming)

For a handler phi, `fixIncomingsInExceptionHandler` does two things for each part after the first. It
**appends** a copy of the "last defined value" (the entry value of that part) to the end of the phi's
incoming list. Then it re-sources the existing incomings defined in that part, and those incomings
keep their old, earlier position. The part's group therefore ends up as
`[values defined in the part..., entry value]`, which is reversed.

The defining positions are still right, so `RegisterAllocator` and `WasmCodeGenerator` alone would
generate correct code. But the next `PhiUpdater` takes the first incoming of the group as the entry
value. That variable is defined *inside* the part, so `variableMap` is `null` at block entry and no entry
incoming is created. `finishExceptionEdges` then removes the old entry incoming because it is mapped.
The entry value is lost.

Coroutine-style split (around `async()`):

```
after BasicBlockSplitter.fixProgram
  $1: bar(); goto $4                       catch -> $2
  $4: @x_1 := async(); goto $5             catch -> $2
  $5: baz(); goto $3                       catch -> $2
  $2: @y := phi @x from $1, @x_1 from $4, @x from $4, @x_1 from $5
                                ^^^^^^^^^^^^^^^^^^^^^^^ reversed for $4

after PhiUpdater
  $2: @y := phi @x_2 from $1, @x_1 from $4, @x_1 from $5
          -- "@x from $4" is gone: if async() throws, the handler has no value for that path
```

DependencyClassSource-style split (after an `indy`), also demonstrated:

```
after split:     @y := phi @x from $1, @r from $1, @x_2 from $4, @r from $4
after PhiUpdater: @y := phi @x_1 from $1, @r from $1, @x_2 from $4      -- "@r from $4" lost
                  (if baz() throws, the handler must see @r)
```

Affected users of the splitter:
- **`CoroutineTransformation`** (C backend, `afterOptimizations`). It calls `PhiUpdater` immediately
  after the splitter. On the straight path the handler register/local may still hold the right value by
  coincidence, because the previous part already assigned it and control falls through directly. On the
  **resume path** (`restoreBlock → intermediate`) nothing assigned it, so the handler sees garbage.
- **`DependencyClassSource.processInvokeDynamic`** runs on every method with `invokedynamic` (lambdas,
  string concat, ...) right after parsing. The wrong order then goes into the whole pipeline. Every later
  `PhiUpdater` run loses the incoming: `NullCheckInsertion`, `BoundCheckInsertion`, `ScalarReplacement`,
  and `NullnessInformation.build`, which runs on every optimized method. Trigger in Java: a lambda or
  string concat inside a `try`, a local reassigned after it in the same `try`, and that local used in
  the `catch`.
- `CheckInstructionTransformation`, `ClassInitializerTransformer`: same bug, but no callers
  (`ClassInitializerTransformer` is constructed in `CTarget`, but `transform()` is never called).

Suggested fix: build each part's group in place, in order: `[entry copy, defined-in-part...]`. Insert the
entry copy *before* the first re-sourced incoming of that part, rather than appending at the end.

There is also a related latent problem, not about order. If the splitter's client moves the definition
of an incoming into a block *outside* the split sequence, the incoming keeps pointing at the first part.
In `processInvokeDynamic`, a substitutor that emits control flow would do this, because `pe.jump(splitBlock)`
starts from the emitter's current block. I did not check whether any substitutor actually emits
branches.

### 3.2 `GlobalValueNumbering`: confirmed miscompile

GVN replaces the uses of a redundant value with the dominating equivalent one. This includes the
handler phi incomings (`outgoings` loop in `optimize`). If the equivalent value is defined before `B`,
there are now two incomings defined outside `B`, which is violation (a). If it is defined earlier in `B`,
before another incoming of the group, the order is wrong.

```
original:            $1: bar(); @v_1 := @a + @b; baz()        catch -> $2
                     $2: @w := phi @v from $1, @v_1 from $1   (@v := 0, @c := @a + @b in $0)
after GVN:           $2: @w := phi @v from $1, @c from $1     -- both defined outside $1
after RegisterAllocator:
  $1
      @v := @c          <-- handler register set at block start
      invokeStatic `Foo.bar()V`
      invokeStatic `Foo.baz()V`
  $2
      return @v
```

If `bar()` throws, the handler returns `a + b` instead of `0`. `WasmCodeGenerator` does the same: it
does `local.set` for both at block start, in list order, and the last one wins. In Java:

```java
int c = a + b; int v = 0;
try { bar(); v = a + b; baz(); } catch (RuntimeException e) { return v; }
```

This needs `namesPreserved == false` (ADVANCED/FULL), or compatible debug names at SIMPLE.

Duplicates also come from GVN copy propagation: `@x_1 := @x` becomes `phi @x, @x`. For example, after
`RedundantNullCheckElimination` turns `@p_1 := nullCheck @p` into an assign. These are benign.

Possible fixes: don't propagate into exception-handler phi incomings, or materialise an assign at the
original position.

### 3.3 `LoopInvariantMotion`: confirmed miscompile

LIM moves the definition of a handler phi input (binary op, assign, cast, ...) from the protected loop
block to the preheader. That input now counts as defined outside `B`.

```
original:   $1: @p := phi @v from $0, @q from $2; bar(); @q := @a + @b; baz()   catch -> $3
            $3: @w := phi @p from $1, @q from $1
after LIM:  @q := @a + @b moved to $0  -> both @p and @q are defined outside $1
after RegisterAllocator:
  $1
      @q := @p      <-- copy of moved @q
      @q := @v      <-- copy of @p wins
      invokeStatic `Foo.bar()V`
      invokeStatic `Foo.baz()V`
```

If `baz()` throws on the first iteration, the handler gets `0` instead of `a + b`. On Wasm GC, the
handler would instead see `a + b` even when `bar()` throws (list order decides). Either way it is wrong.

Possible fix: in the protected block, don't move instructions whose receiver is a handler phi input.
Alternatively, leave an `assign` at the original position.

Unrelated, pre-existing: `insertPreheader` remaps only the last instruction of the predecessors. If a
loop header is an exception handler (e.g. javac's self-protecting `monitorexit` handler), the try/catch
edges are not redirected.

### 3.4 `JSClassProcessor.wrapJsPhis` and `WasmGCMethodGenerator.eliminateMultipleNullConstantUsages`

Both add a definition for a phi input with `incoming.getSource().getLastInstruction().insertPrevious(...)`
and then point the incoming at it. For a normal edge that is correct. For an exception edge it moves the
definition behind every throwing instruction of `B`. The input is now defined at the very end of `B`,
which breaks (b). Both consumers then assign the handler local/register only just before the final
`goto`, so an exception in the middle of `B` sees a stale value.

- `wrapJsPhis` (JS and Wasm GC JSO): happens when a handler phi is `MIXED` and an incoming is `JS`-typed.
  The `wrap` call is also inserted inside the protected range. Fix: for exception edges, insert the
  `wrap` right after the definition of the value, or at block start for the entry value.
- `eliminateMultipleNullConstantUsages`: happens when a GVN-shared `null` constant is a handler phi
  input and is not its first use. Often hidden because a fresh Wasm ref local defaults to `null`.
  It is wrong when the local was reused or coalesced. Fix: for exception edges, insert the new constant
  at the start of `B` (it is the entry value).

I did not run these two; this is from reading the code.

### 3.5 `NullnessInformation`: only duplicates

You were right about the build phase. It inserts `@p := nullCheck @p` (after the instruction, which is
fine) and `PhiUpdater` turns it into correctly ordered `phi @p, @p_1`. But `dispose()` maps the versions
back to their source variables and does not remove the extra incoming, so the IR keeps a **permanent
duplicate**:

```
built:    @w := phi @p from $1, @p_1 from $1
disposed: @w := phi @p from $1, @p from $1
```

This is harmless for both consumers: they copy the same value twice. `PhiUpdater` dedups sources on
input. It only breaks the "no repetitions" part of the contract, if you want that part to be strict.
Optimizations that run while nullness is built (LIM, CCE, RNCE) see SSA that `PhiUpdater` just produced,
so the only problem there is LIM itself (§3.3).

### 3.6 Are the GVN / LIM bugs new?

No. They were already broken before this branch. This branch differs from `master` only in `PhiUpdater`
and its tests. GVN, LIM, `RegisterAllocator` and `WasmCodeGenerator` are untouched, and the harness
cases for §3.2 and §3.3 never call `PhiUpdater`. The input shape that triggers them, a handler phi with
several versions from one protected block, was produced by the old `PhiUpdater` too (`definedVersions`).
Only the list order differed, and the JS/C path does not look at list order. The relevant code is old:
LIM moves instructions to the preheader since 2014–2017, the exception copies in `RegisterAllocator`
date from 2019 at the latest, and the Wasm GC generator from 2026-06. Commit `1d81b7004` ("Fix possible
bug with exceptions in GVN", 2024) is about dominance inside blocks, not this problem.

### 3.7 End-to-end check: wrong code in JS and Wasm GC

Repro: `build/phi-invariant-check/e2e/src/Repro.java`. I compiled it with the prebuilt
`tools/cli/build/libs/cli-0.18.0-SNAPSHOT-all.jar` (built 2026-11-13, so **before** this branch's
commits), without `-g`, and ran it in Node 24. That jar's merged `META-INF/services` only lists
`JCLPlugin`, so I put a merged copy (`e2e/services-override`) first on the classpath.

| case | JVM | JS O1 | JS O2/O3 | Wasm GC O1 | Wasm GC O2/O3 |
|---|---|---|---|---|---|
| `gvn`, throw before `v = a + b` | 0 | **7** | **7** | **7** | **7** |
| `gvn`, throw after | 11 | 11 | 11 | 11 | 11 |
| `licm`, throw before `v = a + b`, iter 0 | 0 | 0 | 0 | 0 | **15** |
| `licm`, throw after, iter 0 | 19 | 19 | **0** | 19 | 19 |
| `licm`, throw before, iter 2 | 23 | 23 | 23 | 23 | 23 |

- The GVN bug fires even at O1 (SIMPLE). Without debug information, variables have no debug names, so
  the `namesPreserved` check does not stop the merge.
- LICM picks the wrong value in opposite cases on the two backends, as predicted.
  - The JS register allocator puts the phi's copy last, so the handler always sees the entry value.
  - Wasm GC sets the handler local in list order, so it always sees the hoisted value.

Why this is rarely seen in practice: it needs a local that is reassigned in a `try` and read in the
`catch`. The new value must be pure arithmetic that GVN finds redundant or LICM can hoist, not a call
result. And the exception has to come from the same basic block, before the assignment. In C, the
splitter path (§3.1) and the shadow-stack transformations hit the contract far more often.

## 4. Checked and not violating

- **Inlining**. `buildPlan` skips blocks that have try/catches, at every depth, so nothing is inlined
  into a protected block. The callee's own handler phis are copied unchanged. Parameters are replaced
  with caller arguments, which are defined outside the inlined region, so they stay "the single outside
  entry value".
- **Irreducible splitting in `CoroutineTransformation.SplittingBackend`**. Copied blocks get their output
  incomings appended in original relative order. Copied handlers keep their incoming order, and whole
  groups are removed from originals. It runs on non-SSA IR right before `PhiUpdater`, and its input
  satisfies `PhiUpdater`'s precondition (as long as the earlier splitter step did, see §3.1).
- **`ProgramNodeSplittingBackend`**: used by `Parser` before SSA (no phis) and by `AsyncProgramSplitter`,
  whose parts contain no phis (instructions only).
- **`ScalarReplacement`, `NullCheckInsertion`, `BoundCheckInsertion`, `ArrayElimination` (metaprogramming)**:
  they introduce non-SSA redefinitions and rebuild through `PhiUpdater`. The old phi groups keep their
  order (ScalarReplacement copies incoming lists 1:1), so `PhiUpdater`'s precondition holds.
  `BoundCheckInsertion` truncation and `RedundantNullCheckElimination`'s null path delete definitions.
  `UnreachableBasicBlockEliminator` then removes incomings with undefined values, and removal keeps order.
- **`RedundantJumpElimination`**: skips protected blocks and their targets.
- **`UnreachableBasicBlockEliminator`, `ConstantConditionElimination`, `FastConstantConditionElimination`,
  `NoSuchFieldCatchElimination`, `UnusedVariableElimination`**: only remove whole source groups, whole
  phis, or incomings with undefined values.
- **`RedundantPhiElimination`**: only replaces phi receivers. A phi receiver of `B` can only be the entry
  value `v0`, and replacing it with another outside value is still a single entry value.
- **`RepeatedFieldReadElimination`**: replaces `getField` with an `assign` *in place*. The later GVN copy
  propagation is covered in §3.2.
- **`ArrayUnwrapMotion`**: moves `unwrapArray` up within its block. Its receiver is a temporary that is
  never a handler phi input.
- **`DivisionCheckInsertion`, `AssertionRemoval`, `Devirtualization`, `ClassInitElimination`,
  `ClassInitializerEliminator`, `WriteBarrierInsertion`**: in-place replacement or insertion of
  instructions that define no handler phi input.
- **`ShadowStackTransformer.splitFirstBlock`, `SynchronizedMethodTransformer`**: remap whole blocks, so
  every group moves as a whole.
- **`RegisterAllocator`, `WasmCodeGenerator`**: consumers that follow the contract. Note that they rely on
  definition positions (rules (a)/(b) above), not on list order.

## 5. Dead / disabled code that would violate

- `CheckInstructionTransformation`: no callers; would hit the splitter bug.
- `ClassInitializerTransformer`: `transform()` is never called; would hit the splitter bug.
- `EmptyBlockElimination`: unused. It remaps an empty protected block's handler incomings onto the next
  block, which creates duplicates or misplaced entry values in that block's group.
- `LoopInversion`: commented out in `TeaVM.getOptimizations()`; copies blocks, not analysed.

## 6. Not fully analysed

- `metaprogramming/.../CompositeMethodGenerator`: it captures phi inputs right before the last instruction
  of the (possibly split) source block, and re-sources incomings to the last emitted block. This is the
  same pattern as §3.4, but it only matters for templates with try/catch.

## 7. Suggested order of work

1. `BasicBlockSplitter`: it feeds wrong order into `PhiUpdater`, which now silently loses incomings, and
   the indy path hits almost every program.
2. GVN and LIM: real miscompiles at ADVANCED/FULL on all three backends. They predate this commit but
   break the same rule.
3. `wrapJsPhis` and `eliminateMultipleNullConstantUsages`.
4. Optionally, a debug-only IR checker for rules 1–3, run after each optimization in tests. It would
   have caught all of the above.
