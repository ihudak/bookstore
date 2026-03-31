#!/usr/bin/env python3
"""
Jira JQL helper for Atlassian CLI.

Build a safe, reusable JQL query (and acli command) for common ticket searches.

Examples:
  python3 scripts/example.py --project TEAM --scope open
  python3 scripts/example.py --project TEAM --assignee me --types Bug,Task --updated-days 7
  python3 scripts/example.py --project TEAM --scope closed --output jql
"""

from __future__ import annotations

import argparse


DEFAULT_FIELDS = "key,summary,assignee,priority,status,updated"


def build_jql(args: argparse.Namespace) -> str:
    clauses = [f"project = {args.project}"]

    if args.scope == "open":
        clauses.append("statusCategory != Done")
    elif args.scope == "closed":
        clauses.append("statusCategory = Done")

    if args.assignee:
        if args.assignee == "me":
            clauses.append("assignee = currentUser()")
        else:
            clauses.append(f"assignee = \"{args.assignee}\"")

    if args.types:
        types_list = ", ".join(args.types)
        clauses.append(f"issuetype in ({types_list})")

    if args.labels:
        labels_list = ", ".join(args.labels)
        clauses.append(f"labels in ({labels_list})")

    if args.updated_days:
        clauses.append(f"updated >= -{args.updated_days}d")

    jql = " AND ".join(clauses)

    if args.order:
        jql = f"{jql} ORDER BY {args.order}"

    return jql


def main() -> None:
    parser = argparse.ArgumentParser(description="Build JQL and acli search commands.")
    parser.add_argument("--project", required=True, help="Project key, e.g., TEAM")
    parser.add_argument(
        "--scope",
        choices=["open", "closed", "all"],
        default="open",
        help="Ticket scope (default: open)",
    )
    parser.add_argument("--assignee", help="User email, account ID, or 'me'")
    parser.add_argument("--types", type=lambda v: v.split(","), help="Comma-separated issue types")
    parser.add_argument("--labels", type=lambda v: v.split(","), help="Comma-separated labels")
    parser.add_argument("--updated-days", type=int, help="Updated within N days")
    parser.add_argument(
        "--order",
        default="updated DESC",
        help="Order by clause (default: updated DESC)",
    )
    parser.add_argument(
        "--fields",
        default=DEFAULT_FIELDS,
        help=f"Fields for acli output (default: {DEFAULT_FIELDS})",
    )
    parser.add_argument(
        "--output",
        choices=["command", "jql"],
        default="command",
        help="Output only JQL or a full acli command (default: command)",
    )
    parser.add_argument(
        "--paginate",
        action="store_true",
        help="Include --paginate in the acli command",
    )

    args = parser.parse_args()

    jql = build_jql(args)

    if args.output == "jql":
        print(jql)
        return

    paginate_flag = " --paginate" if args.paginate else ""
    print(
        "acli jira workitem search --jql "
        f"'{jql}' --fields '{args.fields}'{paginate_flag}"
    )


if __name__ == "__main__":
    main()
