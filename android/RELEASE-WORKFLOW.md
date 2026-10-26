# Release delivery

The user tests Pocket Music by downloading APKs from GitHub Releases. Each delivered Android build must include a source push and a matching GitHub release, not just local APK links.

Keep experimental versions marked as prereleases. Upload ARM64 and ARMv7 APKs and a SHA-256 checksum file; explain how to install over the prior build and disclose verification limits. Preserve the existing signing identity. Verify uploaded sizes/digests before publishing. Keep unrelated desktop work and all account/signing secrets out of Android release commits.
