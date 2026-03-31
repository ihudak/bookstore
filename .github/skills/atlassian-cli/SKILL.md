---
name: atlassian-cli
description: "Use Atlassian CLI (acli) to manage Jira Cloud work items: authenticate, search/fetch tickets by project and status, update/edit fields, transition status, assign, comment, and perform safe bulk operations with JQL. Trigger this skill when the user asks to list Jira tickets, change status, edit issues, or automate Jira workflows via acli."
---

# Atlassian CLI (acli) for Jira work items

## Overview

Use this skill to quickly authenticate, query, and update Jira work items with Atlassian CLI. Focus on JQL-driven search, safe bulk edits, and fast status changes.

## Quick start

1. Verify acli is installed:
	 - Run `acli --version`
2. Authenticate with an API token:
	 - Run `echo "$ATLASSIAN_API_TOKEN" | acli jira auth login --site "<site>.atlassian.net" --email "<email>" --token`
3. Confirm access:
	 - Run `acli jira workitem search --jql "project = <KEY>" --limit 5`
4. If creation/editing requires custom fields (e.g., owning Program for some issuetypes), capture the field key and allowed option value before running create/edit.

## Workflow decision tree

1. Determine intent:
	 - **Fetch tickets** → Use **Search workflow**
	 - **Edit/update fields** → Use **Edit workflow**
	 - **Change status** → Use **Transition workflow**
	 - **Read comments** → Use **Comment workflow**

## Search workflow (fetch tickets by project)

1. Get the project key(s).
2. Choose scope:
	 - **Open**: `statusCategory != Done`
	 - **Closed**: `statusCategory = Done`
	 - **All**: no status filter
3. Build JQL with optional filters (assignee, type, labels).
4. Run search with a useful field set and pagination as needed.

### Search templates

- **Open tickets by project**
	- `acli jira workitem search --jql "project = <KEY> AND statusCategory != Done" --fields "key,summary,assignee,priority,status" --paginate`
- **Closed tickets by project**
	- `acli jira workitem search --jql "project = <KEY> AND statusCategory = Done" --fields "key,summary,assignee,priority,status" --paginate`
- **All tickets by project**
	- `acli jira workitem search --jql "project = <KEY>" --fields "key,summary,assignee,priority,status" --paginate`
- **My tickets in a project**
	- `acli jira workitem search --jql "project = <KEY> AND assignee = currentUser()" --fields "key,summary,status" --paginate`

## View workflow (single ticket details)

1. Use `workitem view` with the key as a positional argument (no `--key` flag).
2. Limit fields with `--fields` or use `--json` for structured output.

### View templates

- **View a work item**
	- `acli jira workitem view KEY-123`
- **View selected fields**
	- `acli jira workitem view KEY-123 --fields "key,summary,description,status,assignee,priority"`
- **View in JSON**
	- `acli jira workitem view KEY-123 --fields "key,summary,description,status,assignee,priority" --json`

## Edit workflow (update fields)

1. Identify targets using **key**, **JQL**, or **filter**.
2. Apply edits with `acli jira workitem edit`.
3. For bulk changes, prefer `--jql` + `--yes` and confirm with a `--count` search first.

### Edit templates

- **Update summary and labels**
	- `acli jira workitem edit --key "KEY-123" --summary "New summary" --labels "triage,backend"`
- **Bulk assign via JQL**
	- `acli jira workitem edit --jql "project = <KEY> AND statusCategory != Done" --assignee "user@company.com" --yes`
- **Edit from JSON**
	- `acli jira workitem edit --generate-json`
	- `acli jira workitem edit --from-json "workitem.json"`

### Description formatting (Jira Cloud)
- Jira Cloud renders descriptions from Atlassian Document Format (ADF), not Markdown. Wiki markup may render as literal text in some instances.
- To edit description reliably, build an ADF JSON payload and use `--from-json`.
- Minimal pattern:
```json
{
  "issues": ["KEY-123"],
  "description": {
    "version": 1,
    "type": "doc",
    "content": [
      {"type": "heading", "attrs": {"level": 2}, "content": [{"type": "text", "text": "Heading"}]},
      {"type": "bulletList", "content": [
        {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Item 1"}]}]},
        {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Item 2"}]}]}
      ]}
    ]
  }
}
```
- Apply with: `acli jira workitem edit --from-json /tmp/update.json --yes`
- Tip: run `acli jira workitem edit --generate-json` to see the schema. Always include `issues: ["<KEY>"]` and the `description` block.

### Required custom fields on create (e.g., owning Program)
- Some issuetypes (e.g., ValueIncrement) require a custom field to be set at creation time (e.g., an owning Program select list).
- Use `acli jira workitem create --generate-json` to see the base schema, then add the required custom field under `additionalAttributes` when using `--from-json`.
- Pattern to set a required select field:
```json
{
  "projectKey": "<PROJECT>",
  "summary": "<Summary>",
  "type": "<IssueType>",
  "assignee": "<email>"  // Avoid '@me'; use the actual email from env/config (e.g., ATLASSIAN_USER_NAME)
  "description": { ... ADF ... },
  "additionalAttributes": {
    "<customfield_key>": { "value": "<option label>" }
  }
}
```
- Apply with: `acli jira workitem create --from-json /tmp/create.json`
- Use a real email for assignee when the user says “assign it to me” (avoid `@me` to prevent misassignment). Pull the email from env/config (e.g., `ATLASSIAN_USER_NAME`).
- If creation fails with “owning Program is required” (or similar), add the correct custom field key/value and retry.

## Transition workflow (change status)

1. Confirm valid target status (case-sensitive).
2. Transition by key or JQL.

### Transition templates

- **Transition a single work item**
	- `acli jira workitem transition --key "KEY-123" --status "In Progress"`
- **Bulk transition by JQL**
	- `acli jira workitem transition --jql "project = <KEY> AND status = \"To Do\"" --status "In Progress" --yes`

## Comment workflow (read/add comments)

1. Use `workitem comment` subcommands for comments (the `workitem get` command does **not** take `--key`).
2. List comments by key, optionally in JSON for scripting.
3. Create, update, or delete a comment as needed.

### Comment templates

- **List comments for a work item**
	- `acli jira workitem comment list --key "KEY-123"`
- **List comments in JSON**
	- `acli jira workitem comment list --key "KEY-123" --json`
- **Create a comment**
	- `acli jira workitem comment create --key "KEY-123" --body "<comment>"`

## Safety and quality checks

- Run a dry search first with `--count` or `--limit` to confirm the target set.
- Use `--json` or `--csv` when you need structured output for scripts.
- Use `--paginate` for large projects to avoid truncation.
- If a field is rejected (for example `updated`), remove it from `--fields` and retry.
- For comments, use `workitem comment list` instead of `workitem get`.

## Helper script

- Use `scripts/example.py` to generate JQL or a ready-to-run search command.
	- Example: `python3 scripts/example.py --project TEAM --scope open --paginate`

## References

- Read [references/jql.md](references/jql.md) for JQL recipes, status scopes, and field sets.
- Read [references/api_reference.md](references/api_reference.md) for acli Jira command flags and examples.
