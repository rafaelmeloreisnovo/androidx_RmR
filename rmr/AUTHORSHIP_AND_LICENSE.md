# RmR Authorship, Attribution, and Licensing

## Purpose

This document records **who created what**, **what license applies**, and **how the RmR module relates to the original AndroidX project**. It is meant to prevent plagiarism, make authorship explicit, and keep legal obligations clear.

---

## 1. Scope

This document applies to the `rmr/` directory and its submodules (`rmr-core`, `rmr-lifecycle`, `rmr-navigation`, `rmr-preference`, `rmr-room`, and `rafaelia`).

---

## 2. Original AndroidX (Upstream Project)

- **Project:** AndroidX (Google and contributors)
- **License:** Apache License 2.0
- **Where the license lives in this repo:** [`LICENSE.txt`](../LICENSE.txt)

**Important note:** AndroidX is a separate upstream project. The RmR module is **not** a copy of AndroidX source code. It is a **new implementation and documentation set** that draws on public concepts and API ideas only. If any file ever contains code derived from AndroidX, that file **must** be explicitly listed in the “Derived or Third‑Party Content” section below.

---

## 3. RmR Authorship (This Module)

- **Author:** Rafael Melo Reis
- **Copyright:** © 2026 Rafael Melo Reis
- **License:** Apache License 2.0
- **License file for this module:** [`rmr/LICENSE.md`](LICENSE.md)

### Authorship Scope
All files under `rmr/` are authored by Rafael Melo Reis **unless explicitly stated otherwise** in a per‑file note or in the section below.

### Name and Attribution
The "RmR" name and the author attribution in `rmr/` must remain intact. If a derived work reuses the name, it must preserve attribution and licensing terms defined in this document and in `rmr/LICENSE.md`.

---

## 4. Derived or Third‑Party Content (Required Listing)

If any RmR file includes external or upstream code, **list it here** with full attribution:

| File(s) | Source | License | Notes |
| --- | --- | --- | --- |
| _None currently_ | _N/A_ | _N/A_ | As of this revision, no AndroidX source code or third‑party code is copied into `rmr/`. |

---

## 5. Separation of Contributions

To keep authorship clear:

- **Original AndroidX** content remains in the upstream project. This repo includes it only via licensing terms that apply to AndroidX as a whole.  
- **RmR** content is authored and maintained separately in `rmr/`, with its own module license and documentation.

If a file merges or adapts upstream code, it **must**:

1. Carry a header referencing the original source,
2. Record the original license,
3. State the changes made, and
4. Be listed in Section 4 above.

---

## 6. Legal & Compliance Notes (Informational)

This document is informational and does **not** constitute legal advice. Typical software‑law concepts that apply include:

- **Copyright** (original creative expression)
- **License grants** (permissions and conditions)
- **Attribution requirements** (credit and NOTICE obligations)

Refer to the Apache License 2.0 text for the authoritative conditions. When in doubt, consult qualified legal counsel.

---

## 7. Change Control

Any change to authorship, licensing, or external content must update this document and the table in Section 4.
