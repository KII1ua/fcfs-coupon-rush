#!/usr/bin/env python3
"""k6 요약의 '/s' 값은 setup(유저 생성) 시간까지 포함한 전체 시간으로 나눈 값이다.
도착률 시나리오에서 발급 요청만의 실제 처리량(건수 ÷ 시나리오 시간)을 계산해 한 줄로 출력한다.

사용법: python3 deploy/throughput.py <summary.txt>
"""
import re
import sys

text = open(sys.argv[1], encoding="utf-8").read()
count = re.search(r"http_reqs\.+:.*\n\s+[✓✗]? ?\{ name:issue \}\.+: (\d+)", text)
duration = re.search(r"\] \d+/\d+ VUs\s+(\d+(?:\.\d+)?)s\s+[\d.]+ iters/s", text)
dropped = re.search(r"dropped_iterations\.+: (\d+)", text)
if count and duration:
    n, sec = int(count.group(1)), float(duration.group(1))
    line = f"== 발급 요청 실제 처리량: {n}건 / {sec:g}초 = {n / sec:.0f} req/s (setup 시간 제외)"
    if dropped:
        line += f", 못 보낸 요청(dropped) {dropped.group(1)}건"
    print(line)
