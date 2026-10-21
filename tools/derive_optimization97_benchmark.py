"""Mechanical update of the frozen receipt benchmark's name/scope only."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / 'tests/BenchmarkOptimization96.java').read_text(encoding='utf-8-sig')
source = source.replace('BenchmarkOptimization96', 'BenchmarkOptimization97')
source = source.replace('modes95/96', 'modes96/97').replace('on95 and96', 'on96 and97')
(root / 'tests/BenchmarkOptimization97.java').write_text(source, encoding='utf-8')
