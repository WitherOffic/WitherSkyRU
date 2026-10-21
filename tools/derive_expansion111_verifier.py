"""Mechanical independent PowerShell delivery check with fixed artifact identities."""
from prepare_npc78 import ROOT
def main():
    s=(ROOT/'tools/verify_game110_delivery.ps1').read_text(encoding='utf-8-sig')
    for old,new in [('2026-10-20_7','2026-10-20_8'),('2026-10-20_6','2026-10-20_7'),('0.15.110','0.15.111'),('0.15.109','0.15.110'),('5652AC2767172D6AE046DA10742BF8D0A63A8F25A3CB863AE19E37A8446D8476','B7549C4076E333C2031DD812C11C10C60908A2FD9EBB948FE850D62A684F5126'),('120A6C334CE41095E37895820439B4028F20006CEFF6AB7B6F535E350BD29B74','EE1D97D3CAF2A1BA7C821AEDD187D4C83D3309F6453D0C57245C99A2C5493563')]:s=s.replace(old,new)
    (ROOT/'tools/verify_expansion111_delivery.ps1').write_text(s,encoding='utf-8')
if __name__=='__main__':main()
