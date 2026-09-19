# Advancement evidence PR verification

This PR retains the provider's existing Java 21 / Paper 1.21.11 compilation baseline. CI now checks the exact PR head on every target branch, runs clean Maven verification without skipping tests, and preserves test reports. It does not deploy or release artifacts.

Original source change remains in feature/commendation-advancement-evidence. Existing player data must be retained. New local/hosted results will be recorded on the PR, not inferred from older test counts.

Fresh PR-cleanup local verification: {"tests":178,"failures":0,"errors":0,"skipped":0}. Clean Maven verify packaged the existing plugin version without gameplay changes. Hosted CI remains a separate check on the pushed head.
