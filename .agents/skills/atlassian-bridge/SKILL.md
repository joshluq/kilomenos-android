---
name: atlassian-bridge
description: Connects agentic workflows with Atlassian Jira and Confluence. Inspects Kanban boards, enforces state guards, refines tickets with acceptance criteria, transitions statuses, comments for human feedback, and syncs living specs to Confluence.
---

# Atlassian Bridge - Jira & Confluence Integration (KmSafe)

Connects our multi-agent Android engineering ecosystem directly to **Atlassian Jira Software** and **Atlassian Confluence**.

---

## 1. Capabilities

- **Kanban Board Querying (`board`)**: Filter active issues by status columns (e.g. `Ready for Dev`, `In Progress`).
- **State Guard Enforcement (`fetch --require-status`)**: Asserts that features are only pulled into development once PO refinement is finished.
- **PO Ticket Refinement (`refine`)**: Injects structured acceptance criteria and transitions tickets into ready states.
- **Lifecycle Transitions (`transition`)**: Synchronizes Jira ticket statuses throughout the engineering lifecycle.
- **Human-in-the-Loop Feedback (`comment`)**: Posts architectural questions and receives decisions directly on tickets.
- **Defect Reporting (`report-crash`)**: Bridges QA test failures and crashes directly into Jira Bug tickets.
- **Living Specifications Sync (`sync-confluence`)**: Automatically publishes markdown living specs from `openspec/specs/` to Confluence wiki spaces.

---

## 2. Quick Command Reference

```bash
# Check connection
python scripts/atlassian_bridge.py check

# Query board
python scripts/atlassian_bridge.py board --status "Ready for Dev"

# Fetch issue and scaffold OpenSpec proposal
python scripts/atlassian_bridge.py fetch <ISSUE_KEY> --require-status "Ready for Dev" --scaffold-openspec

# Transition issue status
python scripts/atlassian_bridge.py transition <ISSUE_KEY> "In Progress"

# Post comment for HITL
python scripts/atlassian_bridge.py comment <ISSUE_KEY> "Clarification question..."

# Sync OpenSpec to Confluence Space
python scripts/atlassian_bridge.py sync-confluence
```
