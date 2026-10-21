"""Prioritise user-reported NPCs without inventing missing literal utterances."""
import argparse,json,re
from pathlib import Path
from export_public_text116 import ROOT,rows,jsonl,write,digest,templates,split_parameters,provisional_text

def main():
    ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);a=ap.parse_args();root=a.root
    report=json.loads((ROOT/'translation-source/NPC_REPORTED_CASES_2026-10-21.json').read_text(encoding='utf-8'));names={r['npc'] for r in report['reports']};known=json.loads((root/'WORKTREE_EXACT_REFERENCE.json').read_text(encoding='utf-8'));selected=[];messages={}
    for p in rows(root/'source_pages.jsonl'):
        if p['title'] not in names|{'Harp',"Melody's Harp"} and not any(p['title'].startswith(n+'/') for n in names|{'Harp'}):continue
        selected.append(p)
        if p['title'] not in names:continue
        for start,end,name,raw in templates(p['raw_wikitext']):
            if name.strip().casefold()!='dialogue':continue
            text=provisional_text('|'.join(split_parameters(raw[2:-2])[1:]));text=re.sub('(?i)§[0-9a-fk-or]','',text)
            for line in text.splitlines():
                line=line.strip()
                speaker=line.split(':',1)[0].removeprefix('[NPC] ')
                if line.startswith('[NPC] ') and speaker in (p['title'],p['title']+' ♫'):
                    messages.setdefault(line,{'source':line,'npc':p['title'],'source_url':p['source_url'],'revision':p['revision'],'block_start':start,'block_end':end,'separate_exact_worktree_record_present':line in known,'existing_worktree_targets':known.get(line,[]),'status':'reported_partial_translation_requires_live_layout_and_editorial_review_not_all_untranslated','translation_ru':'','automatically_added_to_mod':False})
    jsonl(root/'REPORTED_NPC_SOURCE_PAGES.jsonl',selected);jsonl(root/'REPORTED_NPC_PRIORITY_QUEUE.jsonl',(messages[s] for s in sorted(messages)));write(root/'USER_NPC_REPORTS.json',report)
    seen=set();combined=[]
    for r in rows(root/'MASTER_REVIEW_QUEUE.jsonl'):
        seen.add(r['source'])
        if r['source'] in messages:
            if messages[r['source']] not in r['origins']:r['origins'].append(messages[r['source']])
            if 'user_reported_npc_priority_review' not in r['kinds']:r['kinds'].append('user_reported_npc_priority_review')
        combined.append(r)
    for source,r in messages.items():
        if source not in seen:combined.append({'source':source,'translation_ru':'','origins':[r],'kinds':['user_reported_npc_priority_review'],'status':'reported_npc_editorial_or_runtime_review_not_proven_untranslated','automatically_added_to_mod':False})
    jsonl(root/'MASTER_REVIEW_QUEUE.jsonl',sorted(combined,key=lambda r:r['source']))
    result={'reported_npcs':sorted(names),'preserved_full_related_pages':[p['title'] for p in selected],'tagged_priority_messages':len(messages),'by_npc':{n:sum(r['npc']==n for r in messages.values()) for n in sorted(names)},'master_review_unique_sources_or_patterns':len(combined),'all_reported_npcs_fully_translated':False,'no_literal_invented_for_unspecified_melody_report':True};write(root/'REPORTED_NPC_SUMMARY.json',result)
    summary=json.loads((root/'REVIEW_SUMMARY.json').read_text(encoding='utf-8'));summary['master_review_unique_sources_or_patterns']=len(combined);summary['reported_npc_priority_review']=result;write(root/'REVIEW_SUMMARY.json',summary)
    status=json.loads((root/'MASTER_REVIEW_STATUS.json').read_text(encoding='utf-8'));status['unique_sources_or_patterns']=len(combined);status['reported_npc_names']=sorted(names);write(root/'MASTER_REVIEW_STATUS.json',status)
    page=root/'SUMMARY_RU.md';old=page.read_text(encoding='utf-8').split('\n## Ryan и Melody: приоритетная проверка\n',1)[0];old=re.sub(r'\*\*[0-9,]+ уникальных строк или шаблонов\*\*',f'**{len(combined):,} уникальных строк или шаблонов**',old);old=old.replace('Текстовые вхождения из8 официальных ресурсов API','Текстовые вхождения из API и метаданных паков');old+='\n## Ryan и Melody: приоритетная проверка\n\nСохранены полные исходники связанных страниц и все опубликованные реплики обоих персонажей. Имя Melody в публикации включает значок ♫;он сохранён в исходниках,а не удалён как украшение. Для Melody пользователь сообщил о частичном переводе без точной строки;вымышленная английская реплика не добавлялась. В приоритетную очередь включены также существующие переводы,чтобы проверить оформление и частичную латиницу,а не только отсутствие ключа.\n\n[Приоритетная очередь](REPORTED_NPC_PRIORITY_QUEUE.jsonl),[полные связанные страницы](REPORTED_NPC_SOURCE_PAGES.jsonl),[сводка](REPORTED_NPC_SUMMARY.json). Эти строки также добавлены в общую очередь. Это не заявление,что все они не переведены.\n';page.write_text(old,encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
