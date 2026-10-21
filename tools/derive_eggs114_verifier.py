"""Independent PowerShell verification of114 delivery and unchanged113 package."""
from prepare_npc78 import ROOT

def main():
    s=(ROOT/'tools/verify_npc113_delivery.ps1').read_text(encoding='utf-8-sig')
    for old,new in [('2026-10-20_10','2026-10-20_11'),('2026-10-20_9','2026-10-20_10'),('0.15.113','0.15.114'),('0.15.112','0.15.113'),('A714508C7AD1F1B57DEE20CB38227F624FE4ED4FF282320CF1215BAF61AFA072','B0263267FCAA2DD3472676E82D197441938564A21FDB9E54AA8113F0F8E46CDE'),('4350073540D2C94A291C9D8B9390D9BFA731C2F26B337487741AB5D10768A1BE','74AB75EB19EB4B679989A588ACE0CD4DA86417CA5E1AFD93E799BCF97F8C1789')]:s=s.replace(old,new)
    (ROOT/'tools/verify_eggs114_delivery.ps1').write_text(s,encoding='utf-8')

if __name__=='__main__':main()
