"""New mechanical probe harness;all113 sources and deliveries retained."""
from prepare_npc78 import ROOT

def main():
    s=(ROOT/'tests/AuditExpansion112.java').read_text(encoding='utf-8-sig').replace('AuditExpansion112','AuditExpansion114').replace('EXPANSION112','EXPANSION114').replace('EXPANSION_SELECTED_0.15.112','EXPANSION_PROBE_0.15.114')
    (ROOT/'tests/AuditExpansion114.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/snapshot_expansion112.ps1').read_text(encoding='utf-8-sig').replace('0.15.111-dev','0.15.113-dev').replace('expansion112-snapshots','expansion114-snapshots').replace('AuditExpansion112','AuditExpansion114').replace('EXPANSION112','EXPANSION114')
    (ROOT/'tools/snapshot_expansion114.ps1').write_text(s,encoding='utf-8')

if __name__=='__main__':main()
