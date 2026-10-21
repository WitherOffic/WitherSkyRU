"""Mechanically reuse the receipt-dispatch snapshot harness on a new immutable probe."""
from prepare_npc78 import ROOT

def main():
    src=(ROOT/'tests/AuditExpansion114.java').read_text(encoding='utf-8-sig')
    src=src.replace('AuditExpansion114','AuditNpc115').replace('EXPANSION_PROBE_0.15.114','NPC_RECOVERY_PROBE_0.15.115').replace('EXPANSION114','NPC115')
    (ROOT/'tests/AuditNpc115.java').write_text(src,encoding='utf-8')
    src=(ROOT/'tools/snapshot_expansion114.ps1').read_text(encoding='utf-8-sig')
    src=src.replace('0.15.113-dev','0.15.114-dev').replace('expansion114-snapshots','npc115-snapshots').replace('AuditExpansion114','AuditNpc115').replace('EXPANSION114','NPC115')
    (ROOT/'tools/snapshot_npc115.ps1').write_text(src,encoding='utf-8')

if __name__=='__main__':main()
