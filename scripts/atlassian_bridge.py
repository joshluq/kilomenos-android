#!/usr/bin/env python3
"""
atlassian_bridge.py - Jira & Confluence Integration Bridge for Android Agentic Ecosystem

Connects the multi-agent ecosystem (PO, Architect, Dev, QA, Debugger) to:
1. Jira Software: Ingest user stories, update task checklists, create crash bug tickets,
   and post feedback/clarification inquiries.
2. Confluence: Publish and maintain OpenSpec Living Specifications directly in the team wiki.

Authentication:
    Configure credentials via environment variables:
        export ATLASSIAN_URL="https://your-domain.atlassian.net"
        export ATLASSIAN_EMAIL="your-email@example.com"
        export ATLASSIAN_API_TOKEN="your-api-token"
        export CONFLUENCE_SPACE="DEV"
    Or pass via command-line arguments, or use --simulate for testing.
"""

import argparse
import base64
import datetime
import html
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

ROOT_DIR = Path(__file__).resolve().parent.parent
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))


def markdown_to_confluence_html(markdown_text: str) -> str:
    """Converts Markdown text into valid XHTML Confluence Storage Format."""
    lines = markdown_text.replace("\r\n", "\n").split("\n")
    output = []
    in_code_block = False
    code_lines = []
    in_list = False
    list_tag = "ul"
    in_table = False
    table_has_header = False

    def inline_format(text: str) -> str:
        text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', text)
        text = re.sub(r'\*\*\*([^\*]+)\*\*\*', r'<strong><em>\1</em></strong>', text)
        text = re.sub(r'\*\*([^\*]+)\*\*', r'<strong>\1</strong>', text)
        text = re.sub(r'__([^_]+)__', r'<strong>\1</strong>', text)
        text = re.sub(r'\*([^\*]+)\*', r'<em>\1</em>', text)
        text = re.sub(r'_([^_]+)_', r'<em>\1</em>', text)
        text = re.sub(r'`([^`]+)`', r'<code>\1</code>', text)
        return text

    idx = 0
    while idx < len(lines):
        line = lines[idx]

        # 1. Code block fence
        if line.strip().startswith("```"):
            if in_code_block:
                escaped_code = html.escape("\n".join(code_lines))
                output.append(f"<pre><code>{escaped_code}</code></pre>")
                in_code_block = False
                code_lines = []
            else:
                if in_list:
                    output.append(f"</{list_tag}>")
                    in_list = False
                if in_table:
                    output.append("</tbody></table>")
                    in_table = False
                in_code_block = True
            idx += 1
            continue

        if in_code_block:
            code_lines.append(line)
            idx += 1
            continue

        # 2. Table row
        if line.strip().startswith("|") and line.strip().endswith("|"):
            if not in_table:
                if in_list:
                    output.append(f"</{list_tag}>")
                    in_list = False
                output.append("<table>")
                in_table = True
                table_has_header = False

            # Check if current line is separator line |---|---|
            if re.match(r'^\s*\|(\s*[-:]+[-|\s:]*)\|\s*$', line):
                idx += 1
                continue

            raw_cells = [c.strip() for c in line.strip().split("|")[1:-1]]
            is_header = False
            if not table_has_header:
                if idx + 1 < len(lines) and re.match(r'^\s*\|(\s*[-:]+[-|\s:]*)\|\s*$', lines[idx + 1]):
                    is_header = True
                    table_has_header = True

            cell_tag = "th" if is_header else "td"
            row_html = "".join(f"<{cell_tag}>{inline_format(html.escape(c))}</{cell_tag}>" for c in raw_cells)
            if is_header:
                output.append(f"<thead><tr>{row_html}</tr></thead><tbody>")
            else:
                output.append(f"<tr>{row_html}</tr>")
            idx += 1
            continue
        elif in_table:
            output.append("</tbody></table>")
            in_table = False

        # 3. Lists (- or * or 1.)
        m_ul = re.match(r'^\s*[-*]\s+(.*)$', line)
        m_ol = re.match(r'^\s*(\d+)\.\s+(.*)$', line)
        if m_ul or m_ol:
            cur_list_tag = "ul" if m_ul else "ol"
            item_content = m_ul.group(1) if m_ul else m_ol.group(1)
            if not in_list:
                output.append(f"<{cur_list_tag}>")
                in_list = True
                list_tag = cur_list_tag
            elif list_tag != cur_list_tag:
                output.append(f"</{list_tag}>")
                output.append(f"<{cur_list_tag}>")
                list_tag = cur_list_tag
            output.append(f"<li>{inline_format(html.escape(item_content))}</li>")
            idx += 1
            continue
        elif in_list:
            output.append(f"</{list_tag}>")
            in_list = False

        # 4. Headings
        m_h = re.match(r'^(#{1,6})\s+(.*)$', line)
        if m_h:
            level = len(m_h.group(1))
            h_text = inline_format(html.escape(m_h.group(2).strip()))
            output.append(f"<h{level}>{h_text}</h{level}>")
            idx += 1
            continue

        # 5. Horizontal rule
        if re.match(r'^\s*[-*_]{3,}\s*$', line):
            output.append("<hr />")
            idx += 1
            continue

        # 6. Blockquote
        if line.strip().startswith(">"):
            bq_text = inline_format(html.escape(line.strip()[1:].strip()))
            output.append(f"<blockquote><p>{bq_text}</p></blockquote>")
            idx += 1
            continue

        # 7. Blank lines / Paragraphs
        stripped = line.strip()
        if not stripped:
            idx += 1
            continue

        p_text = inline_format(html.escape(stripped))
        output.append(f"<p>{p_text}</p>")
        idx += 1

    if in_code_block:
        escaped_code = html.escape("\n".join(code_lines))
        output.append(f"<pre><code>{escaped_code}</code></pre>")
    if in_list:
        output.append(f"</{list_tag}>")
    if in_table:
        output.append("</tbody></table>")

    return "\n".join(output)


# ==============================================================================
# 1. ATLASSIAN CLIENT (HTTP & SIMULATION ENGINE)
# ==============================================================================

class AtlassianClient:
    def __init__(
        self,
        base_url: Optional[str] = None,
        email: Optional[str] = None,
        api_token: Optional[str] = None,
        project_key: Optional[str] = None,
        board_id: Optional[str] = None,
        label: Optional[str] = None,
        confluence_space: Optional[str] = None,
        simulate: bool = False,
    ):
        self.simulate = simulate
        self.base_url = (base_url or os.environ.get("ATLASSIAN_URL", "")).rstrip("/")
        self.email = email or os.environ.get("ATLASSIAN_EMAIL", "")
        self.api_token = api_token or os.environ.get("ATLASSIAN_API_TOKEN", "")
        self.project_key = project_key or os.environ.get("JIRA_PROJECT_KEY")
        self.board_id = board_id or os.environ.get("JIRA_BOARD_ID")
        self.label = label or os.environ.get("JIRA_LABEL")
        self.confluence_space = confluence_space or os.environ.get("CONFLUENCE_SPACE")

        # Try reading local config file if credentials are empty
        config_path = ROOT_DIR / ".atlassian_config.json"
        if config_path.is_file():
            try:
                cfg = json.loads(config_path.read_text(encoding="utf-8"))
                if not self.base_url:
                    self.base_url = cfg.get("url", "").rstrip("/")
                if not self.email:
                    self.email = cfg.get("email", "")
                if not self.api_token:
                    self.api_token = cfg.get("api_token", "")
                if not self.project_key:
                    self.project_key = cfg.get("project_key")
                if not self.board_id:
                    self.board_id = str(cfg.get("board_id")) if cfg.get("board_id") else None
                if not self.label:
                    self.label = cfg.get("label")
                if not self.confluence_space:
                    self.confluence_space = cfg.get("confluence_space")
            except Exception:
                pass

        if not self.simulate and (not self.base_url or not self.email or not self.api_token):
            self.configured = False
        else:
            self.configured = True

    @property
    def auth_headers(self) -> Dict[str, str]:
        credentials = f"{self.email}:{self.api_token}"
        encoded = base64.b64encode(credentials.encode("utf-8")).decode("utf-8")
        return {
            "Authorization": f"Basic {encoded}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        }

    def _http_request(self, method: str, endpoint: str, payload: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Executes an authenticated HTTP request to the Atlassian REST API."""
        if self.simulate:
            return {"simulated": True, "status": 200, "endpoint": endpoint, "method": method}

        if not self.configured:
            raise ValueError(
                "Atlassian credentials missing. Provide ATLASSIAN_URL, ATLASSIAN_EMAIL, and ATLASSIAN_API_TOKEN."
            )

        url = f"{self.base_url}/{endpoint.lstrip('/')}"
        data = json.dumps(payload).encode("utf-8") if payload else None
        req = urllib.request.Request(url, data=data, headers=self.auth_headers, method=method)

        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                status_code = resp.status
                body = resp.read().decode("utf-8")
                return json.loads(body) if body else {"status": status_code}
        except urllib.error.HTTPError as e:
            err_body = e.read().decode("utf-8") if e.fp else ""
            raise RuntimeError(f"Atlassian API Error ({e.code}) on {url}: {err_body}")
        except urllib.error.URLError as e:
            raise RuntimeError(f"Network error connecting to {url}: {e.reason}")

    # --------------------------------------------------------------------------
    # Jira Operations
    # --------------------------------------------------------------------------

    def test_connection(self) -> Dict[str, Any]:
        """Verifies connection and user permissions on Jira Cloud."""
        if self.simulate:
            return {
                "success": True,
                "simulated": True,
                "user": "developer@example.com",
                "display_name": "Antigravity Agent",
                "jira_status": "ONLINE",
            }
        data = self._http_request("GET", "rest/api/3/myself")
        return {
            "success": True,
            "simulated": False,
            "user": data.get("emailAddress"),
            "display_name": data.get("displayName"),
            "account_id": data.get("accountId"),
            "jira_status": "CONNECTED",
        }

    def get_board_issues(
        self,
        board_id: Optional[str] = None,
        status: Optional[str] = None,
        project_key: Optional[str] = None,
        label: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """Queries tickets from a specific Jira Agile Kanban/Scrum Board with optional status, project, and label filtering."""
        target_board = board_id or self.board_id
        pkey = project_key or self.project_key
        tlabel = label or self.label
        if not target_board and not self.simulate:
            raise ValueError("No Board ID specified. Provide --board-id or set JIRA_BOARD_ID.")

        if self.simulate:
            prefix = pkey or "KMSAFE"
            sample_issues = [
                {"key": f"{prefix}-101", "summary": "Setup Bluetooth tracking receiver", "status": "Ready for Dev", "priority": "High", "labels": ["android"]},
                {"key": f"{prefix}-102", "summary": "CanvasKit Button styling alignment", "status": "In Progress", "priority": "Medium", "labels": ["android"]},
                {"key": f"{prefix}-103", "summary": "Export PDF expenses report", "status": "Ready for Dev", "priority": "High", "labels": ["android"]},
                {"key": f"{prefix}-104", "summary": "Station historical price graphs", "status": "Backlog", "priority": "Low", "labels": ["android"]},
                {"key": f"{prefix}-201", "summary": "Setup Supabase vehicle_contracts table and RLS", "status": "Ready for Dev", "priority": "High", "labels": ["backend"]},
                {"key": f"{prefix}-301", "summary": "Publish Legal Terms and Conditions HTML", "status": "Ready for Dev", "priority": "Medium", "labels": ["web"]},
            ]
            res = sample_issues
            if tlabel and tlabel.upper() not in ("ALL", "ANY"):
                res = [i for i in res if tlabel.lower() in [l.lower() for l in i.get("labels", [])]]
            if status:
                res = [i for i in res if i["status"].lower() == status.lower()]
            return res

        endpoint = f"rest/agile/1.0/board/{target_board}/issue"
        jql_parts = []
        if pkey:
            jql_parts.append(f"project = '{pkey}'")
        if tlabel and tlabel.upper() not in ("ALL", "ANY"):
            jql_parts.append(f"labels = '{tlabel}'")
        if status:
            jql_parts.append(f"status = '{status}'")
        if jql_parts:
            endpoint += "?jql=" + urllib.parse.quote(" AND ".join(jql_parts))

        data = self._http_request("GET", endpoint)
        issues = data.get("issues", [])
        return [
            {
                "key": i.get("key"),
                "summary": i.get("fields", {}).get("summary"),
                "status": i.get("fields", {}).get("status", {}).get("name"),
                "priority": i.get("fields", {}).get("priority", {}).get("name"),
            }
            for i in issues
        ]

    @staticmethod
    def _extract_adf_text(node: Any) -> str:
        """Extracts human-readable plain text recursively from Atlassian Document Format (ADF)."""
        if not node:
            return ""
        if isinstance(node, str):
            return node
        if isinstance(node, dict):
            ntype = node.get("type")
            if ntype == "text":
                return node.get("text", "")
            children = node.get("content", [])
            child_text = "".join(AtlassianClient._extract_adf_text(c) for c in children)
            if ntype in ("paragraph", "heading"):
                return child_text + "\n"
            elif ntype == "bulletList":
                return child_text + "\n"
            elif ntype == "listItem":
                return f"- {child_text.strip()}\n"
            return child_text
        if isinstance(node, list):
            return "".join(AtlassianClient._extract_adf_text(c) for c in node)
        return str(node)

    def get_jira_issue(self, issue_key: str) -> Dict[str, Any]:
        """Fetches issue details (summary, description, status, priority, comments)."""
        if self.simulate:
            return {
                "key": issue_key,
                "summary": f"Simulated Feature: {issue_key}",
                "status": "In Progress",
                "priority": "High",
                "description": (
                    "As a user, I want to export my vehicle balance to PDF so I have proof for the leasing company.\n\n"
                    "Acceptance Criteria:\n"
                    "- AC-1: Given an active contract, When I tap Export, Then a valid PDF is created."
                ),
                "comments_count": 1,
            }
        data = self._http_request("GET", f"rest/api/3/issue/{issue_key}")
        fields = data.get("fields", {})

        # Extract description text (Atlassian Document Format or string)
        raw_desc = fields.get("description", "")
        if isinstance(raw_desc, dict):
            desc = self._extract_adf_text(raw_desc).strip()
        else:
            desc = str(raw_desc or "").strip()

        return {
            "key": data.get("key"),
            "summary": fields.get("summary"),
            "status": fields.get("status", {}).get("name"),
            "priority": fields.get("priority", {}).get("name"),
            "description": desc,
            "issue_type": fields.get("issuetype", {}).get("name"),
        }

    def create_jira_issue(
        self,
        project_key: str,
        summary: str,
        description: str,
        issue_type: str = "Story",
        priority: str = "Medium",
    ) -> Dict[str, Any]:
        """Creates a new Jira issue (Story, Bug, Task)."""
        if self.simulate:
            sim_key = f"{project_key}-101"
            return {
                "key": sim_key,
                "id": "10001",
                "self": f"https://example.atlassian.net/rest/api/3/issue/{sim_key}",
                "simulated": True,
            }

        # ADF format payload for Jira v3 API
        adf_desc = {
            "type": "doc",
            "version": 1,
            "content": [
                {
                    "type": "paragraph",
                    "content": [{"type": "text", "text": description}],
                }
            ],
        }

        payload = {
            "fields": {
                "project": {"key": project_key},
                "summary": summary,
                "description": adf_desc,
                "issuetype": {"name": issue_type},
                "priority": {"name": priority},
            }
        }
        return self._http_request("POST", "rest/api/3/issue", payload)

    def add_jira_comment(self, issue_key: str, comment_text: str) -> Dict[str, Any]:
        """Posts a comment to a Jira ticket (e.g. for HITL feedback or clarification questions)."""
        if self.simulate:
            return {"comment_id": "9999", "issue_key": issue_key, "simulated": True}

        adf_comment = {
            "type": "doc",
            "version": 1,
            "content": [
                {
                    "type": "paragraph",
                    "content": [{"type": "text", "text": comment_text}],
                }
            ],
        }
        return self._http_request("POST", f"rest/api/3/issue/{issue_key}/comment", {"body": adf_comment})

    def update_jira_issue(
        self,
        issue_key: str,
        description: Optional[str] = None,
        summary: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Updates fields of an existing Jira issue (e.g. refining description with Acceptance Criteria)."""
        if self.simulate:
            return {"key": issue_key, "updated": True, "simulated": True}

        fields: Dict[str, Any] = {}
        if summary:
            fields["summary"] = summary
        if description:
            fields["description"] = {
                "type": "doc",
                "version": 1,
                "content": [
                    {
                        "type": "paragraph",
                        "content": [{"type": "text", "text": description}],
                    }
                ],
            }
        return self._http_request("PUT", f"rest/api/3/issue/{issue_key}", {"fields": fields})

    def transition_jira_issue(self, issue_key: str, target_status: str) -> Dict[str, Any]:
        """Transitions an issue to a new status (e.g. 'Ready for Dev', 'In Progress', 'Done')."""
        if self.simulate:
            return {"key": issue_key, "new_status": target_status, "simulated": True}

        transitions_data = self._http_request("GET", f"rest/api/3/issue/{issue_key}/transitions")
        transitions = transitions_data.get("transitions", [])

        target_trans = None
        for t in transitions:
            if t.get("name", "").lower() == target_status.lower() or t.get("to", {}).get("name", "").lower() == target_status.lower():
                target_trans = t
                break

        if not target_trans:
            avail = [t.get("name") for t in transitions]
            raise ValueError(f"Cannot transition {issue_key} to '{target_status}'. Available transitions: {avail}")

        payload = {"transition": {"id": target_trans["id"]}}
        return self._http_request("POST", f"rest/api/3/issue/{issue_key}/transitions", payload)

    # --------------------------------------------------------------------------
    # Confluence & Epic Operations
    # --------------------------------------------------------------------------

    def get_epic_issues(self, epic_key: str) -> List[Dict[str, Any]]:
        """Fetches all child issues of an epic using /rest/api/3/search/jql."""
        if self.simulate:
            return [{"key": f"{epic_key}-1", "summary": "Simulated Issue", "status": "Finalizada"}]
        jql = urllib.parse.quote(f"parent = {epic_key}")
        fields = "summary,status,priority,description,issuetype,resolution,comment,updated,created"
        res = self._http_request("GET", f"rest/api/3/search/jql?jql={jql}&fields={fields}")
        issues = res.get("issues", [])
        parsed = []
        for iss in sorted(issues, key=lambda x: int(x.get("key", "0-0").split("-")[1]) if "-" in x.get("key", "") and x.get("key", "").split("-")[1].isdigit() else 0):
            f = iss.get("fields", {})
            desc = self._extract_adf_text(f.get("description", "")).strip()
            comments = [
                {
                    "author": c.get("author", {}).get("displayName"),
                    "created": c.get("created"),
                    "body": self._extract_adf_text(c.get("body", "")).strip()
                }
                for c in f.get("comment", {}).get("comments", [])
            ]
            parsed.append({
                "key": iss.get("key"),
                "summary": f.get("summary"),
                "status": f.get("status", {}).get("name"),
                "priority": f.get("priority", {}).get("name"),
                "type": f.get("issuetype", {}).get("name"),
                "resolution": f.get("resolution", {}).get("name") if f.get("resolution") else None,
                "created": f.get("created"),
                "updated": f.get("updated"),
                "description": desc,
                "comments": comments
            })
        return parsed

    def find_confluence_page(self, space_key: str, title: str) -> Optional[Dict[str, Any]]:
        """Finds an existing page by title in a Confluence space."""
        if self.simulate:
            return None
        encoded_title = urllib.parse.quote(title)
        endpoint = f"wiki/rest/api/content?spaceKey={space_key}&title={encoded_title}&expand=version,ancestors"
        try:
            res = self._http_request("GET", endpoint)
            results = res.get("results", [])
            return results[0] if results else None
        except Exception:
            return None

    def publish_confluence_page(
        self,
        space_key: str,
        title: str,
        markdown_body: str,
        parent_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Publishes or updates a living specification/document in Confluence with native HTML formatting."""
        if self.simulate:
            return {
                "id": "20001",
                "title": title,
                "space": space_key,
                "url": f"https://example.atlassian.net/wiki/spaces/{space_key}/pages/20001",
                "simulated": True,
                "action": "simulated",
            }

        html_body = markdown_to_confluence_html(markdown_body)
        existing = self.find_confluence_page(space_key, title)

        if existing:
            page_id = existing["id"]
            current_ver = existing.get("version", {}).get("number", 1)
            payload = {
                "id": page_id,
                "type": "page",
                "title": title,
                "space": {"key": space_key},
                "body": {
                    "storage": {
                        "value": html_body,
                        "representation": "storage",
                    }
                },
                "version": {
                    "number": current_ver + 1
                }
            }
            if parent_id:
                payload["ancestors"] = [{"id": parent_id}]
            res = self._http_request("PUT", f"wiki/rest/api/content/{page_id}", payload)
            res["action"] = "updated"
            return res
        else:
            payload = {
                "title": title,
                "type": "page",
                "space": {"key": space_key},
                "body": {
                    "storage": {
                        "value": html_body,
                        "representation": "storage",
                    }
                },
            }
            if parent_id:
                payload["ancestors"] = [{"id": parent_id}]
            res = self._http_request("POST", "wiki/rest/api/content", payload)
            res["action"] = "created"
            return res


# ==============================================================================
# 2. CLI WORKFLOW CONTROLLERS
# ==============================================================================

def cmd_check(client: AtlassianClient):
    print("===========================================================")
    print("   ATLASSIAN JIRA & CONFLUENCE CONNECTIVITY CHECK")
    print("===========================================================")
    print(f"Base URL  : {client.base_url or '(Not set - using simulation)'}")
    print(f"User      : {client.email or '(Not set)'}")
    print(f"Simulated : {client.simulate}")
    print("-----------------------------------------------------------")

    try:
        res = client.test_connection()
        print(f"✅ Connection Status : {res.get('jira_status')}")
        print(f"   Account User      : {res.get('user')}")
        print(f"   Display Name      : {res.get('display_name')}")
        if res.get("simulated"):
            print("   [INFO] Running in mock/simulation mode. No external network calls made.")
    except Exception as e:
        print(f"❌ Connection failed: {e}")
        sys.exit(1)


def cmd_fetch(client: AtlassianClient, issue_key: str, scaffold_openspec: bool = False, require_status: Optional[str] = None):
    print(f"[*] Ingesting Jira ticket: {issue_key}...")
    try:
        issue = client.get_jira_issue(issue_key)
        status = issue.get("status", "")
        print("-----------------------------------------------------------")
        print(f"Key      : {issue.get('key')}")
        print(f"Summary  : {issue.get('summary')}")
        print(f"Status   : {status}")
        print(f"Priority : {issue.get('priority')}")
        print("-----------------------------------------------------------")
        print("Description:")
        print(issue.get("description", "").strip())
        print("-----------------------------------------------------------")

        if require_status:
            if status.lower() != require_status.lower():
                print(f"⚠️ [BLOCKED BY STATUS GUARD] Ticket {issue_key} is in status '{status}'.")
                print(f"   Required status to proceed: '{require_status}'.")
                print(f"   Refine or transition the ticket in Jira before implementing.")
                sys.exit(1)
            else:
                print(f"✅ Status Guard PASSED: Ticket is in required status '{require_status}'.")

        if scaffold_openspec:
            try:
                from scripts.openspec_cli import cmd_propose
            except ImportError:
                from openspec_cli import cmd_propose
            print(f"[*] Scaffolding OpenSpec proposal for {issue_key}...")
            cmd_propose(issue_key, title=issue.get("summary"))
    except Exception as e:
        print(f"❌ Error fetching issue: {e}")
        sys.exit(1)


def cmd_refine(client: AtlassianClient, issue_key: str, criteria_file_or_text: str, transition_to: Optional[str] = None):
    print(f"[*] PO Refinement: Updating Jira ticket {issue_key} with formal acceptance criteria...")
    try:
        # Check if input is a file path
        path = Path(criteria_file_or_text)
        if path.is_file():
            new_desc = path.read_text(encoding="utf-8")
        else:
            new_desc = criteria_file_or_text

        client.update_jira_issue(issue_key, description=new_desc)
        print(f"✅ Issue {issue_key} description successfully updated with refined specifications.")

        if transition_to:
            print(f"[*] Moving issue to '{transition_to}'...")
            client.transition_jira_issue(issue_key, transition_to)
            print(f"✅ Issue transitioned to '{transition_to}'.")
    except Exception as e:
        print(f"❌ Error refining issue: {e}")
        sys.exit(1)


def cmd_transition(client: AtlassianClient, issue_key: str, status_name: str, comment: Optional[str] = None):
    try:
        if comment:
            print(f"[*] Posting comment to {issue_key}...")
            client.add_jira_comment(issue_key, comment)
            print(f"✅ Comment posted successfully.")
        print(f"[*] Transitioning issue {issue_key} to '{status_name}'...")
        res = client.transition_jira_issue(issue_key, status_name)
        print(f"✅ Issue {issue_key} transitioned successfully to '{status_name}'.")
    except Exception as e:
        print(f"❌ Error transitioning issue: {e}")
        sys.exit(1)


def cmd_resolve(client: AtlassianClient, issue_key: str, comment: Optional[str] = None, status_name: str = "Listo"):
    """Atomically post resolution comment and transition ticket to completed/done status."""
    print(f"[*] Resolving issue {issue_key} (Target Status: '{status_name}')...")
    try:
        if comment:
            print(f"[*] Posting resolution comment to {issue_key}...")
            client.add_jira_comment(issue_key, comment)
            print(f"✅ Resolution comment posted.")
        client.transition_jira_issue(issue_key, status_name)
        print(f"✅ Issue {issue_key} marked as '{status_name}'.")
    except Exception as e:
        print(f"❌ Error resolving issue: {e}")
        sys.exit(1)


def cmd_board(
    client: AtlassianClient,
    board_id: Optional[str] = None,
    status: Optional[str] = None,
    project_key: Optional[str] = None,
    label: Optional[str] = None,
):
    target = board_id or client.board_id or "Default Board"
    pkey = project_key or client.project_key or "ALL"
    tlabel = label or client.label
    label_info = f", Label: '{tlabel}'" if tlabel else ""
    print(f"[*] Querying Kanban/Scrum Board '{target}' (Project: {pkey}{label_info}, Status filter: {status or 'ALL'})...")
    try:
        issues = client.get_board_issues(board_id=board_id, status=status, project_key=project_key, label=tlabel)
        print("===========================================================")
        print(f"   KANBAN BOARD ISSUES ({len(issues)})")
        print("===========================================================")
        if not issues:
            print("   (No matching tickets found on this board)")
        else:
            for iss in issues:
                print(f"   [{iss.get('status'):<14}] {iss.get('key'):<12} : {iss.get('summary')} ({iss.get('priority')})")
        print("===========================================================")
    except Exception as e:
        print(f"❌ Error fetching board: {e}")
        sys.exit(1)


def cmd_comment(client: AtlassianClient, issue_key: str, message: str):
    print(f"[*] Posting feedback comment to {issue_key}...")
    try:
        client.add_jira_comment(issue_key, message)
        print(f"✅ Comment posted successfully to {issue_key}.")
    except Exception as e:
        print(f"❌ Error posting comment: {e}")
        sys.exit(1)


def cmd_report_crash(client: AtlassianClient, project_key: str, defect_json_path: str):
    path = Path(defect_json_path)
    if not path.is_file():
        print(f"[ERROR] Defect file not found: {path}")
        sys.exit(1)

    defect = json.loads(path.read_text(encoding="utf-8"))
    summary = f"[CRASH] {defect.get('upstream_artifact_ref', 'Runtime')}: {defect.get('metadata', {}).get('exception_class', 'Fatal Error')}"
    description = (
        f"Automated crash report captured by Senior Debugging Engineer Tooling.\n\n"
        f"- Exception: {defect.get('metadata', {}).get('exception_class')}\n"
        f"- Message: {defect.get('metadata', {}).get('message')}\n"
        f"- Culprit File & Line: {defect.get('upstream_artifact_ref')}\n"
        f"- Thread: {defect.get('metadata', {}).get('thread')}\n"
        f"- Process: {defect.get('metadata', {}).get('process')}\n\n"
        f"Reproduction Steps:\n" + "\n".join(defect.get("reproduction_steps", [])) + "\n\n"
        f"Expected:\n{defect.get('expected')}\n\n"
        f"Actual:\n{defect.get('actual')}\n"
    )

    print(f"[*] Filing Jira Bug ticket for project '{project_key}'...")
    res = client.create_jira_issue(
        project_key=project_key,
        summary=summary,
        description=description,
        issue_type="Bug",
        priority="High" if defect.get("severity") == "CRITICAL" else "Highest",
    )
    print(f"✅ Bug ticket created: {res.get('key')} ({res.get('self', 'simulated')})")


def cmd_sync_confluence(client: AtlassianClient, space_key: str):
    specs_dir = ROOT_DIR / "openspec" / "specs"
    if not specs_dir.is_dir():
        print(f"[ERROR] Living specs directory not found: {specs_dir}")
        sys.exit(1)

    print(f"[*] Publishing OpenSpec Living Specifications to Confluence Space '{space_key}'...")
    specs = list(specs_dir.rglob("*.md"))
    for s in specs:
        title = f"Living Spec: {s.stem.replace('_', ' ').title()}"
        content = s.read_text(encoding="utf-8")
        print(f"    -> Publishing '{title}' ({s.name})...")
        res = client.publish_confluence_page(space_key, title, content)
        print(f"       ✅ Published: Page ID {res.get('id')}")

    print("===========================================================")
    print(f"   🎉 All {len(specs)} Living Specs Synchronized to Confluence!")
    print("===========================================================")


def cmd_fetch_epic(client: AtlassianClient, epic_key: str, output_path: Optional[str] = None):
    print(f"[*] Fetching child issues for Epic '{epic_key}' from Jira...")
    try:
        issues = client.get_epic_issues(epic_key)
        print("===========================================================")
        print(f"   EPIC {epic_key} CHILD ISSUES ({len(issues)})")
        print("===========================================================")
        for iss in issues:
            res_str = f" ({iss.get('resolution')})" if iss.get('resolution') else ""
            print(f"   [{iss.get('status'):<14}] {iss.get('key'):<12} : {iss.get('summary')}{res_str}")
        print("===========================================================")

        if output_path:
            out_file = Path(output_path)
            out_file.parent.mkdir(parents=True, exist_ok=True)
            out_file.write_text(json.dumps(issues, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            print(f"✅ Saved {len(issues)} issues to {out_file.resolve()}")
    except Exception as e:
        print(f"❌ Error fetching epic issues: {e}")
        sys.exit(1)


def cmd_publish_doc(
    client: AtlassianClient,
    file_path: str,
    space_key: Optional[str] = None,
    title: Optional[str] = None,
    parent_id: Optional[str] = None,
    parent_title: Optional[str] = None,
):
    path = Path(file_path)
    if not path.is_file():
        print(f"[ERROR] Document file not found: {path}")
        sys.exit(1)

    content = path.read_text(encoding="utf-8")
    space = space_key or client.confluence_space or "KILOMENOS"

    if not title:
        m = re.search(r'^\s*#\s+(.+)$', content, re.MULTILINE)
        if m:
            title = m.group(1).strip()
        else:
            title = path.stem.replace("_", " ").title()

    resolved_parent_id = parent_id
    if not resolved_parent_id and parent_title:
        p_page = client.find_confluence_page(space, parent_title)
        if p_page:
            resolved_parent_id = p_page["id"]
            print(f"[*] Found parent page '{parent_title}' (ID: {resolved_parent_id})")
        else:
            print(f"⚠️ Parent page '{parent_title}' not found in space '{space}'. Publishing at space root.")

    print(f"[*] Publishing '{title}' from {path.name} to Confluence Space '{space}'...")
    try:
        res = client.publish_confluence_page(space, title, content, parent_id=resolved_parent_id)
        action = res.get("action", "processed")
        print(f"✅ Successfully {action} Confluence page: '{title}' (ID: {res.get('id')})")
        if "url" in res:
            print(f"   URL: {res.get('url')}")
        elif client.base_url:
            print(f"   URL: {client.base_url}/wiki/spaces/{space}/pages/{res.get('id')}")
    except Exception as e:
        print(f"❌ Error publishing to Confluence: {e}")
        sys.exit(1)


# ==============================================================================
# 3. CLI ARGUMENT PARSER
# ==============================================================================

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Jira & Confluence Integration Bridge for Android Agentic Ecosystem."
    )
    parser.add_argument("--url", help="Atlassian Cloud URL (e.g. https://domain.atlassian.net)")
    parser.add_argument("--email", help="Atlassian Account Email")
    parser.add_argument("--token", help="Atlassian API Token")
    parser.add_argument("--project-key", help="Jira Project Key (e.g. KMSAFE, FLEET)")
    parser.add_argument("--board-id", help="Jira Agile Board ID for Kanban/Scrum filtering")
    parser.add_argument("--label", help="Jira Label filter (e.g. android, backend, web)")
    parser.add_argument("--space", help="Confluence Space Key")
    parser.add_argument("--simulate", action="store_true", help="Run in mock/simulation mode without live credentials.")

    subparsers = parser.add_subparsers(dest="command", help="Bridge command to execute")

    # check
    subparsers.add_parser("check", help="Test Atlassian connection and permissions.")

    # board
    p_board = subparsers.add_parser("board", help="List tickets from a specific Jira Agile Kanban/Scrum board.")
    p_board.add_argument("--board-id", help="Jira Agile Board ID (defaults to configured board)")
    p_board.add_argument("--project-key", help="Jira Project Key (e.g. KMSAFE, FLEET)")
    p_board.add_argument("--label", help="Filter by Jira Label (e.g. android, backend, web)")
    p_board.add_argument("--status", help="Filter by column/status (e.g. 'Ready for Dev', 'In Progress')")

    # fetch
    p_fetch = subparsers.add_parser("fetch", help="Fetch a Jira issue.")
    p_fetch.add_argument("issue_key", help="Jira issue key (e.g. KMSAFE-104)")
    p_fetch.add_argument("--scaffold-openspec", action="store_true", help="Scaffold an OpenSpec proposal from this issue.")
    p_fetch.add_argument("--require-status", help="State Guard: Require specific issue status (e.g. 'Ready for Dev')")

    # refine
    p_refine = subparsers.add_parser("refine", help="PO Refinement: Update ticket with formal acceptance criteria.")
    p_refine.add_argument("issue_key", help="Jira issue key (e.g. KMSAFE-105)")
    p_refine.add_argument("criteria", help="Path to markdown file or string containing refined Acceptance Criteria")
    p_refine.add_argument("--status", default="Refined", help="Status to transition issue after refinement (default: Refined)")

    # transition
    p_trans = subparsers.add_parser("transition", help="Transition Jira issue status.")
    p_trans.add_argument("issue_key", help="Jira issue key")
    p_trans.add_argument("target_status", help="Target status name (e.g. 'Ready for Dev', 'In Progress', 'Done', 'Listo')")
    p_trans.add_argument("--comment", "-c", help="Optional comment to post alongside the transition")

    # resolve
    p_resolve = subparsers.add_parser("resolve", help="Atomically post summary comment and transition Jira issue to completed/done status.")
    p_resolve.add_argument("issue_key", help="Jira issue key (e.g. KILOMENOS-14)")
    p_resolve.add_argument("--comment", "-c", help="Resolution summary comment")
    p_resolve.add_argument("--status", "-s", default="Listo", help="Target resolution status (default: 'Listo')")

    # comment
    p_com = subparsers.add_parser("comment", help="Add a comment to a Jira issue.")
    p_com.add_argument("issue_key", help="Jira issue key")
    p_com.add_argument("message", help="Comment text or clarification question")

    # report-crash
    p_rc = subparsers.add_parser("report-crash", help="File a Jira Bug from a defect JSON ticket.")
    p_rc.add_argument("project_key", help="Target Jira Project Key (e.g. KMSAFE)")
    p_rc.add_argument("defect_json", help="Path to defect ticket JSON")

    # sync-confluence
    p_sync = subparsers.add_parser("sync-confluence", help="Publish OpenSpec living specs to Confluence.")
    p_sync.add_argument("space_key", nargs="?", help="Confluence Space Key (defaults to configured space)")

    # fetch-epic
    p_epic = subparsers.add_parser("fetch-epic", help="Fetch all child issues of a Jira Epic.")
    p_epic.add_argument("epic_key", help="Jira Epic Key (e.g. KILOMENOS-3)")
    p_epic.add_argument("--output", "-o", help="Optional output JSON file path to save issue dump")

    # publish-doc
    p_pubdoc = subparsers.add_parser("publish-doc", help="Publish or update any markdown file in Confluence.")
    p_pubdoc.add_argument("file_path", help="Path to markdown document to publish")
    p_pubdoc.add_argument("--space", "-s", help="Confluence Space Key (defaults to configured space)")
    p_pubdoc.add_argument("--title", "-t", help="Confluence page title (defaults to H1 or filename)")
    p_pubdoc.add_argument("--parent-id", help="Parent Confluence page ID")
    p_pubdoc.add_argument("--parent-title", help="Parent Confluence page title")

    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    client = AtlassianClient(
        base_url=args.url,
        email=args.email,
        api_token=args.token,
        project_key=args.project_key,
        board_id=args.board_id,
        label=getattr(args, "label", None),
        confluence_space=args.space,
        simulate=args.simulate,
    )

    if args.command == "check":
        cmd_check(client)
    elif args.command == "board":
        cmd_board(client, board_id=args.board_id, status=args.status, project_key=args.project_key, label=getattr(args, "label", None))
    elif args.command == "fetch":
        cmd_fetch(client, args.issue_key, scaffold_openspec=args.scaffold_openspec, require_status=args.require_status)
    elif args.command == "fetch-epic":
        cmd_fetch_epic(client, args.epic_key, output_path=args.output)
    elif args.command == "refine":
        cmd_refine(client, args.issue_key, args.criteria, transition_to=args.status)
    elif args.command == "transition":
        cmd_transition(client, args.issue_key, args.target_status, comment=args.comment)
    elif args.command == "resolve":
        cmd_resolve(client, args.issue_key, comment=args.comment, status_name=args.status)
    elif args.command == "comment":
        cmd_comment(client, args.issue_key, args.message)
    elif args.command == "report-crash":
        cmd_report_crash(client, args.project_key, args.defect_json)
    elif args.command == "publish-doc":
        cmd_publish_doc(client, args.file_path, space_key=args.space, title=args.title, parent_id=args.parent_id, parent_title=args.parent_title)
    elif args.command == "sync-confluence":
        target_space = args.space_key or client.confluence_space
        if not target_space and not client.simulate:
            print("[ERROR] Confluence Space Key missing. Provide space_key or --space.")
            sys.exit(1)
        cmd_sync_confluence(client, target_space or "DEV")


if __name__ == "__main__":
    main()
