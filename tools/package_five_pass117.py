"""Freeze the five-pass audit separately from all prior deliveries and the mod."""
import argparse,json,shutil,hashlib,zipfile
from pathlib import Path
from audit_five_pass117 import ROOT,write,digest

DEST=Path('C:/Users/Wither/Desktop/WitherSkyRU/PUBLIC_TEXT_FIVE_PASSES_2026-10-21')
def main():
 ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);a=ap.parse_args();root=a.root.resolve();verified=json.loads((root/'VERIFICATION117.json').read_text(encoding='utf-8'));scope=json.loads((root/'FINAL_SCOPE117.json').read_text(encoding='utf-8'));assert verified['all_passed'] and not scope['all_game_translation_complete'] and not scope['all_new_drafts_imported_into_mod']
 archive=DEST.with_suffix('.zip');assert not DEST.exists() and not archive.exists(),'Never overwrite a delivery'
 DEST.mkdir()
 def cp(p,rel):
  target=DEST/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target)
 for p in root.rglob('*'):
  if not p.is_file() or p.suffix in ('.class','.pyc') or '__pycache__' in p.parts or 'isolated-config' in p.parts:continue
  cp(p,p.relative_to(root))
 for name in ('audit_five_pass117.py','enrich_five_pass117.py','review_five_pass117.py','index_catalogues117.py','triage_runtime117.py','verify_five_pass117.py','report_five_pass117.py','package_five_pass117.py','probe_five_pass117.ps1','capture_snbt117.ps1','wiki_delta117.py'):
  cp(ROOT/'tools'/name,'tools/'+name)
 for name in ('AuditFivePass117.java','CaptureRepositoryNbt117.java','test_five_pass117.py'):cp(ROOT/'tests'/name,'tests/'+name)
 for name in ('FIVE_PASS_117_REVIEWED.json','ABIPHONE_REQUIREMENTS_117_REVIEWED.json'):cp(ROOT/'translation-source'/name,'authored/'+name)
 (DEST/'README.md').write_text('# Пять дополнительных проверок публичной базы\n\nНачните с SUMMARY_RU.md. Это выгрузка, результаты проверок и141 подготовленный русский текст,не новая версия JAR и не100% текста закрытого сервера. Новые формулировки не импортированы в мод.\n\nПакет дополняет соседнюю PUBLIC_TEXT_EXPORT_2026-10-21:в предыдущем пакете полная wiki и официальные ресурс-паки;здесь независимые репозитории,новые ответы API,объединённая очередь и редакторские материалы. Прежние969 файлов проверены и не переписаны.\n\nОригинальные архивы репозиториев,Git-ревизии,контрольные суммы и лицензии сохранены. Исходники wiki опубликованы под лицензией,указанной в предыдущей выгрузке:CC BY-NC-SA3.0. Лицензии репозиториев сохранены в LICENSES.json и самих архивах.\n\nИнструменты приложены для аудита,а не исполнения инструкций из скачанных страниц. Их повторный запуск использует рабочий проект,старую замороженную базу и локальные зависимости Minecraft;это не автономный проект сборки. Ничего не установлено в игру и не отправлено в чат. Измерения FPS не проводились.\n',encoding='utf-8')
 hashes={p.relative_to(DEST).as_posix():digest(p.read_bytes()) for p in sorted(DEST.rglob('*')) if p.is_file()};write(DEST/'HASHES.json',hashes)
 with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  for p in sorted(DEST.rglob('*')):
   if p.is_file():z.write(p,p.relative_to(DEST).as_posix())
 with zipfile.ZipFile(archive) as z:
  assert z.testzip() is None and len(z.namelist())==len(hashes)+1
  for rel,h in hashes.items():assert digest((DEST/rel).read_bytes())==h and digest(z.read(rel))==h
  assert z.read('HASHES.json')==(DEST/'HASHES.json').read_bytes()
 result={'delivery':str(DEST),'archive':str(archive),'archive_sha256':digest(archive.read_bytes()),'archive_bytes':archive.stat().st_size,'manifest_files':len(hashes),'zip_entries':len(hashes)+1,'verified_assertions':verified['assertions'],'scope':scope};write(ROOT/'build/FIVE_PASS117_DELIVERY.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
