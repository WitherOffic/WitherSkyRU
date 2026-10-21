"""Bounded frozen-ledger report plus independently counted current-revision additions."""
from prepare_npc102 import ROOT
BASE='E0EE8AA7086809A24B001F57F71902C778349215670C983BAA1382AF82606A09'
def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def versions(s):return s.replace('0.15.101','0.15.102').replace('0.15.100','0.15.101').replace('npc-expansion101','npc-expansion102').replace('NPC101','NPC102').replace('Npc101','Npc102').replace('Choices101','Choices102').replace('Parser101','Parser102').replace('prepare_npc101','prepare_npc102').replace('report_npc101','report_npc102').replace('1FE0127216BD7B39D4F41DEB976F70F25E6DBB1FA2B23B8B32499FDD856B7117',BASE)
s=versions((ROOT/'tools/report_npc101.py').read_text(encoding='utf-8'))
changes={
 '10 391 · 63,97% | 10 776 · 66,34%':'10 776 · 66,34% | 11 092 · 68,28%',
 '5 853 | 5 468 · 33,66%':'5 468 | 5 152 · 31,72%',
 '385 новых реплик / 79 обозначений говорящего, включая 12 телефонных реплик. Шесть точных групп ответов / семь подписей / шесть опубликованных оформлений. Сверены 83 страницы: 82 ревизии совпали; на Professor Wynd изменилась только запись истории, полные диалоги и условия совпадают.':'319 новых переводов / 142 обозначения говорящего: 316 ключей очереди и 3 свежие реплики сверх неё. Четыре телефонных реплики. Семь точных групп ответов / семь подписей / семь опубликованных оформлений. Сверены 148 страниц: 142 ревизии совпали, на шести изменения. Все прежние шаблоны диалогов сохранены; новые строки рассмотрены отдельно.',
 '| Всего | 385 |':'| Очередь | 316 |\n| Свежие сверх очереди: Jeff | 2 |\n| Свежие сверх очереди: Ghost of Armorsmith | 1 |\n| Всего новых переводов | 319 |',
 '544 выбранные строки: 385 новых, 136 прежних, 16 с переменными, 6 нейтральных, 1 прежний динамический перевод. Отдельно сохранена одна неоднозначно оформленная кнопка. Исправлена офлайн-классификация одной постоянной реплики Fragilis. Alpha-условия, стандартный островной NPC и звуки животных не объявлены полноценными новыми сюжетными ветками.':'917 строк очереди: 316 новых, 530 прежних, 43 с переменными, 15 нейтральных, 5 проблем извлечения, 6 прежних динамических результатов, 2 Removed Dialogue. Вне очереди найдены четыре свежие строки: три новые и одна уже переведённая. Только 316 увеличивают числитель старой очереди. Радужный текст, обратная речь и формула проверены; исторические/alpha-условия и звуки не выданы за полноценные текущие сюжетные ветки.',
 '171 группа заново проверена':'174 группы заново проверены',
 '11 248 строк все 385 новых переводов':'11 252 строк все 319 новых переводов',
 'Остальные 10 863 результата':'Остальные 10 933 результата',
 '100 → 101 → 101 → 100':'101 → 102 → 102 → 101',
 '5 468 строк без':'5 152 строки без'
}
for a,b in changes.items():assert a in s,a;s=s.replace(a,b.replace('\n','\\n'))
s=s.replace('подписанный/пользовательский чат.', 'подписанный/пользовательский чат. Отдельный набор проверяет3 новые строки API и сохранение прежнего перевода четвёртой.')
emit('tools/report_npc102.py',s)
s=versions((ROOT/'tools/finalize_npc101.py').read_text(encoding='utf-8'))
for a,b in [('==171','==174'),("{'TestNpc102','TestNpcChoices102'}","{'TestNpc102','TestNpcChoices102','TestNpcCurrent102'}"),("assertion('TestNpc102','NPC102')","assertion('TestNpc102','Npc102')"),('==385','==319'),('==10863','==10933'),('==10776','==11092'),('==11248','==11252'),('==12\n','==4\n'),('[544,260,83,16,0,6,136,0]','[917,471,148,43,5,15,530,2]'),("for r in evidence['records'] if r['runtime_registration'])==319","for r in evidence['records'] if r['runtime_registration'])==317"),("len(choices['records'])==6","len(choices['records'])==7"),("for r in choices['records'])==6","for r in choices['records'])==7"),('==83','==148'),('171_fresh','174_fresh'),('fresh_test_groups=171','fresh_test_groups=174'),('dispatch_tested_rows=11248','dispatch_tested_rows=11252'),('dispatch_new_exact_results=385','dispatch_new_exact_results=319'),('unchanged_other_dispatch_rows=10863','unchanged_other_dispatch_rows=10933'),('extraction_pending_rows=0','extraction_pending_rows=5'),('runtime_retained_rows=1','runtime_retained_rows=6'),('malformed_choice_rows=1','malformed_choice_rows=0'),('Новых в 101','Новых в 102'),('checked101','checked102'),('pending101','pending102')]:s=s.replace(a,b)
needle="new={r['source']:r['translation'] for r in load(DATA/'npc-expansion102.json')['records']};assert len(new)==319 and sum(': ✆ ' in s for s in new)==4";assert needle in s
s=s.replace(needle,"new={r['source']:r['translation'] for r in load(DATA/'npc-expansion102.json')['records']};assert len(new)==316 and sum(': ✆ ' in s for s in new)==4\n extra={r['source']:r['translation'] for r in load(DATA/'npc-current102.json')['records']};assert len(extra)==3 and not new.keys()&extra.keys()\n cur=load(SOURCE/'NPC_CURRENT_SOURCES_0.15.102.json');assert len(cur['records'])==4 and len(cur['retained_originals'])==1\n current_assertions=assertion('TestNpcCurrent102','NpcCurrent102')")
s=s.replace("assert all(r['entire_dialogue_section_including_conditions_unchanged'] and r['all_dialogue_templates_unchanged'] for r in delta['records'])","assert all(r['old_dialogue_templates_retained'] for r in delta['records'])")
s=s.replace('accepted_for_batch101','accepted_for_batch102')
s=s.replace(' assert authored==new', ''' assert authored==new
 current_authored={}
 for line in (SOURCE/'NPC_CURRENT_0.15.102_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
  if line and not line.startswith('#'):f=line.split('|');current_authored[f'[NPC] {f[0]}: {f[1]}']=f'[NPC] {f[0]}: {f[2]}'
 assert current_authored==extra''')
s=s.replace("stats.update(runtime_validation=", "stats.update(new_current_literal_rows=3,retained_current_rows=1,total_runtime_new_literals=319,current_fixture_assertions=current_assertions,runtime_validation=")
s=s.replace("DATA/'npc-expansion102.json',ROOT/", "DATA/'npc-expansion102.json',DATA/'npc-current102.json',ROOT/")
s=s.replace("'prepare_npc100.py','review_npc92_revision.py'", "'prepare_npc100.py','prepare_npc101.py','review_npc92_revision.py'")
s=s.replace("glob('*npc101*')","glob('*npc102*')")
s=s.replace('385 новых реплик / 79 обозначений говорящего. 6 групп ответов / 7 подписей.','319 новых переводов: 316 ключей очереди и 3 свежие реплики сверх неё. 7 групп ответов / 7 подписей.').replace('10 776 / 16 244 · 66,34%','11 092 / 16 244 · 68,28%').replace('Остаток 5 468.','Остаток 5 152.').replace('171 группа + 15','174 группы + 15').replace('Progress10776/16244=66.34%;remaining5468','Progress11092/16244=68.28%;remaining5152;3 current additions separate')
emit('tools/finalize_npc102.py',s)
print('Derived separate frozen and current counts;174 fresh groups;319 new outputs;no old delivery changes.')
