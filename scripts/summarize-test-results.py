#!/usr/bin/env python3
"""Print real JUnit counts and fail if a suite is missing, failed, or skipped."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('directories', nargs='+', type=Path)
args = parser.parse_args()
failed = False
for directory in args.directories:
    files = sorted(directory.rglob('TEST-*.xml'))
    if not files:
        print(f'MISSING JUNIT RESULTS: {directory}', flush=True)
        failed = True
        continue
    totals = dict(tests=0, passed=0, failures=0, errors=0, skipped=0)
    for file in files:
        root = ET.parse(file).getroot()
        suites = [root] if root.tag == 'testsuite' else list(root.iter('testsuite'))
        for suite in suites:
            cases = suite.findall('testcase')
            counts = dict(tests=len(cases),
                failures=sum(case.find('failure') is not None for case in cases),
                errors=sum(case.find('error') is not None for case in cases),
                skipped=sum(case.find('skipped') is not None for case in cases))
            if any(suite.get(key) is not None and int(suite.get(key)) != value for key, value in counts.items()):
                raise ValueError(f'Inconsistent JUnit counters: {file}')
            counts['passed'] = counts['tests'] - counts['failures'] - counts['errors'] - counts['skipped']
            if counts['passed'] < 0:
                raise ValueError(f'Inconsistent JUnit cases: {file}')
            classes = {}
            for case in cases:
                name = case.get('classname') or suite.get('name') or 'unnamed'
                group = classes.setdefault(name, dict(tests=0, passed=0, failures=0, errors=0, skipped=0))
                group['tests'] += 1
                outcome = next((key for key, tag in [('failures', 'failure'), ('errors', 'error'), ('skipped', 'skipped')]
                    if case.find(tag) is not None), 'passed')
                group[outcome] += 1
            for name, group in sorted(classes.items()):
                print('SUITE ' + json.dumps(dict(name=name, **group)), flush=True)
            for key, value in counts.items():
                totals[key] += value
    print('SUMMARY ' + json.dumps(dict(directory=str(directory), files=len(files), **totals)), flush=True)
    failed |= totals['tests'] == 0 or any(totals[key] for key in ('failures', 'errors', 'skipped'))
raise SystemExit(1 if failed else 0)
