# platform-governance-samples

These samples consume the official `platform-governance-bom` and `platform-governance-starter`
combination and verify that the default stack starts in dev and production profiles.

They register one final `PolicyConfigSource`, a `GovernanceAuditSink`, and a `GovernancePolicyPlugin`.
The security-style policy source is adapted behind the platform `PolicyConfigSource` contract.

Current checks:

- dev profile startup with service identity and audit environment
- prod profile startup with strict engine and fatal violation handler policy
