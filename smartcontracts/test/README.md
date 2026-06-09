# Test Layout

Foundry discovers tests recursively under this directory.

| Folder | Purpose |
| --- | --- |
| `helpers/` | Shared fixtures, base contracts, handlers, and reusable test utilities. |
| `unit/` | Focused tests for a single contract or narrow behavior surface. |
| `integration/` | End-to-end scenarios that exercise multiple contracts together. |
| `fuzz/` | Property-style fuzz tests with randomized inputs. |
| `invariant/` | Stateful invariant tests and their handlers. |

Keep new tests close to the behavior they verify. Shared setup belongs in
`helpers/` only when it is used by more than one suite.
