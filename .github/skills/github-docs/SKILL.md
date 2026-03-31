---
name: github-docs
description: Investigate GitHub repositories using the gh CLI. Analyze PRs, issues, commits, CI/CD status, repo health, contributor activity, security vulnerabilities, and fetch repo docs/content. Use for any GitHub-related investigation or documentation retrieval.
---

# GitHub Investigation

Investigate repositories using `gh` CLI. All commands assume `gh auth status` passes.

## Quick Reference

### Repository Context
```bash
gh repo view [owner/repo]                    # Overview, description, stars, forks
gh repo view [owner/repo] --json name,description,defaultBranchRef,stargazerCount,forkCount,issues,pullRequests,url
gh api repos/{owner}/{repo}/readme --jq '.content' | base64 -d  # Fetch README
gh api repos/{owner}/{repo}/contents --jq '.[] | "\(.type) \(.name)"'  # List root
gh api repos/{owner}/{repo}/contents/{path}  # List subdirectory
```

### Pull Requests
```bash
gh pr list                                   # Open PRs
gh pr list --state all --limit 20            # Recent PRs (any state)
gh pr view <number>                          # PR details
gh pr view <number> --json title,body,state,author,reviews,comments,commits,files,mergeable,statusCheckRollup
gh pr diff <number>                          # View diff
gh pr checks <number>                        # CI status
gh api repos/{owner}/{repo}/pulls/<number>/comments  # Review comments
gh api repos/{owner}/{repo}/pulls/<number>/reviews   # Reviews with verdicts
```

### Issues
```bash
gh issue list                                # Open issues
gh issue list --state all --limit 30         # All recent issues
gh issue list --label "bug" --label "critical"  # Filter by labels
gh issue view <number>                       # Issue details
gh issue view <number> --json title,body,state,author,comments,labels,assignees,milestone
gh api repos/{owner}/{repo}/issues/<number>/timeline  # Full issue timeline
```

### Commits & History
```bash
gh api repos/{owner}/{repo}/commits --jq '.[0:10] | .[] | "\(.sha[0:7]) \(.commit.author.name): \(.commit.message | split("\n")[0])"'
gh api repos/{owner}/{repo}/commits/<sha>    # Single commit details
gh api repos/{owner}/{repo}/compare/base...head  # Compare branches/commits
git log --oneline -20                        # If in cloned repo
git log --oneline --since="2 weeks ago"      # Recent commits
```

### CI/CD & Workflows
```bash
gh run list --limit 10                       # Recent workflow runs
gh run view <run-id>                         # Run details
gh run view <run-id> --log-failed            # Failed job logs
gh workflow list                             # Available workflows
gh api repos/{owner}/{repo}/actions/runs --jq '.workflow_runs[0:5] | .[] | "\(.status) \(.conclusion // "running") - \(.name)"'
```

### Contributors & Activity
```bash
gh api repos/{owner}/{repo}/contributors --jq '.[0:10] | .[] | "\(.login): \(.contributions) commits"'
gh api repos/{owner}/{repo}/stats/contributors  # Detailed contributor stats
gh api repos/{owner}/{repo}/activity           # Recent activity feed
gh api repos/{owner}/{repo}/commits --jq 'group_by(.commit.author.name) | map({author: .[0].commit.author.name, count: length}) | sort_by(-.count)'
```

### Branches & Releases
```bash
gh api repos/{owner}/{repo}/branches --jq '.[].name'
gh api repos/{owner}/{repo}/branches/<branch>/protection  # Branch protection rules
gh release list --limit 5                    # Recent releases
gh release view <tag>                        # Release details
```

### Security
```bash
gh api repos/{owner}/{repo}/vulnerability-alerts  # Dependabot alerts (requires admin)
gh api repos/{owner}/{repo}/secret-scanning/alerts  # Secret scanning alerts
gh api repos/{owner}/{repo}/code-scanning/alerts    # Code scanning alerts
gh api repos/{owner}/{repo}/dependabot/alerts --jq '.[] | "\(.security_advisory.severity): \(.security_advisory.summary)"'
```

### Search
```bash
gh search repos "keyword" --limit 10         # Search repos
gh search repos "keyword" --owner {org}      # Search within org
gh search issues "error" --repo owner/repo   # Search issues in repo
gh search prs "fix" --repo owner/repo --state merged  # Search merged PRs
gh search code "function_name" --repo owner/repo  # Search code
```

## Investigation Workflows

### PR Deep Dive
1. `gh pr view <n> --json title,body,author,state,commits,files,reviews,statusCheckRollup`
2. `gh pr diff <n>` - examine changes
3. `gh pr checks <n>` - CI status
4. `gh api repos/{o}/{r}/pulls/<n>/reviews` - review verdicts
5. `gh api repos/{o}/{r}/pulls/<n>/comments` - inline comments

### Issue Triage
1. `gh issue list --state open --limit 50 --json number,title,labels,createdAt,author`
2. Group by labels, identify patterns
3. `gh issue view <n>` for details on high-priority items
4. `gh api repos/{o}/{r}/issues/<n>/timeline` for full context

### Repo Health Check
1. `gh repo view --json stargazerCount,forkCount,issues,pullRequests,updatedAt`
2. `gh pr list --state open` - PR backlog
3. `gh issue list --state open` - Issue backlog
4. `gh run list --limit 5` - CI health
5. `gh api repos/{o}/{r}/contributors` - active maintainers
6. `gh release list --limit 3` - release cadence

### Debug CI Failure
1. `gh run list --limit 5` - find failed run
2. `gh run view <id>` - which job failed
3. `gh run view <id> --log-failed` - error logs
4. `gh run rerun <id>` - retry if flaky

## Common Patterns

### JSON Output & jq
Most commands support `--json` for structured output:
```bash
gh pr list --json number,title,author,createdAt --jq '.[] | "\(.number): \(.title) by \(.author.login)"'
```

### Pagination
API endpoints paginate. Use `--paginate` or iterate:
```bash
gh api repos/{o}/{r}/issues --paginate --jq '.[].title'
```

### Cross-Repo Operations
Always specify repo when not in a clone:
```bash
gh pr list --repo owner/repo
gh issue view 123 --repo owner/repo
```
