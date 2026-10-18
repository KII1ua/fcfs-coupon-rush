#!/usr/bin/env python3
"""k6 실행 기록(텍스트)과 k6 HTML 리포트를 블로그 증빙용 PNG 로 만든다. 표준 라이브러리 + 로컬 Chrome 만 쓴다.

사용법: python3 deploy/capture.py <결과 폴더>
  <결과 폴더>/summary.txt  → summary.png  (터미널 모양으로 렌더링)
  <결과 폴더>/report.html  → report.png   (k6 웹 대시보드 내보내기, 있으면)
"""
import html
import re
import pathlib
import subprocess
import sys

CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"


PROGRESS = re.compile(r"^\S+\s+(✓|✗)?\s*\[\s*\d+%\s*\]")
STEP_TITLE = re.compile(r"^\| (== .*?)\s*\|$")


def clean(lines: list[str]) -> list[str]:
    """증빙에 필요 없는 줄을 빼고, MySQL 이 표로 감싼 단계 제목을 한 줄 제목으로 바꾼다."""
    running = [k for k, l in enumerate(lines) if l.startswith("running (")]
    last_running = running[-1] if running else -1
    out: list[str] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # k6 진행 표시줄은 마지막 100% 줄만 남긴다
        if (PROGRESS.match(line) and "100%" not in line) or (line.startswith("Run ") and "setup()" in line):
            i += 1
            continue
        if line.startswith("running (") and i != last_running:   # 1초마다 찍히는 진행 상황은 마지막 줄만
            i += 1
            continue
        if "docker exec" in line:   # 로컬 실행용 안내 문구
            i += 1
            continue
        # MySQL 제목 표: +--+ / | step | / +--+ / | == 제목 | / +--+  →  "== 제목" 한 줄
        if line.startswith("| step"):
            if out and out[-1].startswith("+-"):
                out.pop()
            title = STEP_TITLE.match(lines[i + 2]) if i + 2 < len(lines) else None
            if title:
                out.append(title.group(1))
                i += 4
                # 바로 다음이 표가 아니면 그 단계는 결과가 0행이다
                has_table = i + 1 < len(lines) and lines[i].startswith("+-") and not lines[i + 1].startswith("| step")
                if not has_table:
                    out.append("(해당 없음: 0행)")
                continue
        out.append(line)
        i += 1
    # 빈 줄이 여러 개 이어지면 하나로 줄인다
    return [l for k, l in enumerate(out) if l.strip() or (k > 0 and out[k - 1].strip())]


def colorize(line: str) -> str:
    text = html.escape(line)
    if "✗" in line or "crossed" in line or "level=error" in line:
        return f'<span class="bad">{text}</span>'
    if "✓" in line:
        return f'<span class="good">{text}</span>'
    if line.startswith("(해당 없음"):
        return f'<span class="good">{text}</span>'
    if line.startswith("==") or line.startswith("#"):
        return f'<span class="head">{text}</span>'
    return text


def render_summary(folder: pathlib.Path) -> None:
    lines = clean((folder / "summary.txt").read_text(encoding="utf-8").splitlines())
    body = "\n".join(colorize(l) for l in lines)
    page = folder / "summary.render.html"
    page.write_text(f"""<!doctype html><meta charset="utf-8">
<style>
  body {{ margin: 0; background: #1e1e2e; }}
  pre {{ margin: 0; padding: 28px 32px; color: #cdd6f4; font: 14px/1.5 Menlo, "D2Coding", "Apple SD Gothic Neo", monospace; white-space: pre; }}
  .good {{ color: #a6e3a1; }} .bad {{ color: #f38ba8; font-weight: bold; }} .head {{ color: #89b4fa; font-weight: bold; }}
</style><pre>{body}</pre>""", encoding="utf-8")
    width = min(1800, max(900, max(len(l) for l in lines) * 9 + 80))
    height = len(lines) * 21 + 70
    shoot(page, folder / "summary.png", width, height)


def render_report(folder: pathlib.Path) -> None:
    report = folder / "report.html"
    if report.exists():
        shoot(report, folder / "report.png", 1400, 2600, wait_ms=8000)


def shoot(page: pathlib.Path, out: pathlib.Path, width: int, height: int, wait_ms: int = 1000) -> None:
    subprocess.run([
        CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=2",
        f"--virtual-time-budget={wait_ms}", f"--window-size={width},{height}",
        f"--screenshot={out}", page.resolve().as_uri(),
    ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print(f"캡처: {out}")


if __name__ == "__main__":
    target = pathlib.Path(sys.argv[1])
    render_summary(target)
    render_report(target)
