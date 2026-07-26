# AndroidX RmR — upstream functional audit contract

This contract makes the fork boundary, upstream drift, artifact output, and
test observations auditable. It does not claim behavioural compatibility,
performance improvement, or licence compatibility without the required proof.

## Scope and source of truth

- The audited tree is the current `androidx_RmR` commit.
- The comparison control is the configured `androidx/androidx` ref, fetched in
  CI and recorded by immutable commit ID and merge base.
- The comparison is read-only. CI never merges, rebases, pushes, or alters the
  upstream remote.
- `rmr/audit/androidx_boundary_allowlist.json` is the checked-in policy that
  defines which fork-owned and governance paths are permitted outside upstream.
  Any changed path outside that policy is a boundary violation and fails CI.

## Evidence pipeline

1. Fetch the upstream ref and resolve the merge base. Failure to do so is
   `TOKEN_VAZIO`, not a substitute comparison.
2. Classify every fork delta against the boundary policy. The report preserves
   every path and the reason it is allowed or rejected.
3. Inventory the declared RmR Gradle modules, source counts, build metadata, and
   native-build declarations.
4. Build the seven declared release AARs through `rmr/scripts/build_rmr_release.sh`.
5. Execute the documented `:rmr:rmr-core:test` and `:rmr:rafaelia:test` tasks.
6. Hash every produced AAR, then re-write and upload the report with the actual
   build/test observations.

Hosted CI supplies a bounded Gradle JVM (`-Xms1g -Xmx5g`) and two workers through
environment variables. This avoids treating a workstation-sized heap declaration
as a portable CI requirement, without changing the repository-wide Gradle policy.

If the public runner cannot resolve a required AndroidX build-service artifact,
the audit records the build as failed and the dependent test observation as
`TOKEN_VAZIO`, uploads that report, and then fails closed. It never turns an
unavailable build dependency into a green functional claim.

The scheduled audit reports upstream age after 30 days as `REVIEW_REQUIRED`.
That is a deliberate review signal rather than a fabricated compatibility result.

## Evidence states

| State | Meaning |
| --- | --- |
| `OBSERVED_IN_CI` | The named build or test command ran and returned success. |
| `STATIC_EVIDENCE` | Module registration or source/build metadata was found. |
| `BUILD_ARTIFACT_EVIDENCE` | AAR files were produced and SHA-256 inventoried. |
| `LEGAL_REVIEW_REQUIRED` | Notices/language signals need qualified legal review; no conclusion is implied. |
| `TOKEN_VAZIO` | Evidence is absent or insufficient; it is never a pass. |

An AAR hash proves the particular package output, not its runtime semantics. Tree
comparison and unit tests also do not prove AndroidX behavioural compatibility or
performance. Those claims remain `TOKEN_VAZIO` until a controlled, reviewable
test or benchmark establishes them.

## Generated artifacts

- `androidx_audit_summary.json`: provenance, upstream age, policy outcome, and
  workflow observations.
- `androidx_upstream_delta.json` and `.md`: file-level delta and boundary class.
- `rmr_capability_inventory.json`: expected modules, source inventory, AAR hashes,
  and legal-notice inventory.
- `rmr_claims.json`: claim-by-claim evidence states.

## Legal and operational boundary

The audit inventories the root license file and the RmR authorship/legal-notice
files. It does not decide compatibility, enforcement, or distribution rights.
External AndroidX material remains an external dependency and this CI does not
copy it into another distribution or alter its license notices.

This separation lets operational review be strict without presenting a source
diff, a package build, or a legal notice as more proof than it is.
