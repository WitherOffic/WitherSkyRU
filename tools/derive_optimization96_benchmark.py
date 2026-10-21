"""Frozen equal-work harness with only its class name and version-label wording updated."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / 'tests/BenchmarkOptimization94.java').read_text(encoding='utf-8-sig')
source = source.replace('BenchmarkOptimization94', 'BenchmarkOptimization96')
source = source.replace('modes93/94', 'modes95/96').replace('on93 and94', 'on95 and96')
(root / 'tests/BenchmarkOptimization96.java').write_text(source, encoding='utf-8')
