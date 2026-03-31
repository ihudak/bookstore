# Atlassian CLI Jira command cheat sheet

Use this reference when you need a quick reminder of common Jira work item commands and flags.

## Authentication

- API token:
	- `echo "$ATLASSIAN_API_TOKEN" | acli jira auth login --site "<site>.atlassian.net" --email "<email>" --token`

## Search

- Search by JQL:
	- `acli jira workitem search --jql "project = <KEY>" --paginate`
- Useful flags:
	- `--fields "key,summary,assignee,priority,status"`
	- `--count` (count only)
	- `--limit 50` (limit results)
	- `--json` or `--csv` (structured output)

## Edit

- Edit by key:
	- `acli jira workitem edit --key "KEY-123" --summary "New summary"`
- Edit by JQL:
	- `acli jira workitem edit --jql "project = <KEY>" --assignee "user@company.com" --yes`
- JSON workflows:
	- `acli jira workitem edit --generate-json`
	- `acli jira workitem edit --from-json "workitem.json"`

## Transition (status changes)

- By key:
	- `acli jira workitem transition --key "KEY-123" --status "In Progress"`
- By JQL:
	- `acli jira workitem transition --jql "project = <KEY>" --status "Done" --yes`

## View and comments

- View a work item:
	- `acli jira workitem view --key "KEY-123"`
- Add a comment:
	- `acli jira workitem comment create --key "KEY-123" --body "Your comment"`

## Assign

- Assign to a user:
	- `acli jira workitem assign --key "KEY-123" --assignee "user@company.com"`
- Self-assign:
	- `acli jira workitem assign --key "KEY-123" --assignee "@me"`
