#!/usr/bin/env python3
"""
Standalone MUST Timetable Scraper
=================================
Scrapes https://timetable.must.ac.ug/index_teaching.html
and outputs normalized JSON matching the Base44 schema.

Usage:
    python3 scraper/timetable_scraper.py
    python3 scraper/timetable_scraper.py -o src/main/assets/timetable_export.json
    python3 scraper/timetable_scraper.py --rooms-url https://timetable.must.ac.ug/index_rooms_teaching.html

Schema (matches Base44 TimetableEntry):
    program_group: str (e.g. "MBR I", "BSE IV")
    day: str (Monday..Sunday)
    time_slot: str (e.g. "08:00-9:00")
    start_time: str HH:MM (24h)
    end_time: str HH:MM (24h)
    course_code: str (e.g. "COMP2101", "OBG5112/SUG5112")
    course_title: str
    session_type: str|null (THEORY|PRACTICAL|CLINICAL|LAB)
    lecturer: str|null
    room: str|null
    shared_with: list[str]

Derived from base44/functions/refreshTimetable/entry.ts
"""

import argparse
import json
import re
import sys
from pathlib import Path

try:
    import urllib.request
except ImportError:
    print("Error: urllib not available", file=sys.stderr)
    sys.exit(1)

TIMETABLE_URL = "https://timetable.must.ac.ug/index_teaching.html"
ROOMS_URL = "https://timetable.must.ac.ug/index_rooms_teaching.html"
USER_AGENT = "MUST-Scholar/1.0"


def fetch_html(url: str) -> str:
    """Fetch HTML from URL."""
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as resp:
        if resp.status != 200:
            raise RuntimeError(f"Fetch failed: HTTP {resp.status}")
        return resp.read().decode("utf-8", errors="replace")


def fallback_code(title: str) -> str:
    """Derive a course code for non-standard cells."""
    t = re.sub(r"[*#]+", "", title).strip()
    m = re.match(r"^([A-Za-z][A-Za-z0-9-]{2,})\s+", t)
    if m:
        return m[1].upper()
    code = re.sub(r"[^A-Za-z0-9]+", "", t).upper()[:12]
    return code or "CLASS"


def cell_to_fields(content: str):
    """Parse a cell's <br>-separated lines into structured fields."""
    lines = [
        re.sub(r"\s+", " ", l.replace("&amp;", "&").replace("&nbsp;", " ")).strip()
        for l in re.split(r"<br\s*/?>", content)
    ]
    lines = [l for l in lines if l]
    if not lines:
        return None

    shared_raw = [s.strip() for s in lines[0].split(",") if s.strip()]

    course_code = None
    course_title = None
    session_type = None

    if len(lines) > 1:
        line = lines[1]
        cm = re.match(
            r"^([A-Z]{2,5}\s?\d{3,5}[A-Z]?(?:\s*/\s*[A-Z]{2,5}\s?\d{3,5}[A-Z]?)?)\s+(.+)$",
            line,
            re.IGNORECASE,
        )
        if cm:
            course_code = re.sub(r"\s+", "", cm.group(1)).upper()
            course_title = cm.group(2).strip()
        else:
            course_title = re.sub(r"[*#]+", "", line).strip()
            course_code = fallback_code(course_title)

        st = re.search(r"\s+(THEORY|PRACTICAL|CLINICAL|LAB)$", course_title or "", re.IGNORECASE)
        if st:
            session_type = st.group(1).upper()
            course_title = course_title[: st.start()].strip()

    lecturer = None
    room = None
    if len(lines) >= 4:
        lecturer = lines[2] or None
        room = lines[3] or None
    elif len(lines) == 3:
        room = lines[2] or None

    if room and re.match(r"^(MON|TUE|WED|THU|FRI|SAT|SUN)", room, re.IGNORECASE):
        room = None

    return {
        "sharedRaw": shared_raw,
        "courseCode": course_code,
        "courseTitle": course_title,
        "sessionType": session_type,
        "lecturer": lecturer,
        "room": room,
    }


def build_entries(html: str) -> list:
    """Extract all TimetableEntry records from HTML."""
    entries = []
    table_regex = re.compile(r'<table[^>]*id="table_\d+"[^>]*>([\s\S]*?)</table>')

    for table_match in table_regex.finditer(html):
        inner = table_match.group(1)

        grp_match = re.search(r'<th\s+colspan="7"[^>]*>([\s\S]*?)</th>', inner)
        group = re.sub(r"<[^>]+>", "", grp_match.group(1)).strip() if grp_match else None
        if not group:
            continue

        days = [x.group(1).strip() for x in re.finditer(r'<th class="xAxis">([^<]+)</th>', inner)]
        tbody_match = re.search(r"<tbody>([\s\S]*?)</tbody>", inner)
        if not tbody_match:
            continue

        tbody = tbody_match.group(1)
        row_htmls = [r.group(1) for r in re.finditer(r"<tr>([\s\S]*?)</tr>", tbody)]

        rows = []
        for row_html in row_htmls:
            ts_match = re.search(r'<th class="yAxis">([^<]+)</th>', row_html)
            time_slot = ts_match.group(1).strip() if ts_match else None

            real_cells = []
            td_regex = re.compile(r"<td([^>]*)>([\s\S]*?)</td>")
            rowspan_regex = re.compile(r'rowspan="(\d+)"')
            for c in td_regex.finditer(row_html):
                rs = rowspan_regex.search(c.group(1))
                real_cells.append({
                    "rowspan": int(rs.group(1)) if rs else 1,
                    "content": c.group(2).strip(),
                })
            rows.append({"timeSlot": time_slot, "realCells": real_cells})

        num_cols = len(days)
        pending = [0] * num_cols

        for ri, row in enumerate(rows):
            col = 0
            for cell in row["realCells"]:
                while col < num_cols and pending[col] > 0:
                    pending[col] -= 1
                    col += 1
                if col >= num_cols:
                    break
                if cell["content"]:
                    f = cell_to_fields(cell["content"])
                    if f and f["courseCode"]:
                        ts = row["timeSlot"] or ""
                        start_time = ts.split("-")[0].strip()
                        end_row_idx = ri + cell["rowspan"] - 1
                        end_slot = rows[end_row_idx]["timeSlot"] if end_row_idx < len(rows) else None
                        end_time = (end_slot or "").split("-")[1].strip() if end_slot else None
                        shared = [g for g in f["sharedRaw"] if g.upper() != group.upper()]

                        entries.append({
                            "program_group": group,
                            "day": days[col],
                            "time_slot": ts,
                            "start_time": start_time,
                            "end_time": end_time,
                            "course_code": f["courseCode"],
                            "course_title": f["courseTitle"],
                            "session_type": f["sessionType"],
                            "lecturer": f["lecturer"],
                            "room": f["room"],
                            "shared_with": shared,
                        })
                if cell["rowspan"] > 1:
                    pending[col] = cell["rowspan"] - 1
                col += 1

            while col < num_cols:
                if pending[col] > 0:
                    pending[col] -= 1
                col += 1

    return entries


def validate_entry(entry: dict) -> tuple:
    """Validate a single entry. Returns (is_valid, reason)."""
    if not entry.get("program_group"):
        return False, "Missing program_group"
    valid_days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    if entry.get("day") not in valid_days:
        return False, f"Invalid day: {entry.get('day')}"
    if not entry.get("start_time"):
        return False, "Missing start_time"
    if not entry.get("course_code"):
        return False, "Missing course_code"
    time_re = re.compile(r"^\d{2}:\d{2}$")
    if not time_re.match(entry.get("start_time", "")):
        return False, f"Invalid start_time format: {entry.get('start_time')}"
    if entry.get("end_time") and not time_re.match(entry["end_time"]):
        return False, f"Invalid end_time format: {entry.get('end_time')}"
    if entry.get("start_time") and entry.get("end_time"):
        # Normalize "9:00" → "09:00"
        st = entry["start_time"]
        et = entry["end_time"]
        if re.match(r"^\d:\d{2}$", st):
            entry["start_time"] = "0" + st
        if re.match(r"^\d:\d{2}$", et):
            entry["end_time"] = "0" + et
        if entry["start_time"] >= entry["end_time"]:
            return False, f"start_time >= end_time: {entry['start_time']} >= {entry['end_time']}"
    return True, "Valid"


def main():
    parser = argparse.ArgumentParser(description="Scrape MUST timetable")
    parser.add_argument("-o", "--output", help="Output JSON file path")
    parser.add_argument("--url", default=TIMETABLE_URL, help="Timetable URL")
    parser.add_argument("--validate", action="store_true", help="Validate entries")
    parser.add_argument("--pretty", action="store_true", help="Pretty print JSON")
    args = parser.parse_args()

    print(f"Fetching {args.url} ...", file=sys.stderr)
    html = fetch_html(args.url)
    print(f"HTML fetched: {len(html)} bytes", file=sys.stderr)

    entries = build_entries(html)
    print(f"Entries parsed: {len(entries)}", file=sys.stderr)

    if args.validate:
        valid = 0
        invalid = 0
        for e in entries:
            ok, reason = validate_entry(e)
            if ok:
                valid += 1
            else:
                invalid += 1
                print(f"  INVALID: {reason} | {e.get('program_group')} {e.get('course_code')}", file=sys.stderr)
        print(f"Validation: {valid} valid, {invalid} invalid", file=sys.stderr)

    groups = set(e["program_group"] for e in entries)
    print(f"Unique program groups: {len(groups)}", file=sys.stderr)

    output = json.dumps(entries, indent=2 if args.pretty else None, ensure_ascii=False)

    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(output, encoding="utf-8")
        print(f"Written to {out_path} ({len(output)} bytes)", file=sys.stderr)
    else:
        print(output)


if __name__ == "__main__":
    main()
