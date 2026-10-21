"""Derive113 probe harness without touching earlier sources or deliveries."""
from prepare_npc78 import ROOT

def main():
    s=(ROOT/'tests/AuditExpansion112.java').read_text(encoding='utf-8-sig').replace('AuditExpansion112','AuditNpc113').replace('EXPANSION112','NPC113').replace('EXPANSION_SELECTED_0.15.112','NPC_PROBE_0.15.113')
    old='out.addProperty("source",source);out.addProperty("flat",output(source));';new='out.addProperty("source",source);out.addProperty("probe_source",r.get("probe_source").getAsString());out.addProperty("flat",output(r.get("probe_source").getAsString()));';assert s.count(old)==1;s=s.replace(old,new)
    (ROOT/'tests/AuditNpc113.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/snapshot_expansion112.ps1').read_text(encoding='utf-8-sig').replace('0.15.111-dev','0.15.112-dev').replace('expansion112-snapshots','npc113-snapshots').replace('AuditExpansion112','AuditNpc113').replace('EXPANSION112','NPC113')
    (ROOT/'tools/snapshot_npc113.ps1').write_text(s,encoding='utf-8')

if __name__=='__main__':main()
