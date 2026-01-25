# RmR Navigation & MVP Architecture Guide

## Purpose

This document defines a **formal, professional navigation structure** and a **clean MVP (Model–View–Presenter) layout** for the RmR modules. It is intended to be the authoritative guide for architecture, ownership boundaries, and low-level implementation discipline inside the `rmr/` directory.

---

## 1. Architectural Goals

1. **Low-level determinism**: state is represented as matrices or fixed arrays, not object graphs.
2. **Minimal runtime overhead**: avoid reflection, avoid implicit allocations in hot paths.
3. **Strict module boundaries**: each module is a functional unit with a clear responsibility.
4. **Explicit ownership**: the `rmr/` tree is authored and maintained by Rafael Melo Reis, with strict separation from upstream AndroidX.

---

## 2. Navigation Structure (Module Layout)

### Primary Modules

| Module | Responsibility | Primary State Type |
| --- | --- | --- |
| `rmr-core` | Matrix primitives, determinism, math | `RmRMatrix`, `RmRState` |
| `rmr-lifecycle` | Lifecycle state encoding | `RmRLifecycleState` |
| `rmr-navigation` | Navigation flow & backstack modeling | `RmRNavigationState` |
| `rmr-preference` | Preference state encoding | `RmRPreferenceState` |
| `rmr-room` | Room/database state modeling | `RmRRoomState` |
| `rafaelia` | Ultra low-level, native/SIMD optimization | Native C++/JNI |

### Formal Navigation Flow

1. **Entry**: `rmr-core` provides base matrix primitives.
2. **Domain layer**: `rmr-*` modules map domain concepts to matrices.
3. **Integration**: Each module exposes deterministic, low-level APIs that can be wired to AndroidX adapters.
4. **Optimization**: `rafaelia` optionally accelerates hot paths and SIMD execution.

---

## 3. MVP Mapping (Model–View–Presenter)

RmR uses MVP with a **low-level model** and **thin, explicit presenters**.

### Model (M)
*Definition:* Pure matrix state with deterministic transforms.

**Examples**
- `RmRMatrix`
- `RmRState`
- `RmRNavigationState`

**Rules**
- No UI references
- No framework objects
- Immutable/functional transforms wherever possible

### View (V)
*Definition:* Platform adapters or UI targets. Views must remain out of `rmr/` modules.

**Rules**
- Views live in application layers, not inside `rmr/`
- RmR modules do not depend on UI frameworks

### Presenter (P)
*Definition:* Lightweight orchestrators that apply deterministic transforms to models.

**Rules**
- No I/O
- No reflection
- No caching outside fixed buffers
- Must remain testable via direct matrix operations

---

## 4. Low-Level Constraints (Formal)

1. **Hot paths must not allocate**
2. **Inner loops must be branch-minimized**
3. **Matrix operations must prefer contiguous access**
4. **Native acceleration is optional but must be deterministic**
5. **No global state except explicit static constants**

---

## 5. Navigation Example (Conceptual)

```java
// Model: deterministic navigation state
RmRState navState = RmRState.forNavigation();

// Presenter: deterministic transform
RmRState next = navState.transform();

// View (outside rmr/): update UI using computed vectors
double[] vector = next.computeDeterministicPoint(input);
```

This example demonstrates the MVP separation:
- Model: `RmRState`
- Presenter: deterministic transformation
- View: external consumer of vectors (outside RmR)

---

## 6. Authorship & Legal Separation

1. **RmR modules are authored under the RmR namespace** and are **not** direct copies of AndroidX source files.
2. **Upstream AndroidX remains under Apache 2.0** as governed by the root `LICENSE.txt`.
3. **`rafaelia` carries additional restrictions** defined in `rafaelia/LEGAL_NOTICE.md`.

If a file incorporates upstream or third‑party content, it **must** be listed in `AUTHORSHIP_AND_LICENSE.md`.

---

## 7. Documentation Navigation Map

For broader documentation and legal structure:

- `DOCUMENTATION_INDEX.md`
- `AUTHORSHIP_AND_LICENSE.md`
- `README.md`
- `REFACTORING_SUMMARY.md`

---

## 8. Change Control

Any change to MVP responsibilities, module boundaries, or low-level constraints requires:

1. Update to this guide
2. Update to `DOCUMENTATION_INDEX.md`
3. Update to `AUTHORSHIP_AND_LICENSE.md` if licensing/ownership changes
