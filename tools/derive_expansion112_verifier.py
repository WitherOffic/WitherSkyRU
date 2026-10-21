"""Independent PowerShell hash/ZIP checker pinned to112 and the immutable111 delivery."""
from prepare_npc78 import ROOT

def main():
    text=(ROOT/'tools/verify_expansion111_delivery.ps1').read_text(encoding='utf-8-sig')
    for old,new in [('2026-10-20_8','2026-10-20_9'),('2026-10-20_7','2026-10-20_8'),('0.15.111','0.15.112'),('0.15.110','0.15.111'),('B7549C4076E333C2031DD812C11C10C60908A2FD9EBB948FE850D62A684F5126','C856979A7AEEAC1DB18DBCFD46F941CB4E391E0A381002E89BCF61793E2EF997'),('EE1D97D3CAF2A1BA7C821AEDD187D4C83D3309F6453D0C57245C99A2C5493563','866E0C2277F876229F4EFA98C388CE942F8E548181EDDBDC0FEB96A91AA0095B')]:text=text.replace(old,new)
    (ROOT/'tools/verify_expansion112_delivery.ps1').write_text(text,encoding='utf-8')

if __name__=='__main__':main()
