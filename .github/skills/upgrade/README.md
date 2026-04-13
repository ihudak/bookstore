# `upgrade:` skill

A GitHub Copilot CLI skill that upgrades components in your repository — libraries, frameworks, language runtimes, build tools, and CI/CD actions — with a single command.

Works on **any project** regardless of language or ecosystem (Java/Gradle/Maven, Node.js/npm, Python/pip/poetry, Go, .NET, Ruby, Rust, PHP, Docker, GitHub Actions, and more).

---

## Quick start

```
upgrade: <component>[:<version>] [<component>[:<version>] ...]
```

**Examples:**

| Command | What it does |
|---|---|
| `upgrade: java:lts` | Upgrades Java to the current LTS release (e.g. 21) |
| `upgrade: springboot:minor` | Upgrades Spring Boot to the latest patch within the current major.minor (e.g. 3.1.x → 3.1.11) |
| `upgrade: gradle:latest` | Upgrades the Gradle wrapper to the latest release |
| `upgrade: lodash:4.17.21` | Pins lodash to exactly 4.17.21 |
| `upgrade: requests` | Upgrades Python `requests` to the latest stable release |
| `upgrade: .github/workflows` | Upgrades all GitHub Actions to their latest versions |
| `upgrade: java:lts springboot:minor gradle:latest` | Upgrades all three, one at a time |

---

## Version specifiers

| Specifier | Meaning |
|---|---|
| `lts` | Latest Long-Term Support release from the official source |
| `latest` | Absolute latest stable release |
| `minor` | Latest patch within the current `MAJOR.MINOR` line |
| `1.2.3` | Exact version |
| *(omitted)* | Same as `latest` |

---

## What happens

The skill runs in two phases: compatibility planning first, execution second.

### Phase 1 — Compatibility planning (no files changed)

1. **Inventory** — Scans the repo and records current versions of all detected components.
2. **Resolve** — Determines the candidate target version for each requested component.
3. **Compatibility check** — Verifies every candidate version is compatible with the rest of the stack (other requested upgrades *and* existing components staying put). Consults official compatibility docs dynamically.
4. **Conflict handling** — If an incompatibility is found, execution stops. The skill explains the conflict and presents ranked alternatives (e.g. a lower compatible version, or a co-upgrade that unblocks the request). You choose before anything changes.
5. **Confirm plan** — The full upgrade plan (including any auto-added companions) is shown for confirmation.

### Phase 2 — Execution (after you confirm)

6. **Baseline** — The test suite is run. Every passing test is recorded.
7. **Upgrade** — Components are upgraded one at a time. The minimal set of files is changed. Companion dependencies are upgraded automatically where required.
8. **Build** — The project is compiled/assembled after each upgrade.
9. **Test** — The test suite is re-run after each upgrade. Tests that were green before **must stay green**. If they don't, the skill attempts to auto-fix test code; if that's not possible, it asks you what to do.
10. **Summary** — A table is printed showing every component, the version change, and the outcome.

All changes are left **uncommitted on the current branch**. No commits, no branches, no PRs.

---

## Component detection

The skill automatically detects the ecosystem from files in the repo:

| File | Ecosystem |
|---|---|
| `build.gradle` / `build.gradle.kts` | Gradle |
| `pom.xml` | Maven |
| `package.json` | npm / yarn / pnpm |
| `requirements.txt` / `pyproject.toml` / `Pipfile` | Python |
| `go.mod` | Go |
| `*.csproj` / `global.json` | .NET |
| `Gemfile` | Ruby |
| `Cargo.toml` | Rust |
| `Dockerfile` | Docker base images |
| `.github/workflows/*.yml` | GitHub Actions |

---

## GitHub Actions upgrade

```
upgrade: .github/workflows
```

Scans every workflow file for `uses: owner/action@ref` lines and upgrades each to the latest release. Also updates SHA-pinned references. Reports any major-version bumps (potential breaking changes) prominently.

---

## Multiple components

```
upgrade: java:lts springboot:minor node:lts
```

Components are upgraded **one at a time in order**. After each upgrade, the build and tests are verified before the next component is started. If an upgrade fails or breaks tests, you are asked what to do before proceeding.

---

## Compatibility checking

Before any files are changed, the skill checks that requested versions are compatible with each other and with the existing stack.

**Incompatible explicit versions** — if you request two versions that won't work together (e.g. `upgrade: gradle:9 java:11` when Gradle 9 requires Java 17+), the skill stops and offers alternatives:
- The highest Gradle version that works with Java 11, **or**
- Upgrading Java to 17 so Gradle 9 becomes compatible

**Bare token (no version)** — treated as "latest compatible", not "absolute latest". The skill finds the highest version that works with everything else in the repo. For example, `upgrade: gradle` with Java 11 in the repo will resolve to Gradle 8.x, not Gradle 9.

**`latest` with incompatible stack** — if the absolute latest release is incompatible with the rest of the repo, the skill reports the conflict and suggests either a lower compatible version or what else would need to change.

The compatibility check consults live official documentation (e.g. the Gradle compatibility page, Spring Boot BOM, npm `engines` fields) rather than a static table, so it stays accurate as new releases come out.

---

## When a component isn't found

If a requested component is not detected in the repo, the skill prints a warning and moves on to the next one.

---

## Test failures after upgrade

If a previously-green test fails after an upgrade:

1. The skill inspects the failure and attempts an automatic fix (e.g. a renamed API, updated import).
2. If it can fix the test, it does so and explains what changed.
3. If it cannot, it asks you:
   - **Keep the upgrade** and leave the failing test for you to fix
   - **Revert this upgrade** and skip it
   - **Investigate further** before deciding

---

## Requirements

- GitHub Copilot CLI installed and authenticated
- The repository must have a working test suite (tests are run with the project's existing test command)
- For GitHub Actions upgrades: the `gh` CLI must be installed and authenticated (for querying release tags)

---

## Tips

- Run `upgrade: .github/workflows` periodically to keep your CI/CD actions current.
- Use `upgrade: <framework>:minor` before a major version jump to ensure you're on the latest patch first.
- For Java, `upgrade: java:lts` updates all version declarations consistently — build files, `.sdkmanrc`, `.java-version`, `.tool-versions`, Dockerfiles, and GitHub Actions workflow files.
- When upgrading Spring Boot across major versions, check the generated summary for companion dependency changes and review the linked migration guide.
