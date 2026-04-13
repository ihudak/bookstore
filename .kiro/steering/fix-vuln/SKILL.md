---
name: fix-vuln
description: >
  Automatically fix security vulnerabilities by updating affected dependencies.
  Triggered by the "vuln:" command followed by space-separated tokens.
  Each token is either JIRA-ID:VULN-ID (e.g. MGD-2423:CVE-2023-46604) or a bare VULN-ID (e.g. CVE-2023-46604) when no Jira ticket exists.
  Handles CVE IDs automatically by looking them up on NVD.
  Skips CWE and OWASP IDs with a warning.
  For each CVE: fetches the affected library, checks if it is used in the repo,
  finds the minimal safe version, runs baseline tests, applies the fix, rebuilds,
  re-runs tests, then commits to a new branch and opens a pull request.
  Use when the user wants to remediate one or more security vulnerabilities in the current repository.
---

# Fix Vulnerabilities

## Overview

Fix one or more CVE vulnerabilities with the smallest possible dependency change, then open a PR per vulnerability.

## Workflow

Process each vulnerability sequentially. For each one:

1. **Parse** - Extract JIRA-ID (optional) and VULN-ID from each input token.
   - With Jira ID: `JIRA-ID:VULN-ID` (e.g. `MGD-2423:CVE-2023-46604`)
   - Without Jira ID: bare `VULN-ID` (e.g. `CVE-2023-46604`)
   - A token is treated as "bare VULN-ID" when it matches a known vulnerability ID pattern and contains no colon, **or** when the part before the first colon does not look like a Jira issue key (Jira keys are `[A-Z]+-\d+`).

   **Determine the no-Jira placeholder** (do this once, before processing any CVEs):
   Scan the last 50 commits (`git log --oneline -50`) and any existing branch names (`git branch -a`) for patterns like `NOJIRA`, `NO-JIRA`, `nojira`, or commits with no issue key at all.
   - If `NOJIRA` (or a variant) appears in the majority of no-ticket commits/branches, use that placeholder in branch names and commit messages where a Jira ID would normally appear.
   - If most no-ticket commits simply omit any issue reference, omit it entirely.
   - If the history is ambiguous or empty, omit the Jira ID rather than invent a placeholder.
2. **Filter** - Skip non-CVE IDs (CWE-*, OWASP `\d{4}:A\d`). Warn the user and continue.
3. **Lookup** - Fetch CVE details from NVD; extract the affected package and vulnerable version range. See `references/nvd-api.md`.
4. **Detect** - Search the repo for the library. See `references/build-systems.md` for per-ecosystem patterns.
5. **Version** - Determine the current version in use and the minimum safe version.
6. **Baseline** - Run the existing test suite; record which tests pass.
7. **Fix** - Apply the minimal version change (prefer a patch/minor bump; avoid major version changes unless unavoidable).
8. **Verify** - Build the project (if applicable) and run the tests again.
9. **Compare** - Diff the before/after test results:
   - Tests that were green before the fix **must stay green**.
   - If previously-green tests fail, **ask the user** whether to proceed, revert, or investigate.
10. **Commit** - Commit to a new branch and open a PR. See "Git Workflow" below.

## Handling Test Failures After Fix

If the fix causes previously-green tests to fail and a quick investigation does not reveal an obvious fix (e.g., the new version changed an API):

- Present the failing tests clearly.
- Ask the user: "These tests were passing before. Would you like me to (1) apply the fix anyway and flag the failures in the PR description, (2) revert the fix, or (3) investigate further?"
- Honor the user's choice.

## Git Workflow

### Branch naming

Inspect recent git history (`git log --oneline -50`) and existing branches (`git branch -a`) to match the project's naming convention.

**When a Jira ID is present**, default to:
```
fix/JIRA-ID-CVE-XXXX-XXXXX
```
Example: `fix/MGD-2423-CVE-2023-46604`

**When no Jira ID is provided**, use the placeholder determined in step 1 (Parse):
```
fix/NOJIRA-CVE-XXXX-XXXXX   (if the project uses NOJIRA)
fix/CVE-XXXX-XXXXX           (if the project omits issue keys)
```

### Commit message

Use the project's existing style (check recent commits). Default template:

**With Jira ID:**
```
fix(deps): upgrade <library> to <version> to remediate <CVE-ID>

Resolves <JIRA-ID>
Fixes <CVE-ID> - <one-line CVE description>

Vulnerable range: <range>
Safe version: <version>

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

**Without Jira ID** (omit the `Resolves` line entirely, or substitute the placeholder if the project uses one):
```
fix(deps): upgrade <library> to <version> to remediate <CVE-ID>

Fixes <CVE-ID> - <one-line CVE description>

Vulnerable range: <range>
Safe version: <version>

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

### PR

- **Base branch**: `main` (fall back to `master` if `main` does not exist).
- **Title**: `fix(deps): <library> upgrade to remediate <CVE-ID>` (append ` [<JIRA-ID>]` only when a Jira ID is present)
- **Body**: Include CVE summary, vulnerable range, the version change made, and test result summary (pass count before vs. after).

## Resources

- `references/nvd-api.md` - NVD REST API usage, response structure, and how to extract affected packages and version ranges.
- `references/build-systems.md` - Per-ecosystem patterns for detecting libraries and updating version pins (Gradle, Maven, npm, pip, Go modules, etc.).
