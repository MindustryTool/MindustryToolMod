# Audit re-check — `audit.md` section 8 gaps 1–8

Re-run after `solim-ownership-unification`. The original audit was written against three separate
implementations of ownership; there is now one, and two rules replace the per-method bookkeeping:

1. `Component`'s constructor announces itself to whatever is building, so a component created
   inside a scope is adopted by that scope.
2. A reactive modifier hands its resource to the component it configures whenever no ambient scope
   captured it, so a binding is never orphaned.

Measured against the ten gaps in `audit.md` §8. Gaps 9 and 10 are out of scope for this change.

| # | Location | Original gap | Status after this change |
|---|---|---|---|
| 1 | `QueryView` `.loading/.error/.data` | factories run under a bare `AttachmentStack.isolate` | **Structurally unchanged, materially closed.** The isolate is still bare, but a component built there now owns its own bindings, and the reconciler that drives it opens an `OwnershipScope` that adopts them. |
| 2 | `ForEach` `applyToCell` | return value discarded | **Open.** `sc.applyToCell(cell)` still drops the effects it creates; they are neither owned by the item nor by the `ForEach`. |
| 3 | `ReactiveGrid` `empty(Runnable)` | output attached to a throwaway `Table`, never disposed | **Open.** The throwaway table is still built and discarded. |
| 4 | `ReactiveGrid.gap(Readable)` | registers its effect but keeps no instance reference | **Closed.** The effect is handed to the grid through `own(...)`. |
| 5 | `Tabs.tab(...)` | tab content mounted from an `Effect` body with an empty stack | **Closed.** The body runs inside the `Effect`'s own `OwnershipScope`, which keeps what it created and releases it with the effect. |
| 6 | `SolimStack.layer(Prov/Func)` | isolate with an empty stack | **Materially closed.** The layer the factory returns is `own(...)`-ed by the stack, so it dies with the stack instead of being tracked in a private list. |
| 7 | `Popup` provider | `provider.get(data)` isolates with an empty stack | **Structurally unchanged, materially closed** for the same reason as gap 1. |
| 8 | `SolimImage`, `NetworkImage` | bindings never stored on the instance | **Closed.** `SolimImage.drawable(Readable)` and `color(Readable)` store through `own(...)`; `NetworkImage.url(Readable)` does the same. This was the reported live bug. |

## What is still worth doing

Gaps 2 and 3 are the only two that still drop a live resource. Neither is caused by the ownership
model; both are a factory building into a throwaway container and forgetting the handles. They are
candidates for their own change.

Gaps 1, 6 and 7 keep an empty ownership stack around a factory call. That is now harmless for the
resources those factories create, but wrapping them in an explicit `OwnershipScope` would make the
guarantee structural rather than incidental.
