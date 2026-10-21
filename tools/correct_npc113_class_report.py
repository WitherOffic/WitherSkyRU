"""Mechanical byte-identical count correction,not a runtime modification."""
from prepare_npc78 import ROOT

def main():
    p=ROOT/'tools/finalize_npc113.py';s=p.read_text(encoding='utf-8')
    for old,new in [("'byte_identical_runtime_classes':121","'byte_identical_runtime_classes':120"),('121 из 125 классов','120 из 125 классов'),("p.name=='NPC113_TEST_FIXTURE_CORRECTION.json'","p.name in ('NPC113_TEST_FIXTURE_CORRECTION.json','NPC113_CLASSFILE_AUDIT_CORRECTION.json')")]:
        assert s.count(old)==1;s=s.replace(old,new)
    old='Классы отрисовки, HUD, клиентского распределения сообщений и словаря не изменены.'
    new='Ещё у одного вложенного класса изменились только отладочные номера строк: после исключения таблицы номеров все остальные байты совпадают. Классы отрисовки, HUD, клиентского распределения сообщений и словаря не изменены.'
    assert s.count(old)==1;s=s.replace(old,new);p.write_text(s,encoding='utf-8')

if __name__=='__main__':main()
