# E2E Test Infra: expedition-skyblock

## Test Philosophy
- Opaque-box, requirement-driven. Derived strictly from `ORIGINAL_REQUEST.md` and user-facing specifications.
- Systematic 4-tier design:
  - **Tier 1 - Feature Coverage**: happy paths for all 16 features.
  - **Tier 2 - Boundary & Corner Cases**: exact boundaries (y = -64, radius = 128, 5.0s combat timer, buffer zone).
  - **Tier 3 - Cross-Feature Combinations**: combat cooldown + /is, visiting locked island + /is kick, multiple /is in Skyblock + /return, void fall with keepInventory.
  - **Tier 4 - Real-World Application Scenarios**: full multiplayer Skyblock lifecycles.

## Feature Inventory
| # | Feature | Source | Tier 1 | Tier 2 | Tier 3 |
|---|---------|--------|:------:|:------:|:------:|
| 1 | Custom Void Dimension | R1 | 5 | 5 | ✓ |
| 2 | Island Allocation & Spacing | R1 | 5 | 5 | ✓ |
| 3 | Starter Island Resources | R1 | 5 | 5 | ✓ |
| 4 | State Persistence | R2 | 5 | 5 | ✓ |
| 5 | Island Teleportation (`/is`) | R2 | 5 | 5 | ✓ |
| 6 | Combat / Falling Cooldown | R2 | 5 | 5 | ✓ |
| 7 | Main World Return (`/return`) | R2 | 5 | 5 | ✓ |
| 8 | Island Home Management (`/is sethome`) | R2 | 5 | 5 | ✓ |
| 9 | Social & Visiting (`/visit`, `/invite`) | R3 | 5 | 5 | ✓ |
| 10 | Lock State & Kicking (`/is lock`, `/is kick`) | R3 | 5 | 5 | ✓ |
| 11 | Island Anti-Grief Protection | R3, R5 | 5 | 5 | ✓ |
| 12 | Per-Dimension keepInventory | R4 | 5 | 5 | ✓ |
| 13 | Void Fall Auto-Rescue | R4 | 5 | 5 | ✓ |
| 14 | Boundary Containment | R5 | 5 | 5 | ✓ |
| 15 | Portal Suppression | R5 | 5 | 5 | ✓ |
| 16 | PvP Combat Cancellation | R5 | 5 | 5 | ✓ |

## Test Architecture
- Test Suite Runner: Standalone or JUnit test harness / Python test script executing unit & integration test scenarios against the compiled mod and logic classes.
- Directory layout:
  - `mods_src/expedition-skyblock/src/test/java/` for JUnit 5 unit & integration suites.
  - `tests/e2e/` for script-based / command simulation suites.

## Coverage Thresholds
- Tier 1: ≥5 per feature
- Tier 2: ≥5 per feature (where boundaries exist)
- Tier 3: pairwise coverage of major feature interactions
- Tier 4: ≥5 realistic application scenarios
