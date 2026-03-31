# JQL recipes and field sets

## Table of contents

- [Project scope](#project-scope)
- [Open, closed, and all tickets](#open-closed-and-all-tickets)
- [Assignee filters](#assignee-filters)
- [Issue type filters](#issue-type-filters)
- [Sorting and recency](#sorting-and-recency)
- [Suggested field sets](#suggested-field-sets)

## Project scope

- Single project: `project = <KEY>`
- Multiple projects: `project in (<KEY1>, <KEY2>)`

## Open, closed, and all tickets

- Open: `statusCategory != Done`
- Closed: `statusCategory = Done`
- All: no status filter

Combine with a project:

- Open: `project = <KEY> AND statusCategory != Done`
- Closed: `project = <KEY> AND statusCategory = Done`
- All: `project = <KEY>`

## Assignee filters

- Assigned to me: `assignee = currentUser()`
- Assigned to a user: `assignee = "user@company.com"`
- Unassigned: `assignee is EMPTY`

## Issue type filters

- Specific types: `issuetype in (Bug, Task, Story)`
- Single type: `issuetype = Bug`

## Sorting and recency

- Recently updated first: `ORDER BY updated DESC`
- Updated in last 7 days: `updated >= -7d`
- Updated since a date: `updated >= "2024-01-01"`

## Suggested field sets

Use with `--fields`:

- Default quick triage: `key,summary,assignee,priority,status`
- With type and reporter: `key,summary,issuetype,assignee,reporter,status`
- Minimal for export: `key,summary,status`
