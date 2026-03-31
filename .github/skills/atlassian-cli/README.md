# Atlassian CLI (acli) skill

## Capabilities

- Authenticate to Jira Cloud using API tokens.
- Search work items by project, status category, assignee, type, labels, and recency using JQL.
- Update work items (summary, description, labels, assignee, type) in bulk with JQL. Use ADF JSON for descriptions; Markdown may render as literal text.
- Transition work items to new statuses safely.
- List and manage comments via `workitem comment` subcommands.
- Generate or consume JSON for bulk edits. For creations that require custom fields (e.g., owning Program), include them via `additionalAttributes` when using `--from-json`. When assigning “to me,” use the actual email (from env/config) instead of `@me` to avoid misassignment.
- Provide a helper script to generate JQL and ready-to-run search commands.

## Use cases

- Fetch all tickets in a project (open, closed, or all).
- List only your tickets and triage them quickly.
- Bulk reassign or label work items in a project.
- Move sets of issues to In Progress or Done.
- Inspect comments on a work item or add a new note.
- Automate repetitive Jira maintenance from the terminal.

## Key files

- SKILL.md: Core workflows and commands.
- references/jql.md: JQL recipes and recommended field sets.
- references/api_reference.md: Command cheat sheet.
- scripts/example.py: JQL and command generator.
