"""Mechanical adaptation of validated delivery checks, with explicit101 scope changes."""
from prepare_npc101 import ROOT
BASE='1FE0127216BD7B39D4F41DEB976F70F25E6DBB1FA2B23B8B32499FDD856B7117'
def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def versions(s):return s.replace('0.15.100','0.15.101').replace('0.15.99','0.15.100').replace('npc-expansion100','npc-expansion101').replace('NPC100','NPC101').replace('Npc100','Npc101').replace('Choices100','Choices101').replace('Parser100','Parser101').replace('prepare_npc100','prepare_npc101').replace('report_npc100','report_npc101').replace('E3441D47BA2C3E9D6ADC745B7BBA3A1F602DC7E4E61719370FAA0BADEEEEAB6A',BASE)
s=versions((ROOT/'tools/report_npc100.py').read_text(encoding='utf-8'))
replacements={
 '10 009 · 61,62% | 10 391 · 63,97%':'10 391 · 63,97% | 10 776 · 66,34%',
 '6 235 | 5 853 · 36,03%':'5 853 | 5 468 · 33,66%',
 '382 новые реплики / 54 обозначения НПС, включая семь телефонных реплик. Восемь точных групп ответов / девять подписей / восемь опубликованных оформлений. Сверены 55 текущих публичных ревизий — все совпали с зафиксированными.':'385 новых реплик / 79 обозначений говорящего, включая 12 телефонных реплик. Шесть точных групп ответов / семь подписей / шесть опубликованных оформлений. Сверены 83 страницы: 82 ревизии совпали; на Professor Wynd изменилась только запись истории, полные диалоги и условия совпадают.',
 '| Всего | 382 |':'| Всего | 385 |',
 '405 выбранных строк: 382 новые, 6 прежних, 14 с переменными, 1 проблема извлечения, 2 нейтральные. Архив Dante и другие ограничения отмечены отдельно. Требуемая английская фраза для задания Eleanor намеренно сохранена.':'544 выбранные строки: 385 новых, 136 прежних, 16 с переменными, 6 нейтральных, 1 прежний динамический перевод. Отдельно сохранена одна неоднозначно оформленная кнопка. Исправлена офлайн-классификация одной постоянной реплики Fragilis. Alpha-условия, стандартный островной NPC и звуки животных не объявлены полноценными новыми сюжетными ветками.',
 '169 групп заново проверены':'171 группа заново проверена',
 '11 247 строк все 382 новых перевода':'11 248 строк все 385 новых переводов',
 'Остальные 10 865 результатов':'Остальные 10 863 результата',
 '99 → 100 → 100 → 99':'100 → 101 → 101 → 100',
 '5 853 строки без':'5 468 строк без'
}
for x,y in replacements.items():assert x in s,x;s=s.replace(x,y)
emit('tools/report_npc101.py',s)
s=versions((ROOT/'tools/finalize_npc100.py').read_text(encoding='utf-8')).replace('finalize_npc100','finalize_npc101')
for x,y in [('==169','==171'),('169_fresh','171_fresh'),('fresh_test_groups=169','fresh_test_groups=171'),("==382","==385"),("==10865","==10863"),("==10391","==10776"),("==11247","==11248"),('==384','==385'),('==7\n','==12\n'),('[405,220,55,14,1,2,6,0]','[544,260,83,16,0,6,136,0]'),("len(choices['records'])==8","len(choices['records'])==6"),("for r in choices['records'])==9","for r in choices['records'])==7"),("for r in choices['records'])==8","for r in choices['records'])==6"),('fresh_test_groups=169','fresh_test_groups=171'),('dispatch_tested_rows=11247','dispatch_tested_rows=11248'),('dispatch_new_exact_results=382','dispatch_new_exact_results=385'),('unchanged_other_dispatch_rows=10865','unchanged_other_dispatch_rows=10863'),('extraction_pending_rows=1','extraction_pending_rows=0'),('171_fresh_groups_and15_parser_tests_passed','171_fresh_groups_and15_parser_tests_passed')]:s=s.replace(x,y)
old="revision=load(SOURCE/'NPC_0.15.101_PUBLIC_REVISION.json');assert revision['matches_pinned_corpus'] and revision['expected']==revision['actual'] and len(revision['actual'])==55"
assert old in s
s=s.replace(old,"revision=load(SOURCE/'NPC_0.15.101_PUBLIC_REVISION.json');delta=load(SOURCE/'NPC_0.15.101_REVISION_DELTA.json');assert delta['accepted_for_batch101'] and len(revision['actual'])==83\n assert {k:(revision['expected'][k],v) for k,v in revision['actual'].items() if revision['expected'].get(k)!=v}=={r['page']:(r['pinned_revision'],r['current_revision']) for r in delta['records']}\n assert all(r['entire_dialogue_section_including_conditions_unchanged'] and r['all_dialogue_templates_unchanged'] for r in delta['records'])")
s=s.replace("if s.startswith('[NPC] Dante: ') and s in new:r['published_context_caveat100']='archive_not_live_availability_certification'", "if s in {\"[NPC] Ghost of Goon: I'm looking for something.\",'[NPC] Ghost of Goon: Something with...reviving capabilities...','[NPC] Ghost of Goon: Thank you...thank you...may he return...'}:r['published_context_caveat101']='inherited_alpha_garden_header_Dante_Goon_rowspan3_not_live_certification'")
s=s.replace("source_review_notes='NPC_0.15.101_REVIEW_NOTES.md'", "runtime_retained_rows=1,malformed_choice_rows=1,source_review_notes='NPC_0.15.101_REVIEW_NOTES.md'")
s=s.replace('authored_and_fixture_checked100','authored_and_fixture_checked101').replace('structured_dynamic_review_pending100','structured_dynamic_review_pending101').replace('malformed_publication_pending100','malformed_publication_pending101').replace('Новых в 100','Новых в 101')
s=s.replace("  if s in pending:r['review_status']", "  if s==\"[NPC] Fragilis: Y'know now that we are getting deeper and deeper a lot of these blocks are getting harder to mine!\":r['reviewed_classifier_exception101']='constant sentence confirmed against full pinned template; frozen variable_definition flag not rewritten'\n  if s in pending:r['review_status']")
s=s.replace("glob('*npc100*')","glob('*npc101*')").replace("'prepare_npc99.py','export_npc_corpus77.py'","'prepare_npc99.py','prepare_npc100.py','review_npc92_revision.py','export_npc_corpus77.py'")
s=s.replace('382 новые реплики / 54 обозначения говорящего. 8 групп ответов / 9 подписей.','385 новых реплик / 79 обозначений говорящего. 6 групп ответов / 7 подписей.').replace('10 391 / 16 244 · 63,97%','10 776 / 16 244 · 66,34%').replace('Остаток 5 853.','Остаток 5 468.').replace('169 групп + 15','171 группа + 15').replace('Progress10391/16244=63.97%;remaining5853','Progress10776/16244=66.34%;remaining5468')
emit('tools/finalize_npc101.py',s)
print('Derived bounded101 report and immutable delivery checks.')
