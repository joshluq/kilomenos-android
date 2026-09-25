---
name: atlassian-bridge
description: Connects agentic workflows with Atlassian Jira and Confluence. Inspects Kanban boards, enforces state guards, refines tickets, transitions statuses, resolves issues, fetches epic summaries, and publishes living specs & technical docs to Confluence.
---

# Atlassian Bridge - Jira & Confluence Integration

Connects our multi-agent Android engineering ecosystem directly to **Atlassian Jira Software** and **Atlassian Confluence**.

---

## 1. Capabilities

- **Kanban Board Querying (`board`)**: Filter active issues by status columns (e.g. `Ready for Dev`, `In Progress`).
- **State Guard Enforcement (`fetch --require-status`)**: Asserts that features are only pulled into development once PO refinement is finished.
- **PO Ticket Refinement (`refine`)**: Injects structured acceptance criteria and transitions tickets into ready states.
- **Lifecycle Transitions (`transition`)**: Synchronizes Jira ticket statuses throughout the engineering lifecycle (with optional `--comment`).
- **Atomic Issue Resolution (`resolve`)**: Posts resolution summary comment and transitions issue to `Listo` / done in a single command.
- **Epic Analysis & Triage (`fetch-epic`)**: Queries all child issues of an Epic for technical retrospectives and conclusions.
- **Technical Documentation Publishing (`publish-doc`)**: Publishes any Markdown technical specification from `docs/` directly to Confluence with native XHTML formatting and hierarchical parent-child relationships.
- **Living Specifications Sync (`sync-confluence`)**: Automatically publishes markdown living specs from `openspec/specs/` to Confluence wiki spaces.
- **Human-in-the-Loop Feedback (`comment`)**: Posts architectural questions and receives decisions directly on tickets.
- **Defect Reporting (`report-crash`)**: Bridges QA test failures and crashes directly into Jira Bug tickets.

---

## 2. Quick Command Reference

```bash
# Check connection
python scripts/atlassian_bridge.py check

# Query board
python scripts/atlassian_bridge.py board --status "Ready for Dev"

# Fetch issue and scaffold OpenSpec proposal
python scripts/atlassian_bridge.py fetch <ISSUE_KEY> --require-status "Ready for Dev" --scaffold-openspec

# Transition issue status (with optional comment)
python scripts/atlassian_bridge.py transition <ISSUE_KEY> "In Progress" --comment "Starting implementation"

# Atomically resolve ticket with completion summary
python scripts/atlassian_bridge.py resolve <ISSUE_KEY> --comment "RCA & Fix verified"

# Fetch all child issues of an Epic
python scripts/atlassian_bridge.py fetch-epic <EPIC_KEY> [--output dump.json]

# Publish or update any Markdown document to Confluence
python scripts/atlassian_bridge.py publish-doc docs/my_spec.md --space KILOMENOS [--title "Title"] [--parent-id <ID>]

# Sync OpenSpec to Confluence Space
python scripts/atlassian_bridge.py sync-confluence
```
