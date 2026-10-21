"""Mechanical egg114 harnesses;prior artifacts remain immutable."""
import argparse
from prepare_npc78 import ROOT

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--selected',action='store_true');a=ap.parse_args()
    s=(ROOT/'tests/AuditExpansion112.java').read_text(encoding='utf-8-sig').replace('AuditExpansion112','AuditEggs114').replace('EXPANSION112','EGGS114').replace('EXPANSION_SELECTED_0.15.112','EGG_SELECTED_0.15.114' if a.selected else 'EGG_PROBE_0.15.114')
    (ROOT/'tests/AuditEggs114.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/snapshot_expansion112.ps1').read_text(encoding='utf-8-sig').replace('0.15.111-dev','0.15.113-dev').replace('expansion112-snapshots','eggs114-snapshots').replace('AuditExpansion112','AuditEggs114').replace('EXPANSION112','EGGS114')
    (ROOT/'tools/snapshot_eggs114.ps1').write_text(s,encoding='utf-8')

if __name__=='__main__':main()
