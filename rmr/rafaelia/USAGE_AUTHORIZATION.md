# USAGE AUTHORIZATION FRAMEWORK
# Rafaelia Module - RmR Library Suite
# Copyright (C) 2026 Rafael Melo Reis

## PURPOSE

This document establishes the technical and legal framework for authorizing usage of the Rafaelia module, which is restricted to authorized users as specified in LEGAL_NOTICE.md.

## AUTHORIZATION MECHANISMS

### 1. Runtime Authorization Check

The Rafaelia module implements authorization validation using a single supported runtime path:

#### Layer 1: User Identity Verification
```
Check system property: user.name
Expected value: "Rafael Melo Reis"
Fallback: License record file verification
```

#### Layer 2: License Record Verification (single supported path)
```
Path source: -Drafaelia.license.path=<absolute-path>
Recommended location: app-internal/private storage (Context.getFilesDir())
Format: key=value license record (see README.md)
Validation: Required fields + optional SHA-256 payload signature
```

External/shared storage fallback locations are intentionally unsupported by runtime validation.
If import from external media is necessary, application code should use SAF and then copy data into app-internal storage.

### 2. Build-Time Authorization

The module build system verifies authorization during compilation:

```gradle
buildConfigField "String", "AUTHORIZED_USER", "\"Rafael Melo Reis\""
buildConfigField "boolean", "ENFORCE_RESTRICTIONS", "true"
```

Build will fail if:
- Authorization file is missing or invalid
- Cryptographic signature verification fails
- Network authorization server denies access
- Hardware binding check fails

### 3. Deployment Authorization

Production deployments require additional verification:

```
Deployment Key: Unique per-deployment authorization
Distribution: Only through authorized channels
Validation: Server-side verification on first launch
Monitoring: Continuous usage tracking
```

## AUTHORIZATION FILE FORMAT

### Supported Authorization Record

Location: path provided through `-Drafaelia.license.path=<absolute-path>`.

Storage requirement:
- Keep the file in app-internal/private storage.
- Avoid external/shared storage to keep zero runtime permission requirements.

Format (key=value):
```
product=RAFAELIA_CORE
authorized_user=Rafael Melo Reis
license_id=<unique-id>
issued_at=2026-01-01T00:00:00Z
expires_at=2027-01-01T00:00:00Z
signature_sha256=<sha256-of-canonical-payload>
```

## OBTAINING AUTHORIZATION

### For Rafael Melo Reis (Copyright Holder)

Authorization is automatically granted. To generate authorization file:

```bash
# Generate personal authorization
./tools/generate_authorization.sh --user "Rafael Melo Reis" --duration 365d

# Generate hardware-bound authorization
./tools/generate_authorization.sh --user "Rafael Melo Reis" --hardware-bind

# Generate permanent authorization
./tools/generate_authorization.sh --user "Rafael Melo Reis" --permanent
```

### For Third Parties

Third-party authorization requires:

1. **Written Request**: Submit via authorized channels
2. **Legal Agreement**: Sign license agreement with terms
3. **Payment**: License fee based on usage tier
4. **Verification**: Identity and organizational verification
5. **Issuance**: Receive signed authorization token

Contact: [Authorization contact to be specified]

## AUTHORIZATION TIERS

### Tier 1: Personal Use (Rafael Melo Reis Only)
- **Cost**: N/A (Copyright holder)
- **Scope**: Unlimited personal and commercial use
- **Duration**: Permanent
- **Deployments**: Unlimited
- **Support**: Self-support

### Tier 2: Development License
- **Cost**: USD $10,000/year
- **Scope**: Development and testing only
- **Duration**: 1 year renewable
- **Deployments**: Up to 10 development instances
- **Support**: Email support (48-hour response)

### Tier 3: Commercial License
- **Cost**: USD $50,000/year + 5% revenue share
- **Scope**: Production deployment
- **Duration**: 1 year renewable
- **Deployments**: Up to 1,000 production instances
- **Support**: Priority support (24-hour response)

### Tier 4: Enterprise License
- **Cost**: Custom pricing (minimum USD $250,000/year)
- **Scope**: Unlimited production deployment
- **Duration**: Multi-year agreements available
- **Deployments**: Unlimited
- **Support**: Dedicated support team (4-hour response)

## AUTHORIZATION MONITORING

### Telemetry Collection

Authorized installations collect anonymized usage data:

```
- Module initialization events
- Operation frequency and types
- Performance metrics
- Error rates and types
- Hardware configuration
- Geographic location (country-level)
```

All telemetry respects privacy regulations (GDPR, CCPA).

### Violation Detection

The module automatically detects:

- Missing or invalid authorization
- Expired authorization tokens
- Hardware binding mismatches
- Unauthorized code modifications
- Tampering with validation mechanisms
- Deployment beyond authorized limits

Detected violations trigger:
1. Immediate functionality disable
2. Local violation logging
3. Remote violation reporting
4. Legal notification generation
5. Automatic penalty calculation

## AUTHORIZATION RENEWAL

### Automatic Renewal

Authorized users with active licenses:
- Receive renewal notification 30 days before expiration
- Can enable automatic renewal
- Authorization automatically updated upon payment

### Manual Renewal

Process:
1. Submit renewal request via authorization portal
2. Update payment information if necessary
3. Receive new authorization token
4. Deploy updated authorization file

## REVOKING AUTHORIZATION

Authorization may be revoked:

### By License Holder (Rafael Melo Reis)
- Breach of license terms
- Unauthorized distribution
- Security concerns
- Non-payment of license fees
- End of license period

### By Licensee
- Voluntary termination
- Change in usage requirements
- Migration to alternative solutions

Revocation process:
1. Notice provided (minimum 30 days for active violations)
2. Authorization token invalidated
3. Module functionality disabled
4. Final usage report generated
5. Refund processed (if applicable, pro-rated)

## SECURITY CONSIDERATIONS

### Token Security

Authorization tokens must be:
- Stored securely (encrypted at rest)
- Transmitted securely (TLS 1.3+)
- Access-controlled (file permissions 600)
- Rotated periodically (recommended: 90 days)
- Backed up securely

### Breach Response

In case of authorization token compromise:
1. Immediately revoke compromised token
2. Generate new token with different ID
3. Audit all usage with compromised token
4. Investigate unauthorized access
5. Update security procedures

## COMPLIANCE REQUIREMENTS

### Legal Compliance

All authorized users must:
- Comply with license terms (Apache 2.0 + Additional Restrictions)
- Maintain accurate usage records
- Report security incidents within 24 hours
- Submit to periodic audits if requested
- Update authorization before expiration

### Technical Compliance

All deployments must:
- Use official unmodified binaries
- Maintain authorization file integrity
- Enable telemetry collection
- Apply security updates within 30 days
- Use supported Android/Java versions

### Audit Rights

Copyright holder reserves right to:
- Audit usage at any time with 7-day notice
- Access deployment logs and metrics
- Verify compliance with license terms
- Inspect hardware and software configurations
- Review source code using the module

## CONTACT INFORMATION

### Authorization Support
- Email: authorization@rafaelia.rmr.dev (to be configured)
- Portal: https://authorization.rafaelia.rmr.dev (to be configured)
- Support Hours: 24/7 for Tier 3+ licenses

### Legal Inquiries
- Email: legal@rafaelia.rmr.dev (to be configured)
- See LEGAL_NOTICE.md for additional contact information

### Technical Support
- Email: support@rafaelia.rmr.dev (to be configured)
- Documentation: https://docs.rafaelia.rmr.dev (to be configured)
- GitHub Issues: For Tier 2+ licenses only

## VERSION HISTORY

- Version 1.0 (January 3, 2026): Initial authorization framework
- Future versions will be documented here

---

This authorization framework is subject to the terms and conditions specified in LEGAL_NOTICE.md and may be updated without prior notice.

Copyright (C) 2026 Rafael Melo Reis. All Rights Reserved.


## ANDROID STORAGE AND PERMISSIONS

- Runtime authorization path requires no storage runtime permission.
- Do not request `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE` for this flow.
- When external selection is needed, use SAF at app layer, then persist/copy content to app-internal storage before module initialization.
