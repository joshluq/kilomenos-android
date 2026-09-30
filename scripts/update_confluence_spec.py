#!/usr/bin/env python3
"""
Updates the Auto-Tracking Technical Specification page in Confluence.
"""
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO_ROOT))

try:
    from scripts.atlassian_bridge import AtlassianClient
except ImportError:
    from atlassian_bridge import AtlassianClient

PAGE_ID = "1572865"
PAGE_TITLE = "Autotracking: Especificación Técnica y Arquitectura"
SPEC_FILE = Path(__file__).resolve().parent.parent / "docs" / "autotracking_technical_spec.md"

def markdown_to_confluence_html(md_text: str) -> str:
    lines = md_text.split("\n")
    html_lines = []
    in_code_block = False
    code_lang = ""
    code_lines = []
    in_table = False
    table_lines = []
    in_ul = False
    in_ol = False

    def close_lists():
        nonlocal in_ul, in_ol
        res = []
        if in_ul:
            res.append("</ul>")
            in_ul = False
        if in_ol:
            res.append("</ol>")
            in_ol = False
        return res

    def close_table():
        nonlocal in_table, table_lines
        if not in_table or not table_lines:
            in_table = False
            table_lines = []
            return []
        
        res = ["<table>"]
        header_processed = False
        for idx, row in enumerate(table_lines):
            cells = [c.strip() for c in row.strip("|").split("|")]
            # Check if separator row
            if all(re.match(r"^:?-+:?$", c) for c in cells if c):
                continue
            
            if not header_processed:
                res.append("<thead><tr>")
                for cell in cells:
                    res.append(f"<th>{inline_format(cell)}</th>")
                res.append("</tr></thead><tbody>")
                header_processed = True
            else:
                res.append("<tr>")
                for cell in cells:
                    res.append(f"<td>{inline_format(cell)}</td>")
                res.append("</tr>")
        
        if header_processed:
            res.append("</tbody>")
        res.append("</table>")
        in_table = False
        table_lines = []
        return res

    def inline_format(text: str) -> str:
        # Escape XML entities first
        t = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        # Bold
        t = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", t)
        # Italic
        t = re.sub(r"\*(.+?)\*", r"<em>\1</em>", t)
        # Inline code
        t = re.sub(r"`(.+?)`", r"<code>\1</code>", t)
        return t

    for line in lines:
        stripped = line.strip()

        # Code blocks
        if stripped.startswith("```"):
            if in_code_block:
                code_content = "\n".join(code_lines)
                code_content_escaped = code_content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                html_lines.append(f'<ac:structured-macro ac:name="code"><ac:parameter ac:name="language">{code_lang}</ac:parameter><ac:plain-text-body><![CDATA[{code_content}]]></ac:plain-text-body></ac:structured-macro>')
                in_code_block = False
                code_lang = ""
                code_lines = []
            else:
                html_lines.extend(close_lists())
                html_lines.extend(close_table())
                in_code_block = True
                code_lang = stripped[3:].strip()
                code_lines = []
            continue

        if in_code_block:
            code_lines.append(line)
            continue

        # Tables
        if stripped.startswith("|") and stripped.endswith("|"):
            html_lines.extend(close_lists())
            in_table = True
            table_lines.append(stripped)
            continue
        elif in_table:
            html_lines.extend(close_table())

        # Empty lines
        if not stripped:
            html_lines.extend(close_lists())
            continue

        # Headings
        if stripped.startswith("# "):
            html_lines.extend(close_lists())
            html_lines.append(f"<h1>{inline_format(stripped[2:])}</h1>")
            continue
        elif stripped.startswith("## "):
            html_lines.extend(close_lists())
            html_lines.append(f"<h2>{inline_format(stripped[3:])}</h2>")
            continue
        elif stripped.startswith("### "):
            html_lines.extend(close_lists())
            html_lines.append(f"<h3>{inline_format(stripped[4:])}</h3>")
            continue
        elif stripped.startswith("#### "):
            html_lines.extend(close_lists())
            html_lines.append(f"<h4>{inline_format(stripped[5:])}</h4>")
            continue

        # Horizontal rule
        if stripped in ("---", "***", "___"):
            html_lines.extend(close_lists())
            html_lines.append("<hr />")
            continue

        # Blockquote / Note
        if stripped.startswith("> "):
            html_lines.extend(close_lists())
            quote_text = stripped[2:].strip()
            if quote_text.startswith("[!NOTE]"):
                quote_text = quote_text[7:].strip()
            html_lines.append(f'<ac:structured-macro ac:name="info"><ac:rich-text-body><p>{inline_format(quote_text)}</p></ac:rich-text-body></ac:structured-macro>')
            continue

        # Lists: Unordered
        ul_match = re.match(r"^[-*]\s+(.*)", stripped)
        if ul_match:
            if not in_ul:
                html_lines.extend(close_lists())
                html_lines.append("<ul>")
                in_ul = True
            html_lines.append(f"<li>{inline_format(ul_match.group(1))}</li>")
            continue

        # Lists: Ordered
        ol_match = re.match(r"^\d+\.\s+(.*)", stripped)
        if ol_match:
            if not in_ol:
                html_lines.extend(close_lists())
                html_lines.append("<ol>")
                in_ol = True
            html_lines.append(f"<li>{inline_format(ol_match.group(1))}</li>")
            continue

        # Paragraph
        html_lines.extend(close_lists())
        html_lines.append(f"<p>{inline_format(stripped)}</p>")

    html_lines.extend(close_lists())
    html_lines.extend(close_table())

    return "\n".join(html_lines)


def main():
    print(f"[*] Reading markdown specification: {SPEC_FILE}...")
    if not SPEC_FILE.is_file():
        print(f"[ERROR] Specification file not found: {SPEC_FILE}")
        sys.exit(1)

    md_content = SPEC_FILE.read_text(encoding="utf-8")
    confluence_html = markdown_to_confluence_html(md_content)

    client = AtlassianClient()
    if not client.configured:
        print("[ERROR] AtlassianClient is not configured.")
        sys.exit(1)

    print(f"[*] Fetching existing Confluence page (ID: {PAGE_ID})...")
    existing_page = client._http_request("GET", f"wiki/rest/api/content/{PAGE_ID}?expand=version,space")
    current_version = existing_page["version"]["number"]
    space_key = existing_page["space"]["key"]
    new_version = current_version + 1

    print(f"[*] Updating Confluence page '{PAGE_TITLE}' (ID: {PAGE_ID}) to version {new_version}...")
    payload = {
        "id": PAGE_ID,
        "type": "page",
        "title": PAGE_TITLE,
        "space": {"key": space_key},
        "body": {
            "storage": {
                "value": confluence_html,
                "representation": "storage"
            }
        },
        "version": {
            "number": new_version,
            "message": "Updated with Phase 6: TrackingMode, Subscription Gating & Manual Trip Inviolability"
        }
    }

    res = client._http_request("PUT", f"wiki/rest/api/content/{PAGE_ID}", payload)
    page_url = f"{client.base_url}/wiki/spaces/{space_key}/pages/{PAGE_ID}"
    print(f"✅ Confluence page successfully updated!")
    print(f"   Title   : {res.get('title')}")
    print(f"   Version : {res.get('version', {}).get('number')}")
    print(f"   URL     : {page_url}")

if __name__ == "__main__":
    main()
