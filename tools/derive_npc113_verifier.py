"""Independent PowerShell ZIP/hash gate,including unchanged112 package."""
from prepare_npc78 import ROOT

def main():
    s=(ROOT/'tools/verify_expansion112_delivery.ps1').read_text(encoding='utf-8-sig')
    for old,new in [('2026-10-20_9','2026-10-20_10'),('2026-10-20_8','2026-10-20_9'),('0.15.112','0.15.113'),('0.15.111','0.15.112'),('C856979A7AEEAC1DB18DBCFD46F941CB4E391E0A381002E89BCF61793E2EF997','A714508C7AD1F1B57DEE20CB38227F624FE4ED4FF282320CF1215BAF61AFA072'),('866E0C2277F876229F4EFA98C388CE942F8E548181EDDBDC0FEB96A91AA0095B','4350073540D2C94A291C9D8B9390D9BFA731C2F26B337487741AB5D10768A1BE')]:s=s.replace(old,new)
    (ROOT/'tools/verify_npc113_delivery.ps1').write_text(s,encoding='utf-8')

if __name__=='__main__':main()
