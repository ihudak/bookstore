---
name: upgrade
description: >
  Upgrade one or more components (libraries, frameworks, languages, build tools, CI/CD actions)
  in the current repository to a target version. Triggered by the "upgrade:" command followed
  by space-separated tokens in the form "component:version", "component:minor", "component:latest",
  "component:lts", or bare "component" (latest compatible assumed). Works on any project regardless of
  language or ecosystem. Upgrades are applied to the current branch with no commits or PRs.
  Runs tests before and after each upgrade; updates test code if required by the new version.
  Use when the user wants to keep dependencies, frameworks, languages, or CI/CD actions current.
---

# Upgrade Components

## Overview

Upgrade one or more components in the current repository to a requested version, verify the project still builds and tests stay green, and leave all changes uncommitted on the current branch.

## Input format

```
upgrade: <token> [<token> ...]
```

Each token is one of:

| Token form | Meaning |
|---|---|
| `component:1.2.3` | Upgrade to exactly this version |
| `component:minor` | Latest patch release within the current major.minor line |
| `component:latest` | Absolute latest stable release |
| `component:lts` | Latest Long-Term Support release |
| `component` (no colon) | **Latest version compatible with everything else in the repo** |

`component` can be a library name, framework name, language runtime, build tool, or a file-system path (e.g. `.github/workflows`).

## Workflow

The workflow has two phases: **Compatibility planning** (no files are changed) followed by **Execution** (changes are applied one component at a time).

### Phase 1 — Compatibility planning (always runs first)

Before touching any files or running any tests:

1. **Inventory** — Detect all components in the repo and record their current versions (build files, runtime version files, CI YAML, etc.). See `references/ecosystems.md`.
2. **Resolve requested versions** — For each token in the command, determine the candidate target version. See "Version resolution" below.
3. **Compatibility check** — For every candidate version, consult the compatibility matrix in `references/compatibility.md` and check the component's own release notes / compatibility pages to verify it works with:
   - All *other* components being upgraded in the same command
   - All *existing* components in the repo that are **not** being upgraded
4. **Detect conflicts** — If any incompatibility is found, **do not proceed**. Instead:
   - Clearly explain the conflict (e.g. "Gradle 9 requires Java 17+, but the repo uses Java 11").
   - Offer concrete, ranked alternatives. See "Conflict resolution options" below.
   - Ask the user to choose before continuing.
5. **Implicit compatibility for bare tokens** — When a token has no version specifier (treated as "latest compatible"), find the highest version that satisfies all constraints from the other requested upgrades *and* the existing repo components. If no version satisfies all constraints, report that and suggest relaxing one of the other constraints.
6. **Confirm plan** — Before making any changes, print the upgrade plan and ask the user to confirm if any version was automatically adjusted or if any companion upgrades were added.

### Conflict resolution options

When a conflict is detected, always present at least two alternatives:

**Option A** — Lower the conflicting component to the highest version that is compatible:
> "Gradle 8.13 is the latest version compatible with Java 11 and Spring Boot 2.7. Upgrade Gradle to 8.13 instead?"

**Option B** — Upgrade the blocking dependency too (suggest what version):
> "Alternatively, upgrade Java to 17 and Spring Boot to 3.3 first — then Gradle 9 will be compatible."

**Option C** — If neither is acceptable, skip the component entirely.

Always rank the options so the least-invasive change comes first.

### Phase 2 — Execution (after user confirms the plan)

Process components **one at a time** in the order given. For each component:

1. **Baseline tests** (first component only) - Run the full test suite; record every passing test. This baseline is reused for all subsequent components.
2. **Detect** - Find the component in the repo. See `references/ecosystems.md`. If not found, print a warning and skip.
3. **Plan changes** - Identify all files that must change (build files, lock files, wrapper scripts, config files, Docker base images, CI YAML, etc.).
4. **Apply** - Make the changes. See `references/ecosystems.md` for per-ecosystem update commands.
5. **Related upgrades** - Some upgrades require companion upgrades (e.g. Spring Boot major bump may require Hibernate, Mockito, test framework updates). Apply those automatically and note them in the summary.
6. **Build** - Run the project's build command (skip test execution at this stage). If the build fails, see "Handling build failures".
7. **Test** - Run the full test suite again.
8. **Compare** - Every test that was green in the baseline must still be green.
   - If all previously-green tests pass: proceed to the next component.
   - If some previously-green tests now fail: see "Handling test failures".
9. **Summary** - After all components are processed, print a final summary table. See "Output" below.

## Version resolution

### bare token (no specifier) — "latest compatible"

1. Fetch all stable versions from the registry.
2. Filter to versions compatible with every other component in the repo (both those being upgraded and those staying at their current version). See `references/compatibility.md`.
3. Select the highest version that passes all compatibility constraints.
4. If no version passes, report the conflict and follow "Conflict resolution options".

### `minor` (stay on current major.minor, get latest patch)

1. Read the current version from the build file.
2. Extract `MAJOR.MINOR`.
3. Query the package registry for all versions matching `MAJOR.MINOR.*`.
4. Select the highest stable (non-pre-release) patch version.

### `latest`

Fetch the highest stable release from the registry, then run a compatibility check (Phase 1, step 3) against the rest of the repo. If it is incompatible, treat it the same as a conflict: offer alternatives rather than blindly applying an incompatible version.

### `lts`

Consult the official LTS source for the technology. Use `web_fetch` on:

| Technology | LTS source URL |
|---|---|
| Java (JDK) | `https://api.adoptium.net/v3/info/available_releases` — field `most_recent_lts` |
| Node.js | `https://nodejs.org/dist/index.json` — latest entry with `lts != false` |
| Python | `https://endoflife.date/api/python.json` — latest entry where `eol` is in the future and it is an active LTS line |
| Ruby | `https://endoflife.date/api/ruby.json` |
| Go | `https://endoflife.date/api/go.json` |
| .NET | `https://endoflife.date/api/dotnet.json` |
| Any other | `https://endoflife.date/api/<product>.json` (substitute the product slug) |

If the lookup fails, ask the user for the target LTS version.

### `exact version` (e.g. `1.2.3`)

Use the version as-is. Verify it exists in the registry, then run a compatibility check against the rest of the repo before applying. If incompatible, report the conflict and present alternatives — **never silently downgrade or ignore an explicit version request**; always surface the incompatibility to the user.

## Handling test failures

If previously-green tests fail after an upgrade:

1. **Inspect** the failures — determine whether they are caused by a breaking API change in the upgraded component (e.g. a renamed class, changed method signature, removed annotation).
2. **Auto-fix test code** if the fix is straightforward (rename import, update assertion syntax, adjust configuration). Explain every test change in the summary.
3. If the failures cannot be auto-fixed, present them clearly and ask the user:
   > "These tests were passing before the upgrade. Would you like me to:
   > (1) Keep the upgrade and leave the failing tests for you to fix,
   > (2) Revert this upgrade and skip it,
   > (3) Investigate further before deciding?"
4. Honor the user's choice. If they choose (1), note the failing tests prominently in the final summary.

## Handling build failures

If the build fails after applying the upgrade:

1. Read the full error output.
2. Attempt an automatic fix (wrong API, missing plugin version, incompatible config).
3. If unfixable, revert the change for this component, warn the user, and continue with the next component.

## Special case: `.github/workflows` (GitHub Actions)

When the component is a directory path matching `**/.github/workflows` or similar CI/CD config paths:

1. Scan every `.yml`/`.yaml` file in the directory for `uses: owner/action@ref` declarations.
2. For each action, fetch the latest release tag from the GitHub API:
   ```
   GET https://api.github.com/repos/<owner>/<action>/releases/latest
   ```
   Use `gh api repos/<owner>/<action>/releases/latest --jq .tag_name` if `gh` CLI is available.
3. Replace the `@ref` with the latest tag (e.g. `actions/checkout@v3` becomes `actions/checkout@v4`).
4. Also update any pinned SHA references to match the new tag's HEAD commit.
5. Validate the updated YAML is syntactically correct.
6. There is no test suite to run for CI/CD files — instead, report a summary of every action upgraded and flag any action where the major version changed (potential breaking change).

## Output

After processing all components, print a summary table:

```
## Upgrade Summary

| Component       | Before  | After   | Status  | Notes                        |
|-----------------|---------|---------|---------|------------------------------|
| springboot      | 3.1.4   | 3.3.11  | OK      | Also upgraded hibernate 6.4  |
| java            | 17      | 21      | OK      | Updated 2 test files         |
| redis           | -       | -       | SKIPPED | Not found in project         |
| gradle          | 8.4     | 8.13    | OK      |                              |

Tests: 142 passed, 0 regressions (baseline: 142 passing)
```

All changes are left **uncommitted** on the current branch.

## Resources

- `references/ecosystems.md` - Detection patterns, update commands, and registry query patterns for every supported ecosystem.
- `references/lts-sources.md` - LTS lookup reference for common runtimes and frameworks.
- `references/compatibility.md` - Known compatibility constraints between common components (Java, Gradle, Maven, Spring Boot, Node, etc.) and how to look up compatibility dynamically.
